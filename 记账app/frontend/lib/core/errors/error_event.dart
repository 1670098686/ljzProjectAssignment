import 'package:flutter/foundation.dart';

enum ErrorSeverity { info, warning, critical, fatal }

typedef RetryCallback = Future<void> Function();

@immutable
class ErrorEvent {
  ErrorEvent({
    required this.message,
    this.description,
    this.retry,
    this.actionLabel = '重试',
    this.severity = ErrorSeverity.warning,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  final String message;
  final String? description;
  final RetryCallback? retry;
  final String actionLabel;
  final ErrorSeverity severity;
  final DateTime timestamp;
}
