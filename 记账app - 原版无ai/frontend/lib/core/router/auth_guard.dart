import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../providers/user_provider.dart';

/// 认证路由守卫
/// 用于检查用户登录状态和权限
class AuthGuard {
  const AuthGuard._();

  /// 检查用户是否已登录
  /// 如果未登录，重定向到登录页面
  static String? requireAuth(BuildContext context, GoRouterState state) {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    
    if (!userProvider.isLoggedIn) {
      // 保存当前路径，登录后可以返回
      final redirectPath = state.matchedLocation;
      return '/login?redirect=$redirectPath';
    }
    
    return null; // 允许访问
  }

  /// 检查用户权限
  /// 如果权限不足，重定向到无权限页面
  static String? requirePermission(
    BuildContext context, 
    GoRouterState state, 
    String requiredPermission,
  ) {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    
    // 首先检查是否已登录
    final authCheck = requireAuth(context, state);
    if (authCheck != null) {
      return authCheck;
    }
    
    // 检查权限
    if (!userProvider.hasPermission(requiredPermission)) {
      return '/unauthorized';
    }
    
    return null; // 允许访问
  }

  /// 检查是否为访客模式（未登录）
  /// 如果已登录，重定向到首页
  static String? requireGuest(BuildContext context, GoRouterState state) {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    
    if (userProvider.isLoggedIn) {
      return '/home';
    }
    
    return null; // 允许访问
  }

  /// 组合多个守卫条件
  static String? combineGuards(
    BuildContext context, 
    GoRouterState state, 
    List<String? Function(BuildContext, GoRouterState)> guards,
  ) {
    for (final guard in guards) {
      final result = guard(context, state);
      if (result != null) {
        return result;
      }
    }
    
    return null; // 所有守卫都通过
  }
}

/// 路由配置辅助类
class RouteConfig {
  const RouteConfig._();

  /// 需要登录的路由配置
  static Map<String, String> get protectedRoutes => {
    '/profile': '查看个人资料',
    '/settings': '应用设置',
    '/transactions/form': '添加交易记录',
    '/budgets/form': '管理预算',
    '/saving-goals/form': '管理储蓄目标',
    '/saving-goals/detail': '查看储蓄目标详情',
    '/saving-goals/records/form': '添加储蓄记录',
  };

  /// 需要特定权限的路由配置
  static Map<String, String> get permissionRoutes => {
    '/admin': '系统管理',
    '/reports': '查看报表',
    '/export': '数据导出',
    '/import': '数据导入',
  };

  /// 检查路由是否需要认证
  static bool requiresAuth(String path) {
    return protectedRoutes.keys.any((route) => path.startsWith(route));
  }

  /// 检查路由是否需要特定权限
  static String? getRequiredPermission(String path) {
    for (final route in permissionRoutes.keys) {
      if (path.startsWith(route)) {
        return permissionRoutes[route];
      }
    }
    return null;
  }
}