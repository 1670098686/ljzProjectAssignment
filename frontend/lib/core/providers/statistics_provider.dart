import 'dart:developer' as developer;
import 'package:flutter/foundation.dart'; // 用于listEquals

import '../../data/models/statistics_model.dart';
import '../../data/services/statistics_service.dart';
import '../../core/services/event_bus_service.dart';
import 'base_provider.dart';

class StatisticsProvider extends BaseProvider {
  final StatisticsService _statisticsService;

  StatisticsProvider(this._statisticsService, {super.errorCenter}) {
    // 监听预算删除事件，自动刷新统计数据
    EventBusService.instance.eventBus.on<BudgetDeletedEvent>().listen((event) {
      developer.log('接收到预算删除事件，刷新统计数据', name: 'StatisticsProvider');
      _refreshAllStats();
    });

    // 监听储蓄目标删除事件，自动刷新统计数据
    EventBusService.instance.eventBus.on<SavingGoalDeletedEvent>().listen((event) {
      developer.log('接收到储蓄目标删除事件，刷新统计数据', name: 'StatisticsProvider');
      _refreshAllStats();
    });
  }

  StatisticsSummary? _summary;
  StatisticsSummary? get summary => _summary;

  List<CategoryStatistics> _categoryStats = [];
  List<CategoryStatistics> get categoryStats => _categoryStats;

  List<TrendStatistics> _trendStats = [];
  List<TrendStatistics> get trendStats => _trendStats;

  /// 获取错误信息
  String? get error {
    return hasError ? (errorMessage ?? '未知错误') : null;
  }

  /// 刷新所有统计数据
  void _refreshAllStats() {
    developer.log('开始刷新所有统计数据', name: 'StatisticsProvider');
    resetStats();
    notifyListeners();
  }

  Future<void> loadSummary(
    int year,
    int month, {
    bool preferLocal = false,
  }) async {
    try {
      final newSummary = await _statisticsService.getSummary(
        year,
        month,
        preferLocal: preferLocal,
      );
      
      // 只有当数据发生变化时才更新
      if (_summary?.totalIncome != newSummary.totalIncome ||
          _summary?.totalExpense != newSummary.totalExpense ||
          _summary?.balance != newSummary.balance) {
        _summary = newSummary;
        if (_categoryStats.isNotEmpty && _trendStats.isNotEmpty) {
          setState(ViewState.success);
        }
        // 不调用notifyListeners，让统计页面自己管理状态
      }
    } catch (e) {
      setError(e);
    }
  }

  Future<void> loadCategoryStats({
    required String startDate,
    required String endDate,
    required int type,
    bool preferLocal = false,
  }) async {
    try {
      // 存储当前状态，避免不必要的notifyListeners调用
      final previousStats = List<CategoryStatistics>.from(_categoryStats);
      
      final newStats = await _statisticsService.getByCategory(
        startDate: startDate,
        endDate: endDate,
        type: type,
        preferLocal: preferLocal,
      );
      
      // 只有当数据发生变化时才更新
      if (!listEquals(previousStats, newStats)) {
        _categoryStats = newStats;
        
        // 不直接调用notifyListeners，让统计页面自己管理状态
        // 这样在切换分类明细时，页面不会自动刷新
      }
    } catch (e) {
      setError(e);
    }
  }

  Future<void> loadTrendStats({
    required String startDate,
    required String endDate,
    bool preferLocal = false,
  }) async {
    try {
      final newTrendStats = await _statisticsService.getTrend(
        startDate: startDate,
        endDate: endDate,
        preferLocal: preferLocal,
      );
      
      // 只有当数据发生变化时才更新
      if (!listEquals(_trendStats, newTrendStats)) {
        _trendStats = newTrendStats;
        if (_summary != null && _categoryStats.isNotEmpty) {
          setState(ViewState.success);
        }
        // 不调用notifyListeners，让统计页面自己管理状态
      }
    } catch (e) {
      setError(e);
    }
  }

  /// 重置所有统计数据，用于数据刷新
  void resetStats() {
    _summary = null;
    _categoryStats.clear();
    _trendStats.clear();
    setBusy();
  }
}
