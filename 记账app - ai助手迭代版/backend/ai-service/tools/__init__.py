"""
工具层入口 —— 全部业务工具汇总

工具清单（17 个）:
    [智能组合]  smart_create_bill, batch_create_bills     ← 自动补分类+记账+备注，首选！
    [交易类]    query_transactions, create_transaction,
                update_transaction, delete_transaction
    [分类类]    list_categories, create_category, delete_category
    [预算类]    query_budgets, create_budget, delete_budget, check_budget_alerts
    [储蓄类]    query_saving_goals, create_saving_goal,
                update_saving_goal_amount, delete_saving_goal
    [统计类]    get_monthly_summary, get_category_stats, get_trend
"""
from __future__ import annotations

from langchain_core.tools import BaseTool

from tools.transaction_tools import get_transaction_tools
from tools.category_tools import get_category_tools
from tools.budget_tools import get_budget_tools
from tools.saving_goal_tools import get_saving_goal_tools
from tools.statistics_tools import get_statistics_tools
from tools.smart_bill_tools import get_smart_bill_tools


def get_all_tools() -> list[BaseTool]:
    """获取全部业务工具，供 LangGraph ReAct Agent 加载"""
    tools: list[BaseTool] = []
    tools.extend(get_smart_bill_tools())      # 智能组合放最前，LLM 优先看到
    tools.extend(get_transaction_tools())
    tools.extend(get_category_tools())
    tools.extend(get_budget_tools())
    tools.extend(get_saving_goal_tools())
    tools.extend(get_statistics_tools())
    return tools


__all__ = ["get_all_tools"]
