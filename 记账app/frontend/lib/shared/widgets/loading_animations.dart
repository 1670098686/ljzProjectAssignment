import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

/// 加载动画组件库
/// 提供多种美观的加载动画效果

/// Shimmer加载效果组件
class ShimmerLoading extends StatelessWidget {
  final double width;
  final double height;
  final double borderRadius;
  final Color? baseColor;
  final Color? highlightColor;

  const ShimmerLoading({
    super.key,
    this.width = double.infinity,
    this.height = 100.0,
    this.borderRadius = 8.0,
    this.baseColor,
    this.highlightColor,
  });

  @override
  Widget build(BuildContext context) {
    final defaultBaseColor = Theme.of(context).brightness == Brightness.light
        ? Colors.grey[300]!
        : Colors.grey[700]!;
    final defaultHighlightColor =
        Theme.of(context).brightness == Brightness.light
        ? Colors.grey[100]!
        : Colors.grey[600]!;

    return Shimmer.fromColors(
      baseColor: baseColor ?? defaultBaseColor,
      highlightColor: highlightColor ?? defaultHighlightColor,
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(borderRadius),
        ),
      ),
    );
  }
}

/// 列表加载骨架屏组件
class ShimmerListLoading extends StatelessWidget {
  final int itemCount;
  final double itemHeight;
  final double borderRadius;
  final EdgeInsetsGeometry? padding;

