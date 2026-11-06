import 'package:flutter/material.dart';

/// 页面切换动画工具类
/// 提供多种流畅的页面切换动画效果
class PageTransitions {
  /// 淡入淡出动画
  static Route<T> fadeRoute<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 300),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 300),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => FadeTransition(
        opacity: CurvedAnimation(
          parent: animation,
          curve: Curves.easeInOut,
        ),
        child: child,
      ),
    );
  }

  /// 滑动动画（从右到左）
  static Route<T> slideRightToLeft<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 300),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 300),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(1.0, 0.0),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.easeInOut,
        )),
        child: child,
      ),
    );
  }

  /// 滑动动画（从左到右）
  static Route<T> slideLeftToRight<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 300),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 300),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(-1.0, 0.0),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.easeInOut,
        )),
        child: child,
      ),
    );
  }

  /// 向上滑入动画
  static Route<T> slideUp<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 300),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 300),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0.0, 1.0),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.easeOut,
        )),
        child: child,
      ),
    );
  }

  /// 缩放动画
  static Route<T> scaleRoute<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 300),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 300),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => ScaleTransition(
        scale: Tween<double>(
          begin: 0.8,
          end: 1.0,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.elasticOut,
        )),
        child: child,
      ),
    );
  }

  /// 旋转淡入动画
  static Route<T> rotateFadeRoute<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 400),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 400),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => RotationTransition(
        turns: Tween<double>(
          begin: 0.0,
          end: 1.0,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: Curves.elasticOut,
        )),
        child: FadeTransition(
          opacity: animation,
          child: child,
        ),
      ),
    );
  }

  /// 组合动画（缩放 + 淡入）
  static Route<T> scaleFadeRoute<T>(Widget page, {Duration? duration}) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 350),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 350),
      transitionsBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
        Widget child,
      ) => FadeTransition(
        opacity: CurvedAnimation(
          parent: animation,
          curve: Curves.easeIn,
        ),
        child: ScaleTransition(
          scale: Tween<double>(
            begin: 0.9,
            end: 1.0,
          ).animate(CurvedAnimation(
            parent: animation,
            curve: Curves.bounceOut,
          )),
          child: child,
        ),
      ),
    );
  }

  /// 定制动画（用于特殊场景）
  static Route<T> customRoute<T>({
    required Widget page,
    required Widget Function(
      BuildContext context,
      Animation<double> animation,
      Animation<double> secondaryAnimation,
      Widget child,
    ) transitionsBuilder,
    Duration? duration,
  }) {
    return PageRouteBuilder<T>(
      pageBuilder: (
        BuildContext context,
        Animation<double> animation,
        Animation<double> secondaryAnimation,
      ) => page,
      transitionDuration: duration ?? const Duration(milliseconds: 300),
      reverseTransitionDuration: duration ?? const Duration(milliseconds: 300),
      transitionsBuilder: transitionsBuilder,
    );
  }
}

/// 导航辅助类，方便在代码中使用各种动画
class NavigationHelper {
  /// 使用指定动画导航到新页面
  static Future<T?> navigateWithAnimation<T>(
    BuildContext context,
    Widget page, {
    PageTransitionsType animation = PageTransitionsType.fade,
    Duration? duration,
  }) {
    Route<T> route;
    
    switch (animation) {
      case PageTransitionsType.fade:
        route = PageTransitions.fadeRoute<T>(page, duration: duration);
        break;
      case PageTransitionsType.slideRightToLeft:
        route = PageTransitions.slideRightToLeft<T>(page, duration: duration);
        break;
      case PageTransitionsType.slideLeftToRight:
        route = PageTransitions.slideLeftToRight<T>(page, duration: duration);
        break;
      case PageTransitionsType.slideUp:
        route = PageTransitions.slideUp<T>(page, duration: duration);
        break;
      case PageTransitionsType.scale:
        route = PageTransitions.scaleRoute<T>(page, duration: duration);
        break;
      case PageTransitionsType.rotateFade:
        route = PageTransitions.rotateFadeRoute<T>(page, duration: duration);
        break;
      case PageTransitionsType.scaleFade:
        route = PageTransitions.scaleFadeRoute<T>(page, duration: duration);
        break;
    }
    
    return Navigator.push<T>(context, route);
  }
}

/// 动画类型枚举
enum PageTransitionsType {
  fade,
  slideRightToLeft,
  slideLeftToRight,
  slideUp,
  scale,
  rotateFade,
  scaleFade,
}