import '../../data/models/saving_goal_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/bill_model.dart';
import '../../data/services/local_saving_goal_service.dart';
import '../../data/services/category_service.dart';
import '../../core/providers/bill_provider.dart';
import './optimized_base_provider.dart';
import 'state_sync_manager.dart';

/// 优化的储蓄目标状态管理 Provider
/// 使用优化基类，提供更好的性能和监控
class OptimizedSavingGoalProvider extends OptimizedBaseProvider 
    with AsyncOperationMixin, ProviderEventBusMixin {
  final LocalSavingGoalService _savingGoalService;
  final OptimizedTransactionProvider? _billProvider;

  OptimizedSavingGoalProvider(this._savingGoalService, {super.errorCenter, OptimizedTransactionProvider? billProvider}) 
      : _billProvider = billProvider {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('saving_goals', _onSyncNotification);
  }

  List<SavingGoal> _savingGoals = [];
  List<SavingGoal> get savingGoals => _savingGoals;

  /// 优化的获取所有储蓄目标方法
  Future<List<SavingGoal>> getAllSavingGoals() async {
    return _savingGoals;
  }

  /// 优化的加载储蓄目标方法
  Future<void> loadGoals() async {
    await executeAsync(
      () async {
        final loadedGoals = await _savingGoalService.getSavingGoals();
        
        updateState(ViewState.idle, () {
          _savingGoals = loadedGoals;
        });
        
        return loadedGoals;
      },
      'loadGoals',
      showLoading: true,
    );
  }

  /// 优化的添加储蓄目标方法
  Future<bool> addGoal(SavingGoal goal) async {
    return await executeAsync(
      () async {
        // 创建合并的分类名称并更新储蓄目标的分类名
        final combinedCategoryName = '${goal.name}-${goal.categoryName}';
        
        // 检查组合分类名是否已存在
        final categoryService = CategoryService();
        final existingCategories = await categoryService.getCategories();
        final categoryExists = existingCategories.any((c) => c.name == combinedCategoryName);
        
        if (categoryExists) {
          setError(
            ArgumentError('组合分类名 "$combinedCategoryName" 已存在，请修改目标名称或分类名称'),
            context: 'addGoal',
          );
          return false;
        }
        
        final goalWithCombinedCategory = goal.copyWith(categoryName: combinedCategoryName);
        final newGoal = await _savingGoalService.createSavingGoal(goalWithCombinedCategory);
        
        updateState(ViewState.idle, () {
          _savingGoals.add(newGoal);
        });

        // 自动创建对应的分类
        final newCategory = Category(
          name: combinedCategoryName,
          type: 1, // 收入类型
          icon: 'savings', // 设置默认图标
        );
        await categoryService.createCategory(newCategory);

        // 发布储蓄目标创建事件
        emitSavingGoalCreated(newGoal);
        
        return true;
      },
      'addGoal',
      showLoading: true,
    );
  }

  /// 优化的更新金额方法
  Future<bool> updateAmount(int id, double amount) async {
    return await executeAsync(
      () async {
        final updatedGoal = await _savingGoalService.updateCurrentAmount(id, amount);
        
        updateState(ViewState.idle, () {
          final index = _savingGoals.indexWhere((g) => g.id == id);
          if (index != -1) {
            // 检查是否首次达到或超过目标
            final wasCompleted = _savingGoals[index].isCompleted;
            final isNowCompleted = updatedGoal.isCompleted;
            
            _savingGoals[index] = updatedGoal;

            // 如果是首次完成，处理相关收支记录
            if (!wasCompleted && isNowCompleted) {
              _processRelatedBillsForGoalCompletion(updatedGoal);
            }
          }
        });

        // 发布储蓄目标更新事件
        emitSavingGoalUpdated(updatedGoal);
        
        return true;
      },
      'updateAmount',
      showLoading: true,
    );
  }

  /// 优化的明确完成储蓄目标方法
  Future<bool> completeGoal(int id) async {
    return await executeAsync(
      () async {
        final goalToComplete = _savingGoals.firstWhere((g) => g.id == id);
        
        // 更新为完成状态（设置为目标金额）
        final completedGoal = goalToComplete.copyWith(
          currentAmount: goalToComplete.targetAmount,
        );
        
        final updatedGoal = await _savingGoalService.updateSavingGoal(completedGoal);
        
        updateState(ViewState.idle, () {
          final index = _savingGoals.indexWhere((g) => g.id == id);
          if (index != -1) {
            _savingGoals[index] = updatedGoal;
            
            // 处理完成相关的收支记录
            _processRelatedBillsForGoalCompletion(updatedGoal);
          }
        });

        // 发布储蓄目标完成事件
        emitSavingGoalUpdated(updatedGoal);
        
        return true;
      },
      'completeGoal',
      showLoading: true,
    );
  }

  /// 优化的更新储蓄目标方法
  Future<bool> updateGoal(SavingGoal goal) async {
    return await executeAsync(
      () async {
        // 如果是更新且分类名发生变化，更新合并的分类名称
        final existingGoal = _savingGoals.firstWhere((g) => g.id == goal.id, orElse: () => goal);
        final combinedCategoryName = '${goal.name}-${goal.categoryName}';
        
        // 只有在分类名实际变化时才更新
        SavingGoal updatedGoal;
        if (existingGoal.categoryName != combinedCategoryName) {
          final goalWithCombinedCategory = goal.copyWith(categoryName: combinedCategoryName);
          updatedGoal = await _savingGoalService.updateSavingGoal(goalWithCombinedCategory);
        } else {
          // 如果分类名没有变化，直接更新
          updatedGoal = await _savingGoalService.updateSavingGoal(goal);
        }
        
        updateState(ViewState.idle, () {
          final index = _savingGoals.indexWhere((g) => g.id == goal.id);
          if (index != -1) {
            _savingGoals[index] = updatedGoal;
          }
        });

        // 发布储蓄目标更新事件
        emitSavingGoalUpdated(updatedGoal);
        
        return true;
      },
      'updateGoal',
      showLoading: true,
    );
  }

  /// 优化的删除储蓄目标方法
  Future<bool> deleteGoal(int id) async {
    return await executeAsync(
      () async {
        // 先获取目标信息以便后续处理相关收支记录和删除分类
        final goalToDelete = _savingGoals.firstWhere((g) => g.id == id);
        
        // 删除目标前先处理相关的收支记录
        await _processRelatedBillsForGoal(goalToDelete);
        
        // 删除对应的组合分类名
        final categoryService = CategoryService();
        final existingCategories = await categoryService.getCategories();
        final combinedCategoryName = goalToDelete.categoryName;
        
        // 检查是否是组合分类名，如果是则删除
        final categoryProvider = OptimizedCategoryProvider(categoryService);
        if (categoryProvider.isCompositeCategoryGeneratedByBudgetOrGoal(combinedCategoryName)) {
          final categoryToDelete = existingCategories.firstWhere(
            (c) => c.name == combinedCategoryName,
            orElse: () => Category(name: '', type: 1, icon: '')
          );
          
          if (categoryToDelete.name.isNotEmpty) {
            await categoryService.deleteCategory(categoryToDelete.id!);
            print('已删除组合分类名: $combinedCategoryName');
          }
        }
        
        await _savingGoalService.deleteSavingGoal(id);
        
        updateState(ViewState.idle, () {
          _savingGoals.removeWhere((g) => g.id == id);
        });

        // 发布储蓄目标删除事件
        emitSavingGoalDeleted(id);
        
        return true;
      },
      'deleteGoal',
      showLoading: true,
    );
  }

  /// 批量添加储蓄目标
  Future<bool> addGoals(List<SavingGoal> goals) async {
    if (goals.isEmpty) {
      return true;
    }

    return await executeAsync(
      () async {
        final operations = goals.map((goal) => () async {
          final combinedCategoryName = '${goal.name}-${goal.categoryName}';
          return await _savingGoalService.createSavingGoal(goal.copyWith(categoryName: combinedCategoryName));
        }).toList();
        
        final createdGoals = await executeBatchAsync(
          operations,
          'batchCreateSavingGoals',
          showLoading: false,
        );
        
        updateState(ViewState.idle, () {
          _savingGoals.addAll(createdGoals);
        });
        
        // 发布批量储蓄目标创建事件
        for (final goal in createdGoals) {
          emitSavingGoalCreated(goal);
        }
        
        return true;
      },
      'addGoals',
      showLoading: true,
    );
  }

  /// 获取未完成的储蓄目标
  List<SavingGoal> getIncompleteGoals() {
    return _savingGoals.where((goal) => !goal.isCompleted).toList();
  }

  /// 获取已完成的储蓄目标
  List<SavingGoal> getCompletedGoals() {
    return _savingGoals.where((goal) => goal.isCompleted).toList();
  }

  /// 获取即将到期的储蓄目标（30天内）
  List<SavingGoal> getUpcomingGoals() {
    final now = DateTime.now();
    final thirtyDaysLater = now.add(const Duration(days: 30));
    
    return _savingGoals.where((goal) {
      return !goal.isCompleted && 
             goal.deadline.isAfter(now) && 
             goal.deadline.isBefore(thirtyDaysLater);
    }).toList();
  }

  /// 计算储蓄进度统计
  Map<String, dynamic> getProgressStatistics() {
    final totalGoals = _savingGoals.length;
    final completedGoals = getCompletedGoals().length;
    final totalTargetAmount = _savingGoals.fold(0.0, (sum, goal) => sum + goal.targetAmount);
    final totalCurrentAmount = _savingGoals.fold(0.0, (sum, goal) => sum + goal.currentAmount);
    
    return {
      'totalGoals': totalGoals,
      'completedGoals': completedGoals,
      'completionRate': totalGoals > 0 ? completedGoals / totalGoals : 0.0,
      'totalTargetAmount': totalTargetAmount,
      'totalCurrentAmount': totalCurrentAmount,
      'overallProgress': totalTargetAmount > 0 ? totalCurrentAmount / totalTargetAmount : 0.0,
    };
  }

  /// 处理储蓄目标相关的收支记录（删除时）
  Future<void> _processRelatedBillsForGoal(SavingGoal goal) async {
    try {
      if (_billProvider == null) return;
      
      // 获取组合分类名
      final combinedCategoryName = goal.categoryName;
      
      // 获取所有相关的收支记录
      final allBills = _billProvider!.getAllBills();
      final relatedBills = allBills.where((bill) => bill.categoryName == combinedCategoryName).toList();
      
      if (relatedBills.isEmpty) return;
      
      // 提取分类名称的后半部分
      final categoryParts = combinedCategoryName.split('-');
      final baseCategoryName = categoryParts.length > 1 
          ? categoryParts.skip(1).join('-') 
          : combinedCategoryName;
      
      // 准备备注内容
      final planStartTime = goal.deadline.toIso8601String().split('T')[0];
      final planDeleteTime = DateTime.now().toIso8601String().split('T')[0];
      final remarkSuffix = '($combinedCategoryName-$planStartTime-$planDeleteTime)';
      
      // 批量更新相关收支记录
      final billIds = relatedBills.map((bill) => bill.id!).toList();
      
      // 更新分类为后半部分
      await _billProvider!.updateBillsCategory(billIds, baseCategoryName);
      
      // 获取更新后的记录以添加备注
      final updatedBills = _billProvider!.getAllBills()
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
      final allBills = _billProvider!.getAllBills();
      final relatedBills = allBills.where((bill) => bill.categoryName == combinedCategoryName).toList();
      
      if (relatedBills.isEmpty) return;
      
      // 提取分类名称的后半部分
      final categoryParts = combinedCategoryName.split('-');
      final baseCategoryName = categoryParts.length > 1 
          ? categoryParts.skip(1).join('-') 
          : combinedCategoryName;
      
      // 准备备注内容
      final planStartTime = goal.deadline.toIso8601String().split('T')[0];
      final planCompleteTime = DateTime.now().toIso8601String().split('T')[0];
      final remarkSuffix = '($combinedCategoryName-$planStartTime-$planCompleteTime)';
      
      // 批量更新相关收支记录
      final billIds = relatedBills.map((bill) => bill.id!).toList();
      
      // 更新分类为后半部分
      await _billProvider!.updateBillsCategory(billIds, baseCategoryName);
      
      // 获取更新后的记录以添加备注
      final updatedBills = _billProvider!.getAllBills()
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
    await executeAsync(
      () async {
        updateState(ViewState.idle, () {
          _savingGoals.clear();
        });
        return true;
      },
      'clearData',
      showLoading: false,
    );
  }

  /// 同步通知处理方法
  void _onSyncNotification() {
    print('📢 储蓄目标Provider收到StateSyncManager同步通知，开始清除数据...');
    clearData();
  }

  /// 获取性能统计
  @override
  Map<String, dynamic> getPerformanceStats() {
    final stats = super.getPerformanceStats();
    stats.addAll({
      'totalSavingGoals': _savingGoals.length,
      'completedGoals': getCompletedGoals().length,
      'incompleteGoals': getIncompleteGoals().length,
      'upcomingGoals': getUpcomingGoals().length,
      'progressStatistics': getProgressStatistics(),
    });
    return stats;
  }

  /// 导出数据
  Future<List<Map<String, dynamic>>> exportSavingGoals() async {
    return await executeAsync(
      () async {
        return _savingGoals.map((goal) => {
          'id': goal.id,
          'name': goal.name,
          'category': goal.categoryName,
          'targetAmount': goal.targetAmount,
          'currentAmount': goal.currentAmount,
          'deadline': goal.deadline.toIso8601String(),
          'description': goal.description,
          'isCompleted': goal.isCompleted,
          'progress': goal.progress,
        }).toList();
      },
      'exportSavingGoals',
      showLoading: false,
    );
  }
}

/// Provider事件总线混入
mixin ProviderEventBusMixin {
  void emitSavingGoalCreated(SavingGoal goal) {
    debugPrint('📢 储蓄目标创建事件: ${goal.id}');
  }

  void emitSavingGoalUpdated(SavingGoal goal) {
    debugPrint('📢 储蓄目标更新事件: ${goal.id}');
  }

  void emitSavingGoalDeleted(int goalId) {
    debugPrint('📢 储蓄目标删除事件: $goalId');
  }
}