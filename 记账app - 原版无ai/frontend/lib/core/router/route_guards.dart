import 'package:go_router/go_router.dart';

/// Shared helpers for validating router parameters.
class RouteGuards {
  const RouteGuards._();

  static int? parseOptionalInt(
    GoRouterState state, {
    required String key,
    bool fromPath = false,
  }) {
    final value = fromPath
        ? state.pathParameters[key]
        : state.uri.queryParameters[key];
    if (value == null) {
      return null;
    }
    final parsed = int.tryParse(value);
    if (parsed == null) {
      throw GoException('Invalid "$key" value: $value');
    }
    return parsed;
  }

  static int requireInt(
    GoRouterState state, {
    required String key,
    bool fromPath = false,
  }) {
    final result = parseOptionalInt(state, key: key, fromPath: fromPath);
    if (result == null) {
      throw GoException('Missing required "$key" parameter.');
    }
    return result;
  }
}
