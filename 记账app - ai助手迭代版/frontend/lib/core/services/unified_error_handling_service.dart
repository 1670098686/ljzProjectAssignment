import 'dart:developer';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import '../network/app_exception.dart';

/// 错误严重程度枚举
enum ErrorSeverity {
  info, // 信息级别
  warning, // 警告级别
  error, // 错误级别
  fatal, // 致命错误级别
}

/// 统一错误码定义
class ErrorCodes {
  // 网络相关错误
  static const String networkTimeout = 'NETWORK_TIMEOUT';
  static const String networkUnavailable = 'NETWORK_UNAVAILABLE';
  static const String serverError = 'SERVER_ERROR';

  // API相关错误
  static const String apiBadRequest = 'API_BAD_REQUEST';
  static const String apiUnauthorized = 'API_UNAUTHORIZED';
  static const String apiForbidden = 'API_FORBIDDEN';
  static const String apiNotFound = 'API_NOT_FOUND';
  static const String apiValidation = 'API_VALIDATION';
  static const String apiRateLimit = 'API_RATE_LIMIT';

  // 数据库相关错误
  static const String databaseConstraint = 'DATABASE_CONSTRAINT';
  static const String databaseNotFound = 'DATABASE_NOT_FOUND';
  static const String databaseIO = 'DATABASE_IO';

  // 业务逻辑错误
  static const String businessValidation = 'BUSINESS_VALIDATION';
  static const String businessLogic = 'BUSINESS_LOGIC';

  // 系统错误
  static const String systemUnknown = 'SYSTEM_UNKNOWN';
}

/// 错误事件封装类
class ErrorEvent {
  final String message;
  final ErrorSeverity severity;
  final StackTrace? stackTrace;
  final DateTime timestamp;
  final String? errorCode;
  final Map<String, dynamic>? metadata;
  final String? source;