  const ShimmerListLoading({
    super.key,
    this.itemCount = 5,
    this.itemHeight = 80.0,
    this.borderRadius = 8.0,
    this.padding = const EdgeInsets.all(16.0),
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: padding,
      itemCount: itemCount,
      itemBuilder: (context, index) {
        return Padding(
          padding: const EdgeInsets.only(bottom: 12.0),
          child: Row(
            children: [
              ShimmerLoading(
                width: 60.0,
                height: 60.0,
                borderRadius: 30.0, // 圆形头像
              ),
              const SizedBox(width: 12.0),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ShimmerLoading(
                      height: 16.0,
                      width: double.infinity,
                      borderRadius: 4.0,
                    ),
                    const SizedBox(height: 8.0),
                    ShimmerLoading(
                      height: 12.0,
                      width: 120.0,
                      borderRadius: 4.0,
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

/// 卡片加载骨架屏组件
class ShimmerCardLoading extends StatelessWidget {
  final int itemCount;
  final EdgeInsetsGeometry? padding;

  const ShimmerCardLoading({
    super.key,
    this.itemCount = 3,
    this.padding = const EdgeInsets.all(16.0),
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: padding,
      itemCount: itemCount,
      itemBuilder: (context, index) {
        return Card(
          margin: const EdgeInsets.only(bottom: 16.0),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ShimmerLoading(
                  height: 20.0,
                  width: double.infinity,
                  borderRadius: 4.0,
                ),
                const SizedBox(height: 12.0),
                ShimmerLoading(height: 16.0, width: 200.0, borderRadius: 4.0),
                const SizedBox(height: 8.0),
                ShimmerLoading(height: 12.0, width: 150.0, borderRadius: 4.0),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// 脉冲动画加载指示器
class PulseLoadingIndicator extends StatefulWidget {
  final Color? color;
  final double size;

  const PulseLoadingIndicator({super.key, this.color, this.size = 40.0});

  @override
  State<PulseLoadingIndicator> createState() => _PulseLoadingIndicatorState();
}

class _PulseLoadingIndicatorState extends State<PulseLoadingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1000),
      vsync: this,
    )..repeat();

    _scaleAnimation = Tween<double>(
      begin: 0.8,
      end: 1.2,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor;

    return Center(
      child: AnimatedBuilder(
        animation: _scaleAnimation,
        builder: (context, child) {
          return Transform.scale(
            scale: _scaleAnimation.value,
            child: Container(
              width: widget.size,
              height: widget.size,
              decoration: BoxDecoration(
                color: widget.color ?? defaultColor,
                shape: BoxShape.circle,
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 波纹加载指示器
class RippleLoadingIndicator extends StatefulWidget {
  final Color? color;
  final double size;

  const RippleLoadingIndicator({super.key, this.color, this.size = 50.0});

  @override
  State<RippleLoadingIndicator> createState() => _RippleLoadingIndicatorState();
}

class _RippleLoadingIndicatorState extends State<RippleLoadingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    )..repeat();

    _animation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor.withAlpha(77);

    return Center(
      child: AnimatedBuilder(
        animation: _animation,
        builder: (context, child) {
          return Container(
            width: widget.size,
            height: widget.size,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(
                color: widget.color ?? defaultColor,
                width: 2.0,
              ),
            ),
            child: Center(
              child: Container(
                width: widget.size * _animation.value,
                height: widget.size * _animation.value,
                decoration: BoxDecoration(
                  color: (widget.color ?? defaultColor).withAlpha(153),
                  shape: BoxShape.circle,
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 旋转加载指示器
class SpinnerLoadingIndicator extends StatefulWidget {
  final Color? color;
  final double size;
  final double strokeWidth;

  const SpinnerLoadingIndicator({
    super.key,
    this.color,
    this.size = 40.0,
    this.strokeWidth = 4.0,
  });

  @override
  State<SpinnerLoadingIndicator> createState() =>
      _SpinnerLoadingIndicatorState();
}

class _SpinnerLoadingIndicatorState extends State<SpinnerLoadingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1200),
      vsync: this,
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor;

    return Center(
      child: SizedBox(
        width: widget.size,
        height: widget.size,
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(
            widget.color ?? defaultColor,
          ),
          strokeWidth: widget.strokeWidth,
          value: _controller.value,
        ),
      ),
    );
  }
}

/// 弹性球加载指示器
class BounceBallLoadingIndicator extends StatefulWidget {
  final Color? color;
  final double size;

  const BounceBallLoadingIndicator({super.key, this.color, this.size = 40.0});

  @override
  State<BounceBallLoadingIndicator> createState() =>
      _BounceBallLoadingIndicatorState();
}

class _BounceBallLoadingIndicatorState extends State<BounceBallLoadingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor;

    return Center(
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(3, (index) {
          return AnimatedBuilder(
            animation: _controller,
            builder: (context, child) {
              final delay = index * 0.2;
              final time = (_controller.value + delay) % 1.0;
              final y = -4 * math.sin(time * 2 * math.pi);

              return Container(
                margin: const EdgeInsets.symmetric(horizontal: 3),
                child: Transform.translate(
                  offset: Offset(0, y),
                  child: Container(
                    width: widget.size / 4,
                    height: widget.size / 4,
                    decoration: BoxDecoration(
                      color: widget.color ?? defaultColor,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              );
            },
          );
        }),
      ),
    );
  }
}

/// 通用加载状态组件
class LoadingStateWidget extends StatelessWidget {
  final LoadingType type;
  final String? message;
  final Color? color;
  final double size;

  const LoadingStateWidget({
    super.key,
    required this.type,
    this.message,
    this.color,
    this.size = 40.0,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _buildLoadingIndicator(),
          if (message != null) ...[
            const SizedBox(height: 16),
            Text(
              message!,
              style: TextStyle(
                color: Theme.of(
                  context,
                ).textTheme.bodyMedium?.color?.withAlpha(179),
                fontSize: 14,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildLoadingIndicator() {
    switch (type) {
      case LoadingType.spinner:
        return SpinnerLoadingIndicator(color: color, size: size);
      case LoadingType.pulse:
        return PulseLoadingIndicator(color: color, size: size);
      case LoadingType.ripple:
        return RippleLoadingIndicator(color: color, size: size);
      case LoadingType.bounceBall:
        return BounceBallLoadingIndicator(color: color, size: size);
      case LoadingType.shimmer:
        return ShimmerLoading(width: size * 3, height: size * 2);
    }
  }
}

/// 加载类型枚举
enum LoadingType { spinner, pulse, ripple, bounceBall, shimmer }
