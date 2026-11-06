"""
LLM 封装 - 千问/通义模型统一入口
使用 ChatTongyi（LangChain Community 提供）

修复说明 (v1.1):
    对 langchain_community ChatTongyi.subtract_client_response 进行 monkey-patch，
    修复当 DashScope 返回的 tool_calls 数量与 prev_response 不一致时的 IndexError。
    这是 LangChain Community 0.3.x 的已知问题。
"""
from __future__ import annotations

from functools import lru_cache
from typing import Optional

from langchain_community.chat_models.tongyi import ChatTongyi
from loguru import logger

from config.settings import settings


# ======================================================================
# Monkey-Patch: 修复 ChatTongyi.subtract_client_response 的 IndexError
# ======================================================================
def _patch_tongyi_streaming_bug():
    """
    原代码 bug:
        for index, tool_call in enumerate(message["tool_calls"]):
            if prev_message.get("tool_calls"):
                prev_function = prev_message["tool_calls"][index]["function"]
                #                                                        ↑
                # 当 prev_message["tool_calls"] 长度小于当前 index 时崩溃

    修复: 加边界检查 index < len(prev_message["tool_calls"])
    """
    import json as _json
    import langchain_community.chat_models.tongyi as _tongyi_module

    original = _tongyi_module.ChatTongyi.subtract_client_response

    def patched(self, resp, prev_resp):
        """修复版 subtract_client_response"""
        resp_copy = _json.loads(_json.dumps(resp))
        prev_resp_copy = _json.loads(_json.dumps(prev_resp))
        prev_choice = prev_resp_copy["output"]["choices"][0]
        prev_message = prev_choice["message"]

        choice = resp_copy["output"]["choices"][0]
        message = choice["message"]

        message["content"] = message["content"].replace(prev_message["content"], "")

        if message.get("tool_calls"):
            prev_tool_calls = prev_message.get("tool_calls", [])
            for index, tool_call in enumerate(message["tool_calls"]):
                function = tool_call["function"]
                # ✅ 修复: 加边界检查
                if prev_tool_calls and index < len(prev_tool_calls):
                    prev_function = prev_tool_calls[index]["function"]
                    if "name" in function:
                        function["name"] = function["name"].replace(
                            prev_function.get("name", ""), ""
                        )
                    if "arguments" in function:
                        function["arguments"] = function["arguments"].replace(
                            prev_function.get("arguments", ""), ""
                        )

        return resp_copy

    _tongyi_module.ChatTongyi.subtract_client_response = patched
    logger.info("✅ 已应用 ChatTongyi.subtract_client_response monkey-patch")


# 模块加载时立即打补丁
_patch_tongyi_streaming_bug()


# ======================================================================
# LLM 单例管理
# ======================================================================
_llm_instance: Optional[ChatTongyi] = None


def create_llm(
    model_name: Optional[str] = None,
    temperature: Optional[float] = None,
    streaming: bool = False,
) -> ChatTongyi:
    """
    创建千问 LLM 实例

    Args:
        model_name: 模型名（DashScope 兼容格式），默认读 settings.MODEL_NAME
        temperature: 采样温度
        streaming: 是否开启流式（Agent 内部用 invoke，默认 False）

    Returns:
        配置好的 ChatTongyi 实例
    """
    model = model_name or settings.MODEL_NAME
    temp = settings.TEMPERATURE if temperature is None else temperature

    logger.info(f"初始化 LLM: model={model}, temperature={temp}, streaming={streaming}")

    llm = ChatTongyi(
        model=model,
        dashscope_api_key=settings.DASHSCOPE_API_KEY,
        temperature=temp,
        streaming=streaming,
        request_timeout=settings.LLM_REQUEST_TIMEOUT,
        extra_body={"enable_thinking": True},
    )

    return llm


@lru_cache(maxsize=1)
def get_llm() -> ChatTongyi:
    """
    获取全局 LLM 单例（启动时创建，后续复用）
    """
    global _llm_instance
    if _llm_instance is None:
        _llm_instance = create_llm()
    return _llm_instance
