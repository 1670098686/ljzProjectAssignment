import 'package:flutter/material.dart';
import 'dart:math' as math;

/// 微交互动画组件库
/// 提供各种微交互动画效果，提升用户体验

/// 动画增强的按钮基类
abstract class AnimatedButton extends StatefulWidget {
  final VoidCallback? onPressed;
  final String? label;
  final Widget? child;
  final Color? backgroundColor;
  final Color? textColor;
  final Duration duration;
  final double borderRadius;
  final double? width;
  final double? height;

  const AnimatedButton({
    super.key,
    this.onPressed,
    this.label,
    this.child,
    this.backgroundColor,
    this.textColor,
    this.duration = const Duration(milliseconds: 200),
    this.borderRadius = 8.0,
    this.width,
    this.height,
  });
}

/// 缩放动画按钮
class ScaleAnimatedButton extends AnimatedButton {
  final double scaleFactor;

  const ScaleAnimatedButton({
    super.key,
    super.onPressed,
    super.label,
    super.child,
    super.backgroundColor,
    super.textColor,
    super.duration = const Duration(milliseconds: 150),
    super.borderRadius = 8.0,
    super.width,
    super.height,
    this.scaleFactor = 0.95,
  });

  @override
  State<ScaleAnimatedButton> createState() => _ScaleAnimatedButtonState();
}

