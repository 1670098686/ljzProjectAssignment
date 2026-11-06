"""
预算管理工具 —— Flutter LocalApiServer
GET    /api/budgets?year=&month=
POST   /api/budgets
DELETE /api/budgets/{id}
GET    /api/budgets/alert?year=&month=
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from tools.base import BackendAPIClient, build_tool, format_money


# ======================================================================
class QueryBudgetsInput(BaseModel):
    year: Optional[int] = Field(default=None, description="年份")
    month: Optional[int] = Field(default=None, description="月份 1-12")


def _query_budgets(
    year: Optional[int] = None,
    month: Optional[int] = None,
    user_id: str = "anonymous",
) -> str:
    client = BackendAPIClient(user_id)
    params: dict = {}
    if year is not None: params["year"] = year
    if month is not None: params["month"] = month
    try:
        data = client.get("/api/budgets", params=params or None)
    except Exception as exc:
        return f"查询预算失败: {exc}"
    if not data:
        return "当前没有设置任何预算。"
    lines = ["🎯 当前预算：\n"]
    for b in data:
        spent = float(b.get("spent", 0) or 0)
        budget = float(b.get("amount", 0) or 0)
        rate = (spent / budget * 100) if budget > 0 else 0
        lines.append(
            f"• {b.get('categoryName')}: {format_money(budget)} "
            f"(已用 {format_money(spent)}, {rate:.0f}%)"
        )
    return "\n".join(lines)


query_budgets_tool = build_tool(
    func=_query_budgets,
    name="query_budgets",
    description="""查询所有预算（含 Flutter 后端实时计算的已用金额和使用率）。""",
    args_schema=QueryBudgetsInput,
)


# ======================================================================
class CreateBudgetInput(BaseModel):
    categoryName: str = Field(..., description="分类名称字符串（如 '餐饮'）")
    amount: float = Field(..., description="预算金额")
    year: int = Field(..., description="年份")
    month: int = Field(..., description="月份 1-12")
    budgetName: Optional[str] = Field(default=None, description="预算名称（可选）")


def _create_budget(
    categoryName: str,
    amount: float,
    year: int,
    month: int,
    budgetName: Optional[str] = None,
    user_id: str = "anonymous",
) -> str:
    client = BackendAPIClient(user_id)
    body = {"categoryName": categoryName, "amount": amount, "year": year, "month": month}
    if budgetName: body["budgetName"] = budgetName
    try:
        data = client.post("/api/budgets", json=body)
    except Exception as exc:
        return f"创建预算失败: {exc}"
    return (
        f"✅ 预算已创建 #{data['id']}\n"
        f"分类: {data.get('categoryName')}\n"
        f"金额: {format_money(data.get('amount'))}\n"
        f"周期: {year}年{month}月"
    )


create_budget_tool = build_tool(
    func=_create_budget,
    name="create_budget",
    description="""为某个分类创建月度预算。
    ⚠️ categoryName 用中文名称字符串（如 '餐饮'），不是 ID！""",
    args_schema=CreateBudgetInput,
)


# ======================================================================
class DeleteBudgetInput(BaseModel):
    id: int = Field(..., description="要删除的预算 ID")


def _delete_budget(id: int, user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        client.delete(f"/api/budgets/{id}")
    except Exception as exc:
        return f"删除预算失败: {exc}"
    return f"🗑️ 预算 #{id} 已删除。"


delete_budget_tool = build_tool(
    func=_delete_budget,
    name="delete_budget",
    description="""删除一个预算。操作不可恢复。
    当用户说「删掉那个预算」「把第 3 条预算删了」时调用。""",
    args_schema=DeleteBudgetInput,
)


# ======================================================================
class CheckBudgetAlertInput(BaseModel):
    pass


def _check_budget_alerts(user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        data = client.get("/api/budgets/alert")
    except Exception as exc:
        return f"查询预算预警失败: {exc}"
    if not data:
        return "📭 预算预警接口返回空（可能没有设置预算）。"
    lines = ["📊 预算使用率一览：\n"]
    for alert in data:
        cat = alert.get("categoryName", "未知")
        spent = float(alert.get("spent", 0) or 0)
        total = float(alert.get("amount", 0) or 0)
        rate = float(alert.get("usageRate", 0) or 0) * 100
        level = alert.get("alertLevel", "")
        emoji = {"OVERSPENT": "🔴", "WARNING": "🟡", "SAFE": "🟢"}.get(level, "⚪")
        status = {"OVERSPENT": "超支！", "WARNING": "接近上限", "SAFE": "安全"}.get(level, "")
        lines.append(
            f"{emoji} {cat}: {rate:.0f}% "
            f"({status}) 已用 {format_money(spent)} / {format_money(total)}"
        )
    return "\n".join(lines)


check_budget_alerts_tool = build_tool(
    func=_check_budget_alerts,
    name="check_budget_alerts",
    description="""实时计算所有预算的使用率。Flutter 后端根据 bills 表动态聚合 spent。
    当用户问「我预算超了吗」「哪个分类花得最多」时调用。""",
    args_schema=CheckBudgetAlertInput,
)


def get_budget_tools():
    return [
        query_budgets_tool, create_budget_tool, delete_budget_tool,
        check_budget_alerts_tool,
    ]
