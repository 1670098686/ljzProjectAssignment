import '../../data/models/category_model.dart';
import '../../data/services/category_service.dart';
import 'optimized_base_provider.dart';
import 'state_sync_manager.dart';
import 'optimized_saving_goal_provider.dart';

/// 优化的分类状态管理 Provider
/// 使用优化基类，提供更好的性能和监控
class OptimizedCategoryProvider extends OptimizedBaseProvider 
    with AsyncOperationMixin {
  final CategoryService _categoryService;
  final OptimizedSavingGoalProvider? _savingGoalProvider;

  /// 响应StateSyncManager的清除数据通知
  void _onSyncNotification() {
    print('📢 分类Provider收到StateSyncManager同步通知，开始清除数据...');
    clearData();
  }

  OptimizedCategoryProvider(
    this._categoryService, {
    super.errorCenter,
    OptimizedSavingGoalProvider? savingGoalProvider,
  }) : _savingGoalProvider = savingGoalProvider {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('categories', _onSyncNotification);
  }

  List<Category> _categories = [];
  List<Category> get categories => _categories;

  /// 优化的分类加载方法
  Future<void> loadCategories({int? type}) async {
    await executeAsync(
      () async {
        // 始终加载完整分类列表，避免后续筛选导致数据缺失
        final loadedCategories = await _categoryService.getCategories();
        
        updateState(ViewState.idle, () {
          _categories = loadedCategories;
        });
        
        return loadedCategories;
      },
      'loadCategories',
      showLoading: true,
    );
  }

  /// 优化的确保分类存在方法
  Future<Category> ensureCategoryExists(String name, int type) async {
    final trimmedName = name.trim();
    if (trimmedName.isEmpty) {
      throw ArgumentError('分类名称不能为空');
    }

    // 检查是否是组合分类名（多个分类组合的分类名）
    if (_isCompositeCategoryName(trimmedName)) {
      // 验证组合分类名中的每个分类是否都已存在
      final categoryNames = trimmedName.split(RegExp(r'[-_，,\s]+'));
      for (final categoryName in categoryNames) {
        final existingCategory = _findCategory(categoryName.trim(), type);
        if (existingCategory == null) {
          throw ArgumentError('组合分类名中的分类 "$categoryName" 不存在，请先创建该分类');
        }
      }
    }

    final existing = _findCategory(trimmedName, type);
    if (existing != null) {
      return existing;
    }

    return await executeAsync(
      () async {
        // 刷新最新分类后再次校验，避免并发创建重复项
        final latestCategories = await _categoryService.getCategories();
        
        updateState(ViewState.idle, () {
          _categories = latestCategories;
        });
        
        final refreshed = _findCategory(trimmedName, type);
        if (refreshed != null) {
          return refreshed;
        }

        final newCategory = Category(
          name: trimmedName,
          icon: type == 1 ? 'income' : 'expense',
          type: type,
        );
        
        final created = await _categoryService.createCategory(newCategory);
        
        updateState(ViewState.idle, () {
          _categories.add(created);
        });
        
        return created;
      },
      'ensureCategoryExists',
      showLoading: true,
    );
  }

  /// 优化的添加分类方法
  Future<bool> addCategory(Category category) async {
    return await executeAsync(
      () async {
        final newCategory = await _categoryService.createCategory(category);
        
        updateState(ViewState.idle, () {
          _categories.add(newCategory);
        });
        
        return true;
      },
      'addCategory',
      showLoading: true,
    );
  }

  /// 优化的更新分类方法
  Future<bool> updateCategory(int id, Category category) async {
    return await executeAsync(
      () async {
        final updatedCategory = await _categoryService.updateCategory(id, category);
        
        updateState(ViewState.idle, () {
          final index = _categories.indexWhere((c) => c.id == id);
          if (index != -1) {
            _categories[index] = updatedCategory;
          }
        });
        
        return true;
      },
      'updateCategory',
      showLoading: true,
    );
  }

  /// 优化的删除分类方法
  Future<bool> deleteCategory(int id) async {
    return await executeAsync(
      () async {
        await _categoryService.deleteCategory(id);
        
        updateState(ViewState.idle, () {
          _categories.removeWhere((c) => c.id == id);
        });
        
        return true;
      },
      'deleteCategory',
      showLoading: true,
    );
  }

  /// 按类型获取分类
  List<Category> getCategoriesByType(int type) {
    return _categories.where((category) => category.type == type).toList();
  }

  /// 查找分类的优化方法
  Category? _findCategory(String name, int type) {
    try {
      return _categories.firstWhere(
        (category) => category.type == type && category.name == name,
      );
    } catch (_) {
      return null;
    }
  }

  /// 清除所有数据
  Future<void> clearData() async {
    await executeAsync(
      () async {
        updateState(ViewState.idle, () {
          _categories.clear();
        });
        return true;
      },
      'clearData',
      showLoading: false,
    );
  }

  /// 预算功能已移除，预算分类获取方法已废弃
  List<String> getBudgetCategories() {
    // 预算功能已移除，返回空列表
    return [];
  }

  /// 获取所有储蓄目标创建的组合分类
  List<String> getSavingGoalCategories() {
    if (_savingGoalProvider == null) {
      return [];
    }
    
    return _savingGoalProvider!.savingGoals
        .map((goal) => '${goal.goalName}-${goal.categoryName}')
        .toList();
  }

  /// 获取所有组合分类
  List<String> getAllCompositeCategories() {
    final budgetCategories = getBudgetCategories();
    final savingGoalCategories = getSavingGoalCategories();
    
    return [...budgetCategories, ...savingGoalCategories];
  }

  /// 判断是否是组合分类名（包含多个分类的组合）
  bool _isCompositeCategoryName(String name) {
    // 组合分类名包含分隔符：-、_、，、,
    return name.contains(RegExp(r'[-_，,\s]+'));
  }

  /// 判断是否是预算或储蓄目标生成的组合分类名
  bool isCompositeCategoryGeneratedByBudgetOrGoal(String categoryName) {
    // 检查是否是预算计划生成的组合分类名（格式：预算名称-分类名称）
    if (categoryName.contains('-') && (categoryName.contains('计划') || categoryName.contains('目标'))) {
      return true;
    }
    
    // 检查是否包含分隔符且分割后有多个部分（这是最常见的组合分类名格式：名称-分类）
    final separators = ['-', '_', '·', '•', '+'];
    for (final separator in separators) {
      if (categoryName.contains(separator)) {
        final parts = categoryName.split(separator).map((part) => part.trim()).where((part) => part.isNotEmpty).toList();
        if (parts.length >= 2) {
          return true;
        }
      }
    }
    
    return false;
  }

  /// 批量创建分类
  Future<bool> addCategories(List<Category> categories) async {
    if (categories.isEmpty) {
      return true;
    }

    return await executeAsync(
      () async {
        final operations = categories.map((category) => () => _categoryService.createCategory(category)).toList();
        final createdCategories = await executeBatchAsync(
          operations,
          'batchCreateCategories',
          showLoading: false,
        );
        
        updateState(ViewState.idle, () {
          _categories.addAll(createdCategories);
        });
        
        return true;
      },
      'addCategories',
      showLoading: true,
    );
  }

  /// 搜索分类
  List<Category> searchCategories(String keyword) {
    if (keyword.isEmpty) {
      return _categories;
    }
    
    final searchTerm = keyword.toLowerCase();
    return _categories.where((category) {
      return category.name.toLowerCase().contains(searchTerm) ||
             category.icon.toLowerCase().contains(searchTerm);
    }).toList();
  }

  /// 获取性能统计
  @override
  Map<String, dynamic> getPerformanceStats() {
    final stats = super.getPerformanceStats();
    stats.addAll({
      'totalCategories': _categories.length,
      'incomeCategories': _categories.where((c) => c.type == 1).length,
      'expenseCategories': _categories.where((c) => c.type == 2).length,
      'compositeCategories': getAllCompositeCategories().length,
    });
    return stats;
  }

  /// 导出数据
  Future<List<Map<String, dynamic>>> exportCategories() async {
    return await executeAsync(
      () async {
        return _categories.map((category) => {
          'id': category.id,
          'name': category.name,
          'icon': category.icon,
          'type': category.type,
        }).toList();
      },
      'exportCategories',
      showLoading: false,
    );
  }
}