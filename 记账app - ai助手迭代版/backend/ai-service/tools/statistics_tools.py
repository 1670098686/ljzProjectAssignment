"""
统计分析工具 —— Flutter LocalApiServer
GET /api/statistics/summary?year=&month=
GET /api/statistics/by-category?startDate=&endDate=&type=
GET /api/statistics/trend?startDate=&endDate=
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from tools.base import BackendAPIClient, build_tool, format_money


# ======================================================================
class MonthlySummaryInput(BaseModel):
    year: int
    month: int


def _get_monthly_summary(year: int, month: int, user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        data = client.get(
            "/api/statistics/summary",
            params={"year": year, "month": month},
        )
    except Exception as exc:
        return f"获取月度汇总失败: {exc}"
    return (
        f"📊 {year}年{month}月 财务汇总\n"
        f"────────────────\n"
        f"💰 总收入: {format_money(data.get('totalIncome'))}\n"
        f"💸 总支出: {format_money(data.get('totalExpense'))}\n"
        f"💵 结余:   {format_money(data.get('balance'))}"
    )


get_monthly_summary_tool = build_tool(
    func=_get_monthly_summary,
    name="get_monthly_summary",
    description="""获取指定月份的财务汇总（收入/支出/结余）。
    当用户问「这个月赚了多少」「上个月收支情况」时调用。""",
    args_schema=MonthlySummaryInput,
)


# ======================================================================
class CategoryStatsInput(BaseModel):
    startDate: str
    endDate: str
    type: Optional[int] = Field(default=None, description="1=收入, 2=支出, 不传=收支都返回")


def _get_category_stats(
    startDate: str,
    endDate: str,
    type: Optional[int] = None,
    user_id: str = "anonymous",
) -> str:
    client = BackendAPIClient(user_id)
    params: dict = {"startDate": startDate, "endDate": endDate}
    if type is not None: params["type"] = type
    try:
        data = client.get("/api/statistics/by-category", params=params)
    except Exception as exc:
        return f"获取分类统计失败: {exc}"
    if not data:
        return f"{startDate} ~ {endDate} 无分类数据。"
    data_sorted = sorted(data, key=lambda x: float(x.get("amount", 0) or 0), reverse=True)
    lines = [f"📋 {startDate} ~ {endDate} 分类统计\n"]
    for i, c in enumerate(data_sorted, 1):
        amount = float(c.get("amount", 0) or 0)
        pct = float(c.get("percentage", 0) or 0)
        lines.append(f"{i}. {c.get('categoryName')} {format_money(amount)}  {pct:.1f}%")
    return "\n".join(lines)


get_category_stats_tool = build_tool(
    func=_get_category_stats,
    name="get_category_stats",
    description="""按分类统计金额和占比。Flutter 后端本地计算。
    当用户问「哪些类别花得最多」「餐饮花了多少」时调用。""",
    args_schema=CategoryStatsInput,
)


# ======================================================================
class TrendAnalysisInput(BaseModel):
    startDate: str
    endDate: str


def _get_trend(startDate: str, endDate: str, user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        data = client.get(
            "/api/statistics/trend",
            params={"startDate": startDate, "endDate": endDate},
        )
    except Exception as exc:
        return f"获取趋势数据失败: {exc}"
    if not data:
        return f"{startDate} ~ {endDate} 无趋势数据。"
    lines = [f"📈 {startDate} ~ {endDate} 收支趋势\n"]
    for point in data:
        lines.append(
            f"  {point.get('date')}: "
            f"收入 {format_money(point.get('income', 0))}  "
            f"支出 {format_money(point.get('expense', 0))}"
        )
    return "\n".join(lines)


get_trend_tool = build_tool(
    func=_get_trend,
    name="get_trend",
    description="""收支趋势分析（按日）。Flutter 后端本地计算。
    当用户问「最近消费趋势」「上半月 vs 下半月」时调用。""",
    args_schema=TrendAnalysisInput,
)


def get_statistics_tools():
    return [get_monthly_summary_tool, get_category_stats_tool, get_trend_tool]
