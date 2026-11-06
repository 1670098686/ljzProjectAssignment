import 'api_client.dart';
import '../services/data_persistence_service.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';

import '../../data/models/transaction_model.dart';
import '../../data/models/category_model.dart';

/// API适配器类
/// 负责统一前后端接口，处理数据格式转换和错误处理
class ApiAdapter {
  final ApiClient _apiClient;
  final DataPersistenceService _persistenceService;

  ApiAdapter({ApiClient? apiClient, DataPersistenceService? persistenceService})
    : _apiClient = apiClient ?? ApiClient(),
      _persistenceService = persistenceService ?? DataPersistenceService();

  // ==================== 储蓄目标接口 ====================

  Future<List<SavingGoal>> getSavingGoals() async {
    try {
      return await _apiClient.get<List<SavingGoal>>(
        '/api/v1/saving-goals',
        fromJson: (data) => _mapList(data, SavingGoal.fromJson),
      );
    } catch (e) {
      return _persistenceService.getSavingGoalsFromLocal();
    }
  }

  Future<SavingGoal> createSavingGoal(SavingGoal goal) async {
    try {
      return await _apiClient.post<SavingGoal>(
        '/api/v1/saving-goals',
        data: goal.toJson(),
        fromJson: (data) => _mapObject(data, SavingGoal.fromJson),
      );
    } catch (e) {
      final savedGoal = goal.copyWith(
        id: DateTime.now().millisecondsSinceEpoch,
      );
      await _persistenceService.saveSavingGoalToLocal(savedGoal);
      return savedGoal;
    }
  }

  Future<SavingGoal> updateSavingGoal(SavingGoal goal) async {
    try {
      return await _apiClient.put<SavingGoal>(
        '/api/v1/saving-goals/${goal.id}',
        data: goal.toJson(),
        fromJson: (data) => _mapObject(data, SavingGoal.fromJson),
      );
    } catch (e) {
      await _persistenceService.saveSavingGoalToLocal(goal);
      return goal;
    }
  }

  Future<void> deleteSavingGoal(int id) async {
    try {
      await _apiClient.delete<dynamic>(
        '/api/v1/saving-goals/$id',
        fromJson: (_) => null,
      );
    } catch (e) {
      await _persistenceService.deleteSavingGoalFromLocal(id);
    }
  }

  // ==================== 交易记录接口 ====================

  Future<List<Transaction>> getTransactions({
    Map<String, dynamic>? query,
  }) async {
    try {
      return await _apiClient.get<List<Transaction>>(
        '/transactions',
        queryParameters: query,
        fromJson: (data) => _mapList(data, Transaction.fromJson),
      );
    } catch (e) {
      return _persistenceService.getAllTransactionsFromLocal();
    }
  }

  Future<Transaction> createTransaction(Transaction transaction) async {
    try {
      return await _apiClient.post<Transaction>(
        '/transactions',
        data: transaction.toJson(),
        fromJson: (data) => _mapObject(data, Transaction.fromJson),
      );
    } catch (e) {
      final savedTransaction = transaction.copyWith(
        id: DateTime.now().millisecondsSinceEpoch,
      );
      await _persistenceService.saveTransactionToLocal(savedTransaction);
      return savedTransaction;
    }
  }

  Future<Transaction> updateTransaction(Transaction transaction) async {
    try {
      return await _apiClient.put<Transaction>(
        '/transactions/${transaction.id}',
        data: transaction.toJson(),
        fromJson: (data) => _mapObject(data, Transaction.fromJson),
      );
    } catch (e) {
      await _persistenceService.saveTransactionToLocal(transaction);
      return transaction;
    }
  }

  Future<void> deleteTransaction(int id) async {
    try {
      await _apiClient.delete<dynamic>(
        '/transactions/$id',
        fromJson: (_) => null,
      );
    } catch (e) {
      await _persistenceService.deleteTransactionFromLocal(id);
    }
  }

  // ==================== 分类接口 ====================

  Future<List<Category>> getCategories({int? type}) async {
    try {
      final params = type != null ? {'type': type} : null;
      return await _apiClient.get<List<Category>>(
        '/categories',
        queryParameters: params,
        fromJson: (data) => _mapList(data, Category.fromJson),
      );
    } catch (e) {
      final localCategories = await _persistenceService
          .getCategoriesFromLocal();
      return localCategories;
    }
  }

  Future<Category> createCategory(Category category) async {
    try {
      return await _apiClient.post<Category>(
        '/categories',
        data: category.toJson(),
        fromJson: (data) => _mapObject(data, Category.fromJson),
      );
    } catch (e) {
      final savedCategory = category.copyWith(
        id: DateTime.now().millisecondsSinceEpoch,
      );
      await _persistenceService.saveCategoryToLocal(savedCategory);
      return savedCategory;
    }
  }

