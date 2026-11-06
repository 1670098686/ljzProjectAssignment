import 'package:flutter/material.dart';
import '../pages/budget_warning/budget_warning_dialog.dart';

/// 预算预警动画服务类
/// 提供预算超支或接近超支时的警告动画效果
class BudgetWarningAnimationService {
  /// 显示预算预警弹窗
  static Future<void> showBudgetWarning(
    BuildContext context, 
    String categoryName,
    String? budgetName,
    double spentAmount,
    double budgetAmount,
    double usagePercentage,
  ) async {
    print('⚠️ 开始显示预算预警动画：分类=$categoryName, 计划名称=$budgetName, 已支出=$spentAmount, 预算=$budgetAmount, 使用百分比=$usagePercentage%');
    
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => BudgetWarningDialog(
        categoryName: categoryName,
        budgetName: budgetName,
        spentAmount: spentAmount,
        budgetAmount: budgetAmount,
        usagePercentage: usagePercentage,
      ),
    );
    
    print('⚠️ 预算预警动画显示完成');
  }
}