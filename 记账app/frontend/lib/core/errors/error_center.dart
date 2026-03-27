import 'package:flutter/material.dart';

/// 错误事件类型
enum ErrorSeverity {
  info,    // 信息
  warning, // 警告
  error,   // 错误
  critical, // 严重错误
  fatal,   // 致命错误
}

/// 重试回调类型
typedef RetryCallback = Future<void> Function();

/// 错误事件模型
class ErrorEvent {
  final String message;
  final String? description;
  final RetryCallback? retry;
  final String actionLabel;
  final ErrorSeverity severity;
  
  ErrorEvent({
    required this.message,
    this.description,
    this.retry,
    this.actionLabel = '重试',
    this.severity = ErrorSeverity.warning,
  });
}

/// 错误中心 - 统一管理应用错误
class ErrorCenter extends ChangeNotifier {
  ErrorEvent? _current;
  final List<ErrorEvent> _history = [];
  static const int _maxHistorySize = 100;

  /// 获取当前错误事件
  ErrorEvent? get current => _current;

  /// 获取错误历史记录
  List<ErrorEvent> get history => _history;

  /// 显示错误
  void showError({
    required String message,
    String? description,
    RetryCallback? retry,
    String actionLabel = '重试',
  }) {
    _current = ErrorEvent(
      message: message,
      description: description,
      retry: retry,
      actionLabel: actionLabel,
      severity: ErrorSeverity.warning,
    );
    notifyListeners();
  }

  /// 显示严重错误
  void showCriticalError({
    required String message,
    String? description,
    RetryCallback? retry,
    String actionLabel = '重试',
  }) {
    _current = ErrorEvent(
      message: message,
      description: description,
      retry: retry,
      actionLabel: actionLabel,
      severity: ErrorSeverity.error,
    );
    notifyListeners();
  }

  /// 显示信息
  void showInfo({
    required String message,
    String? description,
  }) {
    _current = ErrorEvent(
      message: message,
      description: description,
      severity: ErrorSeverity.info,
    );
    notifyListeners();
  }

  /// 消费当前错误事件
  void consume(ErrorEvent event) {
    if (_current == event) {
      _current = null;
      _history.add(event);
      
      // 限制历史记录大小
      if (_history.length > _maxHistorySize) {
        _history.removeAt(0);
      }
      
      notifyListeners();
    }
  }

  /// 清除当前错误
  void clearCurrent() {
    _current = null;
    notifyListeners();
  }

  /// 清除所有错误历史
  void clearHistory() {
    _history.clear();
    notifyListeners();
  }

  /// 检查是否有当前错误
  bool get hasCurrentError => _current != null;

  /// 检查是否有错误历史
  bool get hasErrorHistory => _history.isNotEmpty;
}