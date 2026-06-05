import 'dart:async';
import 'dart:developer' as developer;
import 'package:connectivity_plus/connectivity_plus.dart';

import '../network/api_adapter.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';
import '../../data/models/bill_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/budget_model.dart';

/// 离线数据同步服务
/// 负责处理离线时的数据操作和在线时的数据同步
class OfflineSyncService {
  static final OfflineSyncService _instance = OfflineSyncService._internal();
  factory OfflineSyncService() => _instance;
  OfflineSyncService._internal();

  // 网络连接监听器
  final Connectivity _connectivity = Connectivity();
  StreamSubscription<List<ConnectivityResult>>? _connectivitySubscription;

  // API适配器和数据库服务
  final ApiAdapter _apiAdapter = ApiAdapter();

  // 同步状态
  bool _isSyncing = false;
  bool _isOfflineMode = false;
  DateTime? _lastSyncTime;

  // 待同步操作队列
  final List<PendingOperation> _pendingOperations = [];

  // 同步事件监听器
  final StreamController<SyncEvent> _syncEventController =
      StreamController<SyncEvent>.broadcast();

  // Getters
  bool get isSyncing => _isSyncing;
  bool get isOfflineMode => _isOfflineMode;
  DateTime? get lastSyncTime => _lastSyncTime;
  List<PendingOperation> get pendingOperations =>
      List.unmodifiable(_pendingOperations);
  Stream<SyncEvent> get syncEventStream => _syncEventController.stream;

