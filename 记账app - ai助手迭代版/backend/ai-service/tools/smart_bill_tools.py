"""
智能记账组合工具 —— 一站式完成「查分类→自动补分类→记账→备注」

LLM 不应该手动调多次工具来完成一次记账。
这个 smart_create_bill 组合工具**内部串行调用**后端 API：
    1. 先查分类列表，看 categoryName 存不存在
    2. 不存在 → 自动调 POST /api/categories 创建（ensureCategoryExists）
    3. 再调 POST /api/bills 创建账单（自动附备注）

用户说「帮我记一杯奶茶18块，备注冰的」时，LLM 只需调这一个工具。
"""
from __future__ import annotations

from datetime import date as _date
from typing import Optional

from pydantic import BaseModel, Field

from tools.base import BackendAPIClient, build_tool, format_money


# ======================================================================
# 中文实体 → 标准分类名映射（工具端兜底，不依赖 LLM 判断）
# ======================================================================
_CATEGORY_ALIASES = {
    # 餐饮
    "午饭": "餐饮", "早餐": "餐饮", "晚饭": "餐饮", "夜宵": "餐饮",
    "午餐": "餐饮", "晚餐": "餐饮", "吃饭": "餐饮", "餐厅": "餐饮",
    "下午茶": "餐饮", "奶茶": "餐饮", "咖啡": "餐饮", "零食": "餐饮",
    # 交通
    "地铁": "交通", "公交": "交通", "打车": "交通", "出租车": "交通",
    "出行": "交通", "加油": "交通", "机票": "交通", "高铁": "交通",
    # 住房
    "房租": "住房", "水电": "住房", "物业": "住房", "网费": "住房",
    # 工资
    "工资": "工资", "薪水": "工资", "月薪": "工资",
    # 购物
    "购物": "购物", "淘宝": "购物", "京东": "购物", "拼多多": "购物",
    # 娱乐
    "娱乐": "娱乐", "电影": "娱乐", "游戏": "娱乐", "KTV": "娱乐",
    # 医疗
    "医疗": "医疗", "看病": "医疗", "药品": "医疗", "挂号": "医疗",
    # 红包
    "红包": "红包", "压岁钱": "红包",
}


def _normalize_category(raw_name: str) -> str:
    """把 LLM 传的原始分类名映射成标准分类名"""
    return _CATEGORY_ALIASES.get(raw_name.strip(), raw_name.strip())


# ======================================================================
class SmartCreateBillInput(BaseModel):
    """智能记账——自动补分类 + 备注"""
    type: int = Field(..., description="1=收入, 2=支出")
    categoryName: str = Field(..., description="分类中文名（如'奶茶','餐饮','工资'）。不存在将自动创建！")
    amount: float = Field(..., description="金额（正数）")
    transactionDate: Optional[str] = Field(
        default=None,
        description="交易日期 yyyy-MM-dd。**可不传，不传自动用今天**！",
    )
    remark: Optional[str] = Field(default=None, description="备注（可选，如'冰的','和朋友','付现金'）")


def _smart_create_bill(
    type: int,
    categoryName: str,
    amount: float,
    transactionDate: Optional[str] = None,
    remark: Optional[str] = None,
    user_id: str = "anonymous",
) -> str:
    """
    智能记账：检查分类→不存在则自动创建→记账单。
    比 create_transaction 多一步分类自检，确保不会出现「分类不存在」导致的 400 错误。
    """
    client = BackendAPIClient(user_id)

    # 🔧 分类名归一化：把 LLM 可能传的实体词映射到标准分类
    categoryName = _normalize_category(categoryName)
    
    # 🔧 日期兜底：LLM 不传或传错时用今天
    if not transactionDate:
        transactionDate = _date.today().isoformat()

    # Step 1: 查分类是否存在
    try:
        existing = client.get("/api/categories") or []
    except Exception as exc:
        return f"查询分类失败: {exc}"

    matched = [c for c in existing
               if c.get("name") == categoryName and c.get("type") == type]
    newly_created = False

    # Step 2: 分类不存在 → 自动创建
    if not matched:
        try:
            new_cat = client.post("/api/categories", json={
                "name": categoryName, "type": type,
            })
            newly_created = True
            cat_id = new_cat.get("id", "?")
        except Exception as exc:
            return f"创建分类失败: {exc}"
    else:
        cat_id = matched[0].get("id", "?")

    # Step 3: 创建账单
    body = {
        "type": type,
        "categoryName": categoryName,
        "amount": amount,
        "transactionDate": transactionDate,
    }
    if remark:
        body["remark"] = remark

    try:
        bill = client.post("/api/bills", json=body)
    except Exception as exc:
        return f"记账失败: {exc}"

    # Step 4: 返回结果
    type_text = "收入" if type == 1 else "支出"
    creation_note = f"（首次出现，已自动创建分类 #{cat_id}）" if newly_created else ""
    remark_line = f"备注: {remark}\n" if remark else ""

    return (
        f"✅ 账单已创建 #{bill['id']}{creation_note}\n"
        f"类型: {type_text}\n"
        f"分类: {categoryName}\n"
        f"金额: {format_money(bill.get('amount'))}\n"
        f"日期: {bill.get('transactionDate')}\n"
        f"{remark_line}感谢记账！💪"
    )


smart_create_bill_tool = build_tool(
    func=_smart_create_bill,
    name="smart_create_bill",
    description="""【推荐首选】智能记账——自动检查分类，不存在则自动创建，然后记账并可附备注。

    当用户说：
      • 「帮我记一杯奶茶 18 块」→ 自动创建「奶茶」分类 → 记账
      • 「午饭 25，备注食堂」→ 分类已存在 → 直接记账带备注
      • 「发工资了 8000」→ 收入类型 → 直接记账

    参数：type(1=收入,2=支出), categoryName(中文名), amount, transactionDate, remark(可选)。

    这是你最常用的记账工具！每次用户要记账都**优先调这个**，不要手动拆成 list_categories + create_category + create_transaction。""",
    args_schema=SmartCreateBillInput,
)


# ======================================================================
class BatchCreateBillsInput(BaseModel):
    """批量记账——一次帮用户记多笔（如数完账后一起录入）"""
    bills: list[dict] = Field(
        ...,
        description="账单列表，每个元素是 {'type':1|2, 'categoryName':'餐饮', 'amount':25.0, 'transactionDate':'2026-11-06', 'remark':'午饭'}",
    )


def _batch_create_bills(bills: list[dict], user_id: str = "anonymous") -> str:
    """批量记账（每笔内部同样做分类自检 + 日期兜底 + 分类归一化）"""
    results = []
    for i, b in enumerate(bills, 1):
        result = _smart_create_bill(
            type=b.get("type", 2),
            categoryName=b.get("categoryName", ""),
            amount=b.get("amount", 0),
            transactionDate=b.get("transactionDate"),  # None 或空字符串都会触发今天兜底
            remark=b.get("remark"),
            user_id=user_id,
        )
        results.append(f"{i}. {result.splitlines()[0]}")  # 只取第一行摘要
    return (
        f"📝 共处理 {len(bills)} 笔账单：\n" +
        "\n".join(results)
    )


batch_create_bills_tool = build_tool(
    func=_batch_create_bills,
    name="batch_create_bills",
    description="""批量记账（一次调用处理多笔账单）。
    当用户说「我今天花了不少：早饭 15，午饭 32，地铁 6」时，
    把这些打包成一个列表一次性调用。内部每笔都会自动检查/创建分类。""",
    args_schema=BatchCreateBillsInput,
)


def get_smart_bill_tools():
    return [smart_create_bill_tool, batch_create_bills_tool]
