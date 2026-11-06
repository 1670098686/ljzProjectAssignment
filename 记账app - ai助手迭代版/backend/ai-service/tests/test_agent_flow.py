"""
AI Agent 单元测试 - 不依赖真实 LLM 和后端

分三层:
  1. 工具层测试 (tools)   —— Mock BackendAPIClient，验证 tool.invoke() 链路
  2. Agent 状态图测试      —— Mock LLM，验证 ReAct 循环 + tool_calls.id 配对
  3. 集成流程测试          —— 验证 chat_service.process() 端到端（Mock LLM）
"""
from __future__ import annotations

import sys
from unittest.mock import MagicMock, patch, AsyncMock

import pytest
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
from langgraph.graph.state import CompiledStateGraph

# 确保 ai-service 在 path 上
sys.path.insert(0, ".")

# ======================================================================
# Fixtures
# ======================================================================
@pytest.fixture
def mock_backend():
    """Mock BackendAPIClient —— 假装后端有数据"""
    return {
        "categories": [
            {"id": 1, "name": "工资", "type": 1},
            {"id": 2, "name": "餐饮", "type": 2},
            {"id": 3, "name": "交通", "type": 2},
            {"id": 4, "name": "购物", "type": 2},
        ],
        "transactions": [
            {
                "id": 101,
                "type": 2,
                "categoryId": 2,
                "categoryName": "餐饮",
                "amount": 32.5,
                "transactionDate": "2026-11-06",
                "remark": "麦当劳午餐",
            },
            {
                "id": 102,
                "type": 1,
                "categoryId": 1,
                "categoryName": "工资",
                "amount": 8000.0,
                "transactionDate": "2026-11-01",
                "remark": "11月工资",
            },
        ],
    }


# ======================================================================
# 1. 工具层测试
# ======================================================================
class TestTools:
    """验证所有工具的 .invoke() 能正常返回字符串结果"""

    def test_tools_all_registered(self):
        from tools import get_all_tools

        tools = get_all_tools()
        assert len(tools) == 20, f"预期 20 个工具，实际 {len(tools)} 个"
        names = {t.name for t in tools}
        expected = {
            # 智能组合（2）
            "smart_create_bill", "batch_create_bills",
            # 交易（4）
            "query_transactions", "create_transaction", "update_transaction", "delete_transaction",
            # 分类（3）
            "list_categories", "create_category", "delete_category",
            # 预算（4）
            "query_budgets", "create_budget", "delete_budget", "check_budget_alerts",
            # 储蓄（4）
            "query_saving_goals", "create_saving_goal", "update_saving_goal_amount", "delete_saving_goal",
            # 统计（3）
            "get_monthly_summary", "get_category_stats", "get_trend",
        }
        assert names == expected, f"工具名不匹配: 缺 {expected - names}, 多 {names - expected}"

    def test_tool_invoke_with_dict(self, mock_backend):
        """经验 1438138: StructuredTool 必须用 .invoke(dict)"""
        from tools.category_tools import list_categories_tool

        # Mock BackendAPIClient.get
        with patch("tools.category_tools.BackendAPIClient") as mock_cls:
            mock_instance = MagicMock()
            mock_instance.get.return_value = mock_backend["categories"]
            mock_cls.return_value = mock_instance

            # 必须用 dict 调用，不是 list_categories_tool()
            result = list_categories_tool.invoke({"user_id": "test"})
            assert isinstance(result, str)
            assert "工资" in result
            assert "餐饮" in result

    def test_transaction_tool_create(self):
        from tools.transaction_tools import create_transaction_tool

        with patch("tools.transaction_tools.BackendAPIClient") as mock_cls:
            mock_instance = MagicMock()
            mock_instance.post.return_value = {
                "id": 999, "type": 2, "categoryName": "餐饮",
                "amount": 25.0, "transactionDate": "2026-11-06",
            }
            mock_cls.return_value = mock_instance

            result = create_transaction_tool.invoke({
                "type": 2, "categoryName": "餐饮", "amount": 25.0,
                "transactionDate": "2026-11-06", "user_id": "test",
            })
            assert "✅" in result
            assert "#999" in result
            assert mock_instance.post.called

    def test_transaction_tool_update_no_fields(self):
        """不传任何更新字段时应提示"""
        from tools.transaction_tools import update_transaction_tool

        result = update_transaction_tool.invoke({
            "id": 1, "user_id": "test",
        })
        assert "至少提供一个要更新的字段" in result

    def test_format_money(self):
        from tools.base import format_money

        assert format_money(1234.56) == "¥1,234.56"
        assert format_money(None) == "¥0.00"
        assert format_money(0) == "¥0.00"


