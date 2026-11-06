"""
分类管理工具 —— Flutter LocalApiServer
GET    /api/categories?type=
POST   /api/categories   body: {name, type}   ← 按 name+type 查找/创建
DELETE /api/categories/{id}
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from tools.base import BackendAPIClient, build_tool


# ======================================================================
class ListCategoriesInput(BaseModel):
    type: Optional[int] = Field(default=None, description="1=收入, 2=支出, 不传=全部")


def _list_categories(
    type: Optional[int] = None,
    user_id: str = "anonymous",
) -> str:
    client = BackendAPIClient(user_id)
    params = {"type": type} if type is not None else None
    try:
        data = client.get("/api/categories", params=params)
    except Exception as exc:
        return f"查询分类失败: {exc}"
    if not data:
        return "目前没有任何分类数据。"

    type_map = {1: "💰 收入", 2: "💸 支出"}
    lines = ["📋 可用分类：\n"]
    for c in data:
        tname = type_map.get(c.get("type"), "?")
        icon = c.get("icon") or "📁"
        lines.append(f"• [{c.get('id')}] {icon} {c.get('name')} ({tname})")
    return "\n".join(lines)


list_categories_tool = build_tool(
    func=_list_categories,
    name="list_categories",
    description="""查询可用分类列表（返回中文名称如'餐饮','工资'）。
    当用户要记账时，先用这个查有哪些分类可用。""",
    args_schema=ListCategoriesInput,
)


# ======================================================================
class CreateCategoryInput(BaseModel):
    """创建新分类"""
    name: str = Field(..., description="分类名称（如'奶茶','旅行','礼物'）")
    type: int = Field(..., description="1=收入分类, 2=支出分类")


def _create_category(
    name: str,
    type: int,
    user_id: str = "anonymous",
) -> str:
    """创建新分类（如果已存在同名同类型的分类会直接返回现有记录）"""
    client = BackendAPIClient(user_id)
    try:
        data = client.post("/api/categories", json={"name": name, "type": type})
    except Exception as exc:
        return f"创建分类失败: {exc}"

    type_text = "收入" if type == 1 else "支出"
    is_new = isinstance(data, dict) and data.get("id")
    action = "已找到" if isinstance(data, dict) and data.get("id") else "已创建"
    return (
        f"✅ {action}分类 #{data.get('id') if isinstance(data, dict) else '?'}\n"
        f"名称: {data.get('name') if isinstance(data, dict) else name}\n"
        f"类型: {type_text}"
    )


create_category_tool = build_tool(
    func=_create_category,
    name="create_category",
    description="""创建一个新的分类名称。
    当用户要记的账单类别不在现有分类里时（比如用户说'记一杯奶茶'但没有'奶茶'分类），
    先用这个创建分类，再去记账。后端会自动处理重名（返回已有记录）。""",
    args_schema=CreateCategoryInput,
)


# ======================================================================
class DeleteCategoryInput(BaseModel):
    id: int = Field(..., description="要删除的分类 ID")


def _delete_category(id: int, user_id: str = "anonymous") -> str:
    client = BackendAPIClient(user_id)
    try:
        client.delete(f"/api/categories/{id}")
    except Exception as exc:
        return f"删除分类失败: {exc}"
    return f"🗑️ 分类 #{id} 已删除。"


delete_category_tool = build_tool(
    func=_delete_category,
    name="delete_category",
    description="""删除一个分类。谨慎使用——删除后该分类名称可能仍存在于历史账单中。""",
    args_schema=DeleteCategoryInput,
)


def get_category_tools():
    return [list_categories_tool, create_category_tool, delete_category_tool]
