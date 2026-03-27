import 'package:flutter/material.dart';
import '../../core/services/share_service.dart';

/// 目标完成庆祝页面
class CelebrationPage extends StatefulWidget {
  final String goalName;
  final double targetAmount;

  const CelebrationPage({
    super.key,
    required this.goalName,
    required this.targetAmount,
  });

  @override
  State<CelebrationPage> createState() => _CelebrationPageState();
}

class _CelebrationPageState extends State<CelebrationPage> 
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _opacityAnimation;
  late Animation<double> _scaleAnimation;
  
  bool _showConfetti = true;

  @override
  void initState() {
    super.initState();
    
    _controller = AnimationController(
      duration: const Duration(milliseconds: 2000),
      vsync: this,
    );

    _opacityAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: const Interval(0.0, 0.5, curve: Curves.easeIn),
    ));

    _scaleAnimation = Tween<double>(
      begin: 0.5,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: const Interval(0.3, 1.0, curve: Curves.elasticOut),
    ));

    _controller.forward();

    // 3秒后停止烟花效果
    Future.delayed(const Duration(seconds: 3), () {
      setState(() {
        _showConfetti = false;
      });
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Stack(
        children: [
          // 背景遮罩
          Container(
            color: Colors.black.withAlpha(128),
          ),
          
          // 庆祝动画效果
          if (_showConfetti)
            AnimatedBuilder(
              animation: _controller,
              builder: (context, child) {
                return CustomPaint(
                  painter: CelebrationPainter(
                    animationValue: _controller.value,
                  ),
                  size: Size.infinite,
                );
              },
            ),
          
          // 庆祝内容
          Center(
            child: AnimatedBuilder(
              animation: _controller,
              builder: (context, child) {
                return Opacity(
                  opacity: _opacityAnimation.value,
                  child: Transform.scale(
                    scale: _scaleAnimation.value,
                    child: Container(
                      width: 320,
                      padding: const EdgeInsets.all(32),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: [
                            Colors.green.withAlpha(230),
                            Colors.blue.withAlpha(230),
                          ],
                        ),
                        borderRadius: BorderRadius.circular(24),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withAlpha(77),
                            blurRadius: 20,
                            offset: const Offset(0, 10),
                          ),
                        ],
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          // 庆祝图标
                          Container(
                            width: 100,
                            height: 100,
                            decoration: BoxDecoration(
                              color: Colors.white.withAlpha(51),
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: Colors.white,
                                width: 3,
                              ),
                            ),
                            child: const Icon(
                              Icons.celebration,
                              size: 50,
                              color: Colors.white,
                            ),
                          ),
                          
                          const SizedBox(height: 24),
                          
                          // 标题
                          Text(
                            '🎉 目标达成！',
                            style: theme.textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                              fontSize: 28,
                            ),
                          ),
                          
                          const SizedBox(height: 16),
                          
                          // 目标名称
                          Text(
                            widget.goalName,
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                              fontSize: 20,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          
                          const SizedBox(height: 8),
                          
                          // 金额信息
                          Text(
                            '¥${widget.targetAmount.toStringAsFixed(2)}',
                            style: theme.textTheme.headlineSmall?.copyWith(
                              color: Colors.yellow[200],
                              fontWeight: FontWeight.bold,
                              fontSize: 32,
                            ),
                          ),
                          
                          const SizedBox(height: 16),
                          
                          // 庆祝文字
                          Text(
                            '恭喜您成功完成储蓄目标！\n这是您财务管理的重要里程碑！',
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: Colors.white.withAlpha(230),
                              fontSize: 16,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          
                          const SizedBox(height: 24),
                          
                          // 按钮组
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                            children: [
                              // 分享按钮
                              ElevatedButton(
                                onPressed: () {
                                  ShareService().showShareOptions(
                                    context: context,
                                    goalName: widget.goalName,
                                    targetAmount: widget.targetAmount,
                                  );
                                },
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.white.withAlpha(51),
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(12),
                                    side: const BorderSide(color: Colors.white),
                                  ),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 20,
                                    vertical: 12,
                                  ),
                                ),
                                child: const Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Icon(Icons.share, size: 18),
                                    SizedBox(width: 4),
                                    Text('分享'),
                                  ],
                                ),
                              ),
                              
                              // 确认按钮
                              ElevatedButton(
                                onPressed: () {
                                  Navigator.of(context).pop();
                                },
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.white,
                                  foregroundColor: Colors.green[800],
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 24,
                                    vertical: 12,
                                  ),
                                ),
                                child: const Text(
                                  '太棒了！',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

/// 庆祝动画绘制器
class CelebrationPainter extends CustomPainter {
  final double animationValue;

  CelebrationPainter({required this.animationValue});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..style = PaintingStyle.fill;
    
    // 生成多个庆祝粒子
    for (int i = 0; i < 30; i++) {
      final progress = (animationValue + i * 0.1) % 1.0;
      final x = (size.width / 2) + (i * 20 - 300) * (0.5 + progress);
      final y = (size.height / 2) + (progress * 400 - 200);
      
      final colors = [
        Colors.red[300]!,
        Colors.orange[300]!,
        Colors.yellow[300]!,
        Colors.green[300]!,
        Colors.blue[300]!,
        Colors.purple[300]!,
        Colors.pink[300]!,
      ];
      
      paint.color = colors[i % colors.length].withValues(alpha: (1.0 - progress) * 0.8);
      
      canvas.drawCircle(
        Offset(x, y),
        4 + (3 * (1.0 - progress)),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CelebrationPainter oldDelegate) {
    return oldDelegate.animationValue != animationValue;
  }
}

