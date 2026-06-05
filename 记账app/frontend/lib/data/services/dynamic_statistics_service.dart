import 'dart:developer' as developer;
import 'package:flutter/material.dart';

import '../models/bill_model.dart';
import '../models/budget_model.dart';
import '../models/saving_goal_model.dart';
import '../../core/services/local_data_service.dart';

/// 动态统计服务 - 计算预算和储蓄的实时数据
/// 
/// 该服务负责根据实际收支明细动态计算：
/// 1. 预算使用情况
/// 2. 储蓄目标进度
/// 3. 实时统计数据
/// 
/// 核心功能：
/// - 从收支明细动态计算预算实际支出
/// - 结合储蓄记录和分类收支计算储蓄进度
/// - 批量计算统计数据以优化性能
class DynamicStatisticsService {
  /// 本地数据服务实例
  final LocalDataService _localDataService;

  /// 默认构造函数
  DynamicStatisticsService() : _localDataService = LocalDataService();
  
  /// 分类分割正则表达式 - 用于拆分复合分类名称
  static final RegExp _categorySplitPattern = RegExp(r'[-_·•+,，、/|–—]+');

  /// 计算预算的实际支出（从收支明细动态计算）
  /// 
  /// 根据预算的分类规则，从实际支出账单中匹配相关记录，计算总支出
  /// 
  /// 参数：
  /// - budget: 要计算的预算对象
  /// - context: 上下文对象（可选，用于错误处理）
  /// 
  /// 返回：
  /// - 计算得出的实际支出金额
  Future<double> calculateBudgetActualSpent({
    required Budget budget,
    BuildContext? context,
  }) async {
    try {
      // 获取当月的所有支出账单（类型为2表示支出）
      final bills = await _localDataService.getBillsFromLocal(
        type: 2, // 支出类型
      );
      
      // 构建预算分类候选集，用于匹配账单
      final categoryCandidates = _buildBudgetCategoryCandidates(budget);
      
      // 匹配相关的支出账单
      final matchedBills = <Bill>[];
      for (final bill in bills) {
        if (_isBudgetCategoryMatch(bill.categoryName, categoryCandidates)) {
          matchedBills.add(bill);
        }
      }
      
      // 计算总支出：使用fold函数累加匹配账单的金额
      final totalSpent = matchedBills.fold<double>(
        0.0,
        (sum, bill) => sum + bill.amount,
      );
      
      developer.log(
        '预算 ${budget.budgetName ?? budget.categoryName} 实际支出计算完成: 候选分类 $categoryCandidates，匹配 ${matchedBills.length} 条记录，总支出 ¥$totalSpent',
        name: 'DynamicStatisticsService',
      );
      
      return totalSpent;
    } catch (e, stackTrace) {
      developer.log(
        '计算预算实际支出失败: $e',
        name: 'DynamicStatisticsService',
        error: e,
        stackTrace: stackTrace,
      );
      return 0.0;
    }
  }

