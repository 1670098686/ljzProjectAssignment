import 'package:flutter/material.dart';

/// 烟花/彩带效果组件
class ConfettiWidget extends StatefulWidget {
  final int particleCount;
  final Duration duration;
  final VoidCallback? onComplete;

  const ConfettiWidget({
    super.key,
    this.particleCount = 50,
    this.duration = const Duration(seconds: 3),
    this.onComplete,
  });

  @override
  State<ConfettiWidget> createState() => _ConfettiWidgetState();
}

class _ConfettiWidgetState extends State<ConfettiWidget> 
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  List<ConfettiParticle> particles = [];

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: widget.duration,
      vsync: this,
    );

    // 初始化烟花粒子
    _initializeParticles();
    
    _controller.forward().then((_) {
      widget.onComplete?.call();
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _initializeParticles() {
    particles = List.generate(widget.particleCount, (index) {
      return ConfettiParticle(
        color: _getRandomColor(),
        size: _getRandomSize(),
        startX: _getRandomStartX(),
        startY: _getRandomStartY(),
        horizontalSpeed: _getRandomSpeed(),
        verticalSpeed: _getRandomSpeed(),
        rotationSpeed: _getRandomRotationSpeed(),
      );
    });
  }

  Color _getRandomColor() {
    final colors = [
      Colors.red,
      Colors.orange,
      Colors.yellow,
      Colors.green,
      Colors.blue,
      Colors.purple,
      Colors.pink,
    ];
    return colors[(DateTime.now().millisecond % colors.length)];
  }

  double _getRandomSize() {
    return 4 + (DateTime.now().microsecond % 8).toDouble();
  }

  double _getRandomStartX() {
    return DateTime.now().microsecond % 400 - 200.0;
  }

  double _getRandomStartY() {
    return -50 - (DateTime.now().microsecond % 100).toDouble();
  }

  double _getRandomSpeed() {
    return 0.5 + (DateTime.now().microsecond % 100) / 100.0;
  }

  double _getRandomRotationSpeed() {
    return (DateTime.now().microsecond % 10) - 5.0;
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return CustomPaint(
          painter: ConfettiPainter(
            particles: particles,
            animationValue: _controller.value,
          ),
          size: Size.infinite,
        );
      },
    );
  }
}

/// 烟花粒子类
class ConfettiParticle {
  final Color color;
  final double size;
  final double startX;
  final double startY;
  final double horizontalSpeed;
  final double verticalSpeed;
  final double rotationSpeed;

  ConfettiParticle({
    required this.color,
    required this.size,
    required this.startX,
    required this.startY,
    required this.horizontalSpeed,
    required this.verticalSpeed,
    required this.rotationSpeed,
  });
}

/// 烟花绘制器
class ConfettiPainter extends CustomPainter {
  final List<ConfettiParticle> particles;
  final double animationValue;

  ConfettiPainter({
    required this.particles,
    required this.animationValue,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();
    
    for (final particle in particles) {
      final progress = animationValue;
      
      // 计算粒子位置
      final x = particle.startX + particle.horizontalSpeed * progress * 200;
      final y = particle.startY + particle.verticalSpeed * progress * 500;
      
      // 计算旋转角度
      final rotation = particle.rotationSpeed * progress * 360;
      
      // 计算透明度（淡出效果）
      final opacity = (1.0 - progress).clamp(0.0, 1.0);
      
      paint.color = particle.color.withValues(alpha: opacity);
      
      // 保存画布状态
      canvas.save();
      
      // 平移和旋转
      canvas.translate(size.width / 2 + x, size.height / 2 + y);
      canvas.rotate(rotation * 3.14159 / 180);
      
      // 绘制粒子（矩形或圆形）
      if (DateTime.now().millisecond % 2 == 0) {
        // 矩形粒子
        canvas.drawRect(
          Rect.fromCenter(
            center: Offset.zero,
            width: particle.size,
            height: particle.size / 2,
          ),
          paint,
        );
      } else {
        // 圆形粒子
        canvas.drawCircle(
          Offset.zero,
          particle.size / 2,
          paint,
        );
      }
      
      // 恢复画布状态
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(covariant ConfettiPainter oldDelegate) {
    return oldDelegate.animationValue != animationValue ||
           oldDelegate.particles != particles;
  }
}