  Future<Category> updateCategory(Category category) async {
    try {
      return await _apiClient.put<Category>(
        '/categories/${category.id}',
        data: category.toJson(),
        fromJson: (data) => _mapObject(data, Category.fromJson),
      );
    } catch (e) {
      await _persistenceService.saveCategoryToLocal(category);
      return category;
    }
  }

  Future<void> deleteCategory(int id) async {
    try {
      await _apiClient.delete<dynamic>(
        '/categories/$id',
        fromJson: (_) => null,
      );
    } catch (e) {
      await _persistenceService.deleteCategoryFromLocal(id);
    }
  }

  // ==================== 统计 & 实用工具 ====================

  Future<Map<String, dynamic>> getStatistics({
    Map<String, dynamic>? params,
  }) async {
    try {
      return await _apiClient.get<Map<String, dynamic>>(
        '/statistics',
        queryParameters: params,
        fromJson: (data) =>
            Map<String, dynamic>.from((data as Map?) ?? const {}),
      );
    } catch (e) {
      final transactions = await _persistenceService
          .getAllTransactionsFromLocal();
      return _calculateLocalStatistics(transactions);
    }
  }

  Map<String, dynamic> _calculateLocalStatistics(
    List<Transaction> transactions,
  ) {
    double totalIncome = 0;
    double totalExpense = 0;
    int transactionCount = transactions.length;

    for (final transaction in transactions) {
      if (transaction.type == 1) {
        totalIncome += transaction.amount;
      } else if (transaction.type == 2) {
        totalExpense += transaction.amount;
      }
    }

    return {
      'totalIncome': totalIncome,
      'totalExpense': totalExpense,
      'netIncome': totalIncome - totalExpense,
      'transactionCount': transactionCount,
    };
  }

  Future<bool> isOnline() async {
    try {
      await _apiClient.get<dynamic>('/health', fromJson: (_) => null);
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<void> syncLocalData() async {
    try {
      final online = await isOnline();
      if (!online) {
        throw Exception('网络连接不可用');
      }

      final localGoals = await _persistenceService.getSavingGoalsFromLocal();
      for (final goal in localGoals) {
        if (goal.id != null && goal.id! > 1000000000000) {
          await createSavingGoal(goal);
        }
      }

      final localTransactions = await _persistenceService
          .getAllTransactionsFromLocal();
      for (final transaction in localTransactions) {
        if (transaction.id != null && transaction.id! > 1000000000000) {
          await createTransaction(transaction);
        }
      }
    } catch (e) {
      throw Exception('数据同步失败: $e');
    }
  }

  // ==================== 储蓄记录接口 ====================

  Future<List<SavingRecord>> getSavingRecords() async {
    try {
      return await _apiClient.get<List<SavingRecord>>(
        '/saving-records',
        fromJson: (data) => _mapList(data, SavingRecord.fromJson),
      );
    } catch (e) {
      final localRecords = await _persistenceService.getAllSavingRecordsFromLocal();
      return localRecords;
    }
  }

  Future<SavingRecord> createSavingRecord(SavingRecord record) async {
    try {
      return await _apiClient.post<SavingRecord>(
        '/saving-records',
        data: record.toJson(),
        fromJson: (data) => _mapObject(data, SavingRecord.fromJson),
      );
    } catch (e) {
      final savedRecord = record.copyWith(
        id: DateTime.now().millisecondsSinceEpoch,
      );
      await _persistenceService.saveSavingRecordToLocal(savedRecord);
      return savedRecord;
    }
  }

  Future<SavingRecord> updateSavingRecord(SavingRecord record) async {
    try {
      return await _apiClient.put<SavingRecord>(
        '/saving-records/${record.id}',
        data: record.toJson(),
        fromJson: (data) => _mapObject(data, SavingRecord.fromJson),
      );
    } catch (e) {
      await _persistenceService.saveSavingRecordToLocal(record);
      return record;
    }
  }

  Future<void> deleteSavingRecord(int id) async {
    try {
      await _apiClient.delete<dynamic>(
        '/saving-records/$id',
        fromJson: (_) => null,
      );
    } catch (e) {
      await _persistenceService.deleteSavingRecordFromLocal(id);
    }
  }

  static List<T> _mapList<T>(
    dynamic data,
    T Function(Map<String, dynamic>) builder,
  ) {
    final list = (data as List?) ?? const [];
    return list
        .map((item) => builder(Map<String, dynamic>.from(item as Map)))
        .toList();
  }

  static T _mapObject<T>(
    dynamic data,
    T Function(Map<String, dynamic>) builder,
  ) {
    return builder(Map<String, dynamic>.from(data as Map));
  }
}

class ApiAdapterSingleton {
  static ApiAdapter? _instance;
  static ApiAdapter get instance => _instance ??= ApiAdapter();
}
