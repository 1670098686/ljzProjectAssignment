import 'package:flutter/material.dart';

/// 动画工具类 - 统一管理各种动画效果
class AnimationUtils {
  // 标准动画时长
  static const Duration shortDuration = Duration(milliseconds: 200);
  static const Duration mediumDuration = Duration(milliseconds: 300);
  static const Duration longDuration = Duration(milliseconds: 500);

  // 缓动曲线
  static const Curve standardCurve = Curves.easeInOut;
  static const Curve bounceCurve = Curves.bounceOut;
  static const Curve elasticCurve = Curves.elasticOut;

  /// 创建页面切换动画路由
  static PageRoute<T> createSlideRoute<T>(Widget child, {Offset? beginOffset}) {
    return PageRouteBuilder<T>(
      pageBuilder: (context, animation, secondaryAnimation) => child,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        final begin = beginOffset ?? const Offset(1.0, 0.0);
        const end = Offset.zero;
        final tween = Tween(begin: begin, end: end).chain(
          CurveTween(curve: standardCurve),
        );
        
        return SlideTransition(
          position: animation.drive(tween),
          child: child,
        );
      },
      transitionDuration: mediumDuration,
    );
  }

  /// 创建淡入淡出动画路由
  static PageRoute<T> createFadeRoute<T>(Widget child) {
    return PageRouteBuilder<T>(
      pageBuilder: (context, animation, secondaryAnimation) => child,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return FadeTransition(
          opacity: animation,
          child: child,
        );
      },
      transitionDuration: shortDuration,
    );
  }

  /// 创建缩放动画路由
  static PageRoute<dynamic> createScaleRoute(Widget child, {double? beginScale}) {
    return PageRouteBuilder<dynamic>(
      pageBuilder: (context, animation, secondaryAnimation) => child,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        final scaleAnimation = Tween<double>(
          begin: beginScale ?? 0.8,
          end: 1.0,
        ).chain(CurveTween(curve: bounceCurve));
        
        return ScaleTransition(
          scale: animation.drive(scaleAnimation),
          child: FadeTransition(
            opacity: animation,
            child: child,
          ),
        );
      },
      transitionDuration: mediumDuration,
    );
  }

  /// 创建淡入动画
  static Widget createFadeIn({
    required Widget child,
    Duration duration = shortDuration,
    Duration? delay,
  }) {
    if (delay != null) {
      return FutureBuilder<void>(
        future: Future.delayed(delay),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return child;
          }
          
          return TweenAnimationBuilder<double>(
            tween: Tween(begin: 0.0, end: 1.0),
            duration: duration,
            builder: (context, value, child) {
              return Opacity(
                opacity: value,
                child: child,
              );
            },
            child: child,
          );
        },
      );
    }
    
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: duration,
      builder: (context, value, child) {
        return Opacity(
          opacity: value,
          child: child,
        );
      },
      child: child,
    );
  }

  /// 创建滑入动画
  static Widget createSlideIn({
    required Widget child,
    Offset? beginOffset,
    Duration duration = mediumDuration,
    Duration? delay,
  }) {
    if (delay != null) {
      return FutureBuilder<void>(
        future: Future.delayed(delay),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return child;
          }
          
          return TweenAnimationBuilder<Offset>(
            tween: Tween(
              begin: beginOffset ?? const Offset(0, 30),
              end: Offset.zero,
            ),
            duration: duration,
            builder: (context, offset, child) {
              return Transform.translate(
                offset: offset,
                child: child ?? const SizedBox.shrink(),
              );
            },
            child: child,
          );
        },
      );
    }
    
    return TweenAnimationBuilder<Offset>(
      tween: Tween(
        begin: beginOffset ?? const Offset(0, 30),
        end: Offset.zero,
      ),
      duration: duration,
      builder: (context, offset, child) {
        return Transform.translate(
          offset: offset,
          child: child ?? const SizedBox.shrink(),
        );
      },
      child: child,
    );
  }

  /// 创建缩放动画
  static Widget createScaleIn({
    required Widget child,
    double? beginScale,
    Duration duration = mediumDuration,
    Duration? delay,
  }) {
    if (delay != null) {
      return FutureBuilder<void>(
        future: Future.delayed(delay),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return child;
          }
          
          return TweenAnimationBuilder<double>(
            tween: Tween(
              begin: beginScale ?? 0.8,
              end: 1.0,
            ),
            duration: duration,
            curve: Curves.easeOutBack,
            builder: (context, scale, child) {
              return Transform.scale(
                scale: scale,
                child: child,
              );
            },
            child: child,
          );
        },
      );
    }
    
    return TweenAnimationBuilder<double>(
      tween: Tween(
        begin: beginScale ?? 0.8,
        end: 1.0,
      ),
      duration: duration,
      curve: Curves.easeOutBack,
      builder: (context, scale, child) {
        return Transform.scale(
          scale: scale,
          child: child,
        );
      },
      child: child,
    );
  }

  /// 创建庆祝动画（成功操作后）
  static Widget createSuccessAnimation({
    required Widget child,
    VoidCallback? onComplete,
  }) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: const Duration(milliseconds: 800),
      builder: (context, value, child) {
        // 使用弹簧曲线创建弹性效果
        final scale = Curves.elasticOut.transform(value);
        
        return Transform.scale(
          scale: scale,
          child: Container(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.green.withValues(alpha: value * 0.2),
            ),
            child: Transform.scale(
              scale: 1.0,
              child: IconTheme(
                data: IconThemeData(
                  color: Colors.green,
                  size: 48 * value,
                ),
                child: child ?? const Icon(Icons.check, color: Colors.green),
              ),
            ),
          ),
        );
      },
      onEnd: onComplete,
      child: child,
    );
  }

  /// 创建加载动画
  static Widget createLoadingAnimation({
    double size = 24.0,
    Color? color,
  }) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: const Duration(milliseconds: 1200),
      builder: (context, value, child) {
        return Transform.rotate(
          angle: value * 2 * 3.14159,
          child: child ?? Icon(
            Icons.refresh,
            size: size,
            color: color ?? Theme.of(context).primaryColor,
          ),
        );
      },
    );
  }

  /// 创建脉动动画
  static Widget createPulseAnimation({
    required Widget child,
    double minScale = 0.95,
    double maxScale = 1.05,
    Duration duration = const Duration(milliseconds: 1500),
    Duration? delay,
  }) {
    return FutureBuilder<void>(
      future: delay != null ? Future.delayed(delay) : Future.value(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return child;
        }
        
        return TweenAnimationBuilder<double>(
          tween: Tween(begin: minScale, end: maxScale),
          duration: duration,
          builder: (context, scale, child) {
            return Transform.scale(
              scale: scale,
              child: child ?? const SizedBox.shrink(),
            );
          },
          child: child,
        );
      },
    );
  }

  /// 创建列表项动画
  static Widget createListItemAnimation({
    required Widget child,
    int index = 0,
    Duration duration = const Duration(milliseconds: 200),
  }) {
    return TweenAnimationBuilder<Offset>(
      tween: Tween(
        begin: const Offset(50, 0),
        end: Offset.zero,
      ),
      duration: Duration(milliseconds: duration.inMilliseconds + index * 50),
      builder: (context, offset, child) {
        return Transform.translate(
          offset: offset,
          child: Opacity(
            opacity: (offset.dx + 50) / 50,
            child: child ?? const SizedBox.shrink(),
          ),
        );
      },
      child: child,
    );
  }

  /// 创建卡片弹出动画
  static Widget createCardPopup({
    required Widget child,
    Duration duration = mediumDuration,
  }) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: duration,
      builder: (context, value, child) {
        return Transform.scale(
          scale: 0.8 + 0.2 * value,
          child: Opacity(
            opacity: value,
            child: child ?? const SizedBox.shrink(),
          ),
        );
      },
      child: child,
    );
  }

  /// 创建错误摇摆动画
  static Widget createErrorShakeAnimation({
    required Widget child,
    VoidCallback? onComplete,
  }) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: const Duration(milliseconds: 600),
      builder: (context, value, child) {
        // 创建摇摆效果
        final offset = Offset(
          (value < 0.5 ? -1 : 1) * 5 * (1 - (value - 0.5).abs() * 2),
          0,
        );
        
        return Transform.translate(
          offset: offset,
          child: child,
        );
      },
      onEnd: onComplete,
      child: child,
    );
  }

  /// 创建数字递增动画
  static Widget createCounterAnimation({
    required int from,
    required int to,
    required Widget Function(int value) builder,
    Duration duration = const Duration(milliseconds: 800),
  }) {
    return TweenAnimationBuilder<int>(
      tween: IntTween(begin: from, end: to),
      duration: duration,
      builder: (context, value, child) {
        return builder(value);
      },
    );
  }

  /// 创建进度条动画
  static Widget createProgressAnimation({
    required double progress,
    required Widget child,
    Duration duration = const Duration(milliseconds: 300),
  }) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: progress),
      duration: duration,
      builder: (context, value, child) {
        return child ?? const SizedBox.shrink();
      },
    );
  }

  /// 创建旋转动画
  static Widget createRotateAnimation({
    required Widget child,
    Duration duration = const Duration(milliseconds: 1000),
    bool reverse = false,
  }) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: reverse ? -2 * 3.14159 : 2 * 3.14159),
      duration: duration,
      builder: (context, rotation, child) {
        return Transform.rotate(
          angle: rotation,
          child: child ?? const SizedBox.shrink(),
        );
      },
      child: child,
    );
  }
}