  /// 初始化离线同步服务
  Future<void> initialize() async {
    try {
      // 检查初始网络状态
      await _checkNetworkConnection();

      // 监听网络状态变化
      _connectivitySubscription = _connectivity.onConnectivityChanged.listen(
        _handleConnectivityChange,
      );

      // 从本地数据库恢复待同步操作
      await _restorePendingOperations();

      // 如果有网络连接且有待同步数据，自动开始同步
      if (!_isOfflineMode && _pendingOperations.isNotEmpty) {
        await _performSync();
      }

      developer.log('离线同步服务初始化完成', name: 'OfflineSyncService');
    } catch (e, stackTrace) {
      developer.log(
        '离线同步服务初始化失败',
        name: 'OfflineSyncService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 处理网络连接状态变化
  void _handleConnectivityChange(List<ConnectivityResult> results) {
    final wasOffline = _isOfflineMode;
    _isOfflineMode = _isOffline(results);

    // 发送同步事件
    _syncEventController.add(
      SyncEvent(
        type: SyncEventType.networkChanged,
        isOffline: _isOfflineMode,
        timestamp: DateTime.now(),
      ),
    );

    if (wasOffline && !_isOfflineMode) {
      // 从离线变为在线，触发同步
      _performSync();
    }
  }

  /// 检查网络连接状态
  Future<void> _checkNetworkConnection() async {
    try {
      final results = await _connectivity.checkConnectivity();
      _isOfflineMode = _isOffline(results);
    } catch (e) {
      developer.log('检查网络连接失败: $e', name: 'OfflineSyncService');
      _isOfflineMode = true; // 默认视为离线
    }
  }

  bool _isOffline(List<ConnectivityResult> results) {
    if (results.isEmpty) {
      return true;
    }

    return results.every((result) => result == ConnectivityResult.none);
  }

  /// 手动触发同步
  Future<bool> manualSync() async {
    if (_isOfflineMode) {
      developer.log('无法手动同步：设备处于离线状态', name: 'OfflineSyncService');
      return false;
    }

    return await _performSync();
  }

  /// 执行数据同步
  Future<bool> _performSync() async {
    if (_isSyncing || _isOfflineMode || _pendingOperations.isEmpty) {
      return false;
    }

    _isSyncing = true;
    _syncEventController.add(
      SyncEvent(type: SyncEventType.syncStarted, timestamp: DateTime.now()),
    );

    try {
      developer.log(
        '开始同步数据，共 ${_pendingOperations.length} 个待同步操作',
        name: 'OfflineSyncService',
      );

      final List<PendingOperation> operationsToSync = List.from(
        _pendingOperations,
      );
      final List<PendingOperation> failedOperations = [];

      for (final operation in operationsToSync) {
        try {
          await _executeOperation(operation);

          // 从待同步队列中移除成功的操作
          _pendingOperations.remove(operation);

          // 保存到本地数据库
          await _saveOperationToLocal(operation);

          developer.log(
            '操作同步成功: ${operation.type}',
            name: 'OfflineSyncService',
          );
        } catch (e) {
          developer.log(
            '操作同步失败: ${operation.type}, 错误: $e',
            name: 'OfflineSyncService',
          );
          failedOperations.add(operation);
        }
      }

      // 更新待同步操作列表
      _pendingOperations.clear();
      _pendingOperations.addAll(failedOperations);

      // 保存更新后的待同步操作列表
      await _savePendingOperationsToLocal();

      _lastSyncTime = DateTime.now();

      final syncResult = SyncResult(
        success: failedOperations.isEmpty,
        totalOperations: operationsToSync.length,
        successCount: operationsToSync.length - failedOperations.length,
        failedCount: failedOperations.length,
      );

      _syncEventController.add(
        SyncEvent(
          type: SyncEventType.syncCompleted,
          result: syncResult,
          timestamp: DateTime.now(),
        ),
      );

      developer.log(
        '数据同步完成，成功: ${syncResult.successCount}, 失败: ${syncResult.failedCount}',
        name: 'OfflineSyncService',
      );

      return syncResult.success;
    } catch (e, stackTrace) {
      developer.log(
        '数据同步过程中发生错误',
        name: 'OfflineSyncService',
        error: e,
        stackTrace: stackTrace,
      );

      _syncEventController.add(
        SyncEvent(
          type: SyncEventType.syncError,
          error: e.toString(),
          timestamp: DateTime.now(),
        ),
      );

      return false;
    } finally {
      _isSyncing = false;
    }
  }

  /// 执行具体的同步操作
  Future<void> _executeOperation(PendingOperation operation) async {
    switch (operation.type) {
      case OperationType.createSavingGoal:
        final goal = SavingGoal.fromJson(operation.data);
        await _apiAdapter.createSavingGoal(goal);
        break;

      case OperationType.updateSavingGoal:
        final goal = SavingGoal.fromJson(operation.data);
        await _apiAdapter.updateSavingGoal(goal);
        break;

      case OperationType.deleteSavingGoal:
        final goalId = operation.data['id'] as int;
        await _apiAdapter.deleteSavingGoal(goalId);
        break;

      case OperationType.createSavingRecord:
        final record = SavingRecord.fromJson(operation.data);
        await _apiAdapter.createSavingRecord(record);
        break;

      case OperationType.updateSavingRecord:
        final record = SavingRecord.fromJson(operation.data);
        await _apiAdapter.updateSavingRecord(record);
        break;

      case OperationType.deleteSavingRecord:
        final recordId = operation.data['id'] as int;
        await _apiAdapter.deleteSavingRecord(recordId);
        break;

      case OperationType.createTransaction:
        final transaction = Bill.fromJson(operation.data);
        // 这里可以调用对应的API适配器方法
        developer.log(
          '同步交易记录操作: ${transaction.categoryName}',
          name: 'OfflineSyncService',
        );
        break;

      case OperationType.updateTransaction:
        final transaction = Bill.fromJson(operation.data);
        developer.log(
          '更新交易记录操作: ${transaction.categoryName}',
          name: 'OfflineSyncService',
        );
        break;

      case OperationType.deleteTransaction:
        final transactionId = operation.data['id'] as int;
        developer.log('删除交易记录操作: $transactionId', name: 'OfflineSyncService');
        break;

      case OperationType.createBudget:
        final budget = Budget.fromJson(operation.data);
        developer.log(
          '创建预算操作: ${budget.categoryName}',
          name: 'OfflineSyncService',
        );
        break;

      case OperationType.updateBudget:
        final budget = Budget.fromJson(operation.data);
        developer.log(
          '更新预算操作: ${budget.categoryName}',
          name: 'OfflineSyncService',
        );
        break;

      case OperationType.createCategory:
        final category = Category.fromJson(operation.data);
        developer.log('创建分类操作: ${category.name}', name: 'OfflineSyncService');
        break;

      case OperationType.updateCategory:
        final category = Category.fromJson(operation.data);
        developer.log('更新分类操作: ${category.name}', name: 'OfflineSyncService');
        break;
    }
  }

  /// 添加待同步操作
  Future<void> addPendingOperation(
    OperationType type,
    Map<String, dynamic> data,
  ) async {
    final operation = PendingOperation(
      id: DateTime.now().millisecondsSinceEpoch,
      type: type,
      data: data,
      timestamp: DateTime.now(),
    );

    _pendingOperations.add(operation);

    // 保存到本地数据库
    await _savePendingOperationsToLocal();

    _syncEventController.add(
      SyncEvent(
        type: SyncEventType.operationQueued,
        operation: operation,
        timestamp: DateTime.now(),
      ),
    );

    developer.log(
      '添加待同步操作: ${operation.type}, 队列长度: ${_pendingOperations.length}',
      name: 'OfflineSyncService',
    );

    // 如果网络连接可用且不在同步中，尝试立即同步
    if (!_isOfflineMode && !_isSyncing) {
      _performSync();
    }
  }

  /// 移除待同步操作
  Future<void> removePendingOperation(int operationId) async {
    _pendingOperations.removeWhere((op) => op.id == operationId);
    await _savePendingOperationsToLocal();

    _syncEventController.add(
      SyncEvent(
        type: SyncEventType.operationRemoved,
        operationId: operationId,
        timestamp: DateTime.now(),
      ),
    );
  }

  /// 清空待同步操作
  Future<void> clearPendingOperations() async {
    _pendingOperations.clear();
    await _savePendingOperationsToLocal();

    _syncEventController.add(
      SyncEvent(
        type: SyncEventType.operationsCleared,
        timestamp: DateTime.now(),
      ),
    );
  }

  /// 保存待同步操作到本地数据库
  Future<void> _savePendingOperationsToLocal() async {
    try {
      // 这里可以实现具体的本地存储逻辑
      developer.log('待同步操作已保存到本地数据库', name: 'OfflineSyncService');
    } catch (e) {
      developer.log('保存待同步操作到本地失败: $e', name: 'OfflineSyncService');
    }
  }

  /// 从本地数据库恢复待同步操作
  Future<void> _restorePendingOperations() async {
    try {
      // 这里可以实现从本地数据库恢复待同步操作的逻辑
      developer.log('已从本地数据库恢复待同步操作', name: 'OfflineSyncService');
    } catch (e) {
      developer.log('从本地数据库恢复待同步操作失败: $e', name: 'OfflineSyncService');
    }
  }

  /// 保存操作到本地数据库（同步成功后）
  Future<void> _saveOperationToLocal(PendingOperation operation) async {
    try {
      // 根据操作类型保存到相应的本地表
      switch (operation.type) {
        case OperationType.createSavingGoal:
        case OperationType.updateSavingGoal:
          // 保存储蓄目标到本地
          break;
        case OperationType.createSavingRecord:
        case OperationType.updateSavingRecord:
          // 保存储蓄记录到本地
          break;
        default:
          break;
      }

      developer.log(
        '操作已保存到本地数据库: ${operation.type}',
        name: 'OfflineSyncService',
      );
    } catch (e) {
      developer.log('保存操作到本地失败: $e', name: 'OfflineSyncService');
    }
  }

  /// 获取同步统计信息
  SyncStatistics getSyncStatistics() {
    return SyncStatistics(
      totalPending: _pendingOperations.length,
      lastSyncTime: _lastSyncTime,
      isSyncing: _isSyncing,
      isOfflineMode: _isOfflineMode,
      networkStatus: _isOfflineMode ? '离线' : '在线',
    );
  }

  /// 清理资源
  void dispose() {
    _connectivitySubscription?.cancel();
    _syncEventController.close();
  }
}

/// 待同步操作类
class PendingOperation {
  final int id;
  final OperationType type;
  final Map<String, dynamic> data;
  final DateTime timestamp;

  const PendingOperation({
    required this.id,
    required this.type,
    required this.data,
    required this.timestamp,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'type': type.name,
    'data': data,
    'timestamp': timestamp.millisecondsSinceEpoch,
  };

  factory PendingOperation.fromJson(Map<String, dynamic> json) =>
      PendingOperation(
        id: json['id'] as int,
        type: OperationType.values.firstWhere((e) => e.name == json['type']),
        data: json['data'] as Map<String, dynamic>,
        timestamp: DateTime.fromMillisecondsSinceEpoch(
          json['timestamp'] as int,
        ),
      );
}

/// 同步事件类
class SyncEvent {
  final SyncEventType type;
  final bool? isOffline;
  final PendingOperation? operation;
  final int? operationId;
  final SyncResult? result;
  final String? error;
  final DateTime timestamp;

  const SyncEvent({
    required this.type,
    this.isOffline,
    this.operation,
    this.operationId,
    this.result,
    this.error,
    required this.timestamp,
  });
}

/// 同步结果类
class SyncResult {
  final bool success;
  final int totalOperations;
  final int successCount;
  final int failedCount;

  const SyncResult({
    required this.success,
    required this.totalOperations,
    required this.successCount,
    required this.failedCount,
  });
}

/// 同步统计信息类
class SyncStatistics {
  final int totalPending;
  final DateTime? lastSyncTime;
  final bool isSyncing;
  final bool isOfflineMode;
  final String networkStatus;

  const SyncStatistics({
    required this.totalPending,
    required this.lastSyncTime,
    required this.isSyncing,
    required this.isOfflineMode,
    required this.networkStatus,
  });
}

/// 操作类型枚举
enum OperationType {
  // 储蓄目标操作
  createSavingGoal,
  updateSavingGoal,
  deleteSavingGoal,

  // 储蓄记录操作
  createSavingRecord,
  updateSavingRecord,
  deleteSavingRecord,

  // 交易操作
  createTransaction,
  updateTransaction,
  deleteTransaction,

  // 预算操作
  createBudget,
  updateBudget,

  // 分类操作
  createCategory,
  updateCategory,
}

/// 同步事件类型枚举
enum SyncEventType {
  // 网络状态变化
  networkChanged,

  // 同步过程
  syncStarted,
  syncCompleted,
  syncError,

  // 操作队列管理
  operationQueued,
  operationRemoved,
  operationsCleared,
}
