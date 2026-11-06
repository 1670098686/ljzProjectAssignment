"""
ReAct Agent 核心 - 基于 LangGraph 的状态图实现

节点流程:
    agent_node (LLM 思考+选择工具)
        → 有 tool_calls? → tool_node (执行工具) → agent_node (循环)
        → 无 tool_calls? → END (输出最终答案)

关键经验修复（来自 1438138）:
    1. StructuredTool 必须用 .invoke(args_dict)，不能 tool(args) 直接调用
    2. ToolMessage 的 tool_call_id 必须与 AIMessage.tool_calls[].id 配对
"""
from __future__ import annotations

from typing import Any, Literal

from langchain_core.messages import AIMessage, SystemMessage, ToolMessage
from langchain_core.tools import BaseTool
from langgraph.graph import END, StateGraph
from langgraph.graph.state import CompiledStateGraph
from loguru import logger

from config.settings import settings
from core.llm import create_llm
from core.memory import create_checkpointer
from core.prompts.system_prompt import SYSTEM_PROMPT
from core.state import AgentState, RouteDecision


# ======================================================================
# 1. Agent Node - LLM 思考节点
# ======================================================================
def _agent_node_builder(system_prompt: str, max_iterations: int):
    """
    闭包工厂：让 agent_node 捕获 system_prompt，同时读取本轮迭代次数防止无限循环
    """

    def agent_node(state: AgentState) -> dict[str, Any]:
        """
        LLM 思考节点：
          - 注入 SystemPrompt
          - 调用 LLM.bind_tools(tools)
          - 若返回 tool_calls → 交给 tool_node 执行
          - 否则 → 最终回答
        """
        from core.llm import get_llm

        llm = get_llm()

        # 防死循环：如果已达到最大迭代次数，强制结束
        current_iterations = len(state.get("tool_calls_made", []))
        if current_iterations >= max_iterations:
            logger.warning(
                f"达到最大工具调用次数 {max_iterations}，强制结束 Agent 循环"
            )
            messages_list = state.get("messages", [])
            last_msg = messages_list[-1] if messages_list else None
            return {
                "messages": [],  # 不新增消息
                "final_answer": (
                    last_msg.content
                    if last_msg and isinstance(last_msg, AIMessage) and last_msg.content
                    else "抱歉，我在处理过程中遇到了困难，请换个方式问我~"
                ),
            }

        # 组装消息：SystemPrompt + Context快照 + History
        import json as _json
        from datetime import datetime as _dt
        from langchain_core.messages import SystemMessage as _SysMsg
        
        context = state.get("context", {}) or {}
        context_hint_parts = []
        
        # ===== 关键！注入当前日期，防止 LLM 编造日期 =====
        now = _dt.now()
        weekday_map = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
        date_hint = (
            f"### 📅 当前时间（必须用这个日期，不要编造！）:\n"
            f"今天是 {now.strftime('%Y-%m-%d')} ({weekday_map[now.weekday()]})\n"
            f"本月是 {now.year} 年 {now.month} 月"
        )
        context_hint_parts.append(date_hint)
        
        if context.get("categories"):
            cats_text = "\n".join(
                f"  - {c['name']} (type={c.get('type')})"
                for c in context["categories"]
            )
            context_hint_parts.append(
                f"### 📂 当前可用分类（记账时优先用已有分类名）:\n{cats_text}\n\n"
                f"**实体→分类映射（用户说这些词时，请映射到对应分类）:**\n"
                f"  午饭/早餐/晚饭/夜宵/餐厅/吃饭 → 餐饮\n"
                f"  地铁/公交/打车/出行/加油 → 交通\n"
                f"  房租/水电网/物业 → 住房\n"
                f"  工资/薪水 → 工资\n"
                f"  购物/淘宝/京东 → 购物\n"
                f"  娱乐/电影/游戏 → 娱乐\n"
                f"  医疗/看病/药品 → 医疗\n"
                f"  红包/压岁钱 → 红包\n"
                f"  其他没列的可以直接用 smart_create_bill 自动创建"
            )
        
        if context.get("current_budgets"):
            buds_text = "\n".join(
                f"  - {b.get('categoryName')}: budget={b.get('budget')}, used={b.get('used')}"
                for b in context["current_budgets"]
            )
            context_hint_parts.append(f"### 💰 当前月预算:\n{buds_text}")
        
        context_injection = "\n\n## 📋 用户上下文快照（仅供参考，必须遵守）\n\n" + "\n\n".join(context_hint_parts)
        
        messages = [
            _SysMsg(content=system_prompt + context_injection),
            *state.get("messages", []),
        ]

        # 调用 LLM（带工具绑定）
        llm_with_tools = llm.bind_tools(_tools_registry)
        response: AIMessage = llm_with_tools.invoke(messages)

        # 提取思考链内容（通义千问通过 additional_kwargs 返回 thinking_content）
        reasoning = response.additional_kwargs.get("reasoning_content", "")
        new_reasoning = state.get("reasoning_chain", [])
        if reasoning:
            new_reasoning = new_reasoning + [reasoning]

        return {
            "messages": [response],
            "reasoning_chain": new_reasoning,
        }

    return agent_node


