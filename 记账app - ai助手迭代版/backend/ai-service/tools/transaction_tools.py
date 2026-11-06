"""
账单交易工具 —— 操作 Flutter 本地 SQLite 的 bills 表

表结构:
    bills(id, type, categoryName, amount, transactionDate, remark, imagePath)
    type: 1=收入, 2=支出
    categoryName: 直接存分类名称字符串（无外键）

后端: Flutter LocalApiServer  POST /api/bills, GET /api/bills, PUT /api/bills/{id}, DELETE /api/bills/{id}
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from tools.base import BackendAPIClient, build_tool, format_money


# ======================================================================
# 查询账单
# ======================================================================
class QueryTransactionsInput(BaseModel):
    """查询账单参数"""
    startDate: Optional[str] = Field(default=None, description="开始日期 yyyy-MM-dd")
    endDate: Optional[str] = Field(default=None, description="结束日期 yyyy-MM-dd")
    type: Optional[int] = Field(default=None, description="1=收入, 2=支出, 不传=全部")
    categoryName: Optional[str] = Field(default=None, description="按分类名称过滤（如 '餐饮'）")


def _query_transactions(
    startDate: Optional[str] = None,
    endDate: Optional[str] = None,
    type: Optional[int] = None,
    categoryName: Optional[str] = None,
    user_id: str = "anonymous",
) -> str:
    """查询账单列表"""
    client = BackendAPIClient(user_id)
    params: dict = {}
    if startDate: params["startDate"] = startDate
    if endDate: params["endDate"] = endDate
    if type is not None: params["type"] = type
    if categoryName: params["categoryName"] = categoryName

    try:
        data = client.get("/api/bills", params=params or None)
    except Exception as exc:
        return f"查询账单失败: {exc}"

    if not data:
        return "当前没有符合条件的账单记录。"

    type_map = {1: "💰 收入", 2: "💸 支出"}
    lines = [f"📋 共 {len(data)} 条账单：\n"]
    for t in data:
        tname = type_map.get(t.get("type"), "?")
        lines.append(
            f"• [{t.get('transactionDate')}] "
            f"{tname} {format_money(t.get('amount'))} "
            f"· {t.get('categoryName', '')}"
            f"{('  (' + t['remark'] + ')') if t.get('remark') else ''}"
        )
    return "\n".join(lines)


query_transactions_tool = build_tool(
    func=_query_transactions,
    name="query_transactions",
    description="""查询账单记录。可以按时间范围、收支类型、分类名称过滤。
    当用户说「我花了多少钱」「查一下上周的账单」「餐饮花了多少」时调用。""",
    args_schema=QueryTransactionsInput,
)


# ======================================================================
# 创建账单（核心工具 —— 记账）
# ======================================================================
class CreateTransactionInput(BaseModel):
    """创建账单参数"""
    type: int = Field(..., description="1=收入, 2=支出")
    categoryName: str = Field(..., description="分类名称字符串（如'餐饮','交通','工资'）。先用 list_categories 查可用分类")
    amount: float = Field(..., description="金额（正数）")
    transactionDate: str = Field(..., description="交易日期 yyyy-MM-dd")
    remark: Optional[str] = Field(default=None, description="备注（可选）")


def _create_transaction(
    type: int,
    categoryName: str,
    amount: float,
    transactionDate: str,
    remark: Optional[str] = None,
    user_id: str = "anonymous",
) -> str:
    """创建一条账单（记账）"""
    client = BackendAPIClient(user_id)
    body = {
        "type": type,
        "categoryName": categoryName,
        "amount": amount,
        "transactionDate": transactionDate,
    }
    if remark:
        body["remark"] = remark

    try:
        data = client.post("/api/bills", json=body)
    except Exception as exc:
        return f"创建账单失败: {exc}"

    type_text = "收入" if type == 1 else "支出"
    return (
        f"✅ 账单已创建 #{data['id']}\n"
        f"类型: {type_text}\n"
        f"分类: {data.get('categoryName')}\n"
        f"金额: {format_money(data.get('amount'))}\n"
        f"日期: {data.get('transactionDate')}"
    )


create_transaction_tool = build_tool(
    func=_create_transaction,
    name="create_transaction",
    description="""创建一条账单（记账）。
    ⚠️ categoryName 必须是分类的**中文名称字符串**（如'餐饮','工资'），不是 categoryId！
    ⚠️ 在调用前请先用 list_categories 查用户可用的分类名称，不要自己编造！
    当用户说「帮我记一笔午饭25元」「今天早餐花了15元」时调用。""",
    args_schema=CreateTransactionInput,
)


# ======================================================================
# 更新账单（PATCH 语义：只传需要改的字段）
# ======================================================================
class UpdateTransactionInput(BaseModel):
    """更新账单参数 —— 所有字段可选，只传要改的"""
    id: int = Field(..., description="账单 ID")
    type: Optional[int] = Field(default=None, description="1=收入, 2=支出")
    categoryName: Optional[str] = Field(default=None, description="新的分类名称")
    amount: Optional[float] = Field(default=None, description="新金额")
    transactionDate: Optional[str] = Field(default=None, description="新日期 yyyy-MM-dd")
    remark: Optional[str] = Field(default=None, description="新备注")


def _update_transaction(
    id: int,
    type: Optional[int] = None,
    categoryName: Optional[str] = None,
    amount: Optional[float] = None,
    transactionDate: Optional[str] = None,
    remark: Optional[str] = None,
    user_id: str = "anonymous",
) -> str:
    """更新账单（只传需要改的字段即可）"""
    client = BackendAPIClient(user_id)
    body: dict = {}
    if type is not None: body["type"] = type
    if categoryName is not None: body["categoryName"] = categoryName
    if amount is not None: body["amount"] = amount
    if transactionDate is not None: body["transactionDate"] = transactionDate
    if remark is not None: body["remark"] = remark

    if not body:
        return "⚠️ 请至少提供一个要更新的字段。"

    try:
        data = client.put(f"/api/bills/{id}", json=body)
    except Exception as exc:
        return f"更新账单失败: {exc}"

    return (
        f"✅ 账单 #{id} 已更新\n"
        f"新内容: {data.get('categoryName')} | {format_money(data.get('amount'))} | {data.get('transactionDate')}"
    )


update_transaction_tool = build_tool(
    func=_update_transaction,
    name="update_transaction",
    description="""更新已有账单。只传需要修改的字段（部分更新）。
    当用户说「把昨天那笔早餐改成 20 元」「改一下那条的备注」时调用。""",
    args_schema=UpdateTransactionInput,
)


# ======================================================================
# 删除账单
# ======================================================================
class DeleteTransactionInput(BaseModel):
    """删除账单参数"""
    id: int = Field(..., description="要删除的账单 ID")


def _delete_transaction(id: int, user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        client.delete(f"/api/bills/{id}")
    except Exception as exc:
        return f"删除账单失败: {exc}"
    return f"🗑️ 账单 #{id} 已删除。"


delete_transaction_tool = build_tool(
    func=_delete_transaction,
    name="delete_transaction",
    description="""删除一条账单记录。操作不可恢复。
    当用户说「删掉那条错的」「把 #123 那条删了」时调用。""",
    args_schema=DeleteTransactionInput,
)


def get_transaction_tools():
    return [
        query_transactions_tool,
        create_transaction_tool,
        update_transaction_tool,
        delete_transaction_tool,
    ]
