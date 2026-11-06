import 'package:flutter/material.dart';

/// 预算预警页面
/// 用于显示预算超支或接近超支时的警告动画
class BudgetWarningPage extends StatefulWidget {
  final String categoryName;
  final double spentAmount;
  final double budgetAmount;
  final double usagePercentage;

  const BudgetWarningPage({
    super.key,
    required this.categoryName,
    required this.spentAmount,
    required this.budgetAmount,
    required this.usagePercentage,
  });

  @override
  State<BudgetWarningPage> createState() => _BudgetWarningPageState();
}

class _BudgetWarningPageState extends State<BudgetWarningPage> 
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _opacityAnimation;
  late Animation<double> _scaleAnimation;
  
  bool _showWarningAnimation = true;

  @override
  void initState() {
    super.initState();
    
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    )..repeat(reverse: true);

    _opacityAnimation = Tween<double>(
      begin: 0.6,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOut,
    ));

    _scaleAnimation = Tween<double>(
      begin: 0.95,
      end: 1.05,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: const Interval(0.0, 1.0, curve: Curves.elasticOut),
    ));

    _controller.forward();

    // 3秒后自动关闭动画
    Future.delayed(const Duration(seconds: 3), () {
      setState(() {
        _showWarningAnimation = false;
      });
      // 再延迟0.5秒后关闭页面
      Future.delayed(const Duration(milliseconds: 500), () {
        if (mounted) {
          Navigator.of(context).pop();
        }
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
    final isOverBudget = widget.spentAmount >= widget.budgetAmount;
    final remainingAmount = widget.budgetAmount - widget.spentAmount;
    
    return Dialog(
      backgroundColor: Colors.white,
      elevation: 10,
      child: Stack(
        children: [
          // 背景遮罩
          Container(
            color: Colors.black.withAlpha(128),
          ),
          
          // 警告动画效果
          if (_showWarningAnimation)
            AnimatedBuilder(
              animation: _controller,
              builder: (context, child) {
                return CustomPaint(
                  painter: WarningPainter(
                    animationValue: _controller.value,
                    isOverBudget: isOverBudget,
                  ),
                  size: Size(
                    MediaQuery.of(context).size.width,
                    MediaQuery.of(context).size.height,
                  ),
                );
              },
            ),
          
          // 警告内容
          Center(
            child: AnimatedBuilder(
              animation: _controller,
              builder: (context, child) {
                return Opacity(
                  opacity: _opacityAnimation.value,
                  child: Transform.scale(
                    scale: _scaleAnimation.value,
                    child: Container(
                      width: 340,
                      padding: const EdgeInsets.all(32),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: isOverBudget
                              ? [
                                  Colors.red.withAlpha(230),
                                  Colors.orange.withAlpha(230),
                                ]
                              : [
                                  Colors.orange.withAlpha(230),
                                  Colors.yellow.withAlpha(230),
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
                          // 警告图标
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
                            child: Icon(
                              isOverBudget ? Icons.error_outline : Icons.warning_amber_outlined,
                              size: 50,
                              color: Colors.white,
                            ),
                          ),
                          
                          const SizedBox(height: 24),
                          
                          // 标题
                          Text(
                            isOverBudget ? '⚠️ 预算超支！' : '⚠️ 预算预警',
                            style: theme.textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                              fontSize: 28,
                            ),
                          ),
                          
                          const SizedBox(height: 16),
                          
                          // 分类名称
                          Text(
                            widget.categoryName,
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                              fontSize: 20,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          
                          const SizedBox(height: 8),
                          
                          // 金额信息
                          if (isOverBudget)
                            Column(
                              children: [
                                Text(
                                  '已支出',
                                  style: theme.textTheme.bodyMedium?.copyWith(
                                    color: Colors.white.withAlpha(200),
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  '¥${widget.spentAmount.toStringAsFixed(2)}',
                                  style: theme.textTheme.headlineSmall?.copyWith(
                                    color: Colors.yellow[200],
                                    fontWeight: FontWeight.bold,
                                    fontSize: 32,
                                  ),
                                ),
                                Text(
                                  '超出预算',
                                  style: theme.textTheme.bodyMedium?.copyWith(
                                    color: Colors.white.withAlpha(200),
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  '¥${(-remainingAmount).toStringAsFixed(2)}',
                                  style: theme.textTheme.headlineSmall?.copyWith(
                                    color: Colors.red[200],
                                    fontWeight: FontWeight.bold,
                                    fontSize: 28,
                                  ),
                                ),
                              ],
                            )
                          else
                            Column(
                              children: [
                                Text(
                                  '已使用 ${widget.usagePercentage.toStringAsFixed(0)}%',
                                  style: theme.textTheme.bodyMedium?.copyWith(
                                    color: Colors.white.withAlpha(200),
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  '¥${widget.spentAmount.toStringAsFixed(2)} / ¥${widget.budgetAmount.toStringAsFixed(2)}',
                                  style: theme.textTheme.headlineSmall?.copyWith(
                                    color: Colors.yellow[200],
                                    fontWeight: FontWeight.bold,
                                    fontSize: 28,
                                  ),
                                  textAlign: TextAlign.center,
                                ),
                                Text(
                                  '剩余',
                                  style: theme.textTheme.bodyMedium?.copyWith(
                                    color: Colors.white.withAlpha(200),
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  '¥${remainingAmount.toStringAsFixed(2)}',
                                  style: theme.textTheme.headlineSmall?.copyWith(
                                    color: Colors.green[200],
                                    fontWeight: FontWeight.bold,
                                    fontSize: 28,
                                  ),
                                ),
                              ],
                            ),
                          
                          const SizedBox(height: 16),
                          
                          // 警告文字
                          Text(
                            isOverBudget 
                              ? '您的${widget.categoryName}分类本月已超支，请控制支出！' 
                              : '您的${widget.categoryName}分类本月预算已使用超过${widget.usagePercentage.toStringAsFixed(0)}%，请注意控制支出！',
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: Colors.white.withAlpha(230),
                              fontSize: 16,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          
                          const SizedBox(height: 24),
                          
                          // 确认按钮
                          ElevatedButton(
                            onPressed: () {
                              Navigator.of(context).pop();
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.white,
                              foregroundColor: isOverBudget ? Colors.red[800] : Colors.orange[800],
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 32,
                                vertical: 14,
                              ),
                            ),
                            child: const Text(
                              '知道了',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
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

/// 警告动画绘制器
class WarningPainter extends CustomPainter {
  final double animationValue;
  final bool isOverBudget;

  WarningPainter({required this.animationValue, required this.isOverBudget});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..style = PaintingStyle.fill;
    
    // 生成多个警告粒子
    for (int i = 0; i < 30; i++) {
      final progress = (animationValue + i * 0.1) % 1.0;
      final x = (size.width / 2) + (i * 25 - 375) * (0.5 + progress);
      final y = (size.height / 2) + (progress * 450 - 225);
      
      // 根据是否超支使用不同的颜色
      final colors = isOverBudget
        ? [
            Colors.red[400]!,
            Colors.red[500]!,
            Colors.orange[400]!,
            Colors.orange[500]!,
            Colors.yellow[400]!,
          ]
        : [
            Colors.orange[400]!,
            Colors.orange[500]!,
            Colors.yellow[400]!,
            Colors.yellow[500]!,
            Colors.amber[400]!,
          ];
      
      paint.color = colors[i % colors.length].withAlpha(((1.0 - progress) * 0.7 * 255).round());
      
      // 绘制警告粒子
      canvas.drawCircle(
        Offset(x, y),
        4 + (3 * (1.0 - progress)),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant WarningPainter oldDelegate) {
    return oldDelegate.animationValue != animationValue ||
           oldDelegate.isOverBudget != isOverBudget;
  }
}