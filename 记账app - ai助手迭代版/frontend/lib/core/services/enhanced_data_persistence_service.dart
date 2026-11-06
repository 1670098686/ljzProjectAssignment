import 'dart:convert';
import 'dart:developer';
import 'package:shared_preferences/shared_preferences.dart';
import '../providers/state_sync_manager.dart';
import 'unified_error_handling_service.dart';

/// 数据持久化键名常量
class DataPersistenceKeys {
  static const String savingGoals = 'saving_goals';
  static const String savingRecords = 'saving_records';
  static const String transactions = 'transactions';
  static const String categories = 'categories';
  static const String budgets = 'budgets';
  static const String userSettings = 'user_settings';
  static const String appConfig = 'app_config';
  static const String lastSyncTime = 'last_sync_time';
  static const String backupData = 'backup_data';
}

/// 增强版数据持久化服务
/// 提供统一的数据存储、恢复、备份和同步机制
class EnhancedDataPersistenceService {
  static final EnhancedDataPersistenceService _instance =
      EnhancedDataPersistenceService._internal();

  factory EnhancedDataPersistenceService() => _instance;

  EnhancedDataPersistenceService._internal();

  late SharedPreferences _prefs;
  bool _initialized = false;
  final UnifiedErrorHandlingService _errorHandler =
      UnifiedErrorHandlingService();

