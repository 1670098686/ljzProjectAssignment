import '../../data/models/budget_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/bill_model.dart';
import '../../data/services/budget_service.dart';
import '../../data/services/bill_service.dart';
import '../../data/services/category_service.dart';
import 'base_provider.dart';
import '../mixins/event_bus_mixin.dart';
import '../services/event_bus_service.dart';

/// 预算状态管理Provider
/// 集成统一的错误处理机制
class BudgetProvider extends BaseProvider with ProviderEventBusMixin {
  final BudgetService _budgetService;

  BudgetProvider(this._budgetService, {super.errorCenter}) {
    // 监听分类更新事件，当分类更新时刷新相关预算数据
    onCategoryUpdated((event) {
      _refreshBudgetsByCategory(event.category.name);
    });
    
    // 监听交易创建事件，当有新交易时检查预算
    EventBusService.instance.eventBus.on<TransactionCreatedEvent>().listen((event) {
      print('📢 BudgetProvider收到交易创建事件，开始检查预算...');
      _checkBudgetAfterTransaction();
    });
  }

  List<Budget> _budgets = [];
  List<Budget> get budgets => _budgets;

  Future<void> loadBudgets(int year, int month) async {
    setBusy();
    try {
      _budgets = await _budgetService.getBudgets(year: year, month: month);   
      setState(ViewState.success);
    } catch (e) {
      setError(e, retry: () => loadBudgets(year, month));
    }
  }