class _ScaleAnimatedButtonState extends State<ScaleAnimatedButton>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;
  bool _isPressed = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.duration, vsync: this);
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: widget.scaleFactor,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onTapDown(TapDownDetails details) {
    if (!_isDisabled) {
      setState(() => _isPressed = true);
      _controller.forward();
    }
  }

  void _onTapUp(TapUpDetails details) {
    if (!_isDisabled) {
      setState(() => _isPressed = false);
      _controller.reverse();
    }
  }

  void _onTapCancel() {
    if (!_isDisabled) {
      setState(() => _isPressed = false);
      _controller.reverse();
    }
  }

  bool get _isDisabled => widget.onPressed == null;

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor;
    final defaultTextColor = Colors.white;

    return GestureDetector(
      onTapDown: _onTapDown,
      onTapUp: _onTapUp,
      onTapCancel: _onTapCancel,
      onTap: widget.onPressed,
      child: AnimatedBuilder(
        animation: _scaleAnimation,
        builder: (context, child) {
          return Transform.scale(
            scale: _scaleAnimation.value,
            child: Container(
              width: widget.width,
              height: widget.height ?? 48,
              decoration: BoxDecoration(
                color: widget.backgroundColor ?? defaultColor,
                borderRadius: BorderRadius.circular(widget.borderRadius),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withAlpha((0.1 * 255).round()),
                    blurRadius: _isPressed ? 2 : 8,
                    offset: Offset(0, _isPressed ? 1 : 3),
                  ),
                ],
              ),
              child: Center(
                child:
                    widget.child ??
                    Text(
                      widget.label ?? '',
                      style: TextStyle(
                        color: widget.textColor ?? defaultTextColor,
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                    ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 弹性动画按钮
class BounceAnimatedButton extends AnimatedButton {
  final double bounceScale;

  const BounceAnimatedButton({
    super.key,
    super.onPressed,
    super.label,
    super.child,
    super.backgroundColor,
    super.textColor,
    super.duration = const Duration(milliseconds: 300),
    super.borderRadius = 8.0,
    super.width,
    super.height,
    this.bounceScale = 1.1,
  });

  @override
  State<BounceAnimatedButton> createState() => _BounceAnimatedButtonState();
}

class _BounceAnimatedButtonState extends State<BounceAnimatedButton>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.duration, vsync: this);
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: widget.bounceScale,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.elasticOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onTap() {
    if (widget.onPressed != null) {
      _controller.forward().then((_) {
        _controller.reverse();
      });
      widget.onPressed!();
    }
  }

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor;
    final defaultTextColor = Colors.white;

    return GestureDetector(
      onTap: _onTap,
      child: AnimatedBuilder(
        animation: _scaleAnimation,
        builder: (context, child) {
          return Transform.scale(
            scale: _scaleAnimation.value,
            child: Container(
              width: widget.width,
              height: widget.height ?? 48,
              decoration: BoxDecoration(
                color: widget.backgroundColor ?? defaultColor,
                borderRadius: BorderRadius.circular(widget.borderRadius),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withAlpha((0.2 * 255).round()),
                    blurRadius: 8,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: Center(
                child:
                    widget.child ??
                    Text(
                      widget.label ?? '',
                      style: TextStyle(
                        color: widget.textColor ?? defaultTextColor,
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                    ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// 悬停效果的卡片
class HoverCard extends StatefulWidget {
  final Widget child;
  final Color? hoverColor;
  final Color? shadowColor;
  final double elevation;
  final double hoverElevation;
  final Duration duration;
  final BorderRadius? borderRadius;
  final EdgeInsetsGeometry? padding;
  final Function(bool isHovered)? onHover;

  const HoverCard({
    super.key,
    required this.child,
    this.hoverColor,
    this.shadowColor,
    this.elevation = 2.0,
    this.hoverElevation = 8.0,
    this.duration = const Duration(milliseconds: 200),
    this.borderRadius,
    this.padding,
    this.onHover,
  });

  @override
  State<HoverCard> createState() => _HoverCardState();
}

class _HoverCardState extends State<HoverCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _elevationAnimation;
  late Animation<Color?> _colorAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.duration, vsync: this);
    _elevationAnimation = Tween<double>(
      begin: widget.elevation,
      end: widget.hoverElevation,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _colorAnimation = ColorTween(
      begin: Colors.transparent,
      end: widget.hoverColor ?? Colors.grey.withAlpha((0.1 * 255).round()),
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onHover(bool hovered) {
    widget.onHover?.call(hovered);

    if (hovered) {
      _controller.forward();
    } else {
      _controller.reverse();
    }
  }

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => _onHover(true),
      onExit: (_) => _onHover(false),
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, child) {
          return Container(
            padding: widget.padding,
            decoration: BoxDecoration(
              color: _colorAnimation.value,
              borderRadius: widget.borderRadius ?? BorderRadius.circular(12),
            ),
            child: Material(
              color: Colors.transparent,
              elevation: _elevationAnimation.value,
              shadowColor: widget.shadowColor ?? Colors.black.withAlpha((0.1 * 255).round()),
              borderRadius: widget.borderRadius ?? BorderRadius.circular(12),
              child: widget.child,
            ),
          );
        },
      ),
    );
  }
}

/// 焦点动画输入框
class AnimatedTextField extends StatefulWidget {
  final TextEditingController? controller;
  final String? labelText;
  final String? hintText;
  final String? errorText;
  final bool obscureText;
  final TextInputType? keyboardType;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onTap;
  final FocusNode? focusNode;
  final Color? focusColor;
  final Color? borderColor;
  final Duration duration;

  const AnimatedTextField({
    super.key,
    this.controller,
    this.labelText,
    this.hintText,
    this.errorText,
    this.obscureText = false,
    this.keyboardType,
    this.onChanged,
    this.onTap,
    this.focusNode,
    this.focusColor,
    this.borderColor,
    this.duration = const Duration(milliseconds: 200),
  });

  @override
  State<AnimatedTextField> createState() => _AnimatedTextFieldState();
}

class _AnimatedTextFieldState extends State<AnimatedTextField>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _borderAnimation;
  late Animation<double> _scaleAnimation;
  late Animation<Color?> _colorAnimation;

  FocusNode get _effectiveFocusNode => widget.focusNode ?? FocusNode();

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.duration, vsync: this);
    _borderAnimation = Tween<double>(
      begin: 1.0,
      end: 2.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: 1.02,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _colorAnimation = ColorTween(
      begin: widget.borderColor ?? Colors.grey.withAlpha((0.5 * 255).round()),
      end: widget.focusColor ?? Theme.of(context).primaryColor,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));

    _effectiveFocusNode.addListener(_onFocusChange);
  }

  @override
  void dispose() {
    if (widget.focusNode == null) {
      _effectiveFocusNode.dispose();
    } else {
      _effectiveFocusNode.removeListener(_onFocusChange);
    }
    _controller.dispose();
    super.dispose();
  }

  void _onFocusChange() {
    final isFocused = _effectiveFocusNode.hasFocus;
    if (mounted) {
      if (isFocused) {
        _controller.forward();
      } else {
        _controller.reverse();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final defaultBorderColor = Theme.of(context).brightness == Brightness.light
        ? Colors.grey.withAlpha((0.5 * 255).round())
        : Colors.grey.withAlpha((0.3 * 255).round());

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return Transform.scale(
          scale: _scaleAnimation.value,
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              border: Border.all(
                color: _colorAnimation.value ?? defaultBorderColor,
                width: _borderAnimation.value,
              ),
            ),
            child: TextField(
              controller: widget.controller,
              focusNode: _effectiveFocusNode,
              obscureText: widget.obscureText,
              keyboardType: widget.keyboardType,
              onChanged: widget.onChanged,
              onTap: widget.onTap,
              decoration: InputDecoration(
                labelText: widget.labelText,
                hintText: widget.hintText,
                errorText: widget.errorText,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 16,
                ),
                border: InputBorder.none,
                focusedBorder: InputBorder.none,
                enabledBorder: InputBorder.none,
              ),
            ),
          ),
        );
      },
    );
  }
}

/// 涟漪动画效果
class RippleEffect extends StatefulWidget {
  final Widget child;
  final Color? rippleColor;
  final Duration duration;
  final VoidCallback? onTap;

  const RippleEffect({
    super.key,
    required this.child,
    this.rippleColor,
    this.duration = const Duration(milliseconds: 400),
    this.onTap,
  });

  @override
  State<RippleEffect> createState() => _RippleEffectState();
}

class _RippleEffectState extends State<RippleEffect>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  Offset? _position;
  final List<RippleData> _ripples = [];

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.duration, vsync: this);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onTapDown(TapDownDetails details) {
    setState(() {
      _position = details.localPosition;
    });
    _startRipple();
    widget.onTap?.call();
  }

  void _startRipple() {
    final ripple = RippleData(
      id: DateTime.now().millisecondsSinceEpoch,
      position: _position!,
      controller: AnimationController(duration: widget.duration, vsync: this),
    );

    setState(() {
      _ripples.add(ripple);
    });

    ripple.controller.addListener(() {
      if (ripple.controller.value >= 1.0) {
        setState(() {
          _ripples.remove(ripple);
        });
        ripple.controller.dispose();
      }
    });

    ripple.controller.forward();
  }

  @override
  Widget build(BuildContext context) {
    final defaultRippleColor = Theme.of(context).primaryColor.withAlpha((0.2 * 255).round());

    return GestureDetector(
      onTapDown: _onTapDown,
      child: Stack(
        children: [
          widget.child,
          ..._ripples.map((ripple) {
            return Positioned.fill(
              child: AnimatedBuilder(
                animation: ripple.controller,
                builder: (context, child) {
                  final scale = ripple.controller.value;
                  final opacity = 1.0 - scale;

                  return CustomPaint(
                    painter: RipplePainter(
                      position: ripple.position,
                      scale: scale,
                      color: widget.rippleColor ?? defaultRippleColor,
                      opacity: opacity,
                    ),
                  );
                },
              ),
            );
          }),
        ],
      ),
    );
  }
}

class RippleData {
  final int id;
  final Offset position;
  final AnimationController controller;

  RippleData({
    required this.id,
    required this.position,
    required this.controller,
  });
}

class RipplePainter extends CustomPainter {
  final Offset position;
  final double scale;
  final Color color;
  final double opacity;

  RipplePainter({
    required this.position,
    required this.scale,
    required this.color,
    required this.opacity,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color.withAlpha((opacity * 255).round())
      ..style = PaintingStyle.fill;

    final radius = math.min(size.width, size.height) * 0.5 * scale;
    final center = position;

    canvas.drawCircle(center, radius, paint);
  }

  @override
  bool shouldRepaint(covariant RipplePainter oldDelegate) {
    return oldDelegate.position != position ||
        oldDelegate.scale != scale ||
        oldDelegate.color != color ||
        oldDelegate.opacity != opacity;
  }
}

/// 滑动删除动画项
class AnimatedListItem extends StatefulWidget {
  final Widget child;
  final VoidCallback? onDelete;
  final Color? backgroundColor;
  final Color? deleteColor;
  final String? deleteText;
  final Duration duration;

  const AnimatedListItem({
    super.key,
    required this.child,
    this.onDelete,
    this.backgroundColor,
    this.deleteColor,
    this.deleteText = '删除',
    this.duration = const Duration(milliseconds: 300),
  });

  @override
  State<AnimatedListItem> createState() => _AnimatedListItemState();
}

class _AnimatedListItemState extends State<AnimatedListItem>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _opacityAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.duration, vsync: this);
    _slideAnimation = Tween<Offset>(
      begin: Offset.zero,
      end: const Offset(-1.0, 0.0),
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    _opacityAnimation = Tween<double>(
      begin: 1.0,
      end: 0.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeIn));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final defaultBackgroundColor = Theme.of(context).scaffoldBackgroundColor;
    final defaultDeleteColor = Colors.red;

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return Dismissible(
          key: ValueKey(widget.key),
          direction: DismissDirection.endToStart,
          onDismissed: (direction) {
            widget.onDelete?.call();
          },
          background: Container(
            color: widget.deleteColor ?? defaultDeleteColor,
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.only(right: 20),
            child: Text(
              widget.deleteText!,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          child: Transform.translate(
            offset: _slideAnimation.value * 100,
            child: Opacity(
              opacity: _opacityAnimation.value,
              child: Container(
                color: widget.backgroundColor ?? defaultBackgroundColor,
                child: widget.child,
              ),
            ),
          ),
        );
      },
    );
  }
}

