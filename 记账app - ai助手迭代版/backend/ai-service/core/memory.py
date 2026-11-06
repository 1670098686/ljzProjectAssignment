"""
会话记忆管理 - LangGraph Checkpointer
"""
from __future__ import annotations

from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.memory import InMemorySaver
from loguru import logger

from config.settings import settings


def create_checkpointer() -> BaseCheckpointSaver:
    """
    创建 LangGraph 会话状态持久化 Checkpointer

    策略:
        - 若配置了 REDIS_URL → 使用 Redis Checkpointer（生产推荐）
        - 否则 → 使用 InMemorySaver（开发/单实例）

    注意:
        这里不强制依赖 langgraph-checkpoint-redis，按需加载避免 pip 冲突
    """
    if settings.REDIS_URL:
        try:
            from langgraph.checkpoint.redis import RedisSaver

            logger.info(f"使用 Redis Checkpointer: {settings.REDIS_URL}")
            return RedisSaver(redis_url=settings.REDIS_URL)
        except ImportError:
            logger.warning(
                "未安装 langgraph-checkpoint-redis，降级到 InMemorySaver。"
                "如需 Redis: pip install langgraph-checkpoint-redis"
            )

    logger.info("使用 InMemorySaver（开发模式）")
    return InMemorySaver()
