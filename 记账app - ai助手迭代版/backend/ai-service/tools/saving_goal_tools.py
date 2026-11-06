"""
储蓄目标工具 —— Flutter LocalApiServer
GET    /api/saving-goals
POST   /api/saving-goals
PUT    /api/saving-goals/{id}/amount    body: {current_amount}    ← 存钱/取钱
DELETE /api/saving-goals/{id}

⚠️ deadline_ms 是毫秒时间戳（Flutter SQLite INTEGER 列）
"""
from __future__ import annotations

import datetime as dt
from typing import Optional

from pydantic import BaseModel, Field

from tools.base import BackendAPIClient, build_tool, format_money


def _iso_to_ms(iso: str) -> int:
    d = dt.datetime.strptime(iso, "%Y-%m-%d").replace(tzinfo=dt.timezone.utc)
    return int(d.timestamp() * 1000)


# ======================================================================
class QuerySavingGoalsInput(BaseModel):
    pass


def _query_saving_goals(user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        data = client.get("/api/saving-goals")
    except Exception as exc:
        return f"查询储蓄目标失败: {exc}"
    if not data:
        return "暂无储蓄目标。可以说「帮我创建旅游基金」。"
    lines = ["🎯 我的储蓄目标：\n"]
    for g in data:
        target = float(g.get("target_amount", 0) or 0)
        current = float(g.get("current_amount", 0) or 0)
        progress = float(g.get("progress_percent", 0) or 0)
        achieved = g.get("achieved", False)
        deadline_iso = (g.get("deadline_iso", "?") or "?")[:10]
        status = "✅ 已达成" if achieved else "🔄 进行中"
        lines.append(
            f"• {g.get('name')} {status}\n"
            f"  目标 {format_money(target)} | 已存 {format_money(current)} | 进度 {progress:.1f}%\n"
            f"  截止: {deadline_iso}"
        )
    return "\n\n".join(lines)


query_saving_goals_tool = build_tool(
    func=_query_saving_goals,
    name="query_saving_goals",
    description="""获取所有储蓄目标及进度。""",
    args_schema=QuerySavingGoalsInput,
)


# ======================================================================
class CreateSavingGoalInput(BaseModel):
    name: str
    targetAmount: float
    deadline: str = Field(description="截止日期 yyyy-MM-dd")
    description: Optional[str] = None
    categoryName: Optional[str] = None


def _create_saving_goal(
    name: str,
    targetAmount: float,
    deadline: str,
    description: Optional[str] = None,
    categoryName: Optional[str] = None,
    user_id: str = "anonymous",
) -> str:
    client = BackendAPIClient(user_id)
    body: dict = {
        "name": name,
        "target_amount": targetAmount,
        "current_amount": 0,
        "deadline_ms": _iso_to_ms(deadline),
    }
    if description: body["description"] = description
    if categoryName: body["category_name"] = categoryName
    try:
        data = client.post("/api/saving-goals", json=body)
    except Exception as exc:
        return f"创建储蓄目标失败: {exc}"
    return (
        f"🎉 储蓄目标创建成功 #{data['id']}\n"
        f"名称: {name}\n"
        f"目标: {format_money(targetAmount)}\n"
        f"截止: {deadline}\n"
        f"开始加油吧！💪"
    )


create_saving_goal_tool = build_tool(
    func=_create_saving_goal,
    name="create_saving_goal",
    description="""创建储蓄目标（旅游基金、应急金等）。
    deadline 传 yyyy-MM-dd 字符串即可（内部自动转毫秒时间戳）。""",
    args_schema=CreateSavingGoalInput,
)


# ======================================================================
class UpdateSavingGoalAmountInput(BaseModel):
    """更新储蓄目标已存金额（存钱/取钱都用这个，直接传新的 current_amount）"""
    id: int = Field(..., description="储蓄目标 ID（先查 query_saving_goals）")
    currentAmount: float = Field(..., description="更新后的已存总金额（如存了 2000 就传 2000）")


def _update_saving_goal_amount(
    id: int,
    currentAmount: float,
    user_id: str = "anonymous",
) -> str:
    client = BackendAPIClient(user_id)
    try:
        data = client.put(
            f"/api/saving-goals/{id}/amount",
            json={"current_amount": currentAmount},
        )
    except Exception as exc:
        return f"更新储蓄金额失败: {exc}"
    return (
        f"✅ 储蓄目标 #{id} 已更新\n"
        f"新已存金额: {format_money(data.get('current_amount'))}"
    )


update_saving_goal_amount_tool = build_tool(
    func=_update_saving_goal_amount,
    name="update_saving_goal_amount",
    description="""更新储蓄目标的已存金额（存钱进去 / 取钱出来）。
    直接传新的 total current_amount，不是增量。
    例：目标原来存了 3000，用户又存了 500，就传 3500。
    先 query_saving_goals 查当前金额再加起来。""",
    args_schema=UpdateSavingGoalAmountInput,
)


# ======================================================================
class DeleteSavingGoalInput(BaseModel):
    id: int = Field(..., description="要删除的储蓄目标 ID")


def _delete_saving_goal(id: int, user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        client.delete(f"/api/saving-goals/{id}")
    except Exception as exc:
        return f"删除储蓄目标失败: {exc}"
    return f"🗑️ 储蓄目标 #{id} 已删除。"


delete_saving_goal_tool = build_tool(
    func=_delete_saving_goal,
    name="delete_saving_goal",
    description="""删除一个储蓄目标。操作不可恢复。""",
    args_schema=DeleteSavingGoalInput,
)


def get_saving_goal_tools():
    return [
        query_saving_goals_tool, create_saving_goal_tool,
        update_saving_goal_amount_tool, delete_saving_goal_tool,
    ]