  /// 计算储蓄目标的实际储蓄金额（从储蓄记录和分类收支记录动态计算）
  /// 
  /// 结合储蓄记录和分类收支记录，动态计算储蓄目标的实际储蓄金额
  /// 优先级：分类收支记录 > 储蓄记录 > 当前金额
  /// 
  /// 参数：
  /// - goal: 储蓄目标对象
  /// - cachedIncomeBills: 缓存的收入账单（可选，用于优化性能）
  /// 
  /// 返回：
  /// - 实际储蓄金额
  Future<double> calculateGoalActualSaved({
    required SavingGoal goal,
    List<Bill>? cachedIncomeBills,
  }) async {
    try {
      // 如果目标没有ID，直接返回当前金额
      if (goal.id == null) {
        developer.log(
          '目标 ${goal.name} 缺少ID，直接返回当前金额 ${goal.currentAmount}',
          name: 'DynamicStatisticsService',
        );
        return goal.currentAmount;
      }

      // 构建储蓄目标的分类候选集
      final categoryCandidates = _buildSavingGoalCategoryCandidates(goal);
      
      // 获取收入账单（使用缓存或从本地数据服务获取）
      final incomeBills =
          cachedIncomeBills ??
          await _localDataService.getBillsFromLocal(type: 1); // 类型为1表示收入
      
      if (categoryCandidates.isEmpty) {
        developer.log(
          '储蓄目标 ${goal.name} 未找到分类候选，优先使用储蓄记录',
          name: 'DynamicStatisticsService',
        );
      }

      // 1. 获取储蓄记录并计算累计金额
      final savingRecords = await _localDataService.getSavingRecordsByGoalId(
        goal.id!,
      );
      
      double totalSavedFromRecords = 0.0;
      for (final record in savingRecords) {
        if (record.type == 'deposit') {
          // 存款增加储蓄金额
          totalSavedFromRecords += record.amount;
        } else if (record.type == 'withdraw') {
          // 取款减少储蓄金额
          totalSavedFromRecords -= record.amount;
        }
      }

      // 2. 从分类收支记录中匹配相关收入
      final matchedTransactions = <String, Bill>{};
      if (categoryCandidates.isNotEmpty) {
        for (final bill in incomeBills) {
          if (_isSavingCategoryMatch(bill.categoryName, categoryCandidates)) {
            // 使用交易唯一键避免重复记录
            matchedTransactions[_buildTransactionKey(bill)] = bill;
          }
        }
      }

      // 计算分类收支记录的总金额
      final totalSavedFromCategory = matchedTransactions.values.fold<double>(
        0.0,
        (sum, bill) => sum + bill.amount,
      );

      developer.log(
        '储蓄目标 ${goal.name} 分类候选: ${categoryCandidates.join(', ')}，匹配收入记录: ${matchedTransactions.length}',
        name: 'DynamicStatisticsService',
      );

      // 确定最终的实际储蓄金额，按优先级选择
      double totalSaved = totalSavedFromCategory;
      if (totalSaved <= 0.0 && totalSavedFromRecords != 0.0) {
        // 分类收支记录不足，使用储蓄记录金额
        totalSaved = totalSavedFromRecords;
        developer.log(
          '分类收支记录不足，使用储蓄记录金额 ¥$totalSavedFromRecords',
          name: 'DynamicStatisticsService',
        );
      } else if (totalSaved <= 0.0) {
        // 未找到有效的动态数据，回退到当前金额
        totalSaved = goal.currentAmount;
        developer.log(
          '未找到有效的动态数据，回退到当前金额 ¥${goal.currentAmount}',
          name: 'DynamicStatisticsService',
        );
      }

      developer.log(
        '储蓄金额计算完成: 目标ID${goal.id} ${goal.name} = ¥$totalSaved (分类收支:¥$totalSavedFromCategory, 储蓄记录:¥$totalSavedFromRecords)',
        name: 'DynamicStatisticsService',
      );

      return totalSaved;
    } catch (e, stackTrace) {
      developer.log(
        '计算储蓄金额失败: $e',
        name: 'DynamicStatisticsService',
        error: e,
        stackTrace: stackTrace,
      );

      // 记录错误但不使用BuildContext
      print('计算储蓄金额失败: $e');
      return 0.0;
    }
  }

