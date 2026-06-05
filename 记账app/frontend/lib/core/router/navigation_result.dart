import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

/// A declarative result used by routes to signal follow-up actions.
class RouteDecision {
  const RouteDecision({required this.shouldRefresh, this.payload});

  /// Whether the caller should refresh its data.
  final bool shouldRefresh;

  /// Optional payload returned by the route.
  final Object? payload;

  static const RouteDecision success = RouteDecision(shouldRefresh: true);
  static const RouteDecision noop = RouteDecision(shouldRefresh: false);
}

/// Evaluates whether a route result indicates the caller should refresh.
class RouteResultEvaluator {
  const RouteResultEvaluator._();

  static bool shouldRefresh(dynamic result) {
    if (result == null) {
      return false;
    }
    if (result is bool) {
      return result;
    }
    if (result is RouteDecision) {
      return result.shouldRefresh;
    }
    return true;
  }
}

/// Adds sugar around [GoRouter] push/pop flows for awaiting results.
mixin RouteResultMixin<T extends StatefulWidget> on State<T> {
  Future<void> pushForResult<R>({
    required String location,
    Future<void> Function()? onRefresh,
    bool Function(R? result)? predicate,
  }) async {
    final result = await context.push<R>(location);
    if (!mounted || onRefresh == null) {
      return;
    }
    final shouldRefresh =
        predicate?.call(result) ?? RouteResultEvaluator.shouldRefresh(result);
    if (shouldRefresh) {
      await onRefresh();
    }
  }

  void popWithSuccess([Object? result]) {
    if (!mounted) {
      return;
    }
    final value = result ?? true;
    context.pop(value);
  }
}
