import 'package:flutter/material.dart';

import '../../core/services/local_data_service.dart';
import '../../core/services/unified_error_handling_service.dart';
import '../models/statistics_model.dart';
import '../models/bill_model.dart';

class StatisticsService {
  final LocalDataService _localDataService;
  final UnifiedErrorHandlingService _errorHandler;

  StatisticsService()
    : _localDataService = LocalDataService(),
      _errorHandler = UnifiedErrorHandlingService();

  Future<StatisticsSummary> getSummary(
    int year,
    int month, {
    BuildContext? context,
    bool preferLocal = false,
  }) async {
    return await _safeLocalSummary(year, month, context);
  }

  Future<StatisticsSummary> _safeLocalSummary(
    int year,
    int month,
    BuildContext? context,
  ) async {
    try {
      return await _calculateSummaryFromLocal(year, month);
    } catch (e) {
      if (context != null) {
        _errorHandler.handleDatabaseError(
          context: context,
          error: e,
          operation: '获取统计汇总',
        );
      }
      return StatisticsSummary(
        totalIncome: 0.0,
        totalExpense: 0.0,
        balance: 0.0,
        totalDeposits: 0.0,
        totalWithdraws: 0.0,
        netSaving: 0.0,
      );
    }
  }

  /// 从本地数据计算统计汇总
  Future<StatisticsSummary> _calculateSummaryFromLocal(
    int year,
    int month,
  ) async {
    try {
      // 强制刷新数据，确保获取最新的账单信息
      // 这解决了删除预算计划/储蓄目标后的数据一致性问题
      final bills = await _localDataService.getBillsFromLocal();

      // 筛选指定年月的账单
      final filteredBills = bills.where((bill) {
        final billDate = _tryParseBillDate(bill.transactionDate);
        if (billDate == null) {
          return false;
        }
        return billDate.year == year && billDate.month == month;
      }).toList();

      // 计算收入和支出
      double totalIncome = 0.0;
      double totalExpense = 0.0;

      for (final bill in filteredBills) {
        if (bill.type == 1) {
          // 收入
          totalIncome += bill.amount;
        } else {
          // 支出
          totalExpense += bill.amount;
        }
      }

      final balance = totalIncome - totalExpense;

      // 获取储蓄统计
      final savingStats = await getSavingSummary(year, month);

      return StatisticsSummary(
        totalIncome: totalIncome,
        totalExpense: totalExpense,
        balance: balance,
        totalDeposits: savingStats.totalDeposits,
        totalWithdraws: savingStats.totalWithdraws,
        netSaving: savingStats.netAmount,
      );
    } catch (e) {
      // 返回默认统计
      return StatisticsSummary(
        totalIncome: 0.0,
        totalExpense: 0.0,
        balance: 0.0,
        totalDeposits: 0.0,
        totalWithdraws: 0.0,
        netSaving: 0.0,
      );
    }
  }

  /// 从本地数据计算分类统计
  Future<List<CategoryStatistics>> _calculateCategoryStatsFromLocal(
    String startDateStr,
    String endDateStr,
    int type,
  ) async {
    try {
      final startDate = DateTime.parse(startDateStr);
      final endDate = DateTime.parse(endDateStr);

      // 强制刷新数据，确保获取最新的账单信息
      // 解决删除预算计划/储蓄目标后统计页面数据不同步问题
      final bills = await _localDataService.getBillsFromLocal();

      // 筛选指定日期范围和类型的账单
      final filteredBills = bills.where((bill) {
        final billDate = _tryParseBillDate(bill.transactionDate);
        if (billDate == null) {
          return false;
        }
        return billDate.isAfter(startDate.subtract(const Duration(days: 1))) &&
            billDate.isBefore(endDate.add(const Duration(days: 1))) &&
            bill.type == type;
      }).toList();

      // 按分类分组计算金额
      final categoryMap = <String, double>{};

      for (final bill in filteredBills) {
        if (categoryMap.containsKey(bill.categoryName)) {
          categoryMap[bill.categoryName] =
              categoryMap[bill.categoryName]! + bill.amount;
        } else {
          categoryMap[bill.categoryName] = bill.amount;
        }
      }

      // 转换为CategoryStatistics列表
      final categoryStats = <CategoryStatistics>[];
      final totalAmount = categoryMap.values.fold<double>(
        0,
        (sum, value) => sum + value,
      );

      for (final entry in categoryMap.entries) {
        categoryStats.add(
          CategoryStatistics(
            categoryName: entry.key,
            amount: entry.value,
            percentage: totalAmount > 0 ? entry.value / totalAmount : 0.0,
          ),
        );
      }

      // 按金额降序排序
      categoryStats.sort((a, b) => b.amount.compareTo(a.amount));

      return categoryStats;
    } catch (e) {
      // 返回空列表
      return [];
    }
  }