  Future<bool> addBudget(Budget budget) async {
    setBusy();
    try {
      final newBudget = await _budgetService.createBudget(budget);
      _budgets.add(newBudget);

      // 新增代码：自动创建对应的分类
      final categoryService = CategoryService();
      final combinedCategoryName = '${budget.budgetName}-${budget.categoryName}';

      // 检验分类是否已存在
      final existingCategories = await categoryService.getCategories(type: 2); // 预算分类都是支出类型
      final categoryExists = existingCategories.any((c) => c.name == combinedCategoryName);

      if (!categoryExists) {
        // 创建新分类
        final newCategory = Category(
          name: combinedCategoryName,
          type: 2, // 支出类型
          icon: 'budget', // 设置默认图标
        );
        await categoryService.createCategory(newCategory);
      }

      // 发布预算新增事件
      eventBus.emitBudgetCreated(newBudget);

      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await addBudget(budget);
        },
      );
      return false;
    }
  }

  Future<bool> addBudgets(List<Budget> budgets) async {
    setBusy();
    try {
      for (final budget in budgets) {
        await addBudget(budget);
      }
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await addBudgets(budgets);
        },
      );
      return false;
    }
  }

  Future<bool> updateBudget(Budget budget) async {
    setBusy();
    try {
      final index = _budgets.indexWhere((b) => b.id == budget.id);
      if (index != -1) {
        // 获取旧预算信息
        final oldBudget = _budgets[index];
        final oldCombinedCategory = '${oldBudget.budgetName}-${oldBudget.categoryName}';
        final newCombinedCategory = '${budget.budgetName}-${budget.categoryName}';
        
        // 如果组合分类名发生变化，需要处理分类转移
        if (oldCombinedCategory != newCombinedCategory) {
          // 创建新的组合分类
          final categoryService = CategoryService();
          final existingCategories = await categoryService.getCategories(type: 2);
          final categoryExists = existingCategories.any((c) => c.name == newCombinedCategory);
          
          if (!categoryExists) {
            final newCategory = Category(
              name: newCombinedCategory,
              type: 2, // 支出类型
              icon: 'budget',
            );
            await categoryService.createCategory(newCategory);
          }
          
          // 转移分类指向：更新所有使用旧分类名的账单
          await _budgetService.updateBudgetCategoryName(oldCombinedCategory, newCombinedCategory);
          
          // 删除旧的组合分类
          final oldCategory = existingCategories.firstWhere(
            (c) => c.name == oldCombinedCategory,
            orElse: () => Category(name: '', type: 2, icon: ''),
          );
          if (oldCategory.name.isNotEmpty) {
            await categoryService.deleteCategory(oldCategory.id!);
          }
        }
      }
      
      final updatedBudget = await _budgetService.updateBudget(budget);        
      
      if (index != -1) {
        _budgets[index] = updatedBudget;

        // 发布预算更新事件
        eventBus.emitBudgetUpdated(updatedBudget);
      }
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await updateBudget(budget);
        },
      );
      return false;
    }
  }

  Future<bool> deleteBudget(int id) async {
    setBusy();
    try {
      // 找到要删除的预算
      final budget = _budgets.firstWhere((b) => b.id == id);
      
      // 生成组合分类名
      final combinedCategoryName = '${budget.budgetName}-${budget.categoryName}';
      
      // 获取所有使用该组合分类名的账单
      final billService = BillService();
      final bills = await billService.getBills(categoryName: combinedCategoryName);
      
      // 处理使用该组合分类名的账单
      if (bills.isNotEmpty) {
        // 从组合分类名中提取单分类名（后半段）
        final categoryParts = combinedCategoryName.split('-');
        final baseCategoryName = categoryParts.length > 1
            ? categoryParts.skip(1).join('-')
            : budget.categoryName;
        
        // 批量添加备注并更新分类
        final remarkSuffix = '（${budget.budgetName}-${budget.categoryName}）';
        final billsToUpdate = bills.where((bill) => bill.id != null).toList();
        
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
          await billService.updateBill(bill.id!, updatedBill);
        }
      }
      
      // 删除组合分类
      final categoryService = CategoryService();
      final categories = await categoryService.getCategories(type: 2);
      final categoryToDelete = categories.firstWhere(
        (c) => c.name == combinedCategoryName,
        orElse: () => Category(name: '', type: 2, icon: ''),
      );
      
      if (categoryToDelete.name.isNotEmpty && categoryToDelete.id != null) {
        await categoryService.deleteCategory(categoryToDelete.id!);
      }
      
      // 删除预算
      await _budgetService.deleteBudget(id);
      _budgets.removeWhere((b) => b.id == id);

      // 发布预算删除事件
      eventBus.emitBudgetDeleted(id);

      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(
        e,
        retry: () async {
          await deleteBudget(id);
        },
      );
      return false;
    }
  }

  void clearData() {
    _budgets.clear();
    setState(ViewState.idle);
  }

  /// 私有方法：根据分类名称刷新相关预算
  /// 用于解决分类更新后预算数据同步问题
  Future<void> _refreshBudgetsByCategory(String categoryName) async {
    try {
      // 重新加载当前月份的预算数据
      if (_budgets.isNotEmpty) {
        final currentYear = _budgets.first.year;
        final currentMonth = _budgets.first.month;

        await loadBudgets(currentYear, currentMonth);
      }
    } catch (e) {
      // 记录错误但不影响用户体验
    }
  }
  
  /// 交易创建后检查预算超支情况
  Future<void> _checkBudgetAfterTransaction() async {
    try {
      // 如果当前没有预算数据，直接返回
      if (_budgets.isEmpty) {
        print('💰 _checkBudgetAfterTransaction: 当前没有预算数据，跳过检查');
        return;
      }
      
      // 重新加载最新的预算数据（这会自动计算实际支出）
      final currentYear = _budgets.first.year;
      final currentMonth = _budgets.first.month;
      print('💰 _checkBudgetAfterTransaction: 重新加载预算数据，年份=$currentYear, 月份=$currentMonth');
      
      await loadBudgets(currentYear, currentMonth);
      
      // 遍历所有预算，检查是否超支
      for (final budget in _budgets) {
        if (budget.id == null) continue;
        
        final spentAmount = budget.spent ?? 0.0;
        final usagePercentage = budget.amount > 0 ? (spentAmount / budget.amount) * 100 : 0.0;
        final isOverBudget = spentAmount > budget.amount;
        
        print('🔍 预算检查：${budget.categoryName}, 已支出: $spentAmount, 预算: ${budget.amount}, 使用率: $usagePercentage%, 是否超支: $isOverBudget');
        
        // 如果超支，触发预算预警
        if (isOverBudget) {
          print('⚠️ 预算超支！准备触发预警动画...');
          // 这里需要调用预算预警工具类的方法，但是需要上下文
          // 由于Provider中没有上下文，我们需要通过其他方式处理
          // 暂时打印日志，后续优化
        }
      }
    } catch (e) {
      print('❌ 交易后预算检查失败: $e');
      // 记录错误但不影响用户体验
    }
  }
}