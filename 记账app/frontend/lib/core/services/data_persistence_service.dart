import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/foundation.dart' show debugPrint;

import '../../data/models/category_model.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';
import '../../data/models/transaction_model.dart';

/// 数据持久化服务类
/// 使用SharedPreferences实现应用数据的本地持久化存储
class DataPersistenceService {
  const DataPersistenceService();

  // 实例级操作封装，供 ApiAdapter 等服务调用

  Future<List<SavingGoal>> getSavingGoalsFromLocal() async {
    return await loadSavingGoals();
  }

  Future<void> saveSavingGoalToLocal(SavingGoal goal) async {
    final goals = await loadSavingGoals();
    final goalWithId = goal.id != null
        ? goal
        : goal.copyWith(id: DateTime.now().millisecondsSinceEpoch);
    final index = goals.indexWhere((g) => g.id == goalWithId.id);
    if (index >= 0) {
      goals[index] = goalWithId;
    } else {
      goals.add(goalWithId);
    }
    await saveSavingGoals(goals);
  }

  Future<void> deleteSavingGoalFromLocal(int id) async {
    final goals = await loadSavingGoals();
    goals.removeWhere((g) => g.id == id);
    await saveSavingGoals(goals);
  }

  Future<List<SavingRecord>> getAllSavingRecordsFromLocal() async {
    return await loadSavingRecords();
  }

  Future<void> saveSavingRecordToLocal(SavingRecord record) async {
    final records = await loadSavingRecords();
    final recordWithId = record.id != null
        ? record
        : record.copyWith(id: DateTime.now().millisecondsSinceEpoch);
    final index = records.indexWhere((r) => r.id == recordWithId.id);
    if (index >= 0) {
      records[index] = recordWithId;
    } else {
      records.add(recordWithId);
    }
    await saveSavingRecords(records);
  }

  Future<void> deleteSavingRecordFromLocal(int id) async {
    final records = await loadSavingRecords();
    records.removeWhere((r) => r.id == id);
    await saveSavingRecords(records);
  }

  Future<List<Transaction>> getAllTransactionsFromLocal() async {
    return await loadTransactions();
  }

  Future<void> saveTransactionToLocal(Transaction transaction) async {
    final transactions = await loadTransactions();
    final txWithId = transaction.id != null
        ? transaction
        : transaction.copyWith(id: DateTime.now().millisecondsSinceEpoch);
    final index = transactions.indexWhere((t) => t.id == txWithId.id);
    if (index >= 0) {
      transactions[index] = txWithId;
    } else {
      transactions.add(txWithId);
    }
    await saveTransactions(transactions);
  }

  Future<void> deleteTransactionFromLocal(int id) async {
    final transactions = await loadTransactions();
    transactions.removeWhere((t) => t.id == id);
    await saveTransactions(transactions);
  }

  Future<List<Category>> getCategoriesFromLocal() async {
    final stored = await loadCategories();
    return stored.map((json) => _categoryFromJson(json)).toList();
  }

  Future<void> saveCategoryToLocal(Category category) async {
    final categories = await getCategoriesFromLocal();
    final categoryWithId = category.id != null
        ? category
        : category.copyWith(id: DateTime.now().millisecondsSinceEpoch);
    final index = categories.indexWhere((c) => c.id == categoryWithId.id);
    if (index >= 0) {
      categories[index] = categoryWithId;
    } else {
      categories.add(categoryWithId);
    }
    await saveCategories(categories.map(_categoryToJson).toList());
  }

  Future<void> deleteCategoryFromLocal(int id) async {
    final categories = await getCategoriesFromLocal();
    categories.removeWhere((c) => c.id == id);
    await saveCategories(categories.map(_categoryToJson).toList());
  }

  // 存储键常量
  static const String _goalsKey = 'saving_goals';
  static const String _transactionsKey = 'transactions';
  static const String _budgetsKey = 'budgets';
  static const String _settingsKey = 'user_settings';
  static const String _categoriesKey = 'categories';
  static const String _savingRecordsKey = 'saving_records';

