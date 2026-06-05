import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../app/routes.dart';
import '../router/app_router.dart';

/// 导航服务类
/// 提供统一的导航方法，支持深层导航
class NavigationService {
  NavigationService._();

  /// 导航到指定路由
  static void goTo(String location, {Map<String, String>? queryParameters}) {
    String path = location;
    if (queryParameters != null && queryParameters.isNotEmpty) {
      final queryString = queryParameters.entries
          .map((e) => '${e.key}=${e.value}')
          .join('&');
      path = '$location?$queryString';
    }
    final appRouter = AppRouter.router;
    appRouter.go(path);
  }

  /// 导航到指定路由并等待结果
  static Future<T?> pushNamed<T extends Object?>(
    String name, {
    Map<String, String>? pathParameters,
    Map<String, String>? queryParameters,
    Object? extra,
  }) {
    final appRouter = AppRouter.router;
    return appRouter.pushNamed<T>(
      name,
      pathParameters: pathParameters ?? {},
      queryParameters: queryParameters ?? {},
      extra: extra,
    );
  }

  /// 导航到指定路由并等待结果（使用字符串路径）
  static Future<T?> pushNamedWithPath<T extends Object?>(
    String path, {
    Map<String, String>? queryParameters,
    Object? extra,
  }) {
    String fullPath = path;
    if (queryParameters != null && queryParameters.isNotEmpty) {
      final queryString = queryParameters.entries
          .map((e) => '${e.key}=${e.value}')
          .join('&');
      fullPath = '$path?$queryString';
    }
    final appRouter = AppRouter.router;
    return appRouter.push<T>(fullPath, extra: extra);
  }

  /// 导航返回
  static void goBack(BuildContext context, [dynamic result]) {
    context.pop(result);
  }

  /// 导航到交易列表页面
  static void goToTransactions(BuildContext context) {
    goTo(AppRoutes.transactions);
  }

  /// 导航到交易详情页面
  static Future<void> goToTransactionDetail(
    BuildContext context,
    dynamic transactionId,
  ) {
    return pushNamedWithPath(
      AppRoutes.transactionDetail,
      queryParameters: {'id': transactionId.toString()},
    );
  }

  /// 导航到交易表单页面
  static Future<void> goToTransactionForm(
    BuildContext context,
    dynamic transactionId,
  ) {
    return pushNamedWithPath(
      AppRoutes.transactionForm,
      queryParameters: transactionId != null
          ? {'id': transactionId.toString()}
          : null,
    );
  }

  /// 导航到预算列表页面
  static void goToBudgets(BuildContext context) {
    goTo(AppRoutes.budgets);
  }

  /// 导航到预算表单页面
  static Future<void> goToBudgetForm(BuildContext context, dynamic budgetId) {
    return pushNamedWithPath(
      AppRoutes.budgetForm,
      queryParameters: budgetId != null ? {'id': budgetId.toString()} : null,
    );
  }

  /// 导航到储蓄目标列表页面
  static void goToSavingGoals(BuildContext context) {
    goTo(AppRoutes.savingGoals);
  }

  /// 导航到储蓄目标详情页面（深层导航）
  static Future<void> goToSavingGoalDetail(
    BuildContext context,
    dynamic goalId,
  ) {
    // 使用深层路由路径格式: /saving-goals/detail/:id
    return pushNamedWithPath('${AppRoutes.savingGoals}/detail/$goalId');
  }

  /// 导航到储蓄目标表单页面（深层路由）
  static Future<void> goToSavingGoalForm(BuildContext context, dynamic goalId) {
    return pushNamedWithPath(
      '${AppRoutes.savingGoals}/form/$goalId',
      queryParameters: goalId != null ? null : null,
    );
  }

  /// 导航到储蓄记录列表页面
  static Future<void> goToSavingRecords(BuildContext context, dynamic goalId) {
    return pushNamedWithPath(
      '${AppRoutes.savingGoals}/records',
      queryParameters: {'goalId': goalId.toString()},
    );
  }

  /// 导航到储蓄记录表单页面（深层路由）
  static Future<void> goToSavingRecordForm(
    BuildContext context,
    dynamic goalId, {
    dynamic recordId,
  }) {
    // 使用深层路由格式
    final basePath = '${AppRoutes.savingGoals}/records';
    if (recordId != null) {
      // 编辑模式 - 深层路由
      return pushNamedWithPath('$basePath/form/$recordId');
    } else {
      // 新增模式 - 深层路由
      return pushNamedWithPath(
        '$basePath/form',
        queryParameters: {'goalId': goalId.toString()},
      );
    }
  }

  /// 导航到储蓄记录详情页面
  static Future<void> goToSavingRecordDetail(
    BuildContext context,
    dynamic recordId,
  ) {
    return pushNamedWithPath(
      AppRoutes.savingRecordDetail,
      queryParameters: {'id': recordId.toString()},
    );
  }

  /// 导航到分类管理页面
  static Future<void> goToCategoryManagement(BuildContext context) {
    return pushNamedWithPath(AppRoutes.categoryManagement);
  }

  /// 导航到首页
  static void goToHome(BuildContext context) {
    goTo(AppRoutes.home);
  }

  /// 导航到统计页面
  static void goToStatistics(BuildContext context) {
    goTo(AppRoutes.statistics);
  }

  /// 导航到设置页面
  static void goToSettings(BuildContext context) {
    goTo(AppRoutes.settings);
  }

  /// 导航到计划页面
  static void goToPlan(BuildContext context) {
    goTo(AppRoutes.plan);
  }
}