/// 弹簧加载动画
class SpringLoadingWidget extends StatefulWidget {
  final Color? color;
  final double size;
  final int springCount;

  const SpringLoadingWidget({
    super.key,
    this.color,
    this.size = 40.0,
    this.springCount = 3,
  });

  @override
  State<SpringLoadingWidget> createState() => _SpringLoadingWidgetState();
}

class _SpringLoadingWidgetState extends State<SpringLoadingWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  final List<double> _bounceHeights = [];

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1000),
      vsync: this,
    )..repeat();
    
    // 预计算弹跳高度，避免重复计算
    _precomputeBounceHeights();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _precomputeBounceHeights() {
    _bounceHeights.clear();
    for (int i = 0; i < widget.springCount; i++) {
      final delay = i * 0.1;
      // 预计算100个时间点的弹跳高度
      for (int t = 0; t < 100; t++) {
        final time = (t / 100.0 + delay) % 1.0;
        final bounceHeight = math.sin(time * 2 * math.pi) * (widget.size / (widget.springCount + 1)) * 0.3;
        _bounceHeights.add(bounceHeight);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final defaultColor = Theme.of(context).primaryColor;
    final ballSize = widget.size / (widget.springCount + 1);

    return Center(
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(widget.springCount, (index) {
          return AnimatedBuilder(
            animation: _controller,
            builder: (context, child) {
              // 使用预计算的弹跳高度，提高性能
              final timeIndex = ((_controller.value * 100).round() + (index * 10)) % 100;
              final bounceHeight = _bounceHeights[index * 100 + timeIndex];

              return Container(
                margin: EdgeInsets.symmetric(horizontal: ballSize * 0.2),
                child: Transform.translate(
                  offset: Offset(0, bounceHeight),
                  child: Container(
                    width: ballSize,
                    height: ballSize,
                    decoration: BoxDecoration(
                      color: widget.color ?? defaultColor,
                      shape: BoxShape.circle,
                      boxShadow: [
                        BoxShadow(
                          color: (widget.color ?? defaultColor).withAlpha((0.3 * 255).round()),
                          blurRadius: ballSize * 0.3,
                          offset: Offset(0, ballSize * 0.1),
                        ),
                      ],
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
