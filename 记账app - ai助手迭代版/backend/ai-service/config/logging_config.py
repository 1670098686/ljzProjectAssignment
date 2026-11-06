"""
日志配置 - 基于 loguru
"""
import sys
from loguru import logger
from .settings import settings


def setup_logging() -> None:
    """初始化日志配置"""
    logger.remove()

    # 控制台输出
    logger.add(
        sys.stdout,
        level=settings.LOG_LEVEL,
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | "
               "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
        colorize=True,
    )

    # 文件输出（按天轮转）
    logger.add(
        "logs/ai-agent_{time:YYYY-MM-DD}.log",
        level="DEBUG",
        format="{time:YYYY-MM-DD HH:mm:ss.SSS} | {level: <8} | {name}:{function}:{line} - {message}",
        rotation="00:00",
        retention="7 days",
        encoding="utf-8",
    )
