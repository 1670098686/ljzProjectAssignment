import '../../data/models/saving_goal_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/bill_model.dart';
import '../../data/services/local_saving_goal_service.dart';
import '../../data/services/category_service.dart';
import '../../core/providers/bill_provider.dart';
import './category_provider.dart';
import '../../core/mixins/event_bus_mixin.dart';
import 'base_provider.dart';
import 'state_sync_manager.dart';

/// 储蓄目标状态管理 Provider
/// 集成统一的错误处理机制
class SavingGoalProvider extends BaseProvider with ProviderEventBusMixin {
  final LocalSavingGoalService _savingGoalService;
  BillProvider? _billProvider;
  static final RegExp _categorySplitPattern = RegExp(r'[-_·•+,，、]+');

  SavingGoalProvider(
    this._savingGoalService, {
    super.errorCenter,
    BillProvider? billProvider,
  }) {
    _billProvider = billProvider;
    // 注册状态同步监听器
    StateSyncManager().addSyncListener('saving_goals', () {
      // 当同步管理器触发同步时，重新加载目标数据
      loadGoals();
    });
  }

  List<SavingGoal> _goals = [];
  List<SavingGoal> get goals => _goals;

  /// 获取所有储蓄目标
  Future<List<SavingGoal>> getAllSavingGoals() async {
    return _goals;
  }

  Future<void> loadGoals() async {
    setBusy();
    try {
      _goals = await _savingGoalService.getSavingGoals();
      // 数据加载成功，设置为success状态
      setState(ViewState.success);
    } catch (e) {
      setError(e);
    }
  }

