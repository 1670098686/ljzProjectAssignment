import '../../data/models/bill_model.dart';
import '../../data/services/bill_service.dart';
import './optimized_base_provider.dart';
import 'state_sync_manager.dart';

/// 优化的账单状态管理 Provider
/// 继承优化基类，提供更好的性能和监控
class OptimizedBillProvider extends OptimizedBaseProvider 
    with AsyncOperationMixin {
  final BillService _billService;

  OptimizedBillProvider(this._billService, {super.errorCenter}) {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('bills', _onSyncNotification);
  }

  List<Bill> _bills = [];
  List<Bill> get bills => _bills;

  /// 获取所有账单记录
  List<Bill> getAllBills() {
    return _bills;
  }

  /// 获取最近3条账单记录
  List<Bill> get recentBills {
    return _bills.take(3).toList();
  }

  /// 优化的加载账单方法
  Future<void> loadBills({
    String? startDate,
    String? endDate,
    int? type,
    String? categoryName,
  }) async {
    await executeAsync(
      () async {
        final loadedBills = await _billService.getBills(
          startDate: startDate,
          endDate: endDate,
          type: type,
          categoryName: categoryName,
        );
        
        updateState(ViewState.idle, () {
          _bills = loadedBills;
        });
        
        return loadedBills;
      },
      'loadBills',
      showLoading: true,
    );
  }

  /// 优化的添加账单方法
  Future<Bill> addBill(Bill bill, {bool preferLocal = false}) async {
    return await executeAsync(
      () async {
        final newBill = await _billService.createBill(
          bill,
          preferLocal: preferLocal,
        );
        
        updateState(ViewState.idle, () {
          _bills.insert(0, newBill); // Add to top of list
        });
        
        return newBill;
      },
      'addBill',
      showLoading: !preferLocal,
    );
  }

  /// 优化的更新账单方法
  Future<bool> updateBill(int id, Bill bill) async {
    return await executeAsync(
      () async {
        final updatedBill = await _billService.updateBill(id, bill);
        
        updateState(ViewState.idle, () {
          final index = _bills.indexWhere((t) => t.id == id);
          if (index != -1) {
            _bills[index] = updatedBill;
          }
        });
        
        return true;
      },
      'updateBill_$id',
      showLoading: true,
    );
  }

  /// 优化的删除账单方法
  Future<bool> deleteBill(int id) async {
    return await executeAsync(
      () async {
        await _billService.deleteBill(id);
        
        updateState(ViewState.idle, () {
          _bills.removeWhere((t) => t.id == id);
        });
        
        return true;
      },
      'deleteBill_$id',
      showLoading: true,
    );
  }

  /// 优化的批量删除账单方法
  Future<bool> deleteBills(List<int> ids) async {
    if (ids.isEmpty) return true;

    return await executeAsync(
      () async {
        await _billService.deleteBills(ids);
        
        updateState(ViewState.idle, () {
          _bills.removeWhere((bill) => ids.contains(bill.id));
        });
        
        return true;
      },
      'deleteBills_${ids.length}',
      showLoading: true,
    );
  }

  /// 优化的批量更新账单分类方法
  Future<bool> updateBillsCategory(List<int> ids, String newCategory) async {
    if (ids.isEmpty) return true;

    return await executeAsync(
      () async {
        await _billService.updateBillsCategory(ids, newCategory);
        
        updateState(ViewState.idle, () {
          // 局部更新受影响的项目
          for (var i = 0; i < _bills.length; i++) {
            if (ids.contains(_bills[i].id)) {
              _bills[i] = _bills[i].copyWith(categoryName: newCategory);
            }
          }
        });
        
        return true;
      },
      'updateBillsCategory_${ids.length}',
      showLoading: true,
    );
  }

  /// 优化的批量添加备注方法
  Future<bool> addBillsRemark(List<int> ids, String remark) async {
    if (ids.isEmpty) return true;

    return await executeAsync(
      () async {
        await _billService.addBillsRemark(ids, remark);
        
        updateState(ViewState.idle, () {
          // 局部更新受影响的项目
          for (var i = 0; i < _bills.length; i++) {
            if (ids.contains(_bills[i].id)) {
              final currentRemark = _bills[i].remark ?? '';
              final newRemark = currentRemark.isEmpty 
                  ? remark 
                  : '$currentRemark $remark';
              _bills[i] = _bills[i].copyWith(remark: newRemark);
            }
          }
        });
        
        return true;
      },
      'addBillsRemark_${ids.length}',
      showLoading: true,
    );
  }

  /// 批量添加账单
  Future<bool> addBills(List<Bill> bills) async {
    if (bills.isEmpty) return true;

    return await executeAsync(
      () async {
        final operations = bills.map((bill) => () async {
          return await _billService.createBill(bill, preferLocal: true);
        }).toList();
        
        final createdBills = await executeBatchAsync(
          operations,
          'batchCreateBills',
          showLoading: false,
        );
        
        updateState(ViewState.idle, () {
          // 在列表顶部插入新创建的账单
          _bills.insertAll(0, createdBills);
        });
        
        return true;
      },
      'addBills',
      showLoading: true,
    );
  }

  /// 按条件搜索账单
  List<Bill> searchBills({
    String? keyword,
    double? minAmount,
    double? maxAmount,
    String? categoryName,
    DateTime? startDate,
    DateTime? endDate,
  }) {
    return _bills.where((bill) {
      // 关键词搜索
      if (keyword != null && keyword.isNotEmpty) {
        final searchText = keyword.toLowerCase();
        final matchesSearch = bill.categoryName.toLowerCase().contains(searchText) ||
            (bill.remark?.toLowerCase().contains(searchText) ?? false);
        if (!matchesSearch) return false;
      }

      // 金额范围
      if (minAmount != null && bill.amount < minAmount) return false;
      if (maxAmount != null && bill.amount > maxAmount) return false;

      // 分类名称
      if (categoryName != null && bill.categoryName != categoryName) return false;

      // 日期范围
      if (startDate != null && bill.date.isBefore(startDate)) return false;
      if (endDate != null && bill.date.isAfter(endDate)) return false;

      return true;
    }).toList();
  }

  /// 获取特定类型的账单
  List<Bill> getBillsByType(int type) {
    return _bills.where((bill) => bill.type == type).toList();
  }

  /// 获取收入账单
  List<Bill> get incomeBills {
    return getBillsByType(1);
  }

  /// 获取支出账单
  List<Bill> get expenseBills {
    return getBillsByType(2);
  }

  /// 获取特定分类的账单
  List<Bill> getBillsByCategory(String categoryName) {
    return _bills.where((bill) => bill.categoryName == categoryName).toList();
  }

  /// 获取最近N天的账单
  List<Bill> getBillsByDateRange(DateTime startDate, DateTime endDate) {
    return _bills.where((bill) {
      return bill.date.isAfter(startDate.subtract(const Duration(days: 1))) &&
             bill.date.isBefore(endDate.add(const Duration(days: 1)));
    }).toList();
  }

  /// 获取账单统计信息
  Map<String, dynamic> getBillStatistics() {
    final totalBills = _bills.length;
    final incomeBills = this.incomeBills;
    final expenseBills = this.expenseBills;
    
    final totalIncome = incomeBills.fold(0.0, (sum, bill) => sum + bill.amount);
    final totalExpense = expenseBills.fold(0.0, (sum, bill) => sum + bill.amount);
    final netIncome = totalIncome - totalExpense;

    // 按分类统计
    final categoryStats = <String, double>{};
    for (final bill in _bills) {
      categoryStats[bill.categoryName] = (categoryStats[bill.categoryName] ?? 0) + bill.amount;
    }

    // 按日期统计（最近30天）
    final now = DateTime.now();
    final thirtyDaysAgo = now.subtract(const Duration(days: 30));
    final recentBills = getBillsByDateRange(thirtyDaysAgo, now);
    final dailyAverage = recentBills.isNotEmpty 
        ? recentBills.fold(0.0, (sum, bill) => sum + bill.amount) / recentBills.length
        : 0.0;

    return {
      'totalBills': totalBills,
      'incomeBills': incomeBills.length,
      'expenseBills': expenseBills.length,
      'totalIncome': totalIncome,
      'totalExpense': totalExpense,
      'netIncome': netIncome,
      'categoryStats': categoryStats,
      'dailyAverage': dailyAverage,
      'recentBillsCount': recentBills.length,
    };
  }

  /// 获取性能统计
  @override
  Map<String, dynamic> getPerformanceStats() {
    final stats = super.getPerformanceStats();
    final billStats = getBillStatistics();
    stats.addAll({
      'totalBills': billStats['totalBills'],
      'incomeBills': billStats['incomeBills'],
      'expenseBills': billStats['expenseBills'],
      'totalIncome': billStats['totalIncome'],
      'totalExpense': billStats['totalExpense'],
      'netIncome': billStats['netIncome'],
      'dailyAverage': billStats['dailyAverage'],
    });
    return stats;
  }

  /// 导出账单数据
  Future<List<Map<String, dynamic>>> exportBills() async {
    return await executeAsync(
      () async {
        return _bills.map((bill) => {
          'id': bill.id,
          'type': bill.type,
          'category': bill.categoryName,
          'amount': bill.amount,
          'remark': bill.remark,
          'date': bill.date.toIso8601String(),
          'isIncome': bill.type == 1,
        }).toList();
      },
      'exportBills',
      showLoading: false,
    );
  }

  /// 清除所有数据
  Future<void> clearData() async {
    await executeAsync(
      () async {
        updateState(ViewState.idle, () {
          _bills.clear();
        });
        return true;
      },
      'clearData',
      showLoading: false,
    );
  }

  /// StateSyncManager同步通知处理方法
  void _onSyncNotification() {
    print('📢 账单Provider收到StateSyncManager同步通知，开始清除数据...');
    clearData();
  }

  /// 获取月度账单
  List<Bill> getMonthlyBills(int year, int month) {
    final startDate = DateTime(year, month, 1);
    final endDate = DateTime(year, month + 1, 0);
    return getBillsByDateRange(startDate, endDate);
  }

  /// 获取年度账单
  List<Bill> getYearlyBills(int year) {
    final startDate = DateTime(year, 1, 1);
    final endDate = DateTime(year, 12, 31);
    return getBillsByDateRange(startDate, endDate);
  }

  /// 获取最大金额的账单
  Bill? getMaxAmountBill() {
    if (_bills.isEmpty) return null;
    return _bills.reduce((a, b) => a.amount > b.amount ? a : b);
  }

  /// 获取最小金额的账单
  Bill? getMinAmountBill() {
    if (_bills.isEmpty) return null;
    return _bills.reduce((a, b) => a.amount < b.amount ? a : b);
  }
}