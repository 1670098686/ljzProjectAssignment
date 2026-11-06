"""
用户上下文构建 - 把 Flutter 前端发来的 SQLite JSON 映射成 Agent 期望的结构

架构说明:
    Flutter 前端 → 本地 SQLite → 打包完整 JSON → HTTP POST → Python AI Agent
    Python Agent 不再需要调 Spring Boot（已废弃），直接消费 Flutter 发来的 JSON。

字段映射:
    Flutter budgets:   {category, monthly_budget, spent, year, month}
    Agent 期望:        {categoryName, budget, used}

    Flutter bills:     {type, category, amount, date, remark}
    Agent 期望:        直接在 context 里，agent_node 注入 SystemPrompt 时会用

    Flutter savings_goals: {name, target, current, deadline}
    Agent 期望:             直接在 context 里
"""
from __future__ import annotations

from loguru import logger


class ContextService:
    """构建用户上下文快照 —— 优先消费 Flutter 发来的 JSON"""

    def __init__(self, user_id: str, raw_context: dict | None = None):
        self.user_id = user_id
        self.raw = raw_context or {}

    def build(self) -> dict:
        """
        构建 Agent 期望的 context dict:
            - categories       分类列表（agent_node 会用它做实体映射）
            - current_budgets  当月预算（agent_node 用它回答预算相关问题）
            - bills            交易明细（完整给 LLM 看）
            - savings_goals    储蓄目标（完整给 LLM 看）
            - summary          汇总数据
        """
        ctx: dict = {}
        raw = self.raw

        # 1. categories → 原样传入，agent_node 里读 c['name'] 和 c.get('type')
        categories = raw.get("categories") or []
        if categories:
            ctx["categories"] = [
                {"name": c.get("name", ""), "type": c.get("type")}
                for c in categories
            ]
            logger.debug(f"[ContextService] 映射 categories: {len(ctx['categories'])} 条")

        # 2. budgets → 字段重命名: category/categoryName, monthly_budget/budget, spent/used
        budgets = raw.get("budgets") or []
        if budgets:
            ctx["current_budgets"] = [
                {
                    "categoryName": b.get("category") or b.get("categoryName", ""),
                    "budget": b.get("monthly_budget") or b.get("budget", 0),
                    "used": b.get("spent") or b.get("used", 0),
                }
                for b in budgets
            ]
            logger.debug(f"[ContextService] 映射 budgets: {len(ctx['current_budgets'])} 条")

        # 3. bills → 直接透传，agent_node 会在 SystemPrompt 里注入
        bills = raw.get("bills") or []
        if bills:
            ctx["bills"] = bills
            logger.debug(f"[ContextService] 透传 bills: {len(bills)} 条")

        # 4. savings_goals → 直接透传
        savings = raw.get("savings_goals") or []
        if savings:
            ctx["savings_goals"] = savings
            logger.debug(f"[ContextService] 透传 savings_goals: {len(savings)} 条")

        # 5. summary → 直接透传
        summary = raw.get("summary")
        if summary:
            ctx["summary"] = summary

        logger.info(
            f"[ContextService] 构建完成: "
            f"categories={ctx.get('categories', []) and len(ctx['categories']) or 0}, "
            f"budgets={ctx.get('current_budgets', []) and len(ctx['current_budgets']) or 0}, "
            f"bills={len(ctx.get('bills', []))}, "
            f"from_flutter={bool(raw)}"
        )

        return ctx