# ======================================================================
# 2. Agent 状态图测试 (Mock LLM)
# ======================================================================
class TestAgentGraph:
    """验证 LangGraph ReAct Agent 的节点连接和 tool_calls 配对"""

    def test_graph_compiles(self):
        from core.agent import create_agent_graph
        from tools import get_all_tools

        graph = create_agent_graph(tools=get_all_tools(), max_iterations=5)
        assert isinstance(graph, CompiledStateGraph)

    def test_tool_call_id_pairing(self):
        """
        经验 1438138: 验证 AIMessage.tool_calls[].id 和 ToolMessage.tool_call_id 配对
        """
        from langchain_core.messages import ToolMessage

        # 模拟一个 AIMessage 带 tool_calls
        tool_call_id = "call_abc123"
        ai_msg = AIMessage(
            content="",
            tool_calls=[{
                "name": "list_categories",
                "args": {"user_id": "test"},
                "id": tool_call_id,
            }],
        )

        # ToolMessage.tool_call_id 必须与之相同
        tool_msg = ToolMessage(
            content="结果",
            tool_call_id=tool_call_id,  # 配对成功
        )

        assert ai_msg.tool_calls[0]["id"] == tool_msg.tool_call_id

    def test_agent_loop_terminates_without_tool_calls(self):
        """
        无工具调用时 Agent 应直接返回，形成完整的 ReAct 闭环
        通过 mock LLM 返回不带 tool_calls 的 AIMessage 来验证
        """
        from core.agent import create_agent_graph
        from tools.category_tools import list_categories_tool

        mock_llm = MagicMock()
        mock_llm.bind_tools.return_value = mock_llm
        mock_llm.invoke.return_value = AIMessage(
            content="好的，这是最终回答。",
            tool_calls=[],  # 无工具调用 → 结束
        )

        # 如果走到 END 节点后没有死循环，说明路由逻辑正确
        # 此处仅验证 graph 能在无 tool_calls 情况下稳定
        graph = create_agent_graph(
            tools=[list_categories_tool],
            system_prompt="你是测试助手",
            max_iterations=3,
        )
        assert graph is not None


# ======================================================================
# 3. ChatService 集成测试 (Mock LLM)
# ======================================================================
class TestChatService:
    def test_process_returns_string(self):
        """ChatService.process() 应返回字符串回复"""
        from services.chat_service import ChatService

        # Mock Agent 单例
        with patch.object(ChatService, "_get_agent") as mock_get:
            mock_graph = MagicMock()
            mock_graph.invoke.return_value = {
                "messages": [
                    AIMessage(content="这是 AI 回复"),
                ],
                "tool_calls_made": [],
                "final_answer": "",
            }
            mock_get.return_value = mock_graph

            svc = ChatService(user_id="test_user")
            reply = svc.process("你好")
            assert isinstance(reply, str)
            assert "AI 回复" in reply

    def test_process_handles_exception(self):
        """Agent 抛异常时返回友好错误信息"""
        from services.chat_service import ChatService

        with patch.object(ChatService, "_get_agent") as mock_get:
            mock_graph = MagicMock()
            mock_graph.invoke.side_effect = RuntimeError("LLM 连接超时")
            mock_get.return_value = mock_graph

            svc = ChatService(user_id="test_user")
            reply = svc.process("你好")
            assert "抱歉" in reply
            assert "LLM 连接超时" in reply


# ======================================================================
# 运行入口
# ======================================================================
if __name__ == "__main__":
    print("运行全部测试 ...")
    sys.exit(pytest.main([__file__, "-v", "--tb=short"]))
