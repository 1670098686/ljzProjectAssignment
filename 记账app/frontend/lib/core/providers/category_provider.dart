import '../../data/models/category_model.dart';
import '../../data/services/category_service.dart';
import 'base_provider.dart';
import 'state_sync_manager.dart';
import 'saving_goal_provider.dart';
import '../mixins/event_bus_mixin.dart';

class CategoryProvider extends BaseProvider with ProviderEventBusMixin {
  final CategoryService _categoryService;
  SavingGoalProvider? _savingGoalProvider;

  /// 响应StateSyncManager的清除数据通知
  void _onSyncNotification() {
    print('📢 分类Provider收到StateSyncManager同步通知，开始清除数据...');
    clearData();
  }

  CategoryProvider(
    this._categoryService, {
    super.errorCenter,
    SavingGoalProvider? savingGoalProvider,
  }) : _savingGoalProvider = savingGoalProvider {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('categories', _onSyncNotification);
    
    // 监听分类更新事件，当分类更新时刷新分类列表
    onCategoryUpdated((event) {
      print('📢 分类Provider收到分类更新事件，开始重新加载分类...');
      loadCategories();
    });
    
    // 监听预算更新事件，当预算更新时刷新分类列表
    onBudgetUpdated((event) {
      print('📢 分类Provider收到预算更新事件，开始重新加载分类...');
      loadCategories();
    });
    
    // 监听储蓄目标更新事件，当储蓄目标更新时刷新分类列表
    onSavingGoalUpdated((event) {
      print('📢 分类Provider收到储蓄目标更新事件，开始重新加载分类...');
      loadCategories();
    });
  }

  List<Category> _categories = [];
  List<Category> get categories => _categories;

  Future<void> loadCategories({int? type}) async {
    setBusy();
    try {
      // 始终加载完整分类列表，避免后续筛选导致数据缺失
      _categories = await _categoryService.getCategories();
      setState(ViewState.success);
    } catch (e) {
      setError(e);
    }
  }

  Future<Category> ensureCategoryExists(String name, int type) async {
    final trimmedName = name.trim();
    if (trimmedName.isEmpty) {
      throw ArgumentError('分类名称不能为空');
    }

    // 检查是否是组合分类名（包含多个分隔符的组合）
    if (_isCompositeCategoryName(trimmedName)) {
      // 只允许计划页生成的特定格式组合分类名：计划名称-单分类名
      if (!isCompositeCategoryGeneratedByBudgetOrGoal(trimmedName)) {
        throw ArgumentError('分类名称格式错误，只能是单个分类名或 "计划名称-单分类名" 格式');
      }
    }

    final existing = _findCategory(trimmedName, type);
    if (existing != null) {
      return existing;
    }

    setBusy();
    try {
      // 刷新最新分类后再次校验，避免并发创建重复项
      final latestCategories = await _categoryService.getCategories();
      _categories = latestCategories;
      final refreshed = _findCategory(trimmedName, type);
      if (refreshed != null) {
        setState(ViewState.success);
        return refreshed;
      }

      // 为计划页生成的组合分类设置特殊图标
      final icon = isCompositeCategoryGeneratedByBudgetOrGoal(trimmedName) 
          ? (type == 1 ? 'savings' : 'budget') 
          : (type == 1 ? 'income' : 'expense');

      final newCategory = Category(
        name: trimmedName,
        icon: icon,
        type: type,
      );
      final created = await _categoryService.createCategory(newCategory);
      _categories.add(created);
      setState(ViewState.success);
      return created;
    } catch (e) {
      setError(e);
      rethrow;
    }
  }

  /// 判断是否是组合分类名（包含多个分类的组合）
  bool _isCompositeCategoryName(String name) {
    // 组合分类名包含分隔符：-、_、，、,
    return name.contains(RegExp(r'[-_，,\s]+'));
  }

  /// 判断是否是预算或储蓄目标生成的组合分类名
  bool isCompositeCategoryGeneratedByBudgetOrGoal(String categoryName) {
    // 只允许 "计划名称-单分类名" 格式，即只包含一个分隔符
    if (categoryName.contains('-')) {
      final parts = categoryName.split('-').map((part) => part.trim()).where((part) => part.isNotEmpty).toList();
      // 正确的组合分类名应该只有两个部分：计划名称和单分类名
      return parts.length == 2;
    }
    
    return false;
  }

