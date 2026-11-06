"""
AgentState 状态定义 - LangGraph 状态图 TypedDict
"""
from __future__ import annotations

from typing import Annotated, Any, Literal, TypedDict

from langgraph.graph.message import add_messages
from langchain_core.messages import BaseMessage


class AgentState(TypedDict, total=False):
    """
    LangGraph ReAct Agent 的完整状态

    字段说明:
        messages:         对话消息历史，LangGraph 自动 append-only（Annotated[..., add_messages]）
        user_id:          当前用户 ID（鉴权/数据隔离）
        context:          可选的用户上下文（预算、分类等快照，减少工具调用）
        reasoning_chain:  LLM 的思考链（thinking_content 提取）
        tool_calls_made:  本轮已执行的工具调用清单（用于调试/审计）
        final_answer:     Agent 最终输出（END 节点前赋值）
    """
    messages: Annotated[list[BaseMessage], add_messages]
    user_id: str
    context: dict[str, Any]
    reasoning_chain: list[str]
    tool_calls_made: list[dict]
    final_answer: str


# 分支路由返回值类型
RouteDecision = Literal["tool_node", "end"]
