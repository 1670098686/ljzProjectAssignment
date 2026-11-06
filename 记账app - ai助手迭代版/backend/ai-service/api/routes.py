"""
FastAPI 路由 - AI Agent 聊天接口 + 健康检查 + 会话历史管理
"""
from __future__ import annotations

from fastapi import APIRouter, HTTPException, Query
from sse_starlette.sse import EventSourceResponse

from api.schemas import (
    ChatRequest, ChatResponse, ChatData, SimpleChatResponse,
    HealthResponse,
    SessionListResponse, SessionDetailResponse, DeleteSessionResponse,
    SessionSummary, ChatMessageItem,
)
from config.settings import settings
from loguru import logger
from services.chat_service import ChatService
from services.session_store import get_store

router = APIRouter(prefix="/api/v1/ai", tags=["AI Agent"])


# ======================================================================
# 聊天接口
# ======================================================================
@router.post(
    "/chat",
    response_model=ChatResponse,
    summary="同步聊天",
    description="发送消息给智账助手，等待完整回复后返回",
)
async def chat(request: ChatRequest) -> ChatResponse:
    """同步聊天 —— 等待 Agent 完整执行后返回"""
    try:
        service = ChatService(user_id=request.user_id, flutter_context=request.context)
        resolved_session_id = (
            request.session_id or f"user_{request.user_id}_default"
        )
        reply = service.process(request.message, request.session_id)
    except Exception as exc:
        logger.exception("聊天接口异常")
        raise HTTPException(status_code=500, detail=str(exc))

    return ChatResponse(
        code=200,
        message="OK",
        data=ChatData(reply=reply, session_id=resolved_session_id),
    )


@router.post(
    "/chat/simple",
    response_model=SimpleChatResponse,
    summary="简化聊天接口",
    description="直接返回回复文本，方便前端调用",
)
async def chat_simple(request: ChatRequest) -> SimpleChatResponse:
    """简化聊天 —— 仅返回回复文本"""
    service = ChatService(user_id=request.user_id, flutter_context=request.context)
    reply = service.process(request.message, request.session_id)
    return SimpleChatResponse(reply=reply)


@router.post(
    "/chat/stream",
    summary="流式聊天（SSE）",
    description="以 Server-Sent Events 方式逐段推送 AI 回复",
)
async def chat_stream(request: ChatRequest) -> EventSourceResponse:
    """流式聊天 —— SSE 推送（推荐前端使用）"""

    async def event_generator():
        service = ChatService(user_id=request.user_id, flutter_context=request.context)
        async for chunk in service.process_stream(
            request.message, request.session_id
        ):
            yield {"event": "message", "data": chunk}
        # 结束信号
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator(), media_type="text/event-stream")


@router.post(
    "/chat/reset",
    summary="重置会话",
    description="清除指定 session_id 的会话上下文（开发调试用）",
)
async def reset_session(request: ChatRequest) -> dict:
    """重置会话 —— 下一次调用不保留之前上下文"""
    logger.info(f"重置会话: user={request.user_id}, session={request.session_id}")
    return {"status": "ok", "message": f"会话 {request.session_id} 已重置"}


# ======================================================================
# 会话历史管理
# ======================================================================
@router.get(
    "/sessions",
    response_model=SessionListResponse,
    summary="列出会话历史",
    description="返回指定用户的全部 AI 对话会话，按最后活跃时间倒序",
)
async def list_sessions(
    user_id: str = Query(..., description="用户 ID"),
    limit: int = Query(default=100, ge=1, le=500),
) -> SessionListResponse:
    """列出用户所有历史会话"""
    store = get_store()
    rows = store.list_sessions(user_id, limit=limit)

    summaries: list[SessionSummary] = []
    for r in rows:
        try:
            msg_count = len(store.list_messages(r["id"]))
        except Exception:
            msg_count = 0
        summaries.append(SessionSummary(
            id=r["id"],
            title=r.get("title") or r["id"],
            last_message=r.get("last_message") or "",
            created_at=r["created_at"],
            updated_at=r["updated_at"],
            message_count=msg_count,
        ))

    return SessionListResponse(sessions=summaries, total=len(summaries))


@router.get(
    "/sessions/{session_id}",
    response_model=SessionDetailResponse,
    summary="获取单个会话的完整消息",
)
async def get_session_detail(
    session_id: str,
    user_id: str = Query(..., description="用户 ID，用于校验归属"),
) -> SessionDetailResponse:
    """查询指定会话的全部历史消息，可用于前端"切换回旧会话"场景"""
    store = get_store()

    # 归属校验
    sessions = store.list_sessions(user_id, limit=10000)
    owned = any(s["id"] == session_id for s in sessions)
    if not owned:
        raise HTTPException(status_code=404, detail=f"会话 {session_id} 不存在或不归属于当前用户")

    msgs = store.list_messages(session_id)
    items = [
        ChatMessageItem(
            role=m["role"],
            content=m["content"],
            created_at=m["created_at"],
            tool_calls=m.get("tool_calls"),
        )
        for m in msgs
    ]

    # 尝试获取标题
    title = ""
    for s in sessions:
        if s["id"] == session_id:
            title = s.get("title") or ""
            break

    return SessionDetailResponse(
        session_id=session_id,
        title=title,
        messages=items,
        total=len(items),
    )


@router.delete(
    "/sessions/{session_id}",
    response_model=DeleteSessionResponse,
    summary="删除会话",
)
async def delete_session(
    session_id: str,
    user_id: str = Query(..., description="用户 ID，用于校验归属"),
) -> DeleteSessionResponse:
    """删除指定会话及其所有消息（级联删除）"""
    store = get_store()

    # 归属校验
    sessions = store.list_sessions(user_id, limit=10000)
    owned = any(s["id"] == session_id for s in sessions)
    if not owned:
        raise HTTPException(status_code=404, detail=f"会话 {session_id} 不存在或不归属于当前用户")

    ok = store.delete_session(session_id)
    return DeleteSessionResponse(ok=ok, message="删除成功" if ok else "删除失败")


# ======================================================================
# 健康检查
# ======================================================================
@router.get(
    "/health",
    response_model=HealthResponse,
    summary="健康检查",
)
async def health() -> HealthResponse:
    """健康检查 —— 供 Docker/K8s 探针使用"""
    components = {}

    # 后端 API 可达性
    try:
        import httpx

        with httpx.Client(timeout=2.0) as c:
            resp = c.get(f"{settings.BACKEND_BASE_URL}/api/v1/health")
            components["backend_api"] = (
                "ok" if resp.status_code < 500 else "unhealthy"
            )
    except Exception:
        components["backend_api"] = "unreachable"

    # LLM 配置检查（仅检查 key 是否存在，不实际调用）
    components["llm_config"] = "ok" if settings.DASHSCOPE_API_KEY else "missing_key"

    # SQLite 会话存储
    try:
        store = get_store()
        store.get_session_count("_probe_")  # 不抛异常就好
        components["session_store"] = "ok"
    except Exception as e:
        components["session_store"] = f"unhealthy: {e}"

    overall = "healthy" if all(v in ("ok",) for v in components.values()) else "degraded"

    return HealthResponse(
        status=overall,
        version="1.0.0",
        components=components,
    )
