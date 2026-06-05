import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../data/models/saving_goal_model.dart';

/// 智能建议类型
enum SuggestionType {
  urgent,      // 紧急建议
  warning,     // 警告建议
  suggestion,  // 普通建议
  encouragement, // 鼓励
}

/// 智能建议数据模型
@immutable
class SmartSuggestion {
  final SuggestionType type;
  final String title;
  final String message;
  final String? action;
  final String? icon;
  final DateTime createdAt;

  const SmartSuggestion({
    required this.type,
    required this.title,
    required this.message,
    this.action,
    this.icon,
    required this.createdAt,
  });

  IconData get iconData {
    switch (type) {
      case SuggestionType.urgent:
        return Icons.warning_amber_rounded;
      case SuggestionType.warning:
        return Icons.report_problem_rounded;
      case SuggestionType.suggestion:
        return Icons.lightbulb_outline_rounded;
      case SuggestionType.encouragement:
        return Icons.emoji_events_rounded;
    }
  }

  Color get color {
    switch (type) {
      case SuggestionType.urgent:
        return Colors.red;
      case SuggestionType.warning:
        return Colors.orange;
      case SuggestionType.suggestion:
        return Colors.blue;
      case SuggestionType.encouragement:
        return Colors.green;
    }
  }
}

/// 储蓄目标智能建议服务
class SmartSuggestionService {
  /// 生成储蓄目标的智能建议
  List<SmartSuggestion> generateSuggestions(SavingGoal goal) {
    final List<SmartSuggestion> suggestions = [];
    final now = DateTime.now();
    final progress = goal.progress;
    final remainingDays = goal.remainingDays;
    final isCompleted = goal.isCompleted;
    final isOverdue = goal.isOverdue;

    // 1. 完成目标鼓励
    if (isCompleted) {
      suggestions.add(SmartSuggestion(
        type: SuggestionType.encouragement,
        title: '🎉 恭喜完成储蓄目标！',
        message: '您已经成功完成了 "${goal.name}" 目标，这是个了不起的成就！考虑设定下一个储蓄目标吧。',
        action: '创建新目标',
        createdAt: now,
      ));
      return suggestions;
    }

    // 2. 逾期警告
    if (isOverdue) {
      final overdueDays = now.difference(goal.deadline).inDays;
      suggestions.add(SmartSuggestion(
        type: SuggestionType.urgent,
        title: '⚠️ 目标已逾期',
        message: '"${goal.name}" 已逾期 $overdueDays 天。当前进度: ${(progress * 100).toStringAsFixed(1)}%。建议重新评估目标或调整储蓄计划。',
        action: '调整目标',
        createdAt: now,
      ));
    }

    // 3. 临近截止日期建议
    if (remainingDays <= 7 && remainingDays > 0) {
      final dailyRequired = _calculateDailyRequiredAmount(goal);
      suggestions.add(SmartSuggestion(
        type: SuggestionType.urgent,
        title: '🔥 目标即将到期',
        message: '"${goal.name}" 还剩 $remainingDays 天，需要每天存入约 ¥${dailyRequired.toStringAsFixed(0)} 才能按时完成目标。',
        action: '立即存入',
        createdAt: now,
      ));
    } else if (remainingDays <= 30 && remainingDays > 7) {
      final weeklyRequired = _calculateWeeklyRequiredAmount(goal);
      suggestions.add(SmartSuggestion(
        type: SuggestionType.warning,
        title: '⏰ 时间不多了',
        message: '"${goal.name}" 还剩 $remainingDays 天，建议每周存入约 ¥${weeklyRequired.toStringAsFixed(0)} 以确保按时完成。',
        action: '制定计划',
        createdAt: now,
      ));
    }

    // 4. 进度落后建议
    final expectedProgress = _calculateExpectedProgress(goal);
    if (progress < expectedProgress - 0.2) { // 落后20%以上
      final shortfall = (expectedProgress - progress) * goal.targetAmount;
      suggestions.add(SmartSuggestion(
        type: SuggestionType.warning,
        title: '📉 进度落后提醒',
        message: '"${goal.name}" 进度落后了，需要额外存入约 ¥${shortfall.toStringAsFixed(0)} 才能按计划完成。',
        action: '调整计划',
        createdAt: now,
      ));
    }

    // 5. 正常进度建议
    if (progress >= expectedProgress - 0.1 && progress < expectedProgress + 0.1) {
      if (remainingDays > 30) {
        suggestions.add(SmartSuggestion(
        type: SuggestionType.suggestion,
        title: '💡 保持节奏',
        message: '您的储蓄进度很好！继续保持当前的储蓄节奏，就能按时完成目标。',
        action: '查看详情',
        createdAt: now,
      ));
      }
    }

    // 6. 进度超前建议
    if (progress > expectedProgress + 0.2) {
      suggestions.add(SmartSuggestion(
        type: SuggestionType.encouragement,
        title: '🚀 进度超前！',
        message: '太棒了！您超前完成了储蓄计划。可以考虑提前完成目标，或者提高目标金额。',
        action: '提前完成',
        createdAt: now,
      ));
    }

    // 7. 资金配置建议
    if (goal.currentAmount > goal.targetAmount * 0.8) {
      suggestions.add(SmartSuggestion(
        type: SuggestionType.suggestion,
        title: '💰 资金管理建议',
        message: '您的目标即将完成，建议将多余资金转入更稳健的投资渠道，或设定新的储蓄目标。',
        action: '资金配置',
        createdAt: now,
      ));
    }

    return suggestions;
  }