  /// 获取储蓄目标的实时统计（包含动态计算的储蓄金额）
  /// 
  /// 计算储蓄目标的实时状态，包括进度、剩余金额、状态等
  /// 
  /// 参数：
  /// - goal: 储蓄目标对象
  /// - context: 上下文对象（可选）
  /// - incomeBillsCache: 收入账单缓存（可选，用于优化性能）
  /// 
  /// 返回：
  /// - 储蓄目标的实时统计对象
  Future<SavingGoalRealTimeStats> getSavingGoalRealTimeStats({
    required SavingGoal goal,
    BuildContext? context,
    List<Bill>? incomeBillsCache,
  }) async {
    try {
      // 如果目标没有ID，直接使用当前的currentAmount
      if (goal.id == null) {
        final progress = goal.targetAmount > 0
            ? goal.currentAmount / goal.targetAmount
            : 0.0;
        final remaining = goal.targetAmount - goal.currentAmount;
        final isCompleted = goal.currentAmount >= goal.targetAmount;
        final daysRemaining = goal.deadline.difference(DateTime.now()).inDays;
        final isOverdue = !isCompleted && daysRemaining < 0;

        return SavingGoalRealTimeStats(
          goal: goal,
          actualSaved: goal.currentAmount,
          progress: progress.clamp(0.0, 1.0), // 限制进度在0-1之间
          remaining: remaining.abs(), // 取绝对值，避免负数
          isCompleted: isCompleted,
          daysRemaining: daysRemaining,
          isOverdue: isOverdue,
          status: _calculateSavingStatus(isCompleted, isOverdue, daysRemaining),
        );
      }

      // 动态计算实际储蓄金额
      final actualSaved = await calculateGoalActualSaved(
        goal: goal,
        cachedIncomeBills: incomeBillsCache,
      );

      // 计算进度（限制在0-1之间）
      final progress = goal.targetAmount > 0
          ? actualSaved / goal.targetAmount
          : 0.0;
      
      // 计算剩余金额
      final remaining = goal.targetAmount - actualSaved;
      
      // 判断是否完成
      final isCompleted = actualSaved >= goal.targetAmount;
      
      // 计算剩余天数
      final daysRemaining = goal.deadline.difference(DateTime.now()).inDays;
      
      // 判断是否逾期
      final isOverdue = !isCompleted && daysRemaining < 0;

      // 创建并返回实时统计对象
      return SavingGoalRealTimeStats(
        goal: goal,
        actualSaved: actualSaved,
        progress: progress.clamp(0.0, 1.0),
        remaining: remaining.abs(),
        isCompleted: isCompleted,
        daysRemaining: daysRemaining,
        isOverdue: isOverdue,
        status: _calculateSavingStatus(isCompleted, isOverdue, daysRemaining),
      );
    } catch (e, stackTrace) {
      developer.log(
        '获取储蓄目标实时统计失败: $e',
        name: 'DynamicStatisticsService',
        error: e,
        stackTrace: stackTrace,
      );

      // 发生错误时返回默认值
      return SavingGoalRealTimeStats(
        goal: goal,
        actualSaved: 0.0,
        progress: 0.0,
        remaining: goal.targetAmount,
        isCompleted: false,
        daysRemaining: goal.deadline.difference(DateTime.now()).inDays,
        isOverdue: false,
        status: SavingGoalStatus.inProgress,
      );
    }
  }

  /// 计算单个预算的实时统计
  /// 
  /// 计算预算的实际支出和使用比例
  /// 
  /// 参数：
  /// - budget: 预算对象
  /// - context: 上下文对象（可选）
  /// 
  /// 返回：
  /// - 预算的实时统计对象
  Future<BudgetRealTimeStats> getBudgetRealTimeStats({
    required Budget budget,
    BuildContext? context,
  }) async {
    try {
      // 动态计算实际支出
      final actualSpent = await calculateBudgetActualSpent(
        budget: budget,
        context: context,
      );
      
      // 计算使用比例（使用金额除以预算金额）
      final usage = budget.amount > 0
          ? actualSpent / budget.amount
          : 0.0;
      
      return BudgetRealTimeStats(
        budgetId: budget.id,
        actualSpent: actualSpent,
        budgetedAmount: budget.amount,
        usage: usage,
      );
    } catch (e, stackTrace) {
      developer.log(
        '获取单个预算实时统计失败: $e',
        name: 'DynamicStatisticsService',
        error: e,
        stackTrace: stackTrace,
      );
      
      // 发生错误时返回默认值
      return BudgetRealTimeStats(
        budgetId: budget.id,
        actualSpent: 0.0,
        budgetedAmount: budget.amount,
        usage: 0.0,
      );
    }
  }