  Future<bool> addCategory(Category category) async {
    setBusy();
    try {
      final newCategory = await _categoryService.createCategory(category);
      _categories.add(newCategory);
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  Future<bool> updateCategory(int id, Category category) async {
    setBusy();
    try {
      final updatedCategory = await _categoryService.updateCategory(
        id,
        category,
      );
      final index = _categories.indexWhere((c) => c.id == id);
      if (index != -1) {
        _categories[index] = updatedCategory;
      }
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  Future<bool> deleteCategory(int id) async {
    setBusy();
    try {
      await _categoryService.deleteCategory(id);
      _categories.removeWhere((c) => c.id == id);
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  }

  List<Category> getCategoriesByType(int type) {
    return categories.where((category) => category.type == type).toList();
  }

  Category? _findCategory(String name, int type) {
    try {
      return _categories.firstWhere(
        (category) => category.type == type && category.name == name,
      );
    } catch (_) {
      return null;
    }
  }

  /// 根据名称查找分类
  Future<Category?> findCategoryByName(String name) async {
    try {
      return _categories.firstWhere(
        (category) => category.name == name,
      );
    } catch (_) {
      return null;
    }
  }

  /// 创建组合分类
  Future<void> createCompositeCategory(String categoryName, String goalName) async {
    final compositeName = '${goalName}-${categoryName}';
    
    // 检查是否已存在
    final existing = await findCategoryByName(compositeName);
    if (existing != null) {
      print('组合分类已存在: $compositeName');
      return;
    }
    
    // 创建新的组合分类
    final newCategory = Category(
      name: compositeName,
      icon: 'savings',
      type: 1, // 储蓄目标相关的组合分类为收入类型
    );
    
    await addCategory(newCategory);
    print('创建组合分类完成: $compositeName');
  }

  /// 清除所有数据
  Future<void> clearData() async {
    try {
      print('🧹 开始清除分类数据...');
      
      // 清除本地数据列表
      _categories.clear();
      
      // 通知监听器数据已清除
      setState(ViewState.idle);
      
      print('✅ 分类数据清除完成');
    } catch (e) {
      print('❌ 清除分类数据失败: $e');
    }
  }

  /// 获取所有预算创建的组合分类
  List<String> getBudgetCategories() {
    // 预算功能已移除，返回空列表
    return [];
  }

  /// 获取所有储蓄目标创建的组合分类
  List<String> getSavingGoalCategories() {
    if (_savingGoalProvider == null) {
      return [];
    }
    
    return _savingGoalProvider!.goals
        .map((goal) => '${goal.name}-${goal.categoryName}')
        .toSet()
        .toList();
  }

  /// 获取所有计划页创建的组合分类（预算 + 储蓄目标）
  List<String> getPlanCategories() {
    final budgetCategories = getBudgetCategories();
    final savingGoalCategories = getSavingGoalCategories();
    
    return [...budgetCategories, ...savingGoalCategories].toSet().toList();
  }

  /// 获取所有可用的分类名称（仅显示单个分类，不显示组合分类）
  /// 返回格式：List<String> 仅包含单个分类名
  List<String> getAllAvailableCategoryNames({int? type}) {
    final regularCategories = type != null 
        ? getCategoriesByType(type).where((c) => !_isCompositeCategoryName(c.name)).map((c) => c.name).toList()
        : categories.where((c) => !_isCompositeCategoryName(c.name)).map((c) => c.name).toList();
    
    return regularCategories.toSet().toList();
  }

  /// 转换计划页分类名为Category对象列表
  List<Category> getPlanCategoryObjects() {
    final planCategoryNames = getPlanCategories();
    
    return planCategoryNames.map((name) {
      // 判断是储蓄目标分类
      bool isSavingGoalCategory = false;
      
      if (_savingGoalProvider != null) {
        isSavingGoalCategory = _savingGoalProvider!.goals.any(
          (goal) => '${goal.name}-${goal.categoryName}' == name
        );
      }
      
      // 确定类型：储蓄目标分类是收入(1)
      final type = 1;
      
      // 设置默认图标
      final icon = 'savings';
      
      return Category(
        name: name,
        type: type,
        icon: icon,
      );
    }).toList();
  }

  /// 获取所有可用分类的Category对象列表（包括常规分类和计划页分类）
  List<Category> getAllAvailableCategories({int? type}) {
    final regularCategories = type != null 
        ? getCategoriesByType(type)
        : List<Category>.from(categories);
    
    final planCategories = getPlanCategoryObjects();
    
    // 如果指定了类型，过滤计划页分类
    if (type != null) {
      final planCategoriesFiltered = planCategories.where((category) => category.type == type).toList();
      return [...regularCategories, ...planCategoriesFiltered].toSet().toList();
    }
    
    return [...regularCategories, ...planCategories].toSet().toList();
  }
}