/// AI 智账助手聊天页面
///
/// 架构:
///   ┌───────────────┬───────────────────────────────────┐
///   │  会话历史侧栏   │         聊天主区                       │
///   │  • 新建会话    │  [欢迎语 / 历史消息 / AI 回复]           │
///   │  • 会话列表    │                                        │
///   │  • 删除       │  [输入框 + 发送]                        │
///   └───────────────┴───────────────────────────────────┘
///
/// 后端: Python AI Agent (:8082)
///   - POST /api/v1/ai/chat         聊天（返回 session_id）
///   - GET  /api/v1/ai/sessions      列出用户所有会话
///   - GET  /api/v1/ai/sessions/{id} 获取某个会话完整消息
///   - DELETE /api/v1/ai/sessions/{id}
library;

import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../../core/services/local_database_service.dart';
import '../../core/services/local_data_service.dart';
import '../ai/ai_agent_api.dart';

class AiAssistantPage extends StatefulWidget {
  const AiAssistantPage({super.key});

  @override
  State<AiAssistantPage> createState() => _AiAssistantPageState();
}

class _AiAssistantPageState extends State<AiAssistantPage> {
  List<AiMessage> _messages = [];
  List<AiSessionSummary> _sessions = [];
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey();

  bool _isLoading = false;
  bool _isLoadingSessions = false;
  String? _currentSessionId; // null 表示"新会话，还没发过消息"
  bool _isNewSession = true;

  String? _baseUrl;
  static const String _userId = 'user_001';
  late final AiAgentApi _api;

  @override
  void initState() {
    super.initState();
    _initApi();
    _addWelcomeMessage();
    _loadSessions();
  }

  Future<void> _initApi() async {
    setState(() {
      // Python AI Agent 在电脑 :8082，模拟器通过 10.0.2.2 访问
      _baseUrl = 'http://10.0.2.2:8082';
      _api = AiAgentApi(baseUrl: _baseUrl!, userId: _userId);
    });
  }

  // ------------------------------------------------------------------
  // 会话管理
  // ------------------------------------------------------------------
  Future<void> _loadSessions() async {
    if (_baseUrl == null) return;
    setState(() => _isLoadingSessions = true);
    try {
      final sessions = await _api.listSessions();
      setState(() {
        _sessions = sessions;
        _isLoadingSessions = false;
      });
    } catch (e) {
      debugPrint('加载会话列表失败: $e');
      setState(() => _isLoadingSessions = false);
    }
  }

  Future<void> _switchToSession(AiSessionSummary session) async {
    setState(() {
      _currentSessionId = session.id;
      _isNewSession = false;
      _messages.clear();
      _isLoading = true;
    });
    // 关闭 drawer
    _scaffoldKey.currentState?.closeDrawer();

    try {
      final history = await _api.getSessionMessages(session.id);
      setState(() {
        _messages.addAll(history);
      });
    } catch (e) {
      setState(() {
        _messages.add(AiMessage.ai('⚠️ 加载历史消息失败: $e'));
      });
    } finally {
      setState(() => _isLoading = false);
      _scrollToBottom();
    }
  }

  void _startNewSession() {
    setState(() {
      _currentSessionId = null;
      _isNewSession = true;
      _messages.clear();
    });
    _addWelcomeMessage();
    _scaffoldKey.currentState?.closeDrawer();
  }

