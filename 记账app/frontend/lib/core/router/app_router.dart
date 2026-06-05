import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';


import '../../../features/user/login_page.dart';
import '../../../features/user/register_page.dart';
import '../../../modules/budgets/budgets_module.dart';
import '../../../modules/home/home_module.dart';
import '../../../modules/plan/plan_module.dart';
import '../../../modules/saving_goals/saving_goals_module.dart';
import '../../../modules/settings/settings_module.dart';
import '../../../modules/statistics/statistics_module.dart';
import '../../../modules/transactions/transactions_module.dart';

import '../../app/routes.dart';
import '../../core/providers/user_provider.dart';
import '../../widgets/layout/responsive_layout.dart';
import '../../widgets/navigation/narrow_bottom_navigation_bar.dart';

final GlobalKey<NavigatorState> _rootNavigatorKey = GlobalKey<NavigatorState>();
final GlobalKey<NavigatorState> _shellNavigatorKey =
    GlobalKey<NavigatorState>();

/// 应用路由配置类
class AppRouter {
  const AppRouter._();

  /// 静态路由配置，避免在主题切换时重新创建路由
  static GoRouter get routerConfig => _routerConfig;

  /// 初始化静态路由配置
  static final GoRouter _routerConfig = GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: AppRoutes.login, // 初始路由设置为登录页面
    routes: [
      // 认证路由
      GoRoute(
        path: AppRoutes.login,
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: AppRoutes.register,
        builder: (context, state) => const RegisterPage(),
      ),
      
      // 底部导航栏路由
      ShellRoute(
        navigatorKey: _shellNavigatorKey,
        builder: (context, state, child) => Scaffold(
          body: child,
          bottomNavigationBar: _buildBottomNavigationBar(context, state),
        ),
        routes: [
          HomeModule.route(),
          TransactionsModule.route(),
          SavingGoalsModule.route(),
          PlanModule.route(),
          BudgetsModule.route(),
          StatisticsModule.route(),
          SettingsModule.route(),
        ],
      ),
      ...TransactionsModule.modalRoutes(_rootNavigatorKey),
      ...SavingGoalsModule.modalRoutes(_rootNavigatorKey),
      ...BudgetsModule.modalRoutes(_rootNavigatorKey),
    ],
    errorBuilder: (context, state) => Scaffold(
      appBar: AppBar(title: const Text('页面未找到')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text('页面未找到', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Text(
              '请求的页面不存在或已被移除',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => context.go(AppRoutes.home),
              child: const Text('返回首页'),
            ),
          ],
        ),
      ),
    ),
  );

  /// 获取底部导航栏的路由列表
  static List<String> get bottomNavigationRoutes =>
      AppRoutes.getBottomNavRoutes();

  /// 检查路由是否有效
  static bool isValidRoute(String route) => AppRoutes.isValidRoute(route);

  /// 获取路由显示名称
  static String getRouteDisplayName(String route) =>
      AppRoutes.getRouteDisplayName(route);

  /// 构建底部导航栏
  static Widget _buildBottomNavigationBar(
    BuildContext context,
    GoRouterState state,
  ) {
    // 根据当前路由获取选中的索引
    int currentIndex = 2; // 默认选中首页
    if (state.matchedLocation.startsWith(AppRoutes.statistics)) {
      currentIndex = 0;
    } else if (state.matchedLocation.startsWith(AppRoutes.transactions)) {
      currentIndex = 1;
    } else if (state.matchedLocation.startsWith(AppRoutes.plan) ||
        state.matchedLocation.startsWith(AppRoutes.savingGoals)) {
      currentIndex = 3; // 计划页包含预算和储蓄目标
    } else if (state.matchedLocation.startsWith(AppRoutes.settings)) {
      currentIndex = 4;
    }

    final routes = [
      AppRoutes.statistics,
      AppRoutes.transactions,
      AppRoutes.home,
      AppRoutes.plan,
      AppRoutes.settings,
    ];

    if (NarrowScreenLayout.isNarrowScreen(context)) {
      return NarrowBottomNavigationBar(
        currentIndex: currentIndex,
        onTap: (index) => context.go(routes[index]),
      );
    }

    return NavigationBar(
      height: 70,
      backgroundColor: Theme.of(context).colorScheme.surface,
      elevation: 8,
      selectedIndex: currentIndex,
      onDestinationSelected: (index) => context.go(routes[index]),
      destinations: const [
        NavigationDestination(icon: Icon(Icons.bar_chart), label: '统计'),
        NavigationDestination(icon: Icon(Icons.list), label: '明细'),
        NavigationDestination(icon: Icon(Icons.home), label: '首页'),
        NavigationDestination(icon: Icon(Icons.calendar_month), label: '计划'),
        NavigationDestination(icon: Icon(Icons.person), label: '我的'),
      ],
    );
  }
}