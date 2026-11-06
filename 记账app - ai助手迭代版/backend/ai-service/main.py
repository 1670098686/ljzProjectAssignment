"""
AI Agent 服务入口 - FastAPI App 定义 + 生命周期钩子
"""
from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from api.routes import router
from config.settings import settings
from config.logging_config import setup_logging


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 生命周期: 启动时预加载 Agent，关闭时清理
    """
    # ======= 启动 =======
    setup_logging()
    logger.info("=" * 60)
    logger.info("🚀  记账 App AI Agent Service 启动中 ...")
    logger.info(f"    Model:      {settings.MODEL_NAME}")
    logger.info(f"    Backend:    {settings.BACKEND_BASE_URL}")
    logger.info(f"    Port:       {settings.AI_SERVICE_PORT}")
    logger.info("=" * 60)

    # 1. 预初始化 SQLite 会话存储（建表 + WAL 模式）
    try:
        from services.session_store import get_store
        get_store()
        logger.info("✅ SQLite 会话存储初始化完成")
    except Exception as exc:
        logger.error(f"❌ SQLite 会话存储初始化失败: {exc}")

    # 2. 预构建 Agent（首次请求时也会构建，但启动时预加载可降低首个请求延迟）
    try:
        from services.chat_service import ChatService
        ChatService._get_agent()
        logger.info("✅ LangGraph ReAct Agent 预加载完成")
    except Exception as exc:
        logger.error(f"❌ Agent 预加载失败: {exc}")
        # 不阻塞启动，首次请求时会再次尝试

    yield

    # ======= 关闭 =======
    logger.info("👋 AI Agent Service 关闭")


# 创建 FastAPI App
app = FastAPI(
    title="记账 App — 智账 AI Agent 服务",
    description=(
        "基于 LangChain + LangGraph 的 ReAct 智能 Agent，"
        "为记账 App 用户提供自然语言记账、财务分析、预算管理等能力。"
    ),
    version="1.0.0",
    lifespan=lifespan,
)

# CORS 中间件（Flutter 前端 web/调试用）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(router)


# ----------------------------------------------------------------------
# 根路径 —— 友好欢迎语
# ----------------------------------------------------------------------
@app.get("/")
async def root() -> dict:
    """根路径欢迎"""
    return {
        "service": "Finance AI Agent",
        "status": "running",
        "version": "1.0.0",
        "endpoints": {
            "chat_sync": "/api/v1/ai/chat",
            "chat_simple": "/api/v1/ai/chat/simple",
            "chat_stream": "/api/v1/ai/chat/stream",
            "health": "/api/v1/ai/health",
        },
    }


# ----------------------------------------------------------------------
# 直接运行入口
# ----------------------------------------------------------------------
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host=settings.AI_SERVICE_HOST,
        port=settings.AI_SERVICE_PORT,
        reload=True,
        log_level=settings.LOG_LEVEL.lower(),
    )
