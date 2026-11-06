import 'dart:developer' as developer;
import 'package:flutter/material.dart';

import 'package:connectivity_plus/connectivity_plus.dart';
import '../database/database_service.dart';
import '../../data/services/bill_service.dart';
import '../../data/services/category_service.dart';
import '../../data/services/saving_goal_service.dart';
import '../../data/models/bill_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/saving_goal_model.dart';
import 'unified_error_handling_service.dart';

class SyncService {
  final UnifiedErrorHandlingService _errorHandler;
  final BillService _billService;
  final CategoryService _categoryService;
  final SavingGoalService _savingGoalService;
  final DatabaseService _databaseService;
  final Connectivity _connectivity = Connectivity();

  SyncService({
    required BillService billService,
    required CategoryService categoryService,
    required SavingGoalService savingGoalService,
  }) : _errorHandler = UnifiedErrorHandlingService(),
       _billService = billService,
       _categoryService = categoryService,
       _savingGoalService = savingGoalService,
       _databaseService = DatabaseService.instance;

  // Check if device is connected to internet
  Future<bool> isConnected() async {
    final result = await _connectivity.checkConnectivity();
    return _hasConnection(result);
  }

  // Sync all data from server to local database
  Future<void> syncFromServer({BuildContext? context}) async {
    if (!await isConnected()) return;

    try {
      await _databaseService.setSyncing(true);

      // Sync categories
      final categories = await _categoryService.getCategories();
      await _saveCategoriesToLocal(categories);

      // Sync bills
      final bills = await _billService.getBills();
      await _saveBillsToLocal(bills);

      // 预算功能已移除，跳过预算同步

      // Sync saving goals
      final savingGoals = await _savingGoalService.getSavingGoals();
      await _saveSavingGoalsToLocal(savingGoals);

      // Update last sync time
      await _databaseService.updateLastSyncTime();
    } catch (e, stackTrace) {
      // 使用统一错误处理（不依赖BuildContext）
      _errorHandler.handleError(
        '从服务器同步数据',
        e,
      );
      developer.log(
        'Error syncing from server',
        name: 'SyncService',
        error: e,
        stackTrace: stackTrace,
      );
    } finally {
      await _databaseService.setSyncing(false);
    }
  }

  // Save categories to local database
  Future<void> _saveCategoriesToLocal(List<Category> categories) async {
    final db = await _databaseService.database;
    await db.delete('categories');
    for (final category in categories) {
      await db.insert('categories', {
        'name': category.name,
        'type': category.type,
        'icon': category.icon,
      });
    }
  }

  // Save bills to local database
  Future<void> _saveBillsToLocal(List<Bill> bills) async {
    final db = await _databaseService.database;
    await db.delete('bills');
    for (final bill in bills) {
      await db.insert('bills', {
        'type': bill.type,
        'categoryName': bill.categoryName,
        'amount': bill.amount,
        'transactionDate': bill.transactionDate,
        'remark': bill.remark,
      });
    }
  }

  // 预算功能已移除，预算保存方法已废弃
  Future<void> _saveBudgetsToLocal(List<dynamic> budgets) async {
    // 预算功能已移除，此方法不再使用
    return;
  }

  // Save saving goals to local database
  Future<void> _saveSavingGoalsToLocal(List<SavingGoal> savingGoals) async {
    final db = await _databaseService.database;
    await db.delete('saving_goals');
    for (final goal in savingGoals) {
      await db.insert('saving_goals', {
        'name': goal.name,
        'targetAmount': goal.targetAmount,
        'currentAmount': goal.currentAmount,
        'deadline': goal.deadline,
        'description': goal.description,
      });
    }
  }

  // Get bills from local database
  Future<List<Bill>> getBillsFromLocal() async {
    final db = await _databaseService.database;
    final maps = await db.query('bills');
    return maps
        .map(
          (map) => Bill(
            id: map['id'] as int?,
            type: map['type'] as int,
            categoryName: map['categoryName'] as String,
            amount: map['amount'] as double,
            transactionDate: map['transactionDate'] as String,
            remark: map['remark'] as String?,
          ),
        )
        .toList();
  }

  // Get categories from local database
  Future<List<Category>> getCategoriesFromLocal() async {
    final db = await _databaseService.database;
    final maps = await db.query('categories');
    return maps
        .map(
          (map) => Category(
            name: map['name'] as String,
            type: map['type'] as int,
            icon: map['icon'] as String,
          ),
        )
        .toList();
  }

  // 预算功能已移除，预算获取方法已废弃
  Future<List<dynamic>> getBudgetsFromLocal(int year, int month) async {
    // 预算功能已移除，返回空列表
    return [];
  }

  // Get saving goals from local database
  Future<List<SavingGoal>> getSavingGoalsFromLocal() async {
    final db = await _databaseService.database;
    final maps = await db.query('saving_goals');
    return maps
        .map(
          (map) => SavingGoal(
            name: map['name'] as String,
            targetAmount: map['targetAmount'] as double,
            currentAmount: map['currentAmount'] as double,
            deadline: _parseDeadline(map['deadline']),
            description: map['description'] as String,
            categoryName: map['categoryName'] as String? ?? '未分类',
          ),
        )
        .toList();
  }

  DateTime _parseDeadline(dynamic rawDeadline) {
    if (rawDeadline is DateTime) {
      return rawDeadline;
    }
    if (rawDeadline is int) {
      // Interpret stored integer as milliseconds since epoch.
      return DateTime.fromMillisecondsSinceEpoch(rawDeadline);
    }
    if (rawDeadline is num) {
      return DateTime.fromMillisecondsSinceEpoch(rawDeadline.toInt());
    }
    if (rawDeadline is String && rawDeadline.isNotEmpty) {
      final parsed = DateTime.tryParse(rawDeadline);
      if (parsed != null) {
        return parsed;
      }
    }
    // Fallback to current date to avoid crashes if data is malformed.
    return DateTime.now();
  }

  // Initialize sync service and start periodic sync
  Future<void> initialize() async {
    // Initial sync
    await syncFromServer();

    // Set up periodic sync every 10 minutes
    // This is just an example, you might want to adjust the interval
    // or use a more sophisticated sync strategy
    // Timer.periodic(const Duration(minutes: 10), (timer) {
    //   syncFromServer();
    // });

    // Listen for connectivity changes
    _connectivity.onConnectivityChanged.listen((result) {
      if (_hasConnection(result)) {
        syncFromServer();
      }
    });
  }

  bool _hasConnection(dynamic result) {
    if (result is Iterable<ConnectivityResult>) {
      return result.any((status) => status != ConnectivityResult.none);
    }
    return result != ConnectivityResult.none;
  }
}
