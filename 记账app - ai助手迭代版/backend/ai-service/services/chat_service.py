"""
聊天业务编排服务 - 封装 LangGraph Agent 的同步/异步/流式调用

职责:
    1. 构建 Agent 图（单例，首次调用时初始化）
    2. 处理用户输入 → 调用 Agent → 返回结果
    3. 管理 LangGraph 的 thread_id（会话隔离）
    4. 错误兜底 + 日志追踪
    5. 会话历史持久化（SQLite）—— 每次对话结束后自动保存
    6. 幻觉防护：零工具调用 + 写操作意图 + 成功话术 → 强制替换回复
"""
from __future__ import annotations

import re
from typing import AsyncGenerator, Optional

from langchain_core.messages import AIMessage, HumanMessage
from langgraph.graph.state import CompiledStateGraph
from loguru import logger

from core.agent import build_agent
from core.state import AgentState
from services.context_service import ContextService
from services.session_store import get_store


# 写操作意图关键词 —— 用户请求"要做点什么"
_WRITE_INTENT_KEYWORDS = re.compile(
    r"(记账|记一笔|添加|新增|创建|保存|"
    r"设.{0,3}预算|建预算|设置预算|预算.{0,2}设|"
    r"修改|更新|改|删除|清除|移除|取消.{0,2}预算|"
    r"创建.{0,3}目标|建目标|存.{0,3}基金|存.{0,3}钱|"
    r"写一个?|记)"
)

# 成功话术关键词 —— LLM 声称"已经做了"
_SUCCESS_PHRASE_KEYWORDS = re.compile(
    r"(成功|已记|已创建|已完成|已更新|已删除|帮你记下|记下了|记好了|搞定|OK)",
    re.IGNORECASE,
)


