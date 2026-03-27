import 'package:flutter/foundation.dart';

/// 状态同步管理器
/// 负责协调不同Provider之间的数据同步，确保状态一致性
class StateSyncManager with ChangeNotifier {
  static final StateSyncManager _instance = StateSyncManager._internal();
  
  factory StateSyncManager() => _instance;
  
  StateSyncManager._internal();

  // 同步状态跟踪
  bool _isSyncing = false;
  String? _lastSyncError;
  DateTime? _lastSyncTime;

  // 同步队列
  final List<SyncOperation> _syncQueue = [];
  
  // 同步监听器
  final Map<String, VoidCallback> _syncListeners = {};

  // Getters
  bool get isSyncing => _isSyncing;
  String? get lastSyncError => _lastSyncError;
  DateTime? get lastSyncTime => _lastSyncTime;
  bool get hasPendingSync => _syncQueue.isNotEmpty;

  /// 注册同步监听器
  void addSyncListener(String key, VoidCallback listener) {
    _syncListeners[key] = listener;
  }

  /// 移除同步监听器
  void removeSyncListener(String key) {
    _syncListeners.remove(key);
  }

  /// 触发数据同步
  Future<void> triggerSync({
    required String operation,
    required Future<void> Function() syncAction,
    String? description,
  }) async {
    if (_isSyncing) {
      // 如果正在同步，将操作加入队列
      _syncQueue.add(SyncOperation(
        operation: operation,
        syncAction: syncAction,
        description: description,
      ));
      return;
    }

    _isSyncing = true;
    _lastSyncError = null;
    notifyListeners();

    try {
      await syncAction();
      _lastSyncTime = DateTime.now();
      _notifySyncListeners();
    } catch (e) {
      _lastSyncError = e.toString();
      debugPrint('同步失败 ($operation): $e');
    } finally {
      _isSyncing = false;
      notifyListeners();
      
      // 处理队列中的下一个同步操作
      if (_syncQueue.isNotEmpty) {
        final nextOp = _syncQueue.removeAt(0);
        await triggerSync(
          operation: nextOp.operation,
          syncAction: nextOp.syncAction,
          description: nextOp.description,
        );
      }
    }
  }

  /// 批量同步操作
  Future<void> batchSync(List<SyncOperation> operations) async {
    for (final operation in operations) {
      await triggerSync(
        operation: operation.operation,
        syncAction: operation.syncAction,
        description: operation.description,
      );
    }
  }

  /// 强制同步所有数据
  Future<void> forceFullSync() async {
    final operations = [
      SyncOperation(
        operation: 'sync_saving_goals',
        syncAction: () async {
          // 触发储蓄目标同步
          _notifySyncListeners(operation: 'saving_goals');
        },
        description: '同步储蓄目标数据',
      ),
      SyncOperation(
        operation: 'sync_saving_records',
        syncAction: () async {
          // 触发储蓄记录同步
          _notifySyncListeners(operation: 'saving_records');
        },
        description: '同步储蓄记录数据',
      ),
      SyncOperation(
        operation: 'sync_transactions',
        syncAction: () async {
          // 触发交易记录同步
          _notifySyncListeners(operation: 'transactions');
        },
        description: '同步交易记录数据',
      ),
    ];

    await batchSync(operations);
  }

  /// 清除同步错误
  void clearError() {
    _lastSyncError = null;
    notifyListeners();
  }

  /// 清除同步队列
  void clearQueue() {
    _syncQueue.clear();
    notifyListeners();
  }

  /// 清除所有数据通知（专门处理数据清除场景）
  Future<void> triggerClearDataSync() async {
    await triggerSync(
      operation: 'clear_all_data',
      syncAction: () async {
        // 触发所有Provider的清除数据操作
        _notifySyncListeners(operation: 'clear_data');
        debugPrint('数据清除同步已触发');
      },
      description: '清除所有数据并同步状态',
    );
  }

  /// 通知所有同步监听器
  void _notifySyncListeners({String? operation}) {
    for (final listener in _syncListeners.values) {
      try {
        listener();
      } catch (e) {
        debugPrint('同步监听器通知失败: $e');
      }
    }
  }
}

/// 同步操作定义
class SyncOperation {
  final String operation;
  final Future<void> Function() syncAction;
  final String? description;

  SyncOperation({
    required this.operation,
    required this.syncAction,
    this.description,
  });
}

/// 同步状态枚举
enum SyncState {
  idle,
  syncing,
  success,
  error,
}

/// 同步事件类型
enum SyncEventType {
  dataChanged,
  networkChanged,
  userAction,
  autoSync,
}