"""真实 LLM 联调测试 —— 绕过后端业务 API，直接测 Agent 对话"""
import sys
sys.path.insert(0, ".")

from services.chat_service import ChatService

print("=" * 60)
print("🤖 真实 LLM 联调测试（千问 qwen-max）")
print("=" * 60)

svc = ChatService(user_id="test_user")

tests = [
    "你好，自我介绍一下",
    "帮我查一下我有哪些可用的收支分类",
    "我这个月消费趋势如何（假设 2026 年 11 月）",
]

for i, msg in enumerate(tests, 1):
    print(f"\n{'='*60}")
    print(f"📝 测试 {i}/{len(tests)}: {msg}")
    print(f"{'='*60}")

    try:
        reply = svc.process(msg, session_id="real_test_session")
        print(f"\n🤖 AI 回复:\n{reply}")
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"\n❌ 测试 {i} 失败: {e}")

print("\n" + "=" * 60)
print("✅ 全部真实 LLM 联调测试完成")
print("=" * 60)
