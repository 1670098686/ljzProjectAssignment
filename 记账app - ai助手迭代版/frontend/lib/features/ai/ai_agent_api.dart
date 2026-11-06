/// AI 助手模块 - 聊天 API 服务层
///
/// 调用链路:
///   Flutter → Python AI Agent (:8082) 直接调用（后端 AI Service）
library;

import 'dart:convert';

import 'package:http/http.dart' as http;

/// AI 聊天消息（UI 层数据模型）
class AiMessage {
  final String id;
  final String content;
  final bool isUser;
  final DateTime timestamp;

  const AiMessage({
    required this.id,
    required this.content,
    required this.isUser,
    required this.timestamp,
  });

  factory AiMessage.user(String content, {DateTime? ts}) => AiMessage(
        id: 'u_${(ts ?? DateTime.now()).millisecondsSinceEpoch}',
        content: content,
        isUser: true,
        timestamp: ts ?? DateTime.now(),
      );

  factory AiMessage.ai(String content, {DateTime? ts}) => AiMessage(
        id: 'a_${(ts ?? DateTime.now()).millisecondsSinceEpoch}',
        content: content,
        isUser: false,
        timestamp: ts ?? DateTime.now(),
      );

  /// 从后端会话历史消息 JSON 解析
  factory AiMessage.fromHistory(Map<String, dynamic> json) {
    final role = json['role'] as String? ?? 'ai';
    final isUser = role == 'user';
    final createdAt = json['created_at'] as String?;
    final ts = createdAt != null
        ? DateTime.tryParse(createdAt) ?? DateTime.now()
        : DateTime.now();
    return AiMessage(
      id: '${isUser ? 'u' : 'a'}_${ts.millisecondsSinceEpoch}',
      content: json['content'] as String? ?? '',
      isUser: isUser,
      timestamp: ts,
    );
  }
}

/// 会话摘要（用于侧边栏列表）
class AiSessionSummary {
  final String id;
  final String title;
  final String lastMessage;
  final DateTime updatedAt;
  final int messageCount;

  const AiSessionSummary({
    required this.id,
    required this.title,
    required this.lastMessage,
    required this.updatedAt,
    this.messageCount = 0,
  });

  factory AiSessionSummary.fromJson(Map<String, dynamic> json) {
    final updatedAt = DateTime.tryParse(json['updated_at'] ?? '') ?? DateTime.now();
    return AiSessionSummary(
      id: json['id'] as String? ?? '',
      title: (json['title'] as String?)?.isEmpty == true
          ? (json['last_message'] ?? '新会话')
          : (json['title'] as String? ?? '新会话'),
      lastMessage: json['last_message'] as String? ?? '',
      updatedAt: updatedAt,
      messageCount: (json['message_count'] as num?)?.toInt() ?? 0,
    );
  }
}

/// AI 聊天服务 —— 同时负责聊天 + 会话管理
class AiAgentApi {
  final String baseUrl;
  final String userId;

  AiAgentApi({required this.baseUrl, required this.userId});

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'X-User-Id': userId,
      };

  // ====================================================================
  // 聊天接口
  // ====================================================================

  /// 同步聊天 —— 返回 AI 回复文本 + 实际生效的 session_id
  Future<(String reply, String sessionId)> chat(
    String message, {
    String? sessionId,
  }) async {
    final url = Uri.parse('$baseUrl/api/v1/ai/chat');
    final response = await http.post(
      url,
      headers: _headers,
      body: jsonEncode({
        'user_id': userId,
        'message': message,
        if (sessionId != null && sessionId.isNotEmpty) 'session_id': sessionId,
      }),
    );

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      final data = body['data'];
      final reply = data['reply'] as String? ?? '（空回复）';
      final effectiveSessionId = data['session_id'] as String? ??
          sessionId ??
          'user_${userId}_default';
      return (reply, effectiveSessionId);
    } else {
      throw Exception('AI 请求失败: ${response.statusCode} ${response.body}');
    }
  }

  /// 带 SQLite 上下文的聊天 —— 把完整本地数据打包发给 Python AI
  Future<({String reply, String sessionId})> chatWithContext(
    Map<String, dynamic> payload,
  ) async {
    final url = Uri.parse('$baseUrl/api/v1/ai/chat');
    final response = await http.post(
      url,
      headers: _headers,
      body: jsonEncode(payload),
    );

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      final data = body['data'];
      return (
        reply: data['reply'] as String? ?? '（空回复）',
        sessionId: data['session_id'] as String? ?? 'user_${userId}_default',
      );
    } else {
      throw Exception('AI 请求失败: ${response.statusCode} ${response.body}');
    }
  }

  // ====================================================================
  // 会话历史管理
  // ====================================================================

  /// 列出当前用户的所有会话（按 updated_at 倒序）
  Future<List<AiSessionSummary>> listSessions({int limit = 100}) async {
    final url = Uri.parse('$baseUrl/api/v1/ai/sessions?user_id=$userId&limit=$limit');
    final response = await http.get(url, headers: _headers);

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      final sessions = body['sessions'] as List? ?? [];
      return sessions
          .map((e) => AiSessionSummary.fromJson(e as Map<String, dynamic>))
          .toList();
    } else {
      throw Exception('获取会话列表失败: ${response.statusCode}');
    }
  }

  /// 获取某个会话的完整消息历史
  Future<List<AiMessage>> getSessionMessages(String sessionId) async {
    final url = Uri.parse(
        '$baseUrl/api/v1/ai/sessions/${Uri.encodeComponent(sessionId)}?user_id=$userId');
    final response = await http.get(url, headers: _headers);

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      final msgs = body['messages'] as List? ?? [];
      return msgs
          .map((e) => AiMessage.fromHistory(e as Map<String, dynamic>))
          .toList();
    } else {
      throw Exception('获取会话详情失败: ${response.statusCode}');
    }
  }

  /// 删除一个会话
  Future<bool> deleteSession(String sessionId) async {
    final url = Uri.parse(
        '$baseUrl/api/v1/ai/sessions/${Uri.encodeComponent(sessionId)}?user_id=$userId');
    final response = await http.delete(url, headers: _headers);

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      return body['ok'] == true;
    } else {
      return false;
    }
  }

  // ====================================================================
  // 健康检查
  // ====================================================================
  Future<bool> healthCheck() async {
    try {
      final url = Uri.parse('$baseUrl/api/v1/ai/health');
      final resp = await http.get(url);
      return resp.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
