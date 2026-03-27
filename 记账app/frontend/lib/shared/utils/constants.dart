import 'package:flutter/material.dart';

/// 应用常量定义
class AppConstants {
  static const String appName = '个人收支记账';
  static const String appVersion = '1.0.0';
  static const String appDescription = '一款简单易用的个人收支记账应用';
  
  // API配置
  static const String baseUrl = 'http://localhost:8080/api/v1';
  static const int connectTimeout = 10000; // 10秒
  static const int receiveTimeout = 10000; // 10秒
  
  // 本地存储键名
  static const String themeModeKey = 'theme_mode';
  static const String languageKey = 'language';
  static const String firstLaunchKey = 'first_launch';
  static const String userSettingsKey = 'user_settings';
  
  // 数据库配置
  static const String databaseName = 'finance_app.db';
  static const int databaseVersion = 1;
  
  // 默认配置
  static const String defaultCurrency = '¥';
  static const String defaultDateFormat = 'yyyy-MM-dd';
  static const String defaultTimeFormat = 'HH:mm';
  
  // 验证规则
  static const int maxAmount = 1000000000; // 最大金额
  static const int maxRemarkLength = 200; // 备注最大长度
  static const int maxCategoryNameLength = 20; // 分类名称最大长度
  static const int maxGoalNameLength = 30; // 目标名称最大长度
  
  // 分页配置
  static const int defaultPageSize = 20;
  static const int maxPageSize = 100;
}

/// 应用颜色常量
class AppColors {
  // 主色调
  static const primary = Color(0xFF10B981);      // 现代绿色
  static const primaryLight = Color(0xFFD1FAE5); // 浅绿色
  static const primaryDark = Color(0xFF047857);  // 深绿色
  
  // 辅助色
  static const secondary = Color(0xFF3B82F6);    // 蓝色
  static const secondaryLight = Color(0xFFDBEAFE); // 浅蓝色
  static const secondaryDark = Color(0xFF1E40AF);  // 深蓝色
  
  // 功能色
  static const income = Color(0xFF10B981);       // 收入色
  static const expense = Color(0xFFEF4444);      // 支出色
  static const warning = Color(0xFFF59E0B);      // 预警色
  static const success = Color(0xFF10B981);      // 成功色
  static const error = Color(0xFFEF4444);        // 错误色
  static const info = Color(0xFF3B82F6);         // 信息色
  
  // 中性色（浅色主题）
  static const background = Color(0xFFF8FAFC);   // 背景色
  static const surface = Color(0xFFFFFFFF);     // 表面色
  static const onPrimary = Color(0xFFFFFFFF);   // 主色上的文字色
  static const onSecondary = Color(0xFFFFFFFF); // 辅助色上的文字色
  static const onSurface = Color(0xFF1E293B);   // 表面文字色
  static const onBackground = Color(0xFF334155); // 背景文字色
  static const onSurfaceVariant = Color(0xFF64748B); // 表面变体文字色
  static const onError = Color(0xFFFFFFFF);     // 错误色上的文字色
  
  // 中性色（深色主题）
  static const backgroundDark = Color(0xFF0F172A); // 深色背景
  static const surfaceDark = Color(0xFF1E293B);   // 深色表面
  static const onSurfaceDark = Color(0xFFF1F5F9); // 深色表面文字
  static const onBackgroundDark = Color(0xFFCBD5E1); // 深色背景文字
  
  // 边框和分隔线
  static const border = Color(0xFFE2E8F0);       // 边框色
  static const divider = Color(0xFFE2E8F0);      // 分隔线色
  
  // 阴影色
  static const shadow = Color(0x1A000000);       // 阴影色
  
  // 透明度
  static const disabled = Color(0x61000000);     // 禁用状态透明度
  static const overlay = Color(0x52000000);      // 遮罩层透明度
}

/// 图标常量
class AppIcons {
  // 导航图标
  static const home = Icons.home_outlined;
  static const homeFilled = Icons.home;
  static const transactions = Icons.receipt_long_outlined;
  static const transactionsFilled = Icons.receipt_long;
  static const budgets = Icons.account_balance_wallet_outlined;
  static const budgetsFilled = Icons.account_balance_wallet;
  static const savingGoals = Icons.savings_outlined;
  static const savingGoalsFilled = Icons.savings;
  static const statistics = Icons.bar_chart_outlined;
  static const statisticsFilled = Icons.bar_chart;
  static const settings = Icons.settings_outlined;
  static const settingsFilled = Icons.settings;
  
  // 功能图标
  static const add = Icons.add;
  static const edit = Icons.edit_outlined;
  static const delete = Icons.delete_outlined;
  static const save = Icons.save_outlined;
  static const cancel = Icons.cancel_outlined;
  static const back = Icons.arrow_back_ios_new;
  static const forward = Icons.arrow_forward_ios;
  static const menu = Icons.menu;
  static const search = Icons.search;
  static const filter = Icons.filter_list;
  static const sort = Icons.sort;
  
  // 分类图标
  static const food = Icons.restaurant_outlined;
  static const transportation = Icons.directions_car_outlined;
  static const shopping = Icons.shopping_cart_outlined;
  static const entertainment = Icons.movie_outlined;
  static const health = Icons.local_hospital_outlined;
  static const education = Icons.school_outlined;
  static const salary = Icons.attach_money_outlined;
  static const investment = Icons.trending_up_outlined;
  static const gift = Icons.card_giftcard_outlined;
  static const other = Icons.category_outlined;
}

/// 文本样式常量
class AppTextStyles {
  // 标题
  static const headlineLarge = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.bold,
    height: 1.2,
  );
  
  static const headlineMedium = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.bold,
    height: 1.2,
  );
  
  static const headlineSmall = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.bold,
    height: 1.2,
  );
  
  // 正文
  static const bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.normal,
    height: 1.5,
  );
  
  static const bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.normal,
    height: 1.5,
  );
  
  static const bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.normal,
    height: 1.5,
  );
  
  // 标签
  static const labelLarge = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w500,
    height: 1.4,
  );
  
  static const labelMedium = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w500,
    height: 1.4,
  );
  
  static const labelSmall = TextStyle(
    fontSize: 10,
    fontWeight: FontWeight.w500,
    height: 1.4,
  );
  
  // 特殊样式
  static const amountIncome = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    color: AppColors.income,
  );
  
  static const amountExpense = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    color: AppColors.expense,
  );
  
  static const amountLarge = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.bold,
    color: AppColors.onSurface,
  );
}

/// 间距常量
class AppSpacing {
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 16.0;
  static const double lg = 24.0;
  static const double xl = 32.0;
  static const double xxl = 48.0;
}

/// 圆角常量
class AppBorderRadius {
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 12.0;
  static const double lg = 16.0;
  static const double xl = 24.0;
  static const double full = 100.0;
}