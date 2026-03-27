import 'dart:developer' as developer;

import '../../data/models/statistics_model.dart';
import '../../data/services/statistics_service.dart';
import '../../core/services/event_bus_service.dart';
import './optimized_base_provider.dart';

/// 优化的统计信息 Provider
/// 继承优化基类，提供更好的性能监控和状态管理
class OptimizedStatisticsProvider extends OptimizedBaseProvider 
    with AsyncOperationMixin {
  final StatisticsService _statisticsService;

  OptimizedStatisticsProvider(this._statisticsService, {super.errorCenter}) {
    // 监听预算删除事件，自动刷新统计数据
    _initializeEventListeners();
  }

  StatisticsSummary? _summary;
  StatisticsSummary? get summary => _summary;

  List<CategoryStatistics> _categoryStats = [];
  List<CategoryStatistics> get categoryStats => _categoryStats;

  List<TrendStatistics> _trendStats = [];
  List<TrendStatistics> get trendStats => _trendStats;

  /// 初始化事件监听器
  void _initializeEventListeners() {
    try {
      // 监听预算相关事件
      EventBusService.instance.eventBus.on<BudgetDeletedEvent>().listen((event) {
        developer.log('接收到预算删除事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('budget_deleted');
      });

      // 监听储蓄目标相关事件
      EventBusService.instance.eventBus.on<SavingGoalDeletedEvent>().listen((event) {
        developer.log('接收到储蓄目标删除事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('saving_goal_deleted');
      });

      // 监听储蓄目标更新事件
      EventBusService.instance.eventBus.on<SavingGoalUpdatedEvent>().listen((event) {
        developer.log('接收到储蓄目标更新事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('saving_goal_updated');
      });

      // 监听分类相关事件
      EventBusService.instance.eventBus.on<CategoryDeletedEvent>().listen((event) {
        developer.log('接收到分类删除事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('category_deleted');
      });

      // 监听交易记录相关事件
      EventBusService.instance.eventBus.on<BillUpdatedEvent>().listen((event) {
        developer.log('接收到交易记录更新事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('bill_updated');
      });

      EventBusService.instance.eventBus.on<BillCreatedEvent>().listen((event) {
        developer.log('接收到交易记录创建事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('bill_created');
      });

      EventBusService.instance.eventBus.on<BillDeletedEvent>().listen((event) {
        developer.log('接收到交易记录删除事件，刷新统计数据', name: 'OptimizedStatisticsProvider');
        _refreshAllStats('bill_deleted');
      });

      developer.log('OptimizedStatisticsProvider 事件监听器初始化完成', name: 'OptimizedStatisticsProvider');
    } catch (e) {
      developer.log('初始化事件监听器失败: $e', name: 'OptimizedStatisticsProvider', error: e);
    }
  }

  /// 优化的刷新所有统计数据方法
  Future<void> _refreshAllStats(String source) async {
    await executeAsync(
      () async {
        developer.log('开始刷新所有统计数据 (来源: $source)', name: 'OptimizedStatisticsProvider');
        
        // 清除现有统计数据
        resetStats();
        
        // 如果有summary数据，重新加载
        if (_summary != null) {
          await loadSummary(
            _summary!.year,
            _summary!.month,
            preferLocal: true,
          );
        }
        
        // 如果有分类统计，重新加载
        if (_categoryStats.isNotEmpty) {
          await loadCategoryStats(
            startDate: DateTime.now().subtract(const Duration(days: 30)).toIso8601String().split('T')[0],
            endDate: DateTime.now().toIso8601String().split('T')[0],
            type: 2, // 默认支出类型
            preferLocal: true,
          );
        }
        
        // 如果有趋势统计，重新加载
        if (_trendStats.isNotEmpty) {
          await loadTrendStats(
            startDate: DateTime.now().subtract(const Duration(days: 90)).toIso8601String().split('T')[0],
            endDate: DateTime.now().toIso8601String().split('T')[0],
            preferLocal: true,
          );
        }
        
        developer.log('统计数据刷新完成 (来源: $source)', name: 'OptimizedStatisticsProvider');
        return true;
      },
      'refreshAllStats_$source',
      showLoading: false,
    );
  }

  /// 优化的加载汇总统计方法
  Future<void> loadSummary(
    int year,
    int month, {
    bool preferLocal = false,
  }) async {
    await executeAsync(
      () async {
        final summaryData = await _statisticsService.getSummary(
          year,
          month,
          preferLocal: preferLocal,
        );
        
        updateState(ViewState.idle, () {
          _summary = summaryData;
        });
        
        return summaryData;
      },
      'loadSummary_${year}_$month',
      showLoading: true,
    );
  }

  /// 优化的加载分类统计方法
  Future<void> loadCategoryStats({
    required String startDate,
    required String endDate,
    required int type,
    bool preferLocal = false,
  }) async {
    await executeAsync(
      () async {
        final categoryData = await _statisticsService.getByCategory(
          startDate: startDate,
          endDate: endDate,
          type: type,
          preferLocal: preferLocal,
        );
        
        updateState(ViewState.idle, () {
          _categoryStats = categoryData;
        });
        
        return categoryData;
      },
      'loadCategoryStats_$type',
      showLoading: true,
    );
  }

  /// 优化的加载趋势统计方法
  Future<void> loadTrendStats({
    required String startDate,
    required String endDate,
    bool preferLocal = false,
  }) async {
    await executeAsync(
      () async {
        final trendData = await _statisticsService.getTrend(
          startDate: startDate,
          endDate: endDate,
          preferLocal: preferLocal,
        );
        
        updateState(ViewState.idle, () {
          _trendStats = trendData;
        });
        
        return trendData;
      },
      'loadTrendStats',
      showLoading: true,
    );
  }

  /// 批量加载所有统计数据
  Future<void> loadAllStats({
    required int year,
    required int month,
    String? startDate,
    String? endDate,
    bool preferLocal = false,
  }) async {
    final summaryStartDate = startDate ?? DateTime(year, month, 1).toIso8601String().split('T')[0];
    final summaryEndDate = endDate ?? DateTime(year, month + 1, 0).toIso8601String().split('T')[0];

    await executeAsync(
      () async {
        // 并行加载所有统计数据
        final results = await Future.wait([
          _statisticsService.getSummary(year, month, preferLocal: preferLocal),
          _statisticsService.getByCategory(
            startDate: summaryStartDate,
            endDate: summaryEndDate,
            type: 2, // 支出
            preferLocal: preferLocal,
          ),
          _statisticsService.getByCategory(
            startDate: summaryStartDate,
            endDate: summaryEndDate,
            type: 1, // 收入
            preferLocal: preferLocal,
          ),
          _statisticsService.getTrend(
            startDate: DateTime(year, month, 1).subtract(const Duration(days: 90)).toIso8601String().split('T')[0],
            endDate: summaryEndDate,
            preferLocal: preferLocal,
          ),
        ], eagerError: true);

        updateState(ViewState.idle, () {
          _summary = results[0] as StatisticsSummary;
          // 合并收入和支出分类统计
          final expenseStats = results[1] as List<CategoryStatistics>;
          final incomeStats = results[2] as List<CategoryStatistics>;
          _categoryStats = [...expenseStats, ...incomeStats];
          _trendStats = results[3] as List<TrendStatistics>;
        });

        developer.log('所有统计数据加载完成', name: 'OptimizedStatisticsProvider');
        return true;
      },
      'loadAllStats_${year}_$month',
      showLoading: true,
    );
  }

  /// 重置所有统计数据，用于数据刷新
  Future<void> resetStats() async {
    updateState(ViewState.idle, () {
      _summary = null;
      _categoryStats.clear();
      _trendStats.clear();
    });
  }

  /// 获取分类统计（按类型过滤）
  List<CategoryStatistics> getCategoryStatsByType(int type) {
    return _categoryStats.where((stat) => stat.type == type).toList();
  }

  /// 获取收入分类统计
  List<CategoryStatistics> get incomeCategoryStats {
    return getCategoryStatsByType(1);
  }

  /// 获取支出分类统计
  List<CategoryStatistics> get expenseCategoryStats {
    return getCategoryStatsByType(2);
  }

  /// 计算总支出金额
  double get totalExpense {
    return _summary?.totalExpense ?? 0.0;
  }

  /// 计算总收入金额
  double get totalIncome {
    return _summary?.totalIncome ?? 0.0;
  }

  /// 计算净收入
  double get netIncome {
    return totalIncome - totalExpense;
  }

  /// 获取最支出最高的分类
  CategoryStatistics? get topExpenseCategory {
    if (_categoryStats.isEmpty) return null;
    return _categoryStats
        .where((stat) => stat.type == 2)
        .reduce((a, b) => a.amount > b.amount ? a : b);
  }

  /// 获取最收入最高的分类
  CategoryStatistics? get topIncomeCategory {
    if (_categoryStats.isEmpty) return null;
    return _categoryStats
        .where((stat) => stat.type == 1)
        .reduce((a, b) => a.amount > b.amount ? a : b);
  }

  /// 获取性能统计
  @override
  Map<String, dynamic> getPerformanceStats() {
    final stats = super.getPerformanceStats();
    stats.addAll({
      'hasSummary': _summary != null,
      'categoryStatsCount': _categoryStats.length,
      'trendStatsCount': _trendStats.length,
      'totalExpense': totalExpense,
      'totalIncome': totalIncome,
      'netIncome': netIncome,
      'topExpenseCategory': topExpenseCategory?.categoryName,
      'topIncomeCategory': topIncomeCategory?.categoryName,
    });
    return stats;
  }

  /// 导出统计数据
  Future<Map<String, dynamic>> exportStatistics() async {
    return await executeAsync(
      () async {
        return {
          'summary': {
            'year': _summary?.year,
            'month': _summary?.month,
            'totalIncome': totalIncome,
            'totalExpense': totalExpense,
            'netIncome': netIncome,
            'recordCount': _summary?.recordCount ?? 0,
          },
          'categoryStats': _categoryStats.map((stat) => {
            'categoryName': stat.categoryName,
            'type': stat.type,
            'amount': stat.amount,
            'recordCount': stat.recordCount,
            'percentage': stat.percentage,
          }).toList(),
          'trendStats': _trendStats.map((stat) => {
            'date': stat.date.toIso8601String(),
            'income': stat.income,
            'expense': stat.expense,
            'netIncome': stat.netIncome,
          }).toList(),
        };
      },
      'exportStatistics',
      showLoading: false,
    );
  }

  /// 清除所有数据
  Future<void> clearData() async {
    await executeAsync(
      () async {
        updateState(ViewState.idle, () {
          _summary = null;
          _categoryStats.clear();
          _trendStats.clear();
        });
        return true;
      },
      'clearData',
      showLoading: false,
    );
  }
}

/// 事件类定义（如果EventBusService中不存在）
class BudgetDeletedEvent {
  final int budgetId;
  final String categoryName;
  
  BudgetDeletedEvent(this.budgetId, this.categoryName);
}

class SavingGoalDeletedEvent {
  final int goalId;
  final String goalName;
  
  SavingGoalDeletedEvent(this.goalId, this.goalName);
}

class SavingGoalUpdatedEvent {
  final int goalId;
  final String goalName;
  
  SavingGoalUpdatedEvent(this.goalId, this.goalName);
}

class CategoryDeletedEvent {
  final int categoryId;
  final String categoryName;
  
  CategoryDeletedEvent(this.categoryId, this.categoryName);
}

class BillUpdatedEvent {
  final int billId;
  final String categoryName;
  
  BillUpdatedEvent(this.billId, this.categoryName);
}

class BillCreatedEvent {
  final int billId;
  final String categoryName;
  
  BillCreatedEvent(this.billId, this.categoryName);
}

class BillDeletedEvent {
  final int billId;
  final String categoryName;
  
  BillDeletedEvent(this.billId, this.categoryName);
}