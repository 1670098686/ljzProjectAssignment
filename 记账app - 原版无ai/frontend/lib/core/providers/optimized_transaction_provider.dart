import '../../data/models/bill_model.dart';
import '../../data/services/bill_service.dart';
import 'optimized_base_provider.dart';
import '../mixins/event_bus_mixin.dart';
import 'state_sync_manager.dart';

/// 优化的交易记录状态管理 Provider
/// 
/// 负责管理应用中的所有账单数据，提供优化的状态管理和性能监控
/// 继承自 OptimizedBaseProvider，使用 AsyncOperationMixin 和 ProviderEventBusMixin
/// 实现了高效的账单加载、添加、更新、删除和查询功能
class OptimizedTransactionProvider extends OptimizedBaseProvider 
    with AsyncOperationMixin, ProviderEventBusMixin {
  /// 账单服务实例，用于与数据库交互
  final BillService _billService;

  /// 构造函数
  /// 
  /// 参数：
  /// - billService: 账单服务实例
  /// - errorCenter: 错误中心，用于统一处理错误
  OptimizedTransactionProvider(this._billService, {super.errorCenter}) {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('bills', _onSyncNotification);
  }

  /// 账单数据列表
  List<Bill> _bills = [];
  /// 账单数据的只读访问器
  List<Bill> get bills => _bills;

  /// 优化的账单加载方法
  /// 
  /// 根据指定条件从数据库加载账单数据
  /// 
  /// 参数：
  /// - type: 账单类型（1=收入, 2=支出）
  /// - category: 分类名称
  /// - startDate: 开始日期
  /// - endDate: 结束日期
  /// - limit: 返回的最大记录数
  /// - offset: 查询偏移量
  /// 
  /// 返回：
  /// - Future<void>
  Future<void> loadBills({
    int? type,
    String? category,
    DateTime? startDate,
    DateTime? endDate,
    int limit = 1000,
    int offset = 0,
  }) async {
    await executeAsync(
      () async {
        // 从账单服务获取账单数据
        final loadedBills = await _billService.getBills(
          type: type,
          categoryName: category,
          startDate: startDate?.toIso8601String(),
          endDate: endDate?.toIso8601String(),
        );
        
        // 使用优化的状态更新机制
        updateState(ViewState.idle, () {
          _bills = loadedBills;
        });
        
        return loadedBills;
      },
      'loadBills', // 操作名称，用于性能监控
      showLoading: true, // 显示加载状态
    );
  }

  /// 优化的添加账单方法
  /// 
  /// 将新账单添加到数据库和状态中
  /// 
  /// 参数：
  /// - bill: 要添加的账单对象
  /// 
  /// 返回：
  /// - Future<bool>: 添加成功返回true，失败返回false
  Future<bool> addBill(Bill bill) async {
    return await executeAsync(
      () async {
        // 创建新账单
        final newBill = await _billService.createBill(bill);
        
        // 更新状态，将新账单插入到列表开头
        updateState(ViewState.idle, () {
          _bills.insert(0, newBill); // 插入到列表开头，确保最新账单显示在最前面
        });
        
        // 日志记录
        print('📢 账单创建完成: ${newBill.id}');
        
        return true;
      },
      'addBill',
      showLoading: true,
    );
  }

  /// 优化的批量添加账单方法
  /// 
  /// 批量添加多个账单，用于导入数据等场景
  /// 
  /// 参数：
  /// - bills: 要添加的账单列表
  /// 
  /// 返回：
  /// - Future<bool>: 添加成功返回true，失败返回false
  Future<bool> addBills(List<Bill> bills) async {
    return await executeAsync(
      () async {
        // 创建批量操作列表
        final operations = bills.map((bill) => () => _billService.createBill(bill)).toList();
        
        // 执行批量操作
        final createdBills = await executeBatchAsync(
          operations,
          'batchCreateBills',
          showLoading: false,
        );
        
        // 更新状态，将创建的账单插入到列表开头
        updateState(ViewState.idle, () {
          _bills.insertAll(0, createdBills);
        });
        
        // 日志记录
        print('📢 批量账单创建完成: ${createdBills.length}个');
        
        return true;
      },
      'addBills',
      showLoading: true,
    );
  }

  /// 优化的更新账单方法
  /// 
  /// 更新现有账单的信息
  /// 
  /// 参数：
  /// - bill: 要更新的账单对象（包含更新后的信息）
  /// 
  /// 返回：
  /// - Future<bool>: 更新成功返回true，失败返回false
  Future<bool> updateBill(Bill bill) async {
    return await executeAsync(
      () async {
        // 更新账单
        final updatedBill = await _billService.updateBill(bill.id!, bill);
        
        // 更新状态
        updateState(ViewState.idle, () {
          // 查找要更新的账单索引
          final index = _bills.indexWhere((b) => b.id == bill.id);
          if (index != -1) {
            _bills[index] = updatedBill;
          }
        });
        
        // 日志记录
        print('📢 账单更新完成: ${updatedBill.id}');
        
        return true;
      },
      'updateBill',
      showLoading: true,
    );
  }

  /// 优化的删除账单方法
  /// 
  /// 根据ID删除账单
  /// 
  /// 参数：
  /// - id: 要删除的账单ID
  /// 
  /// 返回：
  /// - Future<bool>: 删除成功返回true，失败返回false
  Future<bool> deleteBill(int id) async {
    return await executeAsync(
      () async {
        // 删除账单
        await _billService.deleteBill(id);
        
        // 更新状态
        updateState(ViewState.idle, () {
          _bills.removeWhere((b) => b.id == id);
        });
        
        // 日志记录
        print('📢 账单删除完成: $id');
        
        return true;
      },
      'deleteBill',
      showLoading: true,
    );
  }

  /// 优化的批量删除账单方法
  /// 
  /// 批量删除多个账单
  /// 
  /// 参数：
  /// - ids: 要删除的账单ID列表
  /// 
  /// 返回：
  /// - Future<bool>: 删除成功返回true，失败返回false
  Future<bool> deleteBills(List<int> ids) async {
    return await executeAsync(
      () async {
        // 创建批量删除操作列表
        final operations = ids.map((id) => () => _billService.deleteBill(id)).toList();
        
        // 执行批量删除
        await executeBatchAsync(
          operations,
          'batchDeleteBills',
          showLoading: false,
        );
        
        // 更新状态
        updateState(ViewState.idle, () {
          _bills.removeWhere((bill) => ids.contains(bill.id));
        });
        
        // 日志记录
        print('📢 批量账单删除完成: ${ids.length}个');
        
        return true;
      },
      'deleteBills',
      showLoading: true,
    );
  }

  /// 优化的条件查询方法
  /// 
  /// 根据多个条件筛选账单
  /// 
  /// 参数：
  /// - type: 账单类型（1=收入, 2=支出）
  /// - category: 分类名称
  /// - startDate: 开始日期
  /// - endDate: 结束日期
  /// - minAmount: 最小金额
  /// - maxAmount: 最大金额
  /// - keyword: 关键词（用于搜索备注和分类）
  /// 
  /// 返回：
  /// - List<Bill>: 符合条件的账单列表
  List<Bill> getBillsByCondition({
    int? type,
    String? category,
    DateTime? startDate,
    DateTime? endDate,
    double? minAmount,
    double? maxAmount,
    String? keyword,
  }) {
    return _bills.where((bill) {
      // 类型筛选
      if (type != null && bill.type != type) {
        return false;
      }
      
      // 分类筛选
      if (category != null && bill.categoryName != category) {
        return false;
      }
      
      // 日期筛选
      if (startDate != null && bill.date.isBefore(startDate)) {
        return false;
      }
      if (endDate != null && bill.date.isAfter(endDate)) {
        return false;
      }
      
      // 金额筛选
      if (minAmount != null && bill.amount < minAmount) {
        return false;
      }
      if (maxAmount != null && bill.amount > maxAmount) {
        return false;
      }
      
      // 关键词筛选（搜索备注和分类）
      if (keyword != null && keyword.isNotEmpty) {
        final searchText = '$keyword ${bill.remark} ${bill.categoryName}'.toLowerCase();
        if (!searchText.contains(keyword.toLowerCase())) {
          return false;
        }
      }
      
      // 所有条件都满足，返回true
      return true;
    }).toList();
  }

  /// 获取按月份分组的账单统计
  /// 
  /// 将账单按月份分组，便于统计和展示
  /// 
  /// 返回：
  /// - Map<String, List<Bill>>: 月份为键，对应月份的账单列表为值
  Map<String, List<Bill>> getBillsGroupedByMonth() {
    final grouped = <String, List<Bill>>{};
    
    for (final bill in _bills) {
      // 生成月份键（格式：YYYY-MM）
      final monthKey = '${bill.date.year}-${bill.date.month.toString().padLeft(2, '0')}';
      // 将账单添加到对应月份的列表中
      grouped.putIfAbsent(monthKey, () => []).add(bill);
    }
    
    return grouped;
  }

  /// 获取按分类分组的账单统计
  /// 
  /// 将账单按分类分组，便于统计和展示
  /// 
  /// 返回：
  /// - Map<String, List<Bill>>: 分类名称为键，对应分类的账单列表为值
  Map<String, List<Bill>> getBillsGroupedByCategory() {
    final grouped = <String, List<Bill>>{};
    
    for (final bill in _bills) {
      // 将账单添加到对应分类的列表中
      grouped.putIfAbsent(bill.categoryName, () => []).add(bill);
    }
    
    return grouped;
  }

  /// 计算总金额
  /// 
  /// 计算所有账单或指定类型账单的总金额
  /// 
  /// 参数：
  /// - type: 账单类型（1=收入, 2=支出），为null时计算所有类型
  /// 
  /// 返回：
  /// - double: 总金额
  double getTotalAmount({int? type}) {
    return _bills
        .where((bill) => type == null || bill.type == type) // 筛选指定类型
        .fold(0.0, (sum, bill) => sum + bill.amount); // 累加金额
  }

  /// 获取所有账单（用于其他Provider查询）
  /// 
  /// 返回不可修改的账单列表，防止外部修改
  /// 
  /// 返回：
  /// - List<Bill>: 不可修改的账单列表
  List<Bill> getAllBills() {
    return List.unmodifiable(_bills);
  }

  /// 清除所有数据
  /// 
  /// 清空状态中的账单数据
  /// 
  /// 返回：
  /// - Future<void>
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

  /// 同步通知处理方法
  /// 
  /// 处理来自StateSyncManager的同步通知，清除本地数据
  void _onSyncNotification() {
    print('📢 交易记录Provider收到StateSyncManager同步通知，开始清除数据...');
    clearData();
  }

  /// 获取性能统计
  /// 
  /// 重写父类方法，添加账单相关的性能统计
  /// 
  /// 返回：
  /// - Map<String, dynamic>: 包含性能统计信息的映射
  @override
  Map<String, dynamic> getPerformanceStats() {
    final stats = super.getPerformanceStats();
    stats.addAll({
      'totalBills': _bills.length, // 账单总数
      'memoryUsage': '${(_bills.length * 200).toInt()} bytes', // 估算内存使用（每个账单约200字节）
    });
    return stats;
  }

  /// 导出数据
  /// 
  /// 将账单数据导出为Map列表，便于序列化和传输
  /// 
  /// 返回：
  /// - Future<List<Map<String, dynamic>>>: 导出的账单数据列表
  Future<List<Map<String, dynamic>>> exportBills() async {
    return await executeAsync(
      () async {
        // 将Bill对象转换为Map
        return _bills.map((bill) => {
          'id': bill.id,
          'type': bill.type,
          'category': bill.categoryName,
          'amount': bill.amount,
          'remark': bill.remark,
          'date': bill.transactionDate,
          'imagePath': bill.imagePath,
        }).toList();
      },
      'exportBills',
      showLoading: false,
    );
  }
}