  /// 批量获取预算实时统计
  /// 
  /// 批量计算多个预算的实时统计数据
  /// 
  /// 参数：
  /// - budgets: 预算对象列表
  /// - context: 上下文对象（可选）
  /// 
  /// 返回：
  /// - 预算实时统计对象列表
  Future<List<BudgetRealTimeStats>> getBudgetsRealTimeStats({
    required List<Budget> budgets,
    BuildContext? context,
  }) async {
    final List<BudgetRealTimeStats> results = [];
    
    // 遍历所有预算，逐个计算实时统计
    for (final budget in budgets) {
      final stats = await getBudgetRealTimeStats(
        budget: budget,
        context: context,
      );
      results.add(stats);
    }
    
    return results;
  }

  /// 批量计算储蓄目标统计（优化性能）
  /// 
  /// 批量计算多个储蓄目标的实时统计数据，优化性能
  /// 
  /// 参数：
  /// - goals: 储蓄目标对象列表
  /// - context: 上下文对象（可选）
  /// 
  /// 返回：
  /// - 储蓄目标实时统计对象列表
  Future<List<SavingGoalRealTimeStats>> getSavingGoalsRealTimeStats({
    required List<SavingGoal> goals,
    BuildContext? context,
  }) async {
    final List<SavingGoalRealTimeStats> results = [];
    
    // 只获取一次收入账单，用于所有目标计算，优化性能
    final List<Bill> incomeBills = goals.isEmpty
        ? <Bill>[]
        : await _localDataService.getBillsFromLocal(type: 1);

    // 遍历所有储蓄目标，逐个计算实时统计
    for (final goal in goals) {
      final stats = await getSavingGoalRealTimeStats(
        goal: goal,
        context: context,
        incomeBillsCache: incomeBills, // 传递缓存的收入账单
      );
      results.add(stats);
    }

    return results;
  }

  /// 判断账单分类是否与预算分类匹配
  /// 
  /// 实现严格的分类匹配逻辑，确保只有精确匹配组合分类名才会返回true
  /// 避免不同预算计划共用同一分类数据
  /// 
  /// 参数：
  /// - billCategoryName: 账单的分类名称
  /// - candidates: 预算的分类候选集
  /// 
  /// 返回：
  /// - 是否匹配成功
  bool _isBudgetCategoryMatch(String billCategoryName, Set<String> candidates) {
    if (candidates.isEmpty) {
      return false;
    }
    
    // 规范化分类名称，去除多余空格
    final normalizedBillCategory = _normalizeCategoryName(billCategoryName);
    
    // 1. 精确匹配 - 只有完全相同的组合分类名才匹配
    // 确保每个预算计划只能匹配自己的组合分类名
    if (candidates.contains(normalizedBillCategory)) {
      return true;
    }
    
    return false;
  }
  
  /// 构建预算分类候选集
  /// 
  /// 根据预算的分类名称和预算名称，构建用于匹配账单的分类候选集
  /// 
  /// 参数：
  /// - budget: 预算对象
  /// 
  /// 返回：
  /// - 分类候选集
  Set<String> _buildBudgetCategoryCandidates(Budget budget) {
    final normalizedNames = <String>{};
    
    // 规范化分类名称和预算名称
    final storedCategory = _normalizeCategoryName(budget.categoryName);
    final budgetName = _normalizeCategoryName(budget.budgetName ?? '');
    
    // 1. 添加存储的分类名作为候选（组合分类名）
    if (storedCategory.isNotEmpty) {
      normalizedNames.add(storedCategory);
    }
    
    // 2. 如果有预算名称，添加组合分类名（预算名称-分类名称）
    if (budgetName.isNotEmpty && storedCategory.isNotEmpty) {
      normalizedNames.add('$budgetName-$storedCategory');
    }
    
    // 移除空字符串
    normalizedNames.removeWhere((name) => name.isEmpty);
    
    developer.log(
      '预算分类候选集: ${budget.budgetName ?? budget.categoryName} -> $normalizedNames',
      name: 'DynamicStatisticsService',
    );
    
    return normalizedNames;
  }

