"""
配置管理 - 使用 pydantic-settings 统一管理环境变量
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


class Settings(BaseSettings):
    """AI Agent 服务全局配置"""

    # ===== LLM 配置 =====
    DASHSCOPE_API_KEY: str = Field(..., description="千问 DashScope API Key")
    MODEL_NAME: str = Field(default="qwen-max", description="DashScope 模型名")
    TEMPERATURE: float = Field(default=0.7, ge=0.0, le=1.0)
    LLM_REQUEST_TIMEOUT: int = Field(default=30, description="LLM 请求超时(秒)")
    MAX_TOOL_ITERATIONS: int = Field(default=8, description="工具调用最大迭代次数")

    # ===== 后端业务 API =====
    BACKEND_BASE_URL: str = Field(
        default="http://localhost:8080",
        description="Spring Boot 后端 API 地址"
    )
    BACKEND_API_PREFIX: str = Field(default="/api/v1")
    BACKEND_REQUEST_TIMEOUT: float = Field(default=10.0)

    # ===== AI 服务自身 =====
    AI_SERVICE_PORT: int = Field(default=8081)
    AI_SERVICE_HOST: str = Field(default="0.0.0.0")

    # ===== 基础设施（可选）=====
    REDIS_URL: str | None = Field(default=None, description="Redis 连接串，None 则用内存 Checkpointer")

    # ===== 日志级别 =====
    LOG_LEVEL: str = Field(default="INFO")

    # ===== 路径配置 =====
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )


settings = Settings()