  /// 获取储蓄记录统计汇总
  Future<SavingRecordStats> getSavingSummary(
    int year,
    int month, {
    BuildContext? context,
  }) async {
    final buildContext = context;
    try {
      // 从本地获取所有储蓄记录
      final allSavingRecords = await _localDataService.getSavingRecordsFromLocal();
      
      // 筛选指定年月的记录
      final filteredRecords = allSavingRecords.where((record) {
        return record.createdAt.year == year && record.createdAt.month == month;
      }).toList();
      
      // 计算总存款和总取款
      double totalDeposits = 0.0;
      double totalWithdraws = 0.0;
      int depositCount = 0;
      int withdrawCount = 0;
      
      for (final record in filteredRecords) {
        if (record.type == 'deposit') {
          totalDeposits += record.amount;
          depositCount++;
        } else if (record.type == 'withdraw') {
          totalWithdraws += record.amount;
          withdrawCount++;
        }
      }
      
      // 计算净储蓄和平均值
      final netAmount = totalDeposits - totalWithdraws;
      final averageDeposit = depositCount > 0 ? totalDeposits / depositCount : 0.0;
      final averageWithdraw = withdrawCount > 0 ? totalWithdraws / withdrawCount : 0.0;
      
      return SavingRecordStats(
        totalDeposits: totalDeposits,
        totalWithdraws: totalWithdraws,
        netAmount: netAmount,
        recordCount: filteredRecords.length,
        averageDeposit: averageDeposit,
        averageWithdraw: averageWithdraw,
      );
    } catch (e) {
      // 使用统一错误处理
      if (buildContext != null) {
        _errorHandler.handleDatabaseError(
          context: buildContext,
          error: e,
          operation: '获取储蓄统计汇总',
        );
      }
      return SavingRecordStats(
        totalDeposits: 0.0,
        totalWithdraws: 0.0,
        netAmount: 0.0,
        recordCount: 0,
      );
    }
  }

  Future<List<CategoryStatistics>> getByCategory({
    required String startDate,
    required String endDate,
    required int type,
    BuildContext? context,
    bool preferLocal = false,
  }) async {
    return _safeLocalCategoryStats(startDate, endDate, type, context);
  }

  Future<List<CategoryStatistics>> _safeLocalCategoryStats(
    String startDate,
    String endDate,
    int type,
    BuildContext? context,
  ) async {
    try {
      return await _calculateCategoryStatsFromLocal(startDate, endDate, type);
    } catch (e) {
      if (context != null) {
        _errorHandler.handleDatabaseError(
          context: context,
          error: e,
          operation: '获取分类统计',
        );
      }
      return [];
    }
  }

  /// 获取带储蓄数据的时间趋势统计
  Future<List<TrendStatistics>> getTrend({
    required String startDate,
    required String endDate,
    BuildContext? context,
    bool preferLocal = false,
  }) async {
    return _safeLocalTrend(startDate, endDate, context);
  }

  Future<List<TrendStatistics>> _safeLocalTrend(
    String startDate,
    String endDate,
    BuildContext? context,
  ) async {
    try {
      return await _calculateTrendFromLocal(startDate, endDate);
    } catch (e) {
      if (context != null) {
        _errorHandler.handleDatabaseError(
          context: context,
          error: e,
          operation: '获取趋势统计',
        );
      }
      return [];
    }
  }