  /// 构建储蓄目标分类候选集
  /// 
  /// 根据储蓄目标的分类名称和目标名称，构建用于匹配账单的分类候选集
  /// 
  /// 参数：
  /// - goal: 储蓄目标对象
  /// 
  /// 返回：
  /// - 分类候选集
  Set<String> _buildSavingGoalCategoryCandidates(SavingGoal goal) {
    final normalizedNames = <String>{};
    
    // 规范化分类名称和目标名称
    final storedCategory = _normalizeCategoryName(goal.categoryName);
    final goalName = _normalizeCategoryName(goal.name);

    // 1. 添加存储的分类名作为候选（组合分类名）
    if (storedCategory.isNotEmpty) {
      normalizedNames.add(storedCategory);
    } else {
      developer.log(
        '储蓄目标 ${goal.name} 没有设置分类',
        name: 'DynamicStatisticsService',
      );
    }

    // 2. 添加目标名称和分类名称的组合
    if (goalName.isNotEmpty && storedCategory.isNotEmpty) {
      normalizedNames.add(_normalizeCategoryName('$goalName-$storedCategory'));
    }

    // 移除空字符串
    normalizedNames.removeWhere((name) => name.isEmpty);

    // 如果候选集为空，使用目标名称作为候选
    if (normalizedNames.isEmpty && goalName.isNotEmpty) {
      normalizedNames.add(goalName);
    }

    developer.log(
      '储蓄目标分类候选集: ${goal.name} -> $normalizedNames',
      name: 'DynamicStatisticsService',
    );
    
    return normalizedNames;
  }

  /// 判断账单分类是否与储蓄目标分类匹配
  /// 
  /// 实现严格的分类匹配逻辑，确保只有精确匹配组合分类名才会返回true
  /// 避免不同储蓄目标共用同一分类数据
  /// 
  /// 参数：
  /// - billCategoryName: 账单的分类名称
  /// - candidates: 储蓄目标的分类候选集
  /// 
  /// 返回：
  /// - 是否匹配成功
  bool _isSavingCategoryMatch(String billCategoryName, Set<String> candidates) {
    if (candidates.isEmpty) {
      return false;
    }
    
    // 规范化分类名称
    final normalizedBillCategory = _normalizeCategoryName(billCategoryName);
    if (normalizedBillCategory.isEmpty) {
      return false;
    }

    // 1. 精确匹配 - 完全相同的分类名匹配
    if (candidates.contains(normalizedBillCategory)) {
      return true;
    }

    // 2. 组合分类名匹配 - 支持 "目标名称-分类名称" 格式匹配
    // 检查账单分类名是否包含候选分类名，或者候选分类名包含账单分类名
    for (final candidate in candidates) {
      if (normalizedBillCategory.contains(candidate) || candidate.contains(normalizedBillCategory)) {
        return true;
      }
    }
    
    return false;
  }

  /// 拆分分类名称为多个部分
  /// 
  /// 使用正则表达式将复合分类名称拆分为多个部分
  /// 
  /// 参数：
  /// - categoryName: 分类名称
  /// 
  /// 返回：
  /// - 拆分后的分类部分列表
  List<String> _splitCategoryParts(String? categoryName) {
    if (categoryName == null || categoryName.isEmpty) {
      return const [];
    }
    
    return categoryName
        .split(_categorySplitPattern) // 使用正则表达式拆分
        .map(_normalizeCategoryName) // 规范化每个部分
        .where((part) => part.isNotEmpty) // 过滤空部分
        .toList();
  }

  /// 构建交易的唯一键
  /// 
  /// 用于避免重复处理同一笔交易
  /// 
  /// 参数：
  /// - bill: 账单对象
  /// 
  /// 返回：
  /// - 交易的唯一键
  String _buildTransactionKey(Bill bill) {
    // 如果有ID，直接使用ID
    if (bill.id != null) {
      return bill.id.toString();
    }
    
    // 否则使用分类、金额、日期和类型的组合作为唯一键
    return '${bill.categoryName}-${bill.amount}-${bill.transactionDate}-${bill.type}';
  }