  /// 计算完成目标需要的日均金额
  double _calculateDailyRequiredAmount(SavingGoal goal) {
    final remainingAmount = goal.targetAmount - goal.currentAmount;
    final remainingDays = goal.remainingDays;
    
    if (remainingDays <= 0) {
      return remainingAmount;
    }
    
    return remainingAmount / remainingDays;
  }

  /// 计算完成目标需要的周均金额
  double _calculateWeeklyRequiredAmount(SavingGoal goal) {
    final remainingAmount = goal.targetAmount - goal.currentAmount;
    final remainingDays = goal.remainingDays;
    
    if (remainingDays <= 0) {
      return remainingAmount;
    }
    
    final remainingWeeks = remainingDays / 7;
    return remainingAmount / remainingWeeks;
  }

  /// 计算期望进度（基于创建日期和截止日期的线性进度）
  double _calculateExpectedProgress(SavingGoal goal) {
    // 这里简化计算，实际应该基于目标创建时间
    // 假设目标创建时间在30天前，或者使用goal对象的创建时间字段
    final totalDays = goal.deadline.difference(DateTime.now().subtract(const Duration(days: 30))).inDays;
    final elapsedDays = math.max(0, 30 - goal.remainingDays);
    
    if (totalDays <= 0) { return 0; }
    
    return elapsedDays / totalDays;
  }

  /// 生成个性化储蓄建议
  List<String> generatePersonalizedTips(SavingGoal goal) {
    final List<String> tips = [];
    final progress = goal.progress;
    final remainingDays = goal.remainingDays;

    // 基于进度的建议
    if (progress < 0.3) {
      tips.add('🏁 刚刚开始，保持动力很重要！可以设置每日/每周的小目标来维持储蓄习惯。');
      tips.add('💡 考虑使用"先储蓄后消费"的原则，每次收入先存入目标金额。');
    } else if (progress < 0.7) {
      tips.add('💪 已经完成了近一半，继续保持！这是最关键的时期，不要松懈。');
      tips.add('📊 可以记录每天的储蓄进展，这会给你更大的动力。');
    } else if (progress < 1.0) {
      tips.add('🎯 胜利在望！最后阶段更要坚持，避免功亏一篑。');
      tips.add('⚡ 可以考虑一次性存入较大金额来加速完成目标。');
    }

    // 基于时间的建议
    if (remainingDays <= 30) {
      tips.add('⏰ 时间紧迫，考虑调整支出结构，优先保证储蓄目标。');
      tips.add('🔥 可以设置自动转账，让储蓄变得更轻松。');
    } else if (remainingDays <= 90) {
      tips.add('📅 还有充足时间，可以制定详细的月度储蓄计划。');
      tips.add('🎁 考虑将意外收入（如奖金、红包）全部存入目标。');
    } else {
      tips.add('📈 有充足时间实现目标，可以考虑多元化储蓄策略。');
      tips.add('🌟 设定里程碑奖励，激励自己坚持储蓄。');
    }

    // 基于金额的建议
    if (goal.targetAmount < 1000) {
      tips.add('💰 小目标更容易完成，建议快速积累信心。');
    } else if (goal.targetAmount < 10000) {
      tips.add('🎯 中等目标需要规划，可以按月分解目标金额。');
    } else {
      tips.add('🏆 大目标需要长期坚持，建议制定阶段性计划。');
    }

    return tips;
  }

