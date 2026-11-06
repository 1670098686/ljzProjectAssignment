import 'package:flutter/material.dart';

/// 庆祝动画服务类
/// 提供目标完成时的庆祝动画效果
class CelebrationAnimationService {
  /// 显示目标完成庆祝弹窗
  static Future<void> showGoalCompletionCelebration(
    BuildContext context, 
    String goalName,
    double targetAmount,
  ) async {
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => GoalCompletionCelebrationDialog(
        goalName: goalName,
        targetAmount: targetAmount,
      ),
    );
  }

  /// 显示迷你庆祝动画（用于列表项中的完成状态）
  static void showMiniCelebration(BuildContext context) {
    // 显示一个短暂的庆祝提示
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            Icon(Icons.celebration, color: Colors.white),
            SizedBox(width: 8),
            Text('目标已完成！恭喜！'),
          ],
        ),
        backgroundColor: Colors.green,
        duration: Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }
}

/// 目标完成庆祝弹窗组件
class GoalCompletionCelebrationDialog extends StatefulWidget {
  final String goalName;
  final double targetAmount;

  const GoalCompletionCelebrationDialog({
    super.key,
    required this.goalName,
    required this.targetAmount,
  });

  @override
  State<GoalCompletionCelebrationDialog> createState() => _GoalCompletionCelebrationDialogState();
}

class _GoalCompletionCelebrationDialogState extends State<GoalCompletionCelebrationDialog> 
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;
  late Animation<double> _fadeAnimation;
  late Animation<Color?> _colorAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    );

    _scaleAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.elasticOut,
    ));

    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeIn,
    ));

    _colorAnimation = ColorTween(
      begin: Colors.orange.withAlpha(77),
      end: Colors.green.withAlpha(26),
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOut,
    ));

    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Dialog(
      backgroundColor: Colors.transparent,
      elevation: 0,
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, child) {
          return Transform.scale(
            scale: _scaleAnimation.value,
            child: Opacity(
              opacity: _fadeAnimation.value,
              child: Container(
                width: 300,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: RadialGradient(
                    colors: [
                      _colorAnimation.value ?? Colors.green.withAlpha(26),
                      Colors.white,
                    ],
                    radius: 1.5,
                  ),
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(51),
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
                      width: 80,
                      height: 80,
                      decoration: BoxDecoration(
                      color: Colors.green.withAlpha(26),
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: Colors.green,
                        width: 2,
                      ),
                    ),
                      child: Icon(
                        Icons.celebration,
                        size: 40,
                        color: Colors.green,
                      ),
                    ),
                    
                    const SizedBox(height: 16),
                    
                    // 标题
                    Text(
                      '🎉 目标达成！',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.green[800],
                      ),
                    ),
                    
                    const SizedBox(height: 8),
                    
                    // 目标名称
                    Text(
                      widget.goalName,
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    
                    const SizedBox(height: 8),
                    
                    // 金额信息
                    Text(
                      '¥${widget.targetAmount.toStringAsFixed(2)}',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: Colors.green[700],
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    
                    const SizedBox(height: 16),
                    
                    // 庆祝文字
                    Text(
                      '恭喜您成功完成储蓄目标！',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: Colors.grey[600],
                      ),
                      textAlign: TextAlign.center,
                    ),
                    
                    const SizedBox(height: 20),
                    
                    // 确认按钮
                    ElevatedButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 32,
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
              ),
            ),
          );
        },
      ),
    );
  }
}