class ChatService:
    """
    聊天业务服务

    使用示例:
        svc = ChatService(user_id="user_001")
        reply = svc.process("帮我查一下这个月花了多少")
    """

    # 模块级 Agent 单例（全局复用）
    _agent: Optional[CompiledStateGraph] = None

    def __init__(self, user_id: str, flutter_context: Optional[dict] = None):
        self.user_id = user_id
        self.flutter_context = flutter_context  # Flutter 发来的 SQLite 上下文
        self._agent = self._get_agent()

    # ------------------------------------------------------------------
    # Agent 单例管理
    # ------------------------------------------------------------------
    @classmethod
    def _get_agent(cls) -> CompiledStateGraph:
        """懒加载 + 单例"""
        if cls._agent is None:
            logger.info("首次构建 LangGraph ReAct Agent ...")
            cls._agent = build_agent()
            logger.info("Agent 构建完成")
        return cls._agent

    # ------------------------------------------------------------------
    # 会话配置
    # ------------------------------------------------------------------
    def _build_config(self, session_id: Optional[str]) -> dict:
        """
        构建 LangGraph invoke 所需的 config dict

        thread_id 决定了 Checkpointer 的 key:
            - None → 每次调用都无状态（新会话）
            - 固定值 → 多轮对话上下文被保留
        """
        thread_id = session_id or f"user_{self.user_id}_default"
        return {"configurable": {"thread_id": thread_id}}

    @staticmethod
    def _resolve_session_id(user_id: str, session_id: Optional[str]) -> str:
        """统一解析 session_id —— 保证有一个稳定可查的 ID"""
        return session_id or f"user_{user_id}_default"

    # ------------------------------------------------------------------
    # 同步调用
    # ------------------------------------------------------------------
    def process(self, user_message: str, session_id: Optional[str] = None) -> str:
        """
        同步处理单轮对话（等待 Agent 完整执行后返回）

        Args:
            user_message: 用户输入
            session_id:   会话 ID（相同 ID 保留上下文）

        Returns:
            Agent 最终自然语言回复
        """
        config = self._build_config(session_id)
        resolved_session_id = self._resolve_session_id(self.user_id, session_id)

        # 构建初始 state
        # 优先使用 Flutter 前端打包的 SQLite 上下文；没有则尝试后端自行构建（fallback）
        if self.flutter_context:
            ctx = ContextService(self.user_id, raw_context=self.flutter_context).build()
        else:
            ctx = ContextService(self.user_id).build()

        state: AgentState = {
            "messages": [HumanMessage(content=user_message)],
            "user_id": self.user_id,
            "context": ctx,
            "reasoning_chain": [],
            "tool_calls_made": [],
            "final_answer": "",
        }

        logger.info(f"[ChatService] user={self.user_id}, session={resolved_session_id}, msg={user_message[:80]}")

        # ---------- 先把 user 消息落库 ----------
        try:
            store = get_store()
            store.save_message(
                session_id=resolved_session_id,
                role="user",
                content=user_message,
                user_id=self.user_id,
            )
        except Exception as e:
            logger.warning(f"[ChatService] 保存 user 消息失败（不影响对话）: {e}")

        try:
            result = self._agent.invoke(state, config=config)
        except Exception as exc:
            logger.exception(f"Agent 执行异常: {exc}")
            error_reply = (
                f"抱歉，我在处理你的请求时遇到了问题。\n"
                f"错误信息: {type(exc).__name__}: {exc}\n"
                f"你可以稍后再试~"
            )
            self._save_ai_reply(resolved_session_id, error_reply, [])
            return error_reply

        # 从最终 messages 里提取回复（最后一条 AIMessage）
        final_messages = result.get("messages", [])
        final_answer = ""
        for msg in reversed(final_messages):
            if isinstance(msg, AIMessage) and msg.content:
                final_answer = msg.content
                break

        if not final_answer:
            final_answer = result.get("final_answer", "抱歉，我没能给出有效回复~")

        tool_calls = result.get("tool_calls_made", [])
        tool_count = len(tool_calls)

        # ---------- 检查工具返回是否全失败 ----------
        # 收集所有 ToolMessage 的 content
        from langchain_core.messages import ToolMessage as _TM
        tool_outputs = [
            m.content for m in final_messages
            if isinstance(m, _TM) and m.content
        ]
        tool_failed = any(
            ("失败" in out or "错误" in out or "❌" in out or
             "Error" in out or "400" in out or "500" in out or
             "HTTP 4" in out or "HTTP 5" in out)
            for out in tool_outputs
        )
        tool_all_failed = (
            tool_count > 0 and len(tool_outputs) > 0 and
            all(
                ("失败" in out or "错误" in out or "❌" in out or
                 "Error" in out or "400" in out or "500" in out or
                 "HTTP 4" in out or "HTTP 5" in out)
                for out in tool_outputs
            )
        )
        tool_success = any("✅" in out or "已创建" in out or "已更新" in out
                           for out in tool_outputs)
        if tool_count > 0:
            logger.info(
                f"[ChatService] 工具输出: {len(tool_outputs)} 条, "
                f"tool_success={tool_success}, tool_all_failed={tool_all_failed}"
            )

        logger.info(
            f"[ChatService] 完成: tools_called={tool_count}, "
            f"reply_len={len(final_answer)}"
        )

        # ---------- 幻觉防护硬约束 ----------
        # 检测两类幻觉：
        #   A: 零工具 + 写意图 + 成功话术 → 纯编造（旧逻辑）
        #   B: 工具全失败 + LLM 声称成功 → 虚假乐观（新增强）
        hallucination_flag = self._detect_hallucination(
            user_message=user_message,
            final_answer=final_answer,
            tool_count=tool_count,
            tool_all_failed=tool_all_failed,
            tool_success=tool_success,
            tool_failed=tool_failed,
        )
        if hallucination_flag:
            logger.warning(
                "[ChatService] 🚨 检测到幻觉，强制替换回复"
            )
            # 如果有部分工具返回，把错误信息嵌入警告中让用户知道
            failed_summary = ""
            if tool_failed and tool_outputs:
                failed_summary = "\n\n工具返回的问题：\n" + "\n".join(
                    f"  • {o[:100]}..." for o in tool_outputs[:5]
                )
            final_answer = (
                "⚠️ **检测到异常**：我刚才调用的工具遇到了问题，"
                "但回复可能不准确。\n\n"
                "请先确认实际数据是否已正确记录。如果没有成功，"
                "请重新告诉我你的需求。" + failed_summary
            )

        # ---------- 把 ai 回复落库 ----------
        self._save_ai_reply(resolved_session_id, final_answer, tool_calls)

        return final_answer

    @staticmethod
    def _detect_hallucination(
        user_message: str,
        final_answer: str,
        tool_count: int,
        tool_all_failed: bool = False,
        tool_success: bool = False,
        tool_failed: bool = False,
    ) -> bool:
        """
        幻觉检测硬约束 —— 两类幻觉：

        A. 零工具幻觉（旧逻辑）：
           tool_count == 0 + 写操作意图 + 成功话术 → 编造

        B. 工具全失败幻觉（新增强）：
           tool_all_failed + 写操作意图 + 成功话术 → 虚假乐观

        返回 True 表示检测到幻觉，需要拦截
        """
        write_intent = bool(_WRITE_INTENT_KEYWORDS.search(user_message))
        success_phrase = bool(_SUCCESS_PHRASE_KEYWORDS.search(final_answer))

        # === A. 零工具 + 写意图 + 成功话术 ===
        if tool_count == 0 and write_intent and success_phrase:
            return True

        # === B. 工具全失败 + LLM 却声称成功 ===
        if tool_all_failed and write_intent and success_phrase:
            logger.warning(
                f"[ChatService] 🚨 检测到工具全失败但 LLM 声称成功！"
                f" tool_count={tool_count}"
            )
            return True

        return False

    def _save_ai_reply(
        self,
        session_id: str,
        reply: str,
        tool_calls: list[dict],
    ) -> None:
        try:
            store = get_store()
            store.save_message(
                session_id=session_id,
                role="ai",
                content=reply,
                user_id=self.user_id,
                tool_calls=tool_calls if tool_calls else None,
            )
        except Exception as e:
            logger.warning(f"[ChatService] 保存 ai 回复失败（不影响对话）: {e}")

    # ------------------------------------------------------------------
    # 异步流式（SSE 用）
    # ------------------------------------------------------------------
    async def process_stream(
        self, user_message: str, session_id: Optional[str] = None
    ) -> AsyncGenerator[str, None]:
        """
        异步流式处理（给前端 SSE 用）

        注意:
            这里用 agent.astream_events 可拿到更细粒度事件，但为了简单先返回完整结果
            后续可增强为逐 token 流式输出
        """
        # 为简洁，先复用同步方法（Agent 内部用 InMemory 工具，性能足够）
        reply = self.process(user_message, session_id)

        # 按句子/换行分段推送（模拟流式）
        for chunk in self._split_into_chunks(reply):
            yield chunk

    @staticmethod
    def _split_into_chunks(text: str, chunk_size: int = 30) -> list[str]:
        """把长文本切小，模拟流式推送"""
        if len(text) <= chunk_size:
            return [text]
        return [text[i : i + chunk_size] for i in range(0, len(text), chunk_size)]