  /// 规范化分类名称
  /// 
  /// 去除多余空格，确保分类名称的一致性
  /// 
  /// 参数：
  /// - name: 分类名称
  /// 
  /// 返回：
  /// - 规范化后的分类名称
  String _normalizeCategoryName(String? name) {
    if (name == null) {
      return '';
    }
    
    // 替换多个连续空格为单个空格，并去除首尾空格
    return name.replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  /// 尝试解析账单日期
  /// 
  /// 安全地解析日期字符串，避免解析失败导致程序崩溃
  /// 
  /// 参数：
  /// - dateStr: 日期字符串
  /// 
  /// 返回：
  /// - 解析后的日期对象，失败则返回null
  DateTime? _tryParseBillDate(String dateStr) {
    try {
      return DateTime.parse(dateStr);
    } catch (e) {
      return null;
    }
  }

  /// 计算预算预警等级
  /// 
  /// 根据预算使用比例和是否超支，计算预警等级
  /// 
  /// 参数：
  /// - usage: 预算使用比例
  /// - isOverBudget: 是否已超支
  /// 
  /// 返回：
  /// - 预警等级
  BudgetWarningLevel _calculateWarningLevel(double usage, bool isOverBudget) {
    if (isOverBudget) return BudgetWarningLevel.critical;
    if (usage >= 0.9) return BudgetWarningLevel.high;
    if (usage >= 0.7) return BudgetWarningLevel.medium;
    return BudgetWarningLevel.normal;
  }

  /// 计算储蓄目标状态
  /// 
  /// 根据储蓄目标的完成情况、是否逾期和剩余天数，计算目标状态
  /// 
  /// 参数：
  /// - isCompleted: 是否已完成
  /// - isOverdue: 是否已逾期
  /// - daysRemaining: 剩余天数
  /// 
  /// 返回：
  /// - 储蓄目标状态
  SavingGoalStatus _calculateSavingStatus(
    bool isCompleted,
    bool isOverdue,
    int daysRemaining,
  ) {
    if (isCompleted) return SavingGoalStatus.completed; // 已完成
    if (isOverdue) return SavingGoalStatus.overdue; // 已逾期
    if (daysRemaining <= 7) return SavingGoalStatus.urgent; // 紧急（7天内到期）
    return SavingGoalStatus.inProgress; // 进行中
  }
}

/// 预算实时统计结果
/// 
/// 包含预算的实际支出、预算金额和使用比例
class BudgetRealTimeStats {
  /// 预算ID
  final int? budgetId;
  /// 实际支出金额
  final double actualSpent;
  /// 预算金额
  final double budgetedAmount;
  /// 使用比例 (0-1)
  final double usage;

  /// 构造函数
  BudgetRealTimeStats({
    required this.budgetId,
    required this.actualSpent,
    required this.budgetedAmount,
    required this.usage,
  });
}

/// 储蓄目标实时统计结果
/// 
/// 包含储蓄目标的实际储蓄金额、进度、剩余金额等信息
class SavingGoalRealTimeStats {
  /// 储蓄目标对象
  final SavingGoal goal;
  /// 实际储蓄金额
  final double actualSaved;
  /// 完成进度 (0-1)
  final double progress;
  /// 剩余金额
  final double remaining;
  /// 是否完成
  final bool isCompleted;
  /// 剩余天数
  final int daysRemaining;
  /// 是否逾期
  final bool isOverdue;
  /// 储蓄目标状态
  final SavingGoalStatus status;

  /// 构造函数
  SavingGoalRealTimeStats({
    required this.goal,
    required this.actualSaved,
    required this.progress,
    required this.remaining,
    required this.isCompleted,
    required this.daysRemaining,
    required this.isOverdue,
    required this.status,
  });
}

/// 预算预警等级
/// 
/// 表示预算使用情况的预警等级
enum BudgetWarningLevel {
  normal, // 正常（使用比例 < 70%）
  medium, // 中等预警（70% ≤ 使用比例 < 90%）
  high, // 高级预警（90% ≤ 使用比例 < 100%）
  critical, // 严重预警（超支）
}

/// 储蓄目标状态
/// 
/// 表示储蓄目标的当前状态
enum SavingGoalStatus {
  inProgress, // 进行中
  urgent, // 紧急（7天内到期）
  completed, // 已完成
  overdue, // 已逾期
}