  /// 获取指定日期的储蓄统计
  Future<SavingRecordStats> getSavingStatsForDate(
    DateTime date, {
    BuildContext? context,
  }) async {
    final buildContext = context;
    try {
      // 从本地获取所有储蓄记录
      final allSavingRecords = await _localDataService.getSavingRecordsFromLocal();
      
      // 筛选指定日期的记录
      final filteredRecords = allSavingRecords.where((record) {
        return record.createdAt.year == date.year && 
               record.createdAt.month == date.month && 
               record.createdAt.day == date.day;
      }).toList();
      
      // 计算总存款和总取款
      double totalDeposits = 0.0;
      double totalWithdraws = 0.0;
      
      for (final record in filteredRecords) {
        if (record.type == 'deposit') {
          totalDeposits += record.amount;
        } else if (record.type == 'withdraw') {
          totalWithdraws += record.amount;
        }
      }
      
      // 计算净储蓄
      final netAmount = totalDeposits - totalWithdraws;
      
      return SavingRecordStats(
        totalDeposits: totalDeposits,
        totalWithdraws: totalWithdraws,
        netAmount: netAmount,
        recordCount: filteredRecords.length,
      );
    } catch (e) {
      // 使用统一错误处理
      if (buildContext != null) {
        _errorHandler.handleDatabaseError(
          context: buildContext,
          error: e,
          operation: '获取指定日期储蓄统计',
        );
      }
      return SavingRecordStats(
        totalDeposits: 0.0,
        totalWithdraws: 0.0,
        netAmount: 0.0,
        recordCount: 0,
      );
    }
  }

  /// 从本地数据计算趋势统计
  Future<List<TrendStatistics>> _calculateTrendFromLocal(
    String startDateStr,
    String endDateStr,
  ) async {
    try {
      final startDate = DateTime.parse(startDateStr);
      final endDate = DateTime.parse(endDateStr);

      // 强制刷新数据，确保获取最新的账单信息
      // 这解决了删除预算计划/储蓄目标后统计页面数据不同步的关键问题
      final List<Bill> allBills = await _localDataService.getBillsFromLocal();

      final List<TrendStatistics> trendData = [];
      var currentDate = DateTime(
        startDate.year,
        startDate.month,
        startDate.day,
      );
      final lastDate = DateTime(endDate.year, endDate.month, endDate.day);

      // 生成每日数据点
      while (currentDate.isBefore(lastDate) ||
          currentDate.isAtSameMomentAs(lastDate)) {
        final dateStr =
            '${currentDate.year}-${currentDate.month.toString().padLeft(2, '0')}-${currentDate.day.toString().padLeft(2, '0')}';

        // 筛选当天的账单数据
        final dailyBills = allBills.where((bill) {
          final billDate = _tryParseBillDate(bill.transactionDate);
          if (billDate == null) return false;
          return billDate.year == currentDate.year &&
              billDate.month == currentDate.month &&
              billDate.day == currentDate.day;
        }).toList();

        // 计算当天的收入和支出
        double income = 0.0;
        double expense = 0.0;

        for (final bill in dailyBills) {
          if (bill.type == 1) {
            // 收入
            income += bill.amount;
          } else if (bill.type == 2) {
            // 支出
            expense += bill.amount;
          }
        }

        trendData.add(
          TrendStatistics(
            date: dateStr,
            income: income,
            expense: expense,
            deposits: 0.0, // 暂时不计算储蓄数据
            withdraws: 0.0, // 暂时不计算储蓄数据
          ),
        );

        // 移动到下一天
        currentDate = currentDate.add(const Duration(days: 1));
      }

      return trendData;
    } catch (e) {
      // 计算趋势统计失败，返回空列表
      return [];
    }
  }


}

DateTime? _tryParseBillDate(String dateStr) {
  try {
    return DateTime.parse(dateStr);
  } catch (_) {
    return null;
  }
}
