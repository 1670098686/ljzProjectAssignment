import '../../core/services/local_data_service.dart';
import '../../core/services/unified_error_handling_service.dart';
import '../models/bill_model.dart';
import '../models/category_model.dart';
import './category_service.dart';

class BillService {
  final LocalDataService _localDataService;
  final UnifiedErrorHandlingService _errorHandler;
  final CategoryService _categoryService;

  BillService({LocalDataService? localDataService})
    : _localDataService = localDataService ?? LocalDataService(),
      _errorHandler = UnifiedErrorHandlingService(),
      _categoryService = CategoryService();

  /// 从本地数据库获取账单列表
  Future<List<Bill>> getBills({
    String? startDate,
    String? endDate,
    int? type,
    String? categoryName,
  }) async {
    try {
      print('🔍 BillService开始获取账单数据...');
      print('🔍 过滤条件: startDate=$startDate, endDate=$endDate, type=$type, categoryName=$categoryName');
      
      final bills = await _localDataService.getBillsFromLocal(
        startDate: startDate,
        endDate: endDate,
        type: type,
        categoryName: categoryName,
      );
      
      print('✅ BillService获取到${bills.length}条账单数据');
      return bills;
    } catch (e) {
      print('❌ BillService获取账单数据失败: $e');
      _errorHandler.handleError('获取账单数据', e);
      return [];
    }
  }

  /// 创建新账单并保存到本地数据库
  Future<Bill> createBill(Bill bill, {bool preferLocal = false}) async {
    try {
      // 确保分类存在
      await _categoryService.ensureCategoryExists(bill.categoryName, bill.type);
      
      // 调用saveBillToLocal方法并获取返回的带有正确id的Bill对象
      final savedBill = await _localDataService.saveBillToLocal(bill);
      print('✅ BillService: 账单创建成功，返回带有id的Bill对象: $savedBill');
      return savedBill;
    } catch (e) {
      print('❌ BillService创建账单失败: $e');
      _errorHandler.handleError('创建账单', e);
      rethrow;
    }
  }

  /// 从本地数据库获取单个账单
  Future<Bill?> getBill(int id) async {
    try {
      final bills = await getBills();
      try {
        return bills.firstWhere((bill) => bill.id == id);
      } catch (e) {
        return null; // 没有找到指定ID的账单
      }
    } catch (e) {
      // 使用统一错误处理（不依赖BuildContext）
      _errorHandler.handleError('获取单个账单', e);
      return null;
    }
  }

  /// 更新账单
  Future<Bill> updateBill(int id, Bill bill) async {
    try {
      final updatedBill = bill.copyWith(id: id);
      await _localDataService.saveBillToLocal(updatedBill);
      return updatedBill;
    } catch (e) {
      _errorHandler.handleError('更新账单', e);
      rethrow;
    }
  }

  /// 删除账单
  Future<void> deleteBill(int id) async {
    try {
      await _localDataService.deleteBillFromLocal(id);
    } catch (inner) {
      _errorHandler.handleError('删除账单', inner);
      rethrow;
    }
  }

  /// 批量删除账单
  Future<void> deleteBills(List<int> ids) async {
    try {
      for (final id in ids) {
        await deleteBill(id);
      }
    } catch (e) {
      _errorHandler.handleError('批量删除账单', e);
      rethrow;
    }
  }

  /// 批量更新账单分类
  Future<void> updateBillsCategory(List<int> ids, String newCategory) async {
    try {
      if (ids.isEmpty) return;
      
      print('🔍 BillService开始批量更新分类: ids=$ids, newCategory=$newCategory');
      
      // 一次性获取所有账单
      final allBills = await getBills();
      final billsToUpdate = allBills.where((bill) => ids.contains(bill.id)).toList();
      
      if (billsToUpdate.isEmpty) {
        print('⚠️ 未找到需要更新的账单');
        return;
      }
      
      print('📝 找到 ${billsToUpdate.length} 条需要更新分类的账单');
      
      // 批量更新所有账单
      for (final bill in billsToUpdate) {
        final updatedBill = bill.copyWith(categoryName: newCategory);
        await _localDataService.saveBillToLocal(updatedBill);
        print('✅ 更新账单 ${bill.id} 分类为: $newCategory');
      }
      
      print('🎉 批量更新分类完成，共更新 ${billsToUpdate.length} 条账单');
    } catch (e) {
      print('❌ BillService批量更新分类失败: $e');
      _errorHandler.handleError('批量更新账单分类', e);
      rethrow;
    }
  }

  /// 批量添加备注
  Future<void> addBillsRemark(List<int> ids, String remark) async {
    try {
      for (final id in ids) {
        final bill = await getBill(id);
        if (bill == null) {
          continue;
        }
        final currentRemark = bill.remark ?? '';
        final newRemark = remark.isEmpty
            ? currentRemark
            : '$currentRemark${currentRemark.isEmpty ? '' : '\n'}$remark';
        final updatedBill = bill.copyWith(remark: newRemark);
        await updateBill(id, updatedBill);
      }
    } catch (e) {
      _errorHandler.handleError('批量添加备注', e);
      rethrow;
    }
  }


}