  /// 分析储蓄习惯
  Map<String, dynamic> analyzeSavingsHabit(List<SavingGoal> goals) {
    final completedGoals = goals.where((g) => g.isCompleted).toList();
    final activeGoals = goals.where((g) => !g.isCompleted && !g.isOverdue).toList();
    final overdueGoals = goals.where((g) => g.isOverdue).toList();

    return {
      'completed_count': completedGoals.length,
      'active_count': activeGoals.length,
      'overdue_count': overdueGoals.length,
      'success_rate': goals.isNotEmpty ? completedGoals.length / goals.length : 0.0,
      'average_progress': activeGoals.isNotEmpty 
          ? activeGoals.map((g) => g.progress).reduce((a, b) => a + b) / activeGoals.length 
          : 0.0,
      'habit_level': _calculateHabitLevel(completedGoals.length, activeGoals.length, overdueGoals.length),
      'recommendations': _generateHabitRecommendations(completedGoals, activeGoals, overdueGoals),
    };
  }

  /// 计算储蓄习惯等级
  String _calculateHabitLevel(int completed, int active, int overdue) {
    if (completed >= 10) {
      return '专家级';
    }
    if (completed >= 5) {
      return '达人级';
    }
    if (completed >= 2) {
      return '熟练级';
    }
    if (completed >= 1) {
      return '入门级';
    }
    if (active > 0) {
      return '新手级';
    }
    return '待开始';
  }

  /// 生成习惯建议
  List<String> _generateHabitRecommendations(List<SavingGoal> completed, List<SavingGoal> active, List<SavingGoal> overdue) {
    final List<String> recommendations = [];

    if (completed.isEmpty && active.isEmpty) {
      recommendations.add('🎯 从设定一个小目标开始，比如100元，建立储蓄习惯。');
      recommendations.add('📱 开启自动储蓄功能，让储蓄变得更简单。');
    } else if (overdue.length > active.length / 2) {
      recommendations.add('⚠️ 您的目标完成率较低，建议设定更现实的目标。');
      recommendations.add('📝 制定详细的储蓄计划，避免目标过于理想化。');
    } else if (completed.isNotEmpty) {
      recommendations.add('🏆 您有很好的储蓄习惯！可以尝试更有挑战性的目标。');
      recommendations.add('💡 考虑将储蓄习惯应用到其他财务目标上。');
    }

    return recommendations;
  }
}

/// 智能建议显示组件
class SmartSuggestionCard extends StatelessWidget {
  final SmartSuggestion suggestion;
  final VoidCallback? onActionTap;

  const SmartSuggestionCard({
    super.key,
    required this.suggestion,
    this.onActionTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: suggestion.color.withAlpha(26), // 0.1 * 255 = 25.5 ≈ 26
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(
                suggestion.iconData,
                color: suggestion.color,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    suggestion.title,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: suggestion.color,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    suggestion.message,
                    style: theme.textTheme.bodySmall?.copyWith(
                      height: 1.4,
                    ),
                  ),
                  if (suggestion.action != null) ...[
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: onActionTap,
                      style: TextButton.styleFrom(
                        foregroundColor: suggestion.color,
                        padding: EdgeInsets.zero,
                        minimumSize: Size.zero,
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      ),
                      child: Text(suggestion.action!),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}