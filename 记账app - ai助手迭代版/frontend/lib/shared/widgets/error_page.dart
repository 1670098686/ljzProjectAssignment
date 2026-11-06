import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../app/routes.dart';
import '../../core/services/unified_error_handling_service.dart';

/// 错误页面组件
/// 提供友好的错误显示界面，支持重试和返回首页功能
class ErrorPage extends StatelessWidget {
  final String errorMessage;
  final String? errorDescription;
  final VoidCallback? onRetry;
  final VoidCallback? onBackToHome;
  final ErrorSeverity severity;
  final IconData? customIcon;

  const ErrorPage({
    super.key,
    required this.errorMessage,
    this.errorDescription,
    this.onRetry,
    this.onBackToHome,
    this.severity = ErrorSeverity.error,
    this.customIcon,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // 错误图标
              _buildErrorIcon(theme),
              const SizedBox(height: 24),

              // 错误标题
              Text(
                _getErrorTitle(),
                style: theme.textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: _getTitleColor(theme),
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),

              // 错误消息
              Text(
                errorMessage,
                style: theme.textTheme.bodyLarge,
                textAlign: TextAlign.center,
              ),

              // 错误描述（如果有）
              if (errorDescription != null) ...[
                const SizedBox(height: 8),
                Text(
                  errorDescription!,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],

              const SizedBox(height: 32),

              // 操作按钮
              _buildActionButtons(context),
            ],
          ),
        ),
      ),
    );
  }

  /// 构建错误图标
  Widget _buildErrorIcon(ThemeData theme) {
    IconData iconData = customIcon ?? _getDefaultIcon();
    Color iconColor = _getIconColor(theme);

    return Container(
      width: 80,
      height: 80,
      decoration: BoxDecoration(
        color: iconColor.withAlpha(26),
        shape: BoxShape.circle,
      ),
      child: Icon(iconData, size: 40, color: iconColor),
    );
  }

  /// 获取错误标题
  String _getErrorTitle() {
    switch (severity) {
      case ErrorSeverity.info:
        return '提示';
      case ErrorSeverity.warning:
        return '警告';
      case ErrorSeverity.error:
        return '发生错误';
      case ErrorSeverity.fatal:
        return '严重错误';
    }
  }

  /// 获取默认图标
  IconData _getDefaultIcon() {
    switch (severity) {
      case ErrorSeverity.info:
        return Icons.info_outline;
      case ErrorSeverity.warning:
        return Icons.warning_outlined;
      case ErrorSeverity.error:
        return Icons.error_outline;
      case ErrorSeverity.fatal:
        return Icons.error_outline;
    }
  }

  /// 获取标题颜色
  Color _getTitleColor(ThemeData theme) {
    switch (severity) {
      case ErrorSeverity.info:
        return Colors.blue;
      case ErrorSeverity.warning:
        return Colors.orange;
      case ErrorSeverity.error:
        return theme.colorScheme.error;
      case ErrorSeverity.fatal:
        return theme.colorScheme.error;
    }
  }

  /// 获取图标颜色
  Color _getIconColor(ThemeData theme) {
    switch (severity) {
      case ErrorSeverity.info:
        return Colors.blue;
      case ErrorSeverity.warning:
        return Colors.orange;
      case ErrorSeverity.error:
        return theme.colorScheme.error;
      case ErrorSeverity.fatal:
        return theme.colorScheme.error;
    }
  }

  /// 构建操作按钮
  Widget _buildActionButtons(BuildContext context) {
    return Column(
      children: [
        // 重试按钮（如果有回调）
        if (onRetry != null) ...[
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                onRetry!();
              },
              child: const Text('重试'),
            ),
          ),
          const SizedBox(height: 12),
        ],

        // 返回首页按钮
        SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: () {
              if (onBackToHome != null) {
                onBackToHome!();
              } else {
                // 默认返回首页
                context.go(AppRoutes.home);
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.primary,
              foregroundColor: Theme.of(context).colorScheme.onPrimary,
            ),
            child: const Text('返回首页'),
          ),
        ),
      ],
    );
  }
}

/// 错误页面路由包装器
class ErrorPageRoute extends StatelessWidget {
  final Widget child;
  final String? errorMessage;
  final VoidCallback? onError;

  const ErrorPageRoute({
    super.key,
    required this.child,
    this.errorMessage,
    this.onError,
  });

  @override
  Widget build(BuildContext context) {
    return _ErrorBoundary(
      errorMessage: errorMessage,
      onError: onError,
      child: child,
    );
  }
}

/// 错误边界组件
class _ErrorBoundary extends StatefulWidget {
  final Widget child;
  final String? errorMessage;
  final VoidCallback? onError;

  const _ErrorBoundary({required this.child, this.errorMessage, this.onError});

  @override
  State<_ErrorBoundary> createState() => _ErrorBoundaryState();
}

class _ErrorBoundaryState extends State<_ErrorBoundary> {
  bool _hasError = false;
  String _errorMessage = '';

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _checkForErrors();
  }

  void _checkForErrors() {
    try {
      // 尝试执行子组件的构建
      widget.child;
      setState(() {
        _hasError = false;
      });
    } catch (e) {
      setState(() {
        _hasError = true;
        _errorMessage = widget.errorMessage ?? '页面加载失败';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_hasError) {
      return ErrorPage(
        errorMessage: _errorMessage,
        onRetry: () {
          setState(() {
            _hasError = false;
            _errorMessage = '';
          });
          if (widget.onError != null) {
            widget.onError!();
          }
        },
      );
    }

    return widget.child;
  }
}

/// 静态错误页面创建方法
class ErrorPageFactory {
  /// 创建网络错误页面
  static Widget createNetworkErrorPage({
    VoidCallback? onRetry,
    VoidCallback? onBackToHome,
  }) {
    return ErrorPage(
      errorMessage: '网络连接失败',
      errorDescription: '请检查您的网络设置，然后重试',
      onRetry: onRetry,
      onBackToHome: onBackToHome,
      severity: ErrorSeverity.warning,
    );
  }

  /// 创建服务器错误页面
  static Widget createServerErrorPage({
    VoidCallback? onRetry,
    VoidCallback? onBackToHome,
  }) {
    return ErrorPage(
      errorMessage: '服务器错误',
      errorDescription: '服务器暂时无法响应，请稍后重试',
      onRetry: onRetry,
      onBackToHome: onBackToHome,
      severity: ErrorSeverity.error,
    );
  }

  /// 创建数据加载错误页面
  static Widget createDataLoadErrorPage({
    VoidCallback? onRetry,
    VoidCallback? onBackToHome,
  }) {
    return ErrorPage(
      errorMessage: '数据加载失败',
      errorDescription: '无法获取数据，请检查网络连接或稍后重试',
      onRetry: onRetry,
      onBackToHome: onBackToHome,
      severity: ErrorSeverity.warning,
    );
  }

  /// 创建通用错误页面
  static Widget createGeneralErrorPage({
    required String message,
    String? description,
    VoidCallback? onRetry,
    VoidCallback? onBackToHome,
  }) {
    return ErrorPage(
      errorMessage: message,
      errorDescription: description,
      onRetry: onRetry,
      onBackToHome: onBackToHome,
      severity: ErrorSeverity.error,
    );
  }
}
