"""
Pydantic 请求/响应 Schema
"""
from pydantic import BaseModel, Field
from typing import Optional


# ======================================================================
# 聊天接口
# ======================================================================
class ChatRequest(BaseModel):
    """用户聊天请求"""
    user_id: str = Field(
        ...,
        description="用户 ID（后端鉴权后透传）",
        example="user_001",
    )
    message: str = Field(
        ...,
        min_length=1,
        max_length=2000,
        description="用户输入的自然语言消息",
        example="帮我查一下这个月花了多少",
    )
    session_id: str | None = Field(
        default=None,
        description="会话 ID，相同 ID 会保留多轮上下文；不传则每次都是新会话",
        example="session_abc123",
    )
    context: Optional[dict] = Field(
        default=None,
        description="Flutter 前端打包的 SQLite 上下文快照（可选，不传则后端自行构建）",
    )


class ChatResponse(BaseModel):
    """AI 回复"""
    code: int = Field(default=200, description="响应码")
    message: str = Field(default="OK", description="状态描述")
    data: "ChatData"


class ChatData(BaseModel):
    """回复主体"""
    reply: str = Field(..., description="AI 的自然语言回复")
    session_id: str | None = Field(default=None, description="当前会话 ID")


class SimpleChatResponse(BaseModel):
    """简化版（直接返回回复文本）"""
    reply: str


# ======================================================================
# 会话历史
# ======================================================================
class SessionSummary(BaseModel):
    """会话列表项（轻量，用于侧边栏展示）"""
    id: str = Field(..., description="session_id，同时也是 LangGraph thread_id")
    title: str = Field(default="", description="会话标题（自动从首条用户消息截取）")
    last_message: str = Field(default="", description="最后一条消息摘要")
    created_at: str = Field(..., description="ISO 时间戳，创建时间")
    updated_at: str = Field(..., description="ISO 时间戳，最后活跃时间")
    message_count: int = Field(default=0, description="消息数量（可选扩展）")


class SessionListResponse(BaseModel):
    """列出用户所有会话"""
    sessions: list[SessionSummary] = Field(default_factory=list)
    total: int = Field(default=0)


class ChatMessageItem(BaseModel):
    """单条消息"""
    role: str = Field(..., description="'user' 或 'ai'")
    content: str = Field(...)
    created_at: str = Field(...)
    tool_calls: Optional[list[dict]] = Field(default=None, description="AI 的工具调用链（如该条消息是 AI）")


class SessionDetailResponse(BaseModel):
    """某个会话的完整消息历史"""
    session_id: str
    title: str = ""
    messages: list[ChatMessageItem] = Field(default_factory=list)
    total: int = Field(default=0)


class DeleteSessionResponse(BaseModel):
    ok: bool = True
    message: str = ""


# ======================================================================
# 健康检查
# ======================================================================
class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str = Field(description="服务状态")
    version: str = Field(default="1.0.0")
    components: dict[str, str] = Field(default_factory=dict, description="各组件健康状态")
