import 'package:flutter/material.dart';

/// 通用异步提交封装，统一处理 mounted 检查与错误展示。
Future<T?> safeSubmit<T>({
  required BuildContext context,
  required Future<T> Function() action,
  VoidCallback? onSuccess,
  void Function(Object error, StackTrace stackTrace)? onError,
  bool popOnSuccess = false,
}) async {
  try {
    final result = await action();

    if (!context.mounted) return result;

    if (onSuccess != null) {
      onSuccess();
    } else if (popOnSuccess) {
      Navigator.of(context).pop(true);
    }

    return result;
  } catch (e, st) {
    if (onError != null) {
      onError(e, st);
    } else {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('操作失败：$e'), backgroundColor: Colors.red));
      }
    }
    return null;
  }
}