  ErrorEvent({
    required this.message,
    this.severity = ErrorSeverity.error,
    this.stackTrace,
    this.errorCode,
    this.metadata,
    this.source,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  @override
  String toString() {
    return '[$severity] ${errorCode != null ? '[$errorCode] ' : ''}$message';
  }

  /// 转换为JSON格式，便于存储和传输
  Map<String, dynamic> toJson() {
    return {
      'message': message,
      'severity': severity.name,
      'timestamp': timestamp.toIso8601String(),
      'errorCode': errorCode,
      'metadata': metadata,
      'source': source,
    };
  }
}

/// 统一错误处理服务
/// 合并了原有的ErrorHandlingService和EnhancedErrorHandlingService功能
class UnifiedErrorHandlingService {
  static final UnifiedErrorHandlingService _instance =
      UnifiedErrorHandlingService._internal();

  factory UnifiedErrorHandlingService() => _instance;

  UnifiedErrorHandlingService._internal();

  // 错误监听器列表
  final List<Function(ErrorEvent)> _errorListeners = [];
  // 错误历史记录
  final List<ErrorEvent> _errorHistory = [];
  // 最大历史记录数
  static const int _maxHistorySize = 100;

  /// 添加错误监听器
  void addErrorListener(Function(ErrorEvent) listener) {
    _errorListeners.add(listener);
  }

  /// 移除错误监听器
  void removeErrorListener(Function(ErrorEvent) listener) {
    _errorListeners.remove(listener);
  }

  /// 通知所有错误监听器
  void _notifyListeners(ErrorEvent errorEvent) {
    for (final listener in _errorListeners) {
      try {
        listener(errorEvent);
      } catch (e) {
        // 防止监听器自身出错导致循环
        log('错误监听器执行失败: $e');
      }
    }
  }

  /// 显示错误提示条
  void showErrorSnackBar({
    BuildContext? context,
    required String message,
    Duration duration = const Duration(seconds: 4),
    VoidCallback? onAction,
    String actionLabel = '重试',
  }) {
    if (context == null || !context.mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: duration,
        action: SnackBarAction(
          label: onAction != null ? actionLabel : '关闭',
          textColor: Colors.white,
          onPressed: () {
            ScaffoldMessenger.of(context).hideCurrentSnackBar();
            onAction?.call();
          },
        ),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  /// 显示成功提示条
  void showSuccessSnackBar({
    BuildContext? context,
    required String message,
    Duration duration = const Duration(seconds: 3),
  }) {
    if (context == null || !context.mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
        duration: duration,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  /// 显示警告提示条
  void showWarningSnackBar({
    BuildContext? context,
    required String message,
    Duration duration = const Duration(seconds: 4),
  }) {
    if (context == null || !context.mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.orange,
        duration: duration,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  /// 显示信息提示条
  void showInfoSnackBar({
    BuildContext? context,
    required String message,
    Duration duration = const Duration(seconds: 3),
  }) {
    if (context == null || !context.mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.blue,
        duration: duration,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  /// 处理API错误
  void handleApiError({
    BuildContext? context,
    required dynamic error,
    String? customMessage,
    VoidCallback? onRetry,
    String? errorCode,
    Map<String, dynamic>? metadata,
  }) {
    String errorMessage = _getErrorMessage(error);

    if (customMessage != null) {
      errorMessage = '$customMessage: $errorMessage';
    }

    final errorEvent = ErrorEvent(
      message: errorMessage,
      severity: ErrorSeverity.error,
      stackTrace: error is Error ? error.stackTrace : null,
      errorCode: errorCode ?? 'API_ERROR',
      metadata: metadata,
      source: 'API',
    );

    showErrorSnackBar(
      context: context,
      message: errorMessage,
      onAction: onRetry,
    );

    // 记录错误日志并通知监听器
    _logError(errorEvent);
    _notifyListeners(errorEvent);

    // 触发状态同步
    _triggerStateSyncAfterError(errorEvent);
  }

  /// 处理数据库错误
  void handleDatabaseError({
    BuildContext? context,
    required dynamic error,
    String operation = '数据库操作',
    VoidCallback? onRetry,
    Map<String, dynamic>? metadata,
  }) {
    String errorMessage = '$operation失败: ${_getErrorMessage(error)}';

    final errorEvent = ErrorEvent(
      message: errorMessage,
      severity: ErrorSeverity.error,
      stackTrace: error is Error ? error.stackTrace : null,
      errorCode: 'DATABASE_ERROR',
      metadata: metadata,
      source: 'Database',
    );

    showErrorSnackBar(
      context: context,
      message: errorMessage,
      onAction: onRetry,
    );

    // 记录错误日志并通知监听器
    _logError(errorEvent);
    _notifyListeners(errorEvent);

    // 触发状态同步
    _triggerStateSyncAfterError(errorEvent);
  }

  /// 处理网络错误
  void handleNetworkError({
    BuildContext? context,
    required dynamic error,
    String? customMessage,
    VoidCallback? onRetry,
    Map<String, dynamic>? metadata,
  }) {
    String errorMessage = '网络连接失败';

    if (customMessage != null) {
      errorMessage = '$customMessage: $errorMessage';
    }

    final errorEvent = ErrorEvent(
      message: errorMessage,
      severity: ErrorSeverity.warning,
      stackTrace: error is Error ? error.stackTrace : null,
      errorCode: 'NETWORK_ERROR',
      metadata: metadata,
      source: 'Network',
    );

    showErrorSnackBar(
      context: context,
      message: errorMessage,
      onAction: onRetry,
    );

    // 记录错误日志并通知监听器
    _logError(errorEvent);
    _notifyListeners(errorEvent);

    // 触发状态同步
    _triggerStateSyncAfterError(errorEvent);
  }

  /// 处理验证错误
  void handleValidationError({
    BuildContext? context,
    required String message,
    String? fieldName,
    VoidCallback? onRetry,
  }) {
    final errorMessage = fieldName != null ? '$fieldName: $message' : message;

    final errorEvent = ErrorEvent(
      message: errorMessage,
      severity: ErrorSeverity.warning,
      errorCode: 'VALIDATION_ERROR',
      metadata: {'fieldName': fieldName},
      source: 'Validation',
    );

    showWarningSnackBar(context: context, message: errorMessage);

    // 记录错误日志并通知监听器
    _logError(errorEvent);
    _notifyListeners(errorEvent);
  }

  /// 处理业务逻辑错误
  void handleBusinessError({
    BuildContext? context,
    required String message,
    String? errorCode,
    VoidCallback? onRetry,
    Map<String, dynamic>? metadata,
  }) {
    final errorEvent = ErrorEvent(
      message: message,
      severity: ErrorSeverity.warning,
      errorCode: errorCode ?? 'BUSINESS_ERROR',
      metadata: metadata,
      source: 'Business',
    );

    showWarningSnackBar(context: context, message: message);

    // 记录错误日志并通知监听器
    _logError(errorEvent);
    _notifyListeners(errorEvent);
  }

  /// 显示加载对话框
  void showLoadingDialog({
    required BuildContext context,
    String message = '加载中...',
    bool barrierDismissible = false,
  }) {
    if (!context.mounted) return;

    showDialog(
      context: context,
      barrierDismissible: barrierDismissible,
      builder: (BuildContext context) {
        return Dialog(
          backgroundColor: Colors.transparent,
          elevation: 0,
          child: Center(
            child: Container(
              padding: EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(width: 16),
                  Text(message),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  /// 隐藏加载对话框
  void hideLoadingDialog(BuildContext context) {
    if (!context.mounted) return;
    Navigator.of(context, rootNavigator: true).pop();
  }

  /// 通用错误处理（不需要BuildContext）
  void handleError(
    String operation,
    dynamic error, {
    String? customMessage,
    String? errorCode,
    Map<String, dynamic>? metadata,
  }) {
    String errorMessage = _getErrorMessage(error);

    if (customMessage != null) {
      errorMessage = '$customMessage: $errorMessage';
    }

    final errorEvent = ErrorEvent(
      message: errorMessage,
      severity: ErrorSeverity.error,
      stackTrace: error is Error ? error.stackTrace : null,
      errorCode: errorCode ?? 'SYSTEM_ERROR',
      metadata: metadata,
      source: 'System',
    );

    // 记录错误日志并通知监听器
    _logError(errorEvent);
    _notifyListeners(errorEvent);

    // 触发状态同步
    _triggerStateSyncAfterError(errorEvent);
  }

  /// 显示确认对话框
  Future<bool> showConfirmDialog({
    required BuildContext context,
    required String title,
    required String message,
    String confirmText = '确认',
    String cancelText = '取消',
    bool destructive = false,
  }) async {
    if (!context.mounted) return false;

    final result = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text(cancelText),
            ),
            ElevatedButton(
              style: destructive
                  ? ElevatedButton.styleFrom(backgroundColor: Colors.red)
                  : null,
              onPressed: () => Navigator.of(context).pop(true),
              child: Text(confirmText),
            ),
          ],
        );
      },
    );

    return result ?? false;
  }

  /// 获取错误消息
  String _getErrorMessage(dynamic error) {
    if (error is AppException) {
      return error.message;
    } else if (error is String) {
      return error;
    } else if (error is DioException) {
      return _getDioErrorMessage(error);
    } else {
      return error.toString();
    }
  }

  /// 获取Dio错误消息
  String _getDioErrorMessage(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return '网络连接超时，请检查网络设置';
      case DioExceptionType.badResponse:
        final statusCode = error.response?.statusCode;
        if (statusCode == null) {
          return '服务器错误';
        }

        if (statusCode == 400) {
          return '请求参数错误';
        } else if (statusCode == 401) {
          return '身份验证失效，请重新登录';
        } else if (statusCode == 403) {
          return '权限不足';
        } else if (statusCode == 404) {
          return '资源不存在或已删除';
        } else if (statusCode == 429) {
          return '请求频率超限，请稍后重试';
        } else if (statusCode >= 500) {
          return '服务器繁忙，请稍后重试';
        }
        return '服务器错误: $statusCode';
      case DioExceptionType.cancel:
        return '请求已取消';
      case DioExceptionType.unknown:
        return '网络连接失败，请检查网络设置';
      default:
        return '网络连接失败，请重试';
    }
  }

  /// 记录错误日志
  void _logError(ErrorEvent errorEvent) {
    // 添加到历史记录
    _errorHistory.add(errorEvent);

    // 限制历史记录大小
    if (_errorHistory.length > _maxHistorySize) {
      _errorHistory.removeAt(0);
    }

    // 打印到控制台（开发环境）
    log('错误记录: ${errorEvent.toString()}');

    // 可以在这里添加错误日志上报逻辑
    // _reportErrorToServer(errorEvent);
  }

  /// 触发状态同步
  void _triggerStateSyncAfterError(ErrorEvent errorEvent) {
    // 这里可以触发状态同步管理器进行数据同步
    try {
      // StateSyncManager.getInstance().syncAfterError(errorEvent);
    } catch (e) {
      log('状态同步触发失败: $e');
    }
  }

  /// 获取错误历史记录
  List<ErrorEvent> getErrorHistory() {
    return List.from(_errorHistory);
  }

  /// 清除错误历史记录
  void clearErrorHistory() {
    _errorHistory.clear();
  }

  /// 根据后端错误码获取前端错误码
  static String getFrontendErrorCode(int backendCode) {
    switch (backendCode) {
      case 400:
        return ErrorCodes.apiBadRequest;
      case 401:
        return ErrorCodes.apiUnauthorized;
      case 403:
        return ErrorCodes.apiForbidden;
      case 404:
        return ErrorCodes.apiNotFound;
      case 429:
        return ErrorCodes.apiRateLimit;
      case 500:
      case 502:
      case 503:
        return ErrorCodes.serverError;
      default:
        return ErrorCodes.systemUnknown;
    }
  }

  /// 根据后端错误码获取用户友好消息
  static String getUserFriendlyMessage(
    int backendCode,
    String? backendMessage,
  ) {
    switch (backendCode) {
      case 400:
        return backendMessage ?? '请求参数错误';
      case 401:
        return '身份验证失效，请重新登录';
      case 403:
        return '权限不足，无法访问该资源';
      case 404:
        return backendMessage ?? '资源不存在或已删除';
      case 429:
        return '请求频率超限，请稍后重试';
      case 500:
        return '服务器内部错误，请稍后重试';
      case 502:
        return '网关错误，请稍后重试';
      case 503:
        return '服务暂时不可用，请稍后重试';
      default:
        return backendMessage ?? '系统错误，请重试';
    }
  }
}

// 兼容性别名，便于逐步迁移
typedef ErrorHandlingService = UnifiedErrorHandlingService;
