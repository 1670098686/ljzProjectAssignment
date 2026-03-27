import 'package:flutter/material.dart';
import '../../data/models/bill_model.dart';
import '../../data/services/bill_service.dart';
import 'base_provider.dart';
import 'state_sync_manager.dart';
import '../mixins/event_bus_mixin.dart';
import '../services/event_bus_service.dart';

/// 账单状态管理 Provider
/// 集成统一的错误处理机制
class BillProvider extends BaseProvider with ProviderEventBusMixin {
  final BillService _billService;

  /// 响应StateSyncManager的清除数据通知
  void _onSyncNotification() {
    print('📢 账单Provider收到StateSyncManager同步通知，开始清除数据...');
    clearData();
  }

  BillProvider(this._billService, {super.errorCenter}) {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('bills', _onSyncNotification);
    
    // 监听分类更新事件，当分类更新时重新加载账单数据
    onCategoryUpdated((event) {
      print('📢 BillProvider收到分类更新事件，开始重新加载账单数据...');
      loadBills();
    });
    
    // 监听预算更新事件，当预算更新时重新加载账单数据
    onBudgetUpdated((event) {
      print('📢 BillProvider收到预算更新事件，开始重新加载账单数据...');
      loadBills();
    });
    
    // 监听储蓄目标更新事件，当储蓄目标更新时重新加载账单数据
    onSavingGoalUpdated((event) {
      print('📢 BillProvider收到储蓄目标更新事件，开始重新加载账单数据...');
      loadBills();
    });
    
    // 延迟初始化，避免阻塞应用启动
    // 首页加载时会主动触发数据加载
  }

  List<Bill> _bills = [];
  List<Bill> get bills => _bills;

  /// 初始化时加载数据
  Future<void> _initialLoad() async {
    try {
      print('🔄 BillProvider开始初始化加载数据...');
      await loadBills();
      print('✅ BillProvider初始化数据加载完成，共${_bills.length}条记录');
    } catch (e) {
      print('❌ BillProvider初始化数据加载失败: $e');
      // 不重新抛出异常，避免阻塞应用启动
    }
  }

  /// 获取最近3条账单记录
  List<Bill> get recentBills {
    return _bills.take(3).toList();
  }

  Future<void> loadBills({
    String? startDate,
    String? endDate,
    int? type,
    String? categoryName,
  }) async {
    print('📊 开始加载账单数据...');
    setBusy();
    try {
      print('📊 过滤条件: startDate=$startDate, endDate=$endDate, type=$type, categoryName=$categoryName');
      
      _bills = await _billService.getBills(
        startDate: startDate,
        endDate: endDate,
        type: type,
        categoryName: categoryName,
      );
      
      print('✅ 账单数据加载成功，共${_bills.length}条记录');
      print('📋 数据详情:');
      _bills.forEach((bill) {
        print('  - ID: ${bill.id}, 类型: ${bill.type}, 分类: ${bill.categoryName}, 金额: ${bill.amount}, 日期: ${bill.transactionDate}');
      });
      
      setState(ViewState.success);
    } catch (e) {
      print('❌ 账单数据加载失败: $e');
      setError(
        e,
        retry: () => loadBills(
          startDate: startDate,
          endDate: endDate,
          type: type,
          categoryName: categoryName,
        ),
      );
    }
  }

  Future<bool> addBill(Bill bill, {bool preferLocal = false}) async {
    if (!preferLocal) {
      setBusy();
    }
    try {
      final newBill = await _billService.createBill(
        bill,
        preferLocal: preferLocal,
      );
      _bills.insert(0, newBill); // Add to top of list
      setState(ViewState.success);
      
      // 发布交易创建事件，通知预算模块进行检查
      EventBusService.instance.emitTransactionCreated(newBill);
      
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await addBill(bill, preferLocal: preferLocal);
        },
      );
      return false;
    }
  }

  Future<bool> updateBill(int id, Bill bill) async {
    setBusy();
    try {
      final updatedBill = await _billService.updateBill(id, bill);
      final index = _bills.indexWhere((t) => t.id == id);
      if (index != -1) {
        _bills[index] = updatedBill;
      }
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await updateBill(id, bill);
        },
      );
      return false;
    }
  }

  Future<bool> deleteBill(int id) async {
    setBusy();
    try {
      await _billService.deleteBill(id);
      _bills.removeWhere((t) => t.id == id);
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await deleteBill(id);
        },
      );
      return false;
    }
  }

  /// 清除所有数据
  Future<void> clearData() async {
    try {
      print('🧹 开始清除账单数据...');
      
      // 清除本地数据列表
      _bills.clear();
      
      // 通知监听器数据已清除
      setState(ViewState.idle);
      
      print('✅ 账单数据清除完成');
    } catch (e) {
      print('❌ 清除账单数据失败: $e');
    }
  }

  /// 批量删除账单
  Future<bool> deleteBills(List<int> ids) async {
    setBusy();
    try {
      await _billService.deleteBills(ids);
      _bills.removeWhere((bill) => ids.contains(bill.id));
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await deleteBills(ids);
        },
      );
      return false;
    }
  }

  /// 批量更新账单分类
  Future<bool> updateBillsCategory(List<int> ids, String newCategory) async {
    setBusy();
    try {
      await _billService.updateBillsCategory(ids, newCategory);
      // 重新加载数据以确保状态同步
      await loadBills();
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await updateBillsCategory(ids, newCategory);
        },
      );
      return false;
    }
  }

  /// 批量添加备注
  Future<bool> addBillsRemark(List<int> ids, String remark) async {
    setBusy();
    try {
      await _billService.addBillsRemark(ids, remark);
      // 重新加载数据以确保状态同步
      await loadBills();
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await addBillsRemark(ids, remark);
        },
      );
      return false;
    }
  }

  /// 获取所有账单记录
  List<Bill> getAllBills() {
    return _bills;
  }
}