  // 分类由用户自定义，不插入预设分类
  static const List<Map<String, dynamic>> _defaultCategories = [];

  // ==================== 储蓄目标数据持久化 ====================

  /// 保存储蓄目标列表
  static Future<void> saveSavingGoals(List<SavingGoal> goals) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final goalsJson = goals.map((g) => _goalToJson(g)).toList();
      await prefs.setString(_goalsKey, json.encode(goalsJson));
      debugPrint('已保存 ${goals.length} 个储蓄目标到本地存储');
    } catch (e) {
      debugPrint('保存储蓄目标失败: $e');
      rethrow;
    }
  }

  /// 加载储蓄目标列表
  static Future<List<SavingGoal>> loadSavingGoals() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final goalsJson = prefs.getString(_goalsKey);

      if (goalsJson == null) {
        debugPrint('未找到本地存储的储蓄目标，返回空列表');
        return [];
      }

      final goalsList = json.decode(goalsJson) as List;
      final goals = goalsList.map((json) => _goalFromJson(json)).toList();

      debugPrint('从本地存储加载了 ${goals.length} 个储蓄目标');
      return goals;
    } catch (e) {
      debugPrint('加载储蓄目标失败: $e');
      return [];
    }
  }

  // ==================== 储蓄记录数据持久化 ====================

  /// 保存储蓄记录列表
  static Future<void> saveSavingRecords(List<SavingRecord> records) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final recordsJson = records.map((r) => _recordToJson(r)).toList();
      await prefs.setString(_savingRecordsKey, json.encode(recordsJson));
      debugPrint('已保存 ${records.length} 条储蓄记录到本地存储');
    } catch (e) {
      debugPrint('保存储蓄记录失败: $e');
      rethrow;
    }
  }

  /// 加载储蓄记录列表
  static Future<List<SavingRecord>> loadSavingRecords() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final recordsJson = prefs.getString(_savingRecordsKey);

      if (recordsJson == null) {
        debugPrint('未找到本地存储的储蓄记录，返回空列表');
        return [];
      }

      final recordsList = json.decode(recordsJson) as List;
      final records = recordsList.map((json) => _recordFromJson(json)).toList();

      debugPrint('从本地存储加载了 ${records.length} 条储蓄记录');
      return records;
    } catch (e) {
      debugPrint('加载储蓄记录失败: $e');
      return [];
    }
  }

  // ==================== 交易数据持久化 ====================
  static Future<void> saveTransactions(List<Transaction> transactions) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonList = transactions.map(_transactionToJson).toList();
      await prefs.setString(_transactionsKey, json.encode(jsonList));
      debugPrint('已保存 ${transactions.length} 条交易记录到本地存储');
    } catch (e) {
      debugPrint('保存交易记录失败: $e');
      rethrow;
    }
  }

  static Future<List<Transaction>> loadTransactions() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonString = prefs.getString(_transactionsKey);
      if (jsonString == null) {
        debugPrint('未找到本地存储的交易记录，返回空列表');
        return [];
      }
      final data = json.decode(jsonString) as List;
      final transactions = data
          .map((json) => _transactionFromJson(json))
          .toList();
      debugPrint('从本地存储加载了 ${transactions.length} 条交易记录');
      return transactions;
    } catch (e) {
      debugPrint('加载交易记录失败: $e');
      return [];
    }
  }

  // ==================== 预算数据持久化 ====================

  /// 保存预算数据
  static Future<void> saveBudgets(List<Map<String, dynamic>> budgets) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_budgetsKey, json.encode(budgets));
      debugPrint('已保存预算数据到本地存储');
    } catch (e) {
      debugPrint('保存预算数据失败: $e');
      rethrow;
    }
  }

  /// 加载预算数据
  static Future<List<Map<String, dynamic>>> loadBudgets() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final budgetsJson = prefs.getString(_budgetsKey);

      if (budgetsJson == null) {
        debugPrint('未找到本地存储的预算数据，返回空列表');
        return [];
      }

      final budgetsList = json.decode(budgetsJson) as List;
      debugPrint('从本地存储加载了 ${budgetsList.length} 条预算数据');
      return budgetsList.cast<Map<String, dynamic>>();
    } catch (e) {
      debugPrint('加载预算数据失败: $e');
      return [];
    }
  }

  // ==================== 分类数据持久化 ====================

  /// 保存分类数据
  static Future<void> saveCategories(
    List<Map<String, dynamic>> categories,
  ) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_categoriesKey, json.encode(categories));
      debugPrint('已保存分类数据到本地存储');
    } catch (e) {
      debugPrint('保存分类数据失败: $e');
      rethrow;
    }
  }

  /// 加载分类数据
  static Future<List<Map<String, dynamic>>> loadCategories() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final categoriesJson = prefs.getString(_categoriesKey);

      if (categoriesJson == null) {
        // 如果没有存储的分类数据，返回预置默认分类
        debugPrint('未找到本地存储的分类数据，返回默认分类');
        await saveCategories(_defaultCategories);
        return _defaultCategories;
      }

      final categoriesList = json.decode(categoriesJson) as List;
      debugPrint('从本地存储加载了 ${categoriesList.length} 个分类');
      return categoriesList.cast<Map<String, dynamic>>();
    } catch (e) {
      debugPrint('加载分类数据失败，返回默认分类: $e');
      return _defaultCategories;
    }
  }

  // ==================== 用户设置数据持久化 ====================

  /// 保存用户设置
  static Future<void> saveUserSettings(Map<String, dynamic> settings) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_settingsKey, json.encode(settings));
      debugPrint('已保存用户设置到本地存储');
    } catch (e) {
      debugPrint('保存用户设置失败: $e');
      rethrow;
    }
  }

  /// 加载用户设置
  static Future<Map<String, dynamic>> loadUserSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final settingsJson = prefs.getString(_settingsKey);

      if (settingsJson == null) {
        // 返回默认设置
        debugPrint('未找到本地存储的用户设置，返回默认设置');
        return _getDefaultSettings();
      }

      final settings = json.decode(settingsJson) as Map<String, dynamic>;
      debugPrint('从本地存储加载了用户设置');
      return settings;
    } catch (e) {
      debugPrint('加载用户设置失败，返回默认设置: $e');
      return _getDefaultSettings();
    }
  }

  /// 获取默认用户设置
  static Map<String, dynamic> _getDefaultSettings() {
    return {
      'theme': 'light',
      'currency': '¥',
      'language': 'zh-CN',
      'notifications': true,
      'autoBackup': false,
      'budgetAlerts': true,
      'savingReminders': false,
    };
  }

  // ==================== 应用初始化和数据恢复 ====================

  /// 应用启动时初始化数据
  static Future<Map<String, dynamic>> initializeAppData() async {
    debugPrint('开始初始化应用数据...');

    final loadingResults = <String, dynamic>{};

    try {
      // 并行加载所有数据
      final results = await Future.wait([
        loadSavingGoals(),
        loadSavingRecords(),
        loadTransactions(),
        loadBudgets(),
        loadCategories(),
        loadUserSettings(),
      ]);

      loadingResults['savingGoals'] = results[0] as List<SavingGoal>;
      loadingResults['savingRecords'] = results[1] as List<SavingRecord>;
      loadingResults['transactions'] = results[2] as List<Transaction>;
      loadingResults['budgets'] = results[3] as List<Map<String, dynamic>>;
      loadingResults['categories'] = results[4] as List<Map<String, dynamic>>;
      loadingResults['settings'] = results[5] as Map<String, dynamic>;

      debugPrint('应用数据初始化完成');
      debugPrint('加载结果: ${loadingResults.length} 类数据');

      return loadingResults;
    } catch (e) {
      debugPrint('应用数据初始化失败: $e');
      rethrow;
    }
  }

  // ==================== 数据清理和重置 ====================

  /// 清除所有持久化数据
  static Future<void> clearAllData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await Future.wait([
        prefs.remove(_goalsKey),
        prefs.remove(_transactionsKey),
        prefs.remove(_budgetsKey),
        prefs.remove(_settingsKey),
        prefs.remove(_categoriesKey),
        prefs.remove(_savingRecordsKey),
      ]);
      debugPrint('已清除所有持久化数据');
    } catch (e) {
      debugPrint('清除数据失败: $e');
      rethrow;
    }
  }

  /// 检查是否有持久化数据
  static Future<bool> hasPersistedData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final keys = [
        _goalsKey,
        _transactionsKey,
        _budgetsKey,
        _categoriesKey,
        _savingRecordsKey,
      ];

      for (final key in keys) {
        if (prefs.containsKey(key)) {
          return true;
        }
      }
      return false;
    } catch (e) {
      debugPrint('检查持久化数据失败: $e');
      return false;
    }
  }

  // ==================== 数据备份和恢复 ====================

  /// 导出所有数据为JSON字符串
  static Future<String> exportAllData() async {
    final data = await initializeAppData();
    return json.encode(data);
  }

  /// 导入数据从JSON字符串
  static Future<void> importAllData(String jsonData) async {
    try {
      final data = json.decode(jsonData) as Map<String, dynamic>;

      if (data.containsKey('savingGoals')) {
        final goals = (data['savingGoals'] as List)
            .map((json) => SavingGoal.fromJson(Map<String, dynamic>.from(json)))
            .toList();
        await saveSavingGoals(goals);
      }

      if (data.containsKey('savingRecords')) {
        final records = (data['savingRecords'] as List)
            .map(
              (json) => SavingRecord.fromJson(Map<String, dynamic>.from(json)),
            )
            .toList();
        await saveSavingRecords(records);
      }

      if (data.containsKey('transactions')) {
        final transactions = (data['transactions'] as List)
            .map(
              (json) => Transaction.fromJson(Map<String, dynamic>.from(json)),
            )
            .toList();
        await saveTransactions(transactions);
      }

      if (data.containsKey('budgets')) {
        final budgets = (data['budgets'] as List)
            .map((json) => Map<String, dynamic>.from(json as Map))
            .toList();
        await saveBudgets(budgets);
      }

      if (data.containsKey('categories')) {
        final categories = (data['categories'] as List)
            .map((json) => Map<String, dynamic>.from(json as Map))
            .toList();
        await saveCategories(categories);
      }

      if (data.containsKey('settings')) {
        await saveUserSettings(data['settings'] as Map<String, dynamic>);
      }

      debugPrint('数据导入成功');
    } catch (e) {
      debugPrint('数据导入失败: $e');
      rethrow;
    }
  }

  static Map<String, dynamic> _goalToJson(SavingGoal goal) => goal.toJson();

  static SavingGoal _goalFromJson(dynamic json) {
    return SavingGoal.fromJson(Map<String, dynamic>.from(json as Map));
  }

  static Map<String, dynamic> _recordToJson(SavingRecord record) =>
      record.toJson();

  static SavingRecord _recordFromJson(dynamic json) {
    return SavingRecord.fromJson(Map<String, dynamic>.from(json as Map));
  }

  static Map<String, dynamic> _transactionToJson(Transaction transaction) =>
      transaction.toJson();

  static Transaction _transactionFromJson(dynamic json) {
    return Transaction.fromJson(Map<String, dynamic>.from(json as Map));
  }

  static Map<String, dynamic> _categoryToJson(Category category) =>
      category.toJson();

  static Category _categoryFromJson(dynamic json) {
    return Category.fromJson(Map<String, dynamic>.from(json as Map));
  }
}