/// 通用动画扩展
extension AnimationExtensions on Widget {
  /// 淡入动画
  Widget fadeIn({
    Duration duration = const Duration(milliseconds: 300),
  }) {
    return AnimationUtils.createFadeIn(child: this, duration: duration);
  }

  /// 滑入动画
  Widget slideIn({
    Offset? beginOffset,
    Duration duration = const Duration(milliseconds: 300),
  }) {
    return AnimationUtils.createSlideIn(
      child: this,
      beginOffset: beginOffset,
      duration: duration,
    );
  }

  /// 缩放入场动画
  Widget scaleIn({
    double beginScale = 0.8,
    Duration duration = const Duration(milliseconds: 300),
  }) {
    return AnimationUtils.createScaleIn(
      child: this,
      beginScale: beginScale,
      duration: duration,
    );
  }

  /// 脉动动画
  Widget pulse({
    double minScale = 0.95,
    double maxScale = 1.05,
    Duration duration = const Duration(milliseconds: 1500),
  }) {
    return AnimationUtils.createPulseAnimation(
      child: this,
      minScale: minScale,
      maxScale: maxScale,
      duration: duration,
    );
  }

  /// 列表项动画
  Widget listItem({
    int index = 0,
    Duration duration = const Duration(milliseconds: 200),
  }) {
    return AnimationUtils.createListItemAnimation(
      child: this,
      index: index,
      duration: duration,
    );
  }
}