import 'package:flutter/material.dart';

import '../../../core/providers/budget_provider.dart';
import '../../../data/models/budget_model.dart';
import '../../../services/budget_warning_animation_service.dart';

/// 预算预警工具类
class BudgetWarningUtils {
  /// 已显示过预警的预算ID集合，用于避免重复显示
  static final Set<int> _shownWarningIds = <int>{};
  
  /// 当前是否正在显示预警动画
  static bool _isShowingWarning = false;

  /// 显示预算预警动画
  static Future<void> showBudgetWarning(
    BuildContext context, 
    String categoryName,
    String? budgetName,
    double spentAmount,
    double budgetAmount,
    double usagePercentage,
  ) async {
    // 如果已经在显示预警，直接返回
    if (_isShowingWarning) {
      print('⚠️ 已经在显示预警动画，跳过此次调用');
      return;
    }
    
    try {
      _isShowingWarning = true;
      // 使用统一的动画服务类
      await BudgetWarningAnimationService.showBudgetWarning(
        context, categoryName, budgetName, spentAmount, budgetAmount, usagePercentage
      );
    } finally {
      _isShowingWarning = false;
    }
  }

  /// 检查并触发预算预警
  static Future<void> checkAndTriggerBudgetWarning(
    BuildContext context,
    BudgetProvider provider,
  ) async {
    print('🔍 开始检查预算预警，预算数量：${provider.budgets.length}');
    
    // 只检查当前月份的预算
    final currentMonth = DateTime.now();
    print('🔍 当前日期：${currentMonth.year}-${currentMonth.month}-${currentMonth.day}');
    
    final currentBudgets = provider.budgets.where((budget) {
      final isCurrentMonth = budget.year == currentMonth.year && budget.month == currentMonth.month;
      print('🔍 预算ID: ${budget.id}, 年份: ${budget.year}, 月份: ${budget.month}, 是否当前月份: $isCurrentMonth');
      return isCurrentMonth;
    }).toList();
    
    print('🔍 当前月份的预算数量：${currentBudgets.length}');
    
    // 检查每个预算是否超过预警阈值
    for (final budget in currentBudgets) {
      // 如果预算没有ID，跳过
      if (budget.id == null) {
        print('🔍 预算没有ID，跳过');
        continue;
      }
      
      // 如果已经显示过预警，跳过
      if (_shownWarningIds.contains(budget.id)) {
        print('🔍 预算ID: ${budget.id} 已经显示过预警，跳过');
        print('🔍 已显示预警ID集合：$_shownWarningIds');
        continue;
      }
      
      final spentAmount = budget.spent ?? 0.0;
      final usagePercentage = budget.amount > 0 ? (spentAmount / budget.amount) * 100 : 0.0;
      final isOverBudget = spentAmount >= budget.amount;
      
      print('🔍 预算ID: ${budget.id}, 分类: ${budget.categoryName}, 预算金额: ${budget.amount}, 已支出: $spentAmount, 使用百分比: $usagePercentage%, 是否超支: $isOverBudget');
      
      // 超过30%或超支时触发预警（大幅降低阈值，便于测试和用户体验）
      if (usagePercentage >= 30 || isOverBudget) {
        print('⚠️ 预算ID: ${budget.id} 超过预警阈值，准备显示预警动画');
        
        // 标记为已显示
        _shownWarningIds.add(budget.id!);
        
        // 显示预警动画
        await showBudgetWarning(
          context,
          budget.categoryName,
          budget.budgetName,
          spentAmount,
          budget.amount,
          usagePercentage,
        );
        
        // 只显示第一个超过阈值的预算预警，避免同时显示多个
        break;
      }
    }
    
    print('🔍 预算预警检查完成');
  }

  /// 检查单个预算并触发预警（点击触发版本）
  static void checkSingleBudgetWarning(
    BuildContext context,
    Budget budget,
  ) {
    // 如果预算没有ID，跳过
    if (budget.id == null) {
      print('🔍 预算没有ID，跳过');
      return;
    }
    
    final spentAmount = budget.spent ?? 0.0;
    final usagePercentage = budget.amount > 0 ? (spentAmount / budget.amount) * 100 : 0.0;
    final isOverBudget = spentAmount >= budget.amount;
    
    print('🔍 点击检查单个预算：ID=${budget.id}, 分类=${budget.categoryName}, 预算金额=${budget.amount}, 已支出=$spentAmount, 使用百分比=$usagePercentage%, 是否超支=$isOverBudget');
    
    // 超过30%或超支时触发预警（大幅降低阈值，提升用户体验）
    if (usagePercentage >= 30 || isOverBudget) {
      print('⚠️ 点击触发预算预警动画：预算ID=${budget.id}, 分类=${budget.categoryName}');
      
      // 立即显示预警动画（不受已显示状态限制）
      showBudgetWarning(
        context,
        budget.categoryName,
        budget.budgetName,
        spentAmount,
        budget.amount,
        usagePercentage,
      );
    } else {
      print('🔍 点击的预算未达到预警阈值：使用率=$usagePercentage%, 超支=$isOverBudget');
    }
  }
  
  /// 重置已显示预警的预算ID集合，用于刷新页面时重新检查
  static void resetShownWarnings() {
    print('🔄 重置已显示预警的预算ID集合');
    _shownWarningIds.clear();
  }
  

}
