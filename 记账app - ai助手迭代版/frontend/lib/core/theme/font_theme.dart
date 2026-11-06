import 'package:flutter/material.dart';

/// 字体家族配置 - 确保在各种平台上都能正确显示中文字体
class AppFonts {
  /// 主字体家族 - 使用系统默认字体以确保离线可用性
  static const String fontFamily = 'system-ui';
  
  /// 字体回退列表 - 按优先级排列，确保中文字体正确显示
  static const List<String> fontFamilyFallback = <String>[
    // Android系统字体
    'Roboto',
    'Noto Sans CJK SC',
    'Noto Sans SC',
    
    // iOS系统字体
    '-apple-system',
    'PingFang SC',
    'Heiti SC',
    
    // Windows系统字体
    'Microsoft YaHei',
    'Segoe UI',
    
    // macOS系统字体
    'BlinkMacSystemFont',
    'Helvetica Neue',
    
    // 通用字体
    'sans-serif',
    'Arial',
  ];
}

/// 文字样式配置 - 符合Material Design 3规范
class AppTextStyles {
  // 显示字体 (Display)
  static const TextStyle displayLarge = TextStyle(
    fontSize: 57,
    fontWeight: FontWeight.normal,
    height: 1.12,
    letterSpacing: -0.25,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle displayMedium = TextStyle(
    fontSize: 45,
    fontWeight: FontWeight.normal,
    height: 1.16,
    letterSpacing: 0,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle displaySmall = TextStyle(
    fontSize: 36,
    fontWeight: FontWeight.normal,
    height: 1.22,
    letterSpacing: 0,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  // 大标题 (Headline)
  static const TextStyle headlineLarge = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.normal,
    height: 1.25,
    letterSpacing: 0,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle headlineMedium = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.normal,
    height: 1.29,
    letterSpacing: 0,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle headlineSmall = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.normal,
    height: 1.33,
    letterSpacing: 0,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  // 标题 (Title)
  static const TextStyle titleLarge = TextStyle(
    fontSize: 22,
    fontWeight: FontWeight.bold, // MD3中title通常为bold
    height: 1.27,
    letterSpacing: 0,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle titleMedium = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.bold, // MD3中title通常为bold
    height: 1.50,
    letterSpacing: 0.15,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle titleSmall = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.bold, // MD3中title通常为bold
    height: 1.43,
    letterSpacing: 0.1,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  // 正文 (Body)
  static const TextStyle bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.normal,
    height: 1.50,
    letterSpacing: 0.5,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.normal,
    height: 1.43,
    letterSpacing: 0.25,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.normal,
    height: 1.33,
    letterSpacing: 0.4,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  // 标签 (Label)
  static const TextStyle labelLarge = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w500,
    height: 1.43,
    letterSpacing: 0.1,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle labelMedium = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w500,
    height: 1.33,
    letterSpacing: 0.5,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );

  static const TextStyle labelSmall = TextStyle(
    fontSize: 11,
    fontWeight: FontWeight.w500,
    height: 1.45,
    letterSpacing: 0.5,
    fontFamilyFallback: AppFonts.fontFamilyFallback,
  );
}