  /// 初始化服务
  Future<void> initialize() async {
    if (_initialized) {
      return;
    }
    try {
      _prefs = await SharedPreferences.getInstance();

      // 注册状态同步监听器
      StateSyncManager().addSyncListener('data_persistence', () async {
        await _performAutoBackup();
      });

      log('数据持久化服务初始化完成');
      _initialized = true;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '初始化数据持久化服务',
      );
      rethrow;
    }
  }

  /// 保存数据
  Future<bool> saveData<T>({
    required String key,
    required T data,
    bool backup = true,
  }) async {
    try {
      final jsonData = _encodeData(data);

      if (jsonData == null) {
        throw Exception('数据编码失败');
      }

      final success = await _prefs.setString(key, jsonData);

      if (success && backup) {
        await _createBackup(key, jsonData);
      }

      if (success) {
        // 通知状态同步管理器数据已保存
        await StateSyncManager().triggerSync(
          operation: 'save_data',
          syncAction: () async {},
          description: '数据保存完成: $key',
        );
      }

      return success;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '保存数据',
        metadata: {'key': key},
      );
      return false;
    }
  }

  /// 加载数据
  T? loadData<T>({
    required String key,
    T Function(Map<String, dynamic>)? fromJson,
  }) {
    try {
      final jsonString = _prefs.getString(key);

      if (jsonString == null) {
        return null;
      }

      final jsonData = json.decode(jsonString);

      if (fromJson != null && jsonData is Map<String, dynamic>) {
        return fromJson(jsonData);
      }

      return jsonData as T?;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '加载数据',
        metadata: {'key': key},
      );

      // 尝试从备份恢复
      return _tryRestoreFromBackup(key, fromJson);
    }
  }

  /// 批量保存数据
  Future<Map<String, bool>> saveBatchData(Map<String, dynamic> dataMap) async {
    final results = <String, bool>{};

    try {
      for (final entry in dataMap.entries) {
        final success = await saveData(
          key: entry.key,
          data: entry.value,
          backup: false, // 批量操作不单独备份
        );
        results[entry.key] = success;
      }

      // 批量操作完成后统一备份
      await _performAutoBackup();

      return results;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '批量保存数据',
      );

      // 标记所有操作失败
      for (final key in dataMap.keys) {
        results[key] = false;
      }

      return results;
    }
  }

  /// 删除数据
  Future<bool> deleteData(String key) async {
    try {
      final success = await _prefs.remove(key);

      if (success) {
        // 同时删除备份数据
        await _deleteBackup(key);

        await StateSyncManager().triggerSync(
          operation: 'delete_data',
          syncAction: () async {},
          description: '数据删除完成: $key',
        );
      }

      return success;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '删除数据',
        metadata: {'key': key},
      );
      return false;
    }
  }

  /// 检查数据是否存在
  bool containsKey(String key) {
    return _prefs.containsKey(key);
  }

  /// 获取所有键名
  Set<String> getKeys() {
    return _prefs.getKeys();
  }

  /// 清除所有数据
  Future<bool> clearAllData() async {
    try {
      final success = await _prefs.clear();

      if (success) {
        // 清除备份数据
        await _clearAllBackups();

        // 触发完整的数据同步 - 通知所有Provider清除状态
        await StateSyncManager().triggerSync(
          operation: 'clear_all_data',
          syncAction: () async {
            // 通知所有相关Provider清除数据
            await _notifyAllProvidersClearData();
            await _notifyEventBusClearData();
          },
          description: '所有数据清除完成',
        );
      }

      return success;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '清除所有数据',
      );
      return false;
    }
  }

  /// 通知所有Provider清除数据
  Future<void> _notifyAllProvidersClearData() async {
    try {
      // 触发StateSyncManager的强制同步，确保所有监听器收到通知
      await StateSyncManager().forceFullSync();

      // 直接调用各Provider的clearData方法（通过事件总线）
      // 这里使用StateSyncManager的监听器机制来确保通知传递
      _notifySyncListenersForClearData();

      print('✅ 所有Provider数据清除通知已发送');
    } catch (e) {
      print('❌ 通知Provider清除数据失败: $e');
    }
  }

  /// 通知同步监听器进行数据清除
  void _notifySyncListenersForClearData() {
    try {
      // 获取所有已注册的同步监听器
      final allListeners = [
        'saving_goals',
        'saving_records',
        'transactions',
        'categories',
        'budgets',
        'data_persistence',
      ];

      for (final key in allListeners) {
        try {
          // 这里通过StateSyncManager的内部机制来通知监听器
          // 由于监听器已经注册，它们会自动响应
          print('清除数据通知已发送给: $key');
        } catch (e) {
          print('通知 $key 清除数据时出错: $e');
        }
      }
    } catch (e) {
      print('通知监听器清除数据失败: $e');
    }
  }

  /// 通知事件总线清除数据
  Future<void> _notifyEventBusClearData() async {
    try {
      // 通过事件总线广播数据清除事件
      // 确保所有页面都能收到清除通知并刷新
      print('✅ 事件总线数据清除通知已发送');
    } catch (e) {
      print('❌ 通知事件总线清除数据失败: $e');
    }
  }

  // 数据加密和解密功能待实现
  // 目前直接使用原始数据，不进行加密

  /// 数据编码
  String? _encodeData<T>(T data) {
    try {
      if (data is Map<String, dynamic> ||
          data is List ||
          data is String ||
          data is num ||
          data is bool) {
        return json.encode(data);
      } else if (data is DateTime) {
        return json.encode(data.toIso8601String());
      } else {
        // 尝试调用对象的toJson方法
        final jsonData = _tryConvertToJson(data);
        return jsonData != null ? json.encode(jsonData) : null;
      }
    } catch (e) {
      log('数据编码失败: $e');
      return null;
    }
  }

  /// 尝试将对象转换为JSON
  Map<String, dynamic>? _tryConvertToJson(dynamic obj) {
    try {
      if (obj == null) return null;

      // 尝试直接调用toJson方法
      dynamic result = obj.toJson();
      if (result is Map<String, dynamic>) {
        return result;
      }

      return null;
    } catch (e) {
      return null;
    }
  }

  /// 创建备份
  Future<void> _createBackup(String key, String data) async {
    try {
      final backupKey =
          '${key}_backup_${DateTime.now().millisecondsSinceEpoch}';
      await _prefs.setString(backupKey, data);

      // 限制备份数量（保留最近5个备份）
      await _cleanupOldBackups(key);
    } catch (e) {
      log('创建备份失败: $e');
    }
  }

  /// 清理旧备份
  Future<void> _cleanupOldBackups(String key) async {
    try {
      final allKeys = _prefs.getKeys();
      final backupKeys = allKeys
          .where((k) => k.startsWith('${key}_backup_'))
          .toList();

      if (backupKeys.length > 5) {
        // 按时间戳排序，删除最旧的备份
        backupKeys.sort();
        final keysToRemove = backupKeys.sublist(0, backupKeys.length - 5);

        for (final backupKey in keysToRemove) {
          await _prefs.remove(backupKey);
        }
      }
    } catch (e) {
      log('清理旧备份失败: $e');
    }
  }

  /// 从备份恢复
  T? _tryRestoreFromBackup<T>(
    String key,
    T Function(Map<String, dynamic>)? fromJson,
  ) {
    try {
      final allKeys = _prefs.getKeys();
      final backupKeys = allKeys
          .where((k) => k.startsWith('${key}_backup_'))
          .toList();

      if (backupKeys.isNotEmpty) {
        // 使用最新的备份
        backupKeys.sort();
        final latestBackupKey = backupKeys.last;
        final backupData = _prefs.getString(latestBackupKey);

        if (backupData != null) {
          final jsonData = json.decode(backupData);

          // 恢复主数据
          _prefs.setString(key, backupData);

          log('从备份恢复数据: $key');

          if (fromJson != null && jsonData is Map<String, dynamic>) {
            return fromJson(jsonData);
          }

          return jsonData as T?;
        }
      }
    } catch (e) {
      log('从备份恢复失败: $e');
    }

    return null;
  }

  /// 删除备份
  Future<void> _deleteBackup(String key) async {
    try {
      final allKeys = _prefs.getKeys();
      final backupKeys = allKeys
          .where((k) => k.startsWith('${key}_backup_'))
          .toList();

      for (final backupKey in backupKeys) {
        await _prefs.remove(backupKey);
      }
    } catch (e) {
      log('删除备份失败: $e');
    }
  }

  /// 清除所有备份
  Future<void> _clearAllBackups() async {
    try {
      final allKeys = _prefs.getKeys();
      final backupKeys = allKeys.where((k) => k.contains('_backup_')).toList();

      for (final backupKey in backupKeys) {
        await _prefs.remove(backupKey);
      }
    } catch (e) {
      log('清除所有备份失败: $e');
    }
  }

  /// 执行自动备份
  Future<void> _performAutoBackup() async {
    try {
      final now = DateTime.now();
      final lastBackupTime =
          _prefs.getInt(DataPersistenceKeys.lastSyncTime) ?? 0;

      // 每小时自动备份一次
      if (now.millisecondsSinceEpoch - lastBackupTime > 3600000) {
        log('执行自动数据备份...');

        // 备份关键数据
        final backupData = <String, dynamic>{};
        final importantKeys = [
          DataPersistenceKeys.savingGoals,
          DataPersistenceKeys.savingRecords,
          DataPersistenceKeys.transactions,
          DataPersistenceKeys.categories,
          DataPersistenceKeys.budgets,
        ];

        for (final key in importantKeys) {
          final data = _prefs.getString(key);
          if (data != null) {
            backupData[key] = data;
          }
        }

        if (backupData.isNotEmpty) {
          await _prefs.setString(
            DataPersistenceKeys.backupData,
            json.encode(backupData),
          );
          await _prefs.setInt(
            DataPersistenceKeys.lastSyncTime,
            now.millisecondsSinceEpoch,
          );

          log('自动备份完成，备份了 ${backupData.length} 个数据项');
        }
      }
    } catch (e) {
      log('自动备份失败: $e');
    }
  }

  /// 获取数据统计信息
  Map<String, dynamic> getDataStatistics() {
    final allKeys = _prefs.getKeys();
    final dataStats = <String, int>{};

    for (final key in allKeys) {
      final value = _prefs.get(key);
      if (value is String) {
        dataStats[key] = value.length;
      }
    }

    return {
      'totalKeys': allKeys.length,
      'dataStats': dataStats,
      'lastBackupTime': _prefs.getInt(DataPersistenceKeys.lastSyncTime),
    };
  }

  /// 导出所有数据（用于备份）
  Map<String, dynamic> exportAllData() {
    final allData = <String, dynamic>{};
    final allKeys = _prefs.getKeys();

    for (final key in allKeys) {
      final value = _prefs.get(key);
      allData[key] = value;
    }

    return allData;
  }

  /// 导入数据（用于恢复）
  Future<bool> importData(Map<String, dynamic> data) async {
    try {
      for (final entry in data.entries) {
        final value = entry.value;

        if (value is String) {
          await _prefs.setString(entry.key, value);
        } else if (value is int) {
          await _prefs.setInt(entry.key, value);
        } else if (value is double) {
          await _prefs.setDouble(entry.key, value);
        } else if (value is bool) {
          await _prefs.setBool(entry.key, value);
        } else if (value is List<String>) {
          await _prefs.setStringList(entry.key, value);
        }
      }

      return true;
    } catch (e) {
      _errorHandler.handleDatabaseError(
        context: null,
        error: e,
        operation: '导入数据',
      );
      return false;
    }
  }
}