  Future<bool> addGoal(SavingGoal goal) async {
    setBusy();
    try {
      // 创建合并的分类名称并更新储蓄目标的分类名
      final combinedCategoryName = _composeGoalCategoryName(
        goal.name,
        goal.categoryName,
      );

      // 检查组合分类名是否已存在
      final categoryService = CategoryService();
      final existingCategories = await categoryService.getCategories();
      final categoryExists = existingCategories.any(
        (c) => c.name == combinedCategoryName,
      );

      if (categoryExists) {
        // 直接设置错误信息并返回false，不抛出异常
        setError(
          ArgumentError('组合分类名 "$combinedCategoryName" 已存在，请修改目标名称或分类名称'),
          retry: () async {
            await addGoal(goal);
          },
        );
        return false;
      }

      final goalWithCombinedCategory = goal.copyWith(
        categoryName: combinedCategoryName,
      );
      final newGoal = await _savingGoalService.createSavingGoal(
        goalWithCombinedCategory,
      );
      _goals.add(newGoal);

      // 自动创建对应的分类
      final newCategory = Category(
        name: combinedCategoryName,
        type: 1, // 收入类型
        icon: 'savings', // 设置默认图标
      );
      await categoryService.createCategory(newCategory);

      // 发布储蓄目标创建事件（即使事件发布失败，也不影响目标创建）
      try {
        eventBus.emitSavingGoalCreated(newGoal);
      } catch (eventError) {
        // 事件发布失败，仅记录错误，不影响目标创建
        print('发布储蓄目标创建事件失败: $eventError');
      }

      // 操作成功，设置为success状态
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  Future<bool> updateAmount(int id, double amount) async {
    setBusy();
    try {
      final updatedGoal = await _savingGoalService.updateCurrentAmount(
        id,
        amount,
      );
      final index = _goals.indexWhere((g) => g.id == id);
      if (index != -1) {
        // 检查是否首次达到或超过目标
        final wasCompleted = _goals[index].isCompleted;
        final isNowCompleted = updatedGoal.isCompleted;

        _goals[index] = updatedGoal;

        // 如果是首次完成，处理相关收支记录
        if (!wasCompleted && isNowCompleted) {
          await _processRelatedBillsForGoalCompletion(updatedGoal);
        }

        // 发布储蓄目标更新事件
        eventBus.emitSavingGoalUpdated(updatedGoal);
      }
      // 操作成功，设置为success状态
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  /// 明确完成储蓄目标的方法
  Future<bool> completeGoal(int id) async {
    setBusy();
    try {
      final goalToComplete = _goals.firstWhere((g) => g.id == id);

      // 更新为完成状态（设置为目标金额）
      final completedGoal = goalToComplete.copyWith(
        currentAmount: goalToComplete.targetAmount,
      );

      final updatedGoal = await _savingGoalService.updateSavingGoal(
        completedGoal,
      );
      final index = _goals.indexWhere((g) => g.id == id);
      if (index != -1) {
        _goals[index] = updatedGoal;

        // 处理完成相关的收支记录
        await _processRelatedBillsForGoalCompletion(updatedGoal);

        // 发布储蓄目标完成事件
        eventBus.emitSavingGoalUpdated(updatedGoal);
      }
      setState(ViewState.idle);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  Future<bool> updateGoal(SavingGoal goal) async {
    setBusy();
    try {
      final index = _goals.indexWhere((g) => g.id == goal.id);
      String? oldCombinedCategory;
      
      if (index != -1) {
        // 获取旧储蓄目标信息
        final oldGoal = _goals[index];
        oldCombinedCategory = oldGoal.categoryName;
      }
      
      // 生成新的组合分类名称
      final combinedCategoryName = _composeGoalCategoryName(
        goal.name,
        goal.categoryName,
      );
      final goalWithCombinedCategory = goal.copyWith(
        categoryName: combinedCategoryName,
      );

      final updatedGoal = await _savingGoalService.updateSavingGoal(
        goalWithCombinedCategory,
      );
      
      if (index != -1) {
        _goals[index] = updatedGoal;
        
        // 如果组合分类名发生变化，需要处理分类转移
        if (oldCombinedCategory != null && oldCombinedCategory != combinedCategoryName) {
          // 创建新的组合分类
          final categoryService = CategoryService();
          final existingCategories = await categoryService.getCategories(type: 1);
          final categoryExists = existingCategories.any((c) => c.name == combinedCategoryName);
          
          if (!categoryExists) {
            final newCategory = Category(
              name: combinedCategoryName,
              type: 1, // 收入类型
              icon: 'savings',
            );
            await categoryService.createCategory(newCategory);
          }
          
          // 转移分类指向：更新所有使用旧分类名的账单
          await _savingGoalService.updateSavingGoalCategoryName(oldCombinedCategory, combinedCategoryName);
          
          // 删除旧的组合分类
          final oldCategory = existingCategories.firstWhere(
            (c) => c.name == oldCombinedCategory,
            orElse: () => Category(name: '', type: 1, icon: ''),
          );
          if (oldCategory.name.isNotEmpty) {
            await categoryService.deleteCategory(oldCategory.id!);
          }
        }
      }

      // 发布储蓄目标更新事件
      eventBus.emitSavingGoalUpdated(updatedGoal);

      // 操作成功，设置为success状态
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  Future<bool> deleteGoal(int id) async {
    setBusy();
    try {
      // 先获取目标信息以便后续处理相关收支记录和删除分类
      final goalToDelete = _goals.firstWhere((g) => g.id == id);

      // 删除目标前先处理相关的收支记录
      await _processRelatedBillsForGoal(goalToDelete);

      // 删除对应的组合分类名
      final categoryService = CategoryService();
      final existingCategories = await categoryService.getCategories();
      final combinedCategoryName = goalToDelete.categoryName;

      // 检查是否是组合分类名，如果是则删除
      final categoryProvider = CategoryProvider(categoryService);
      if (categoryProvider.isCompositeCategoryGeneratedByBudgetOrGoal(
        combinedCategoryName,
      )) {
        final categoryToDelete = existingCategories.firstWhere(
          (c) => c.name == combinedCategoryName,
          orElse: () => Category(name: '', type: 1, icon: ''),
        );

        if (categoryToDelete.name.isNotEmpty) {
          await categoryService.deleteCategory(categoryToDelete.id!);
          print('已删除组合分类名: $combinedCategoryName');
        }
      }

      await _savingGoalService.deleteSavingGoal(id);
      _goals.removeWhere((g) => g.id == id);

      // 发布储蓄目标删除事件
      eventBus.emitSavingGoalDeleted(id);

      // 操作成功，设置为success状态
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  String _composeGoalCategoryName(String goalName, String categoryName) {
    final trimmedGoal = goalName.trim();
    final trimmedCategory = categoryName.trim();
    if (trimmedGoal.isEmpty || trimmedCategory.isEmpty) {
      return trimmedCategory;
    }

    // 确保只生成 "计划名称-单分类名" 格式，不生成双重以上的组合
    return '$trimmedGoal-$trimmedCategory';
  }

  /// 处理储蓄目标相关的收支记录（删除时）
  Future<void> _processRelatedBillsForGoal(SavingGoal goal) async {
    try {
      if (_billProvider == null) return;

      // 获取组合分类名
      final combinedCategoryName = goal.categoryName;

      // 获取所有相关的收支记录
      final allBills = await _billProvider!.getAllBills();
      final relatedBills = allBills
          .where((bill) => bill.categoryName == combinedCategoryName)
          .toList();

      if (relatedBills.isEmpty) return;

      // 提取分类名称的后半部分
      final categoryParts = combinedCategoryName.split('-');
      final baseCategoryName = categoryParts.length > 1
          ? categoryParts.skip(1).join('-')
          : combinedCategoryName;

      // 准备备注内容：（计划名称-计划分类）
      // 从组合分类名中提取计划分类（后半部分）
      final planCategory = categoryParts.length > 1
          ? categoryParts.skip(1).join('-')
          : combinedCategoryName;
      final remarkSuffix = '（${goal.name}-$planCategory）';

      // 批量添加备注并更新分类
      final billsToUpdate = relatedBills.where((bill) => bill.id != null).toList();
      
      for (final bill in billsToUpdate) {
        final currentRemark = bill.remark ?? '';
        final newRemark = currentRemark.isNotEmpty 
            ? '$currentRemark $remarkSuffix' 
            : remarkSuffix;
        
        // 同时更新备注和分类
        final updatedBill = bill.copyWith(
          remark: newRemark,
          categoryName: baseCategoryName,
        );
        await _billProvider!.updateBill(bill.id!, updatedBill);
      }

      print('已处理 ${relatedBills.length} 条相关收支记录（储蓄目标删除）');
    } catch (e) {
      print('处理储蓄目标相关收支记录时出错: $e');
      // 不抛出异常，避免影响目标删除操作
    }
  }

  /// 处理储蓄目标完成相关的收支记录
  Future<void> _processRelatedBillsForGoalCompletion(SavingGoal goal) async {
    try {
      if (_billProvider == null) return;

      // 获取组合分类名
      final combinedCategoryName = goal.categoryName;

      // 获取所有相关的收支记录
      final allBills = await _billProvider!.getAllBills();
      final relatedBills = allBills
          .where((bill) => bill.categoryName == combinedCategoryName)
          .toList();

      if (relatedBills.isEmpty) return;

      // 提取分类名称的后半部分
      final categoryParts = combinedCategoryName.split('-');
      final baseCategoryName = categoryParts.length > 1
          ? categoryParts.skip(1).join('-')
          : combinedCategoryName;

      // 准备备注内容
      final planStartTime = goal.deadline.toIso8601String().split('T')[0];
      final planCompleteTime = DateTime.now().toIso8601String().split('T')[0];
      final remarkSuffix =
          '($combinedCategoryName-$planStartTime-$planCompleteTime)';

      // 批量更新相关收支记录
      final billIds = relatedBills.map((bill) => bill.id!).toList();

      // 更新分类为后半部分
      await _billProvider!.updateBillsCategory(billIds, baseCategoryName);

      // 获取更新后的记录以添加备注
      final updatedBills = _billProvider!
          .getAllBills()
          .where((bill) => billIds.contains(bill.id))
          .toList();

      // 准备添加备注的记录
      final billsToAddRemark = <int, String>{};
      for (final bill in updatedBills) {
        final newRemark = (bill.remark?.isEmpty ?? true)
            ? remarkSuffix
            : '${bill.remark} $remarkSuffix';
        billsToAddRemark[bill.id!] = newRemark;
      }

      // 批量添加备注（通过逐个更新实现）
      for (final entry in billsToAddRemark.entries) {
        final bill = updatedBills.firstWhere((b) => b.id == entry.key);
        final updatedBill = bill.copyWith(remark: entry.value);
        await _billProvider!.updateBill(entry.key, updatedBill);
      }

      print('已处理 ${relatedBills.length} 条相关收支记录（储蓄目标完成）');
    } catch (e) {
      print('处理储蓄目标完成相关收支记录时出错: $e');
      // 不抛出异常，避免影响目标完成操作
    }
  }

  /// 清除所有数据
  Future<void> clearData() async {
    try {
      print('🧹 开始清除储蓄目标数据...');

      // 清除本地数据列表
      _goals.clear();

      // 设置为空闲状态
      setState(ViewState.idle);

      print('✅ 储蓄目标数据清除完成');
    } catch (e) {
      print('❌ 清除储蓄目标数据失败: $e');
    }
  }

  /// 响应状态同步通知
  void _onSyncNotification() async {
    try {
      // 响应StateSyncManager的清除数据通知
      await clearData();
      print('🔄 储蓄目标Provider已响应同步通知');
    } catch (e) {
      print('❌ 响应同步通知失败: $e');
    }
  }
}