# ======================================================================
# 2. Tool Node - 工具执行节点
# ======================================================================
def _tool_node(state: AgentState) -> dict[str, Any]:
    """
    工具执行节点：
      - 解析 AIMessage.tool_calls 列表
      - 按序调用对应工具
      - 每个结果封装成 ToolMessage，tool_call_id 必须与 AIMessage.tool_calls[].id 配对
    """
    from core.llm import get_llm  # noqa: F401  # 保持依赖显式

    # 取最后一条 AIMessage
    messages_list = state.get("messages", [])
    if not messages_list:
        logger.warning("tool_node 被调用但 messages 列表为空")
        return {"messages": []}
    last_message = messages_list[-1]

    if not hasattr(last_message, "tool_calls") or not last_message.tool_calls:
        logger.warning("tool_node 被调用但最后一条消息没有 tool_calls")
        return {"messages": []}

    tool_calls = last_message.tool_calls
    logger.debug(f"本轮调用 {len(tool_calls)} 个工具: {[tc['name'] for tc in tool_calls]}")

    tool_results: list[ToolMessage] = []
    executed_calls: list[dict] = []

    for tc in tool_calls:
        tool_name = tc["name"]
        tool_args = tc.get("args", {})
        tool_call_id = tc["id"]  # 关键：这个 id 必须透传到 ToolMessage.tool_call_id

        tool = _tool_map.get(tool_name)
        if tool is None:
            logger.error(f"找不到工具: {tool_name}")
            tool_results.append(
                ToolMessage(
                    content=f"错误: 工具 '{tool_name}' 未注册",
                    tool_call_id=tool_call_id,
                )
            )
            continue

        # 注入 user_id 到工具参数（用户隔离）
        tool_args.setdefault("user_id", state.get("user_id", "anonymous"))

        try:
            # StructuredTool 必须用 .invoke() 调用，不能直接 tool(args_dict)
            # 经验 1438138: 否则报 TypeError: 'StructuredTool' object is not callable
            result = tool.invoke(tool_args)
            logger.debug(f"  工具 {tool_name} 执行成功, 返回长度={len(str(result))}")
        except Exception as exc:
            logger.exception(f"  工具 {tool_name} 执行失败: {exc}")
            result = f"工具执行失败: {type(exc).__name__}: {exc}"

        tool_results.append(
            ToolMessage(
                content=str(result),
                tool_call_id=tool_call_id,  # 必须和 AIMessage.tool_calls[].id 配对
                name=tool_name,
            )
        )
        executed_calls.append(tc)

    return {
        "messages": tool_results,
        "tool_calls_made": state.get("tool_calls_made", []) + executed_calls,
    }


# ======================================================================
# 3. 路由判断
# ======================================================================
def _should_continue(state: AgentState) -> RouteDecision:
    """
    判断是否继续循环:
        - AIMessage 带有 tool_calls → "tool_node"
        - 否则 → "end"
    """
    messages_list = state.get("messages", [])
    if not messages_list:
        return "end"
    last_message = messages_list[-1]
    if (
        isinstance(last_message, AIMessage)
        and getattr(last_message, "tool_calls", None)
    ):
        return "tool_node"
    return "end"


# ======================================================================
# 4. 构建 Agent 图（对外主入口）
# ======================================================================
# 模块级变量：由 build_agent() 初始化后被节点闭包引用
_tools_registry: list[BaseTool] = []
_tool_map: dict[str, BaseTool] = {}


def create_agent_graph(
    tools: list[BaseTool],
    system_prompt: str | None = None,
    max_iterations: int | None = None,
    llm_model: str | None = None,
) -> CompiledStateGraph:
    """
    构建并编译 LangGraph ReAct Agent

    Args:
        tools:          工具列表（LangChain BaseTool）
        system_prompt:  系统提示词，默认用 SYSTEM_PROMPT
        max_iterations: 最大工具调用迭代次数，默认读 settings
        llm_model:      指定 LLM 模型名（主要用于测试时 mock）

    Returns:
        已编译的 CompiledStateGraph，可直接 .invoke() / .ainvoke()
    """
    # 注入全局工具注册表（供 tool_node / agent_node 闭包使用）
    global _tools_registry, _tool_map
    _tools_registry = tools
    _tool_map = {t.name: t for t in tools}

    prompt = system_prompt or SYSTEM_PROMPT
    iterations = max_iterations or settings.MAX_TOOL_ITERATIONS

    # 初始化 LLM（默认 get_llm() 单例）
    if llm_model is not None:
        create_llm(model_name=llm_model)

    # 构建状态图
    workflow = StateGraph(AgentState)

    agent_node_fn = _agent_node_builder(prompt, iterations)

    workflow.add_node("agent", agent_node_fn)
    workflow.add_node("tools", _tool_node)

    workflow.set_entry_point("agent")

    # LangGraph 0.2.x API: add_conditional_edges(source, path, path_map)
    # - source: 源节点名
    # - path:   路由函数（接收 state，返回目标节点 key）
    # - path_map: 路由返回值 → 目标节点名的映射
    workflow.add_conditional_edges(
        "agent",
        _should_continue,
        {
            "tool_node": "tools",
            "end": END,
        },
    )
    workflow.add_edge("tools", "agent")

    # 编译 + Checkpointer（支持多轮会话）
    checkpointer = create_checkpointer()
    compiled = workflow.compile(
        checkpointer=checkpointer,
        name="FinanceReActAgent",
    )

    logger.info(
        f"LangGraph ReAct Agent 构建完成: "
        f"tools={len(tools)}, max_iterations={iterations}"
    )
    return compiled


def build_agent() -> CompiledStateGraph:
    """
    快捷入口：使用默认配置构建 Agent（加载所有业务工具）
    """
    from tools import get_all_tools

    tools = get_all_tools()
    return create_agent_graph(tools=tools)
