"""
HTTP 后端 API 客户端 —— 统一封装 httpx 调用

目标: Flutter App 内嵌 LocalApiServer (0.0.0.0:8999)
    Python Agent 通过 ADB forward tcp:8999 tcp:8999 访问

所有工具文件都使用这个 BackendAPIClient，请求/响应自动用 Flutter DTO 结构。
"""
from __future__ import annotations

from typing import Optional, Any

import httpx
from loguru import logger

from config.settings import settings


def format_money(amount: Any) -> str:
    """格式化金额为 ¥1,234.56 格式"""
    try:
        val = float(amount or 0)
    except (ValueError, TypeError):
        val = 0.0
    sign = '-' if val < 0 else ''
    abs_val = abs(val)
    parts = f"{abs_val:,.2f}".split('.')
    return f"{sign}¥{parts[0]}.{parts[1]}"


class BackendAPIClient:
    """封装 httpx 客户端，统一处理 ApiResponse 包装"""

    def __init__(self, user_id: str = "anonymous"):
        self.base_url = settings.BACKEND_BASE_URL.rstrip("/")
        self.timeout = settings.BACKEND_REQUEST_TIMEOUT
        self.user_id = user_id
        self._client = httpx.Client(timeout=self.timeout)

    def close(self) -> None:
        self._client.close()

    # ------------------------------------------------------------------
    def get(self, path: str, params: Optional[dict] = None) -> Any:
        logger.debug(f"[Backend GET] {self.base_url}{path} params={params}")
        resp = self._client.get(f"{self.base_url}{path}", params=params)
        return self._handle(resp, path)

    def post(self, path: str, json: Optional[dict] = None) -> Any:
        logger.debug(f"[Backend POST] {self.base_url}{path} body={json}")
        resp = self._client.post(f"{self.base_url}{path}", json=json)
        return self._handle(resp, path)

    def put(self, path: str, json: Optional[dict] = None) -> Any:
        logger.debug(f"[Backend PUT] {self.base_url}{path} body={json}")
        resp = self._client.put(f"{self.base_url}{path}", json=json)
        return self._handle(resp, path)

    def delete(self, path: str) -> Any:
        logger.debug(f"[Backend DELETE] {self.base_url}{path}")
        resp = self._client.delete(f"{self.base_url}{path}")
        return self._handle(resp, path)

    # ------------------------------------------------------------------
    def _handle(self, resp: httpx.Response, path: str) -> Any:
        if resp.status_code >= 400:
            raise RuntimeError(
                f"后端调用失败: HTTP {resp.status_code} | {path} | {resp.text[:200]}"
            )
        try:
            body = resp.json()
        except Exception:
            raise RuntimeError(f"后端返回非 JSON: {resp.text[:200]}")

        # Flutter LocalApiServer 返回结构: {'code': 200, 'message': 'OK', 'data': ...}
        if isinstance(body, dict) and "code" in body and "data" in body:
            return body["data"]
        return body


def build_tool(func, name, description, args_schema=None):
    """把普通函数包装成 LangChain StructuredTool"""
    from langchain_core.tools import StructuredTool

    kwargs: dict = {
        "func": func,
        "name": name,
        "description": description,
    }
    if args_schema is not None:
        kwargs["args_schema"] = args_schema
    return StructuredTool.from_function(**kwargs)
