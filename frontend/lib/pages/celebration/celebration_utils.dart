import 'package:flutter/material.dart';

import '../../core/providers/saving_goal_provider.dart';
import 'celebration_page.dart';

/// 庆祝动画工具类
class CelebrationUtils {
  /// 显示完整庆祝动画
  static Future<void> showFullCelebration(
    BuildContext context, 
    String goalName,
    double targetAmount,
  ) async {
    // 先显示烟花效果
    await showDialog(
      context: context,
      barrierColor: Colors.transparent,
      builder: (context) => CelebrationPage(
        goalName: goalName,
        targetAmount: targetAmount,
      ),
    );
  }

  /// 检查并触发庆祝动画
  static void checkAndTriggerCelebration(
    BuildContext context,
    SavingGoalProvider provider,
  ) {
    final completedGoals = provider.goals.where((goal) => goal.isCompleted).toList();
    
    for (final goal in completedGoals) {
      // 检查是否已经庆祝过（可以通过添加字段来记录）
      if (!goal.isCompleted) continue;
      
      // 显示庆祝动画
      showFullCelebration(context, goal.name, goal.targetAmount);
    }
  }
}