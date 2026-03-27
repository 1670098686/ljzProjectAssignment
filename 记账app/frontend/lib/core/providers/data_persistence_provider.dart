import 'dart:convert';
import 'dart:developer';
import 'package:flutter/foundation.dart';
import '../services/enhanced_data_persistence_service.dart';
import '../providers/state_sync_manager.dart';
import '../services/unified_error_handling_service.dart';
import '../../data/models/saving_goal_model.dart';


/// 数据持久化Provider
/// 负责统一管理应用数据的本地存储和恢复
/// 集成统一的错误处理机制和状态同步机制
class DataPersistenceProvider extends ChangeNotifier {
  bool _isLoading = false;
  bool _hasError = false;
  String? _errorMessage;
  
  final EnhancedDataPersistenceService _dataService = EnhancedDataPersistenceService();
  final UnifiedErrorHandlingService _errorHandler = UnifiedErrorHandlingService();

  DataPersistenceProvider() {
    _initialize();
  }

  /// 初始化Provider
  Future<void> _initialize() async {
    try {
      await _dataService.initialize();
      
      // 注册状态同步监听器
      StateSyncManager().addSyncListener('data_persistence', _onSyncNotification);
      
      log('数据持久化Provider初始化完成');
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '初始化数据持久化Provider',
      );
    }
  }

  /// 响应StateSyncManager同步通知
  void _onSyncNotification() {
    try {
      log('DataPersistenceProvider收到同步通知，开始清除数据...');
      clearAllData();
    } catch (e) {
      log('DataPersistenceProvider处理同步通知时出错: $e');
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '处理同步通知',
      );
    }
  }

  /// 清除数据方法
  void clearData() {
    try {
      _savingGoals.clear();
      _budgets.clear();
      _categories.clear();
      _userSettings.clear();
      notifyListeners();
      log('DataPersistenceProvider数据已清除');
    } catch (e) {
      log('DataPersistenceProvider清除数据时出错: $e');
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '清除数据',
      );
    }
  }

  // 数据状态
  List<SavingGoal> _savingGoals = [];

  List<Map<String, dynamic>> _budgets = [];
  List<Map<String, dynamic>> _categories = [];
  Map<String, dynamic> _userSettings = {};

  // Getters
  bool get isLoading => _isLoading;
  bool get hasError => _hasError;
  String? get errorMessage => _errorMessage;

  List<SavingGoal> get savingGoals => _savingGoals;
  List<Map<String, dynamic>> get budgets => _budgets;
  List<Map<String, dynamic>> get categories => _categories;
  Map<String, dynamic> get userSettings => _userSettings;

  bool get hasData =>
      _savingGoals.isNotEmpty ||
      _budgets.isNotEmpty ||
      _categories.isNotEmpty;

  // ==================== 储蓄目标数据管理 ====================

  /// 加载所有数据
  Future<void> _loadAllData() async {
    try {
      _setLoading(true);
      
      await loadSavingGoals();
      await loadBudgets();
      await loadCategories();
      await loadUserSettings();
      
      _setLoading(false);
      
      // 通知状态同步管理器数据加载完成
      await StateSyncManager().triggerSync(
        operation: 'load_all_data',
        syncAction: () async {},
        description: '所有数据加载完成',
      );
    } catch (e) {
      _setLoading(false);
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '加载所有数据',
      );
    }
  }

  /// 保存储蓄目标列表
  Future<void> saveSavingGoals(
    List<SavingGoal> goals, {
    VoidCallback? onRetry,
  }) async {
    try {
      final success = await _dataService.saveData(
        key: 'saving_goals',
        data: goals.map((goal) => goal.toJson()).toList(),
      );
      
      if (success) {
        _savingGoals = goals;
        notifyListeners();
        log('储蓄目标已保存到本地存储');
      } else {
        throw Exception('保存失败');
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '保存储蓄目标',
      );
      rethrow;
    }
  }

  /// 加载储蓄目标列表
  Future<void> loadSavingGoals() async {
    try {
      final data = _dataService.loadData<List<dynamic>>(
        key: 'saving_goals',
        fromJson: (json) => json as List<dynamic>,
      );
      
      if (data != null) {
        _savingGoals = data.map((item) => SavingGoal.fromJson(item)).toList();
        notifyListeners();
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '加载储蓄目标',
      );
    }
  }

  /// 添加储蓄目标
  Future<void> addSavingGoal(SavingGoal goal) async {
    try {
      _savingGoals.add(goal);
      await saveSavingGoals(_savingGoals);
    } catch (e) {
      _setError('添加储蓄目标失败: $e');
      rethrow;
    }
  }

  /// 更新储蓄目标
  Future<void> updateSavingGoal(SavingGoal goal) async {
    try {
      final index = _savingGoals.indexWhere((g) => g.id == goal.id);
      if (index != -1) {
        _savingGoals[index] = goal;
        await saveSavingGoals(_savingGoals);
      }
    } catch (e) {
      _setError('更新储蓄目标失败: $e');
      rethrow;
    }
  }

  /// 删除储蓄目标
  Future<void> deleteSavingGoal(int id) async {
    try {
      _savingGoals.removeWhere((g) => g.id == id);
      await saveSavingGoals(_savingGoals);
    } catch (e) {
      _setError('删除储蓄目标失败: $e');
      rethrow;
    }
  }



  // ==================== 预算数据管理 ====================

  /// 保存预算数据
  Future<void> saveBudgets(List<Map<String, dynamic>> budgets) async {
    try {
      final success = await _dataService.saveData(
        key: 'budgets',
        data: budgets,
      );
      
      if (success) {
        _budgets = budgets;
        notifyListeners();
        log('预算数据已保存到本地存储');
      } else {
        throw Exception('保存失败');
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '保存预算数据',
      );
      rethrow;
    }
  }

  /// 加载预算数据
  Future<void> loadBudgets() async {
    try {
      final data = _dataService.loadData<List<dynamic>>(
        key: 'budgets',
        fromJson: (json) => json as List<dynamic>,
      );
      
      if (data != null) {
        _budgets = data.cast<Map<String, dynamic>>();
        notifyListeners();
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '加载预算数据',
      );
    }
  }

  // ==================== 分类数据管理 ====================

  /// 保存分类数据
  Future<void> saveCategories(List<Map<String, dynamic>> categories) async {
    try {
      final success = await _dataService.saveData(
        key: 'categories',
        data: categories,
      );
      
      if (success) {
        _categories = categories;
        notifyListeners();
        log('分类数据已保存到本地存储');
      } else {
        throw Exception('保存失败');
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '保存分类数据',
      );
      rethrow;
    }
  }

  /// 加载分类数据
  Future<void> loadCategories() async {
    try {
      final data = _dataService.loadData<List<dynamic>>(
        key: 'categories',
        fromJson: (json) => json as List<dynamic>,
      );
      
      if (data != null) {
        _categories = data.cast<Map<String, dynamic>>();
        notifyListeners();
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '加载分类数据',
      );
    }
  }

  // ==================== 用户设置管理 ====================

  /// 保存用户设置
  Future<void> saveUserSettings(Map<String, dynamic> settings) async {
    try {
      final success = await _dataService.saveData(
        key: 'user_settings',
        data: settings,
      );
      
      if (success) {
        _userSettings = settings;
        notifyListeners();
        log('用户设置已保存到本地存储');
      } else {
        throw Exception('保存失败');
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '保存用户设置',
      );
      rethrow;
    }
  }

  /// 加载用户设置
  Future<void> loadUserSettings() async {
    try {
      final data = _dataService.loadData<Map<String, dynamic>>(
        key: 'user_settings',
        fromJson: (json) => json,
      );
      
      if (data != null) {
        _userSettings = data;
        notifyListeners();
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '加载用户设置',
      );
    }
  }

  // ==================== 数据同步和恢复 ====================

  /// 同步所有数据到本地存储
  Future<void> syncAllData({
    List<SavingGoal>? goals,
    List<Map<String, dynamic>>? budgets,
    List<Map<String, dynamic>>? categories,
    Map<String, dynamic>? settings,
  }) async {
    try {
      _setLoading(true);

      // 并行保存所有数据
      final tasks = <Future>[];

      if (goals != null) {
        tasks.add(saveSavingGoals(goals));
      }

      if (budgets != null) {
        tasks.add(saveBudgets(budgets));
      }

      if (categories != null) {
        tasks.add(saveCategories(categories));
      }

      if (settings != null) {
        tasks.add(saveUserSettings(settings));
      }

      await Future.wait(tasks);

      log('所有数据同步完成');
    } catch (e) {
      _setError('数据同步失败: $e');
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// 从本地存储恢复所有数据
  Future<void> restoreAllData() async {
    try {
      _setLoading(true);

      // 并行加载所有数据
      await Future.wait([
        loadSavingGoals(),
        loadBudgets(),
        loadCategories(),
        loadUserSettings(),
      ]);

      log('所有数据恢复完成');
    } catch (e) {
      _setError('数据恢复失败: $e');
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// 导出所有数据
  Future<String> exportAllData() async {
    try {
      final data = _dataService.exportAllData();
      return json.encode(data);
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '导出数据',
      );
      rethrow;
    }
  }

  /// 导入数据
  Future<void> importData(String jsonData) async {
    try {
      _setLoading(true);
      
      final data = json.decode(jsonData) as Map<String, dynamic>;
      final success = await _dataService.importData(data);
      
      if (success) {
        await _loadAllData(); // 重新加载数据
        log('数据导入成功');
      } else {
        throw Exception('导入失败');
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '导入数据',
      );
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// 清除所有数据
  Future<void> clearAllData() async {
    try {
      final success = await _dataService.clearAllData();
      
      if (success) {
        _savingGoals.clear();
        _budgets.clear();
        _categories.clear();
        _userSettings.clear();
        notifyListeners();
        log('所有数据已清除');
      } else {
        throw Exception('清除失败');
      }
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '清除数据',
      );
      rethrow;
    }
  }

  /// 检查是否有持久化数据
  Future<bool> checkHasData() async {
    try {
      final keys = _dataService.getKeys();
      final importantKeys = [
        'saving_goals',
        'budgets',
        'categories',
        'user_settings',
      ];
      
      return keys.any((key) => importantKeys.contains(key));
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '检查数据',
      );
      return false;
    }
  }

  /// 获取数据统计信息
  Map<String, dynamic> getDataStatistics() {
    return _dataService.getDataStatistics();
  }

  /// 执行数据备份
  Future<void> performBackup() async {
    try {
      await _dataService.saveData(
        key: 'backup_${DateTime.now().millisecondsSinceEpoch}',
        data: _dataService.exportAllData(),
        backup: false, // 避免递归备份
      );
      log('数据备份完成');
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '数据备份',
      );
    }
  }

  /// 批量保存所有数据
  Future<Map<String, bool>> saveAllData({
    List<SavingGoal>? goals,
    List<Map<String, dynamic>>? budgets,
    List<Map<String, dynamic>>? categories,
    Map<String, dynamic>? settings,
  }) async {
    try {
      final dataMap = <String, dynamic>{};
      
      if (goals != null) {
        dataMap['saving_goals'] = goals.map((goal) => goal.toJson()).toList();
        _savingGoals = goals;
      }
      
      if (budgets != null) {
        dataMap['budgets'] = budgets;
        _budgets = budgets;
      }
      
      if (categories != null) {
        dataMap['categories'] = categories;
        _categories = categories;
      }
      
      if (settings != null) {
        dataMap['user_settings'] = settings;
        _userSettings = settings;
      }
      
      final results = await _dataService.saveBatchData(dataMap);
      
      if (results.values.every((success) => success)) {
        notifyListeners();
        log('批量数据保存完成');
      }
      
      return results;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '批量保存数据',
      );
      
      // 返回所有操作失败的结果
      final dataMap = <String, dynamic>{};
      if (goals != null) dataMap['saving_goals'] = false;
      if (budgets != null) dataMap['budgets'] = false;
      if (categories != null) dataMap['categories'] = false;
      if (settings != null) dataMap['user_settings'] = false;
      
      return dataMap.map((key, value) => MapEntry(key, false));
    }
  }

  // ==================== 状态管理方法 ====================

  void _setLoading(bool loading) {
    if (_isLoading != loading) {
      _isLoading = loading;
      notifyListeners();
    }
  }

  void _setError(String message) {
    _hasError = true;
    _errorMessage = message;
    debugPrint('DataPersistenceProvider Error: $message');
    notifyListeners();
  }

  void clearError() {
    _hasError = false;
    _errorMessage = null;
    notifyListeners();
  }

  /// 重置Provider状态
  void reset() {
    _isLoading = false;
    _hasError = false;
    _errorMessage = null;
    _savingGoals.clear();
    _budgets.clear();
    _categories.clear();
    _userSettings.clear();
    notifyListeners();
  }

  /// 检查数据完整性
  Map<String, bool> checkDataIntegrity() {
    return {
      'saving_goals': _savingGoals.isNotEmpty,
      'budgets': _budgets.isNotEmpty,
      'categories': _categories.isNotEmpty,
      'user_settings': _userSettings.isNotEmpty,
    };
  }

  /// 验证数据格式
  bool validateDataFormat() {
    try {
      // 验证储蓄目标数据格式
      for (final goal in _savingGoals) {
        if (goal.id == null || goal.name.isEmpty || goal.targetAmount <= 0) {
          return false;
        }
      }

      // 验证预算数据格式
      for (final budget in _budgets) {
        if (budget['category'] == null || budget['monthly_budget'] == null) {
          return false;
        }
      }

      return true;
    } catch (e) {
      return false;
    }
  }


}