  Future<void> _deleteSession(AiSessionSummary session) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除会话'),
        content: Text('确定要删除会话 "${session.title}" 吗？此操作不可恢复。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('删除', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    final ok = await _api.deleteSession(session.id);
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? '会话已删除' : '删除失败')),
    );

    // 如果删除的是当前会话，自动回到新会话状态
    if (_currentSessionId == session.id) {
      _startNewSession();
    }
    await _loadSessions();
  }

  // ------------------------------------------------------------------
  // 聊天
  // ------------------------------------------------------------------
  void _addWelcomeMessage() {
    _messages.add(
      AiMessage.ai(
        '你好👋 我是「智账 AI 助手」！\n\n'
        '我可以帮你：\n'
        '💰 快速记账 —— "帮我记一笔午饭25元"\n'
        '📊 查收支 —— "我这个月花了多少"\n'
        '🎯 预算管理 —— "给餐饮设2000预算"\n'
        '🏦 储蓄目标 —— "帮我创建旅游基金"\n'
        '⚠️ 预警提醒 —— "我预算超了吗"\n\n'
        '试试对我说点什么吧~',
      ),
    );
  }

  Future<void> _sendMessage() async {
    final text = _inputController.text.trim();
    if (text.isEmpty || _isLoading) return;

    final userMsg = AiMessage.user(text);
    setState(() {
      _messages.add(userMsg);
      _isLoading = true;
      _inputController.clear();
    });
    _scrollToBottom();

    final effectiveSessionId = _isNewSession ? null : _currentSessionId;

    try {
      // ===== 从本地 SQLite 读取上下文数据 =====
      final localData = context.read<LocalDataService>();
      final allData = await localData.getAllDataFromLocal();
      final bills = await localData.getBillsFromLocal();
      final categories = await localData.getCategoriesFromLocal();
      final budgets = await localData.getBudgetsFromLocal();
      final savings = await context.read<LocalDatabaseService>().getAllSavingGoals();

      // 组装 AI 上下文（精简版，避免 token 爆炸）
      final contextPayload = <String, dynamic>{
        'user_id': _userId,
        'message': text,
        if (effectiveSessionId != null) 'session_id': effectiveSessionId,
        'context': {
          'summary': allData,
          'bills': bills.take(50).map((b) => {
                'type': b.type,
                'category': b.categoryName,
                'amount': b.amount,
                'date': b.transactionDate,
                'remark': b.remark,
              }).toList(),
          'categories': categories.map((c) => {
                'name': c.name,
                'type': c.type,
              }).toList(),
          'budgets': budgets.map((b) => {
                'category': b.categoryName,
                'monthly_budget': b.amount,
                'spent': b.spent,
                'year': b.year,
                'month': b.month,
              }).toList(),
          'savings_goals': savings.map((s) => {
                'name': s.name,
                'target': s.targetAmount,
                'current': s.currentAmount,
                'deadline': s.deadline?.toIso8601String() ?? '',
              }).toList(),
        },
      };

      final reply = await _api.chatWithContext(contextPayload);

      setState(() {
        _currentSessionId = reply.sessionId;
        _isNewSession = false;
        _messages.add(AiMessage.ai(reply.reply));
      });
    } catch (e) {
      setState(() {
        _messages.add(AiMessage.ai('❌ 请求失败: $e'));
      });
    } finally {
      setState(() => _isLoading = false);
      _scrollToBottom();
      await _loadSessions();
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  // ------------------------------------------------------------------
  // UI
  // ------------------------------------------------------------------
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.menu),
          tooltip: '会话历史',
          onPressed: () => _scaffoldKey.currentState?.openDrawer(),
        ),
        title: const Row(
          children: [
            Icon(Icons.auto_awesome, color: Colors.amber),
            SizedBox(width: 8),
            Text('智账 AI 助手'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_comment),
            tooltip: '新建会话',
            onPressed: _startNewSession,
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: '刷新会话列表',
            onPressed: _loadSessions,
          ),
        ],
      ),
      drawer: _buildSessionDrawer(),
      body: Column(
        children: [
          Expanded(child: _buildMessageList()),
          if (_isLoading) _buildTypingIndicator(),
          _buildInputBar(),
        ],
      ),
    );
  }

  Widget _buildSessionDrawer() {
    return Drawer(
      child: Column(
        children: [
          DrawerHeader(
            decoration: BoxDecoration(
              color: Theme.of(context).primaryColor,
            ),
            child: SizedBox.expand(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const CircleAvatar(
                    radius: 28,
                    backgroundColor: Colors.white24,
                    child: Icon(Icons.auto_awesome,
                        color: Colors.amber, size: 32),
                  ),
                  const SizedBox(height: 12),
                  Text('AI 会话历史',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            color: Colors.white,
                          )),
                  const SizedBox(height: 4),
                  Text('共 ${_sessions.length} 个会话',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Colors.white70,
                          )),
                ],
              ),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.add),
            title: const Text('🆕 新建会话'),
            onTap: _startNewSession,
          ),
          const Divider(height: 1),
          Expanded(
            child: _isLoadingSessions
                ? const Center(child: CircularProgressIndicator())
                : _sessions.isEmpty
                    ? Center(
                        child: Text(
                          '暂无历史会话\n开始你的第一次对话吧~',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      )
                    : ListView.separated(
                        itemCount: _sessions.length,
                        separatorBuilder: (_, __) => const Divider(height: 1),
                        itemBuilder: (ctx, i) {
                          final session = _sessions[i];
                          final isCurrent =
                              session.id == _currentSessionId && !_isNewSession;
                          return _SessionListTile(
                            session: session,
                            isCurrent: isCurrent,
                            onTap: () => _switchToSession(session),
                            onDelete: () => _deleteSession(session),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageList() {
    if (_messages.isEmpty && _isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      itemCount: _messages.length,
      itemBuilder: (ctx, i) => _MessageBubble(message: _messages[i]),
    );
  }

  Widget _buildTypingIndicator() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      alignment: Alignment.centerLeft,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(3, (i) {
            return Container(
              width: 8,
              height: 8,
              margin: const EdgeInsets.symmetric(horizontal: 2),
              decoration: const BoxDecoration(
                color: Colors.grey,
                shape: BoxShape.circle,
              ),
            );
          }),
        ),
      ),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Theme.of(context).scaffoldBackgroundColor,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _inputController,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => _sendMessage(),
                decoration: InputDecoration(
                  hintText: _isNewSession
                      ? '开启新对话...'
                      : '继续上一个会话...',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                ),
                maxLines: null,
              ),
            ),
            const SizedBox(width: 8),
            IconButton.filled(
              onPressed: _isLoading ? null : _sendMessage,
              icon: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.send),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _inputController.dispose();
    _scrollController.dispose();
    super.dispose();
  }
}

