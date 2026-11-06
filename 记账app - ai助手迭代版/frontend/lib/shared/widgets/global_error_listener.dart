import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/errors/error_center.dart';

class GlobalErrorListener extends StatefulWidget {
  const GlobalErrorListener({super.key, required this.child});

  final Widget child;

  @override
  State<GlobalErrorListener> createState() => _GlobalErrorListenerState();
}

class _GlobalErrorListenerState extends State<GlobalErrorListener> {
  ErrorCenter? _errorCenter;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final nextCenter = context.read<ErrorCenter>();
    if (_errorCenter == nextCenter) {
      return;
    }
    _errorCenter?.removeListener(_handleEvent);
    _errorCenter = nextCenter;
    _errorCenter?.addListener(_handleEvent);
  }

  @override
  void dispose() {
    _errorCenter?.removeListener(_handleEvent);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;

  void _handleEvent() {
    if (!mounted) return;
    final event = _errorCenter?.current;
    if (event == null) return;

    // 检查是否存在 ScaffoldMessenger
    try {
      final messenger = ScaffoldMessenger.of(context);
      messenger.clearSnackBars();
      messenger.showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(event.message),
              if (event.description?.isNotEmpty == true)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    event.description!,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onInverseSurface,
                    ),
                  ),
                ),
            ],
          ),
          action: event.retry != null
              ? SnackBarAction(
                  label: event.actionLabel,
                  onPressed: () {
                    event.retry?.call();
                  },
                )
              : null,
        ),
      );
    } catch (e) {
      // 如果找不到 ScaffoldMessenger，仅打印错误，不影响应用程序运行
      print('无法显示错误提示：$e');
    }

    _errorCenter?.consume(event);
  }
}