// ======================================================================
// 会话列表项
// ======================================================================
class _SessionListTile extends StatelessWidget {
  final AiSessionSummary session;
  final bool isCurrent;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  const _SessionListTile({
    required this.session,
    required this.isCurrent,
    required this.onTap,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      tileColor: isCurrent
          ? Theme.of(context).primaryColor.withValues(alpha: 0.1)
          : null,
      leading: Icon(
        Icons.chat_bubble_outline,
        color: isCurrent ? Theme.of(context).primaryColor : null,
      ),
      title: Text(
        session.title.isEmpty ? '新会话' : session.title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontWeight: isCurrent ? FontWeight.bold : FontWeight.normal,
        ),
      ),
      subtitle: Text(
        session.lastMessage.isEmpty
            ? '${session.messageCount} 条消息'
            : session.lastMessage,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: Theme.of(context).textTheme.bodySmall,
      ),
      trailing: IconButton(
        icon: const Icon(Icons.delete_outline, size: 18, color: Colors.red),
        onPressed: onDelete,
        tooltip: '删除会话',
      ),
      onTap: onTap,
    );
  }
}

// ======================================================================
// 消息气泡
// ======================================================================
class _MessageBubble extends StatelessWidget {
  final AiMessage message;

  const _MessageBubble({required this.message});

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment:
            isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        children: [
          if (!isUser) _avatar(Icons.auto_awesome, Colors.amber),
          const SizedBox(width: 8),
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: isUser
                    ? Theme.of(context).primaryColor
                    : Colors.grey[200],
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    message.content,
                    style: TextStyle(
                      color: isUser ? Colors.white : Colors.black87,
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _formatTime(message.timestamp),
                    style: TextStyle(
                      color: isUser
                          ? Colors.white60
                          : Colors.black45,
                      fontSize: 10,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),
          if (isUser) _avatar(Icons.person, Colors.blue),
        ],
      ),
    );
  }

  Widget _avatar(IconData icon, Color color) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
      child: Icon(icon, color: Colors.white, size: 20),
    );
  }

  static String _formatTime(DateTime ts) {
    final now = DateTime.now();
    final diff = now.difference(ts);
    if (diff.inMinutes < 1) return '刚刚';
    if (diff.inHours < 1) return '${diff.inMinutes}分钟前';
    if (diff.inDays < 1) return '${diff.inHours}小时前';
    if (diff.inDays < 7) return '${diff.inDays}天前';
    return '${ts.month}-${ts.day} ${ts.hour.toString().padLeft(2, '0')}:${ts.minute.toString().padLeft(2, '0')}';
  }
}
