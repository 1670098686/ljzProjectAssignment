import 'dart:developer' as developer;
import '../services/enhanced_data_persistence_service.dart';

/// 查询缓存键
class CacheKey {
  static const String billsByDateRange = 'bills_by_date_range';
  static const String billsByCategory = 'bills_by_category';
  static const String billsByType = 'bills_by_type';
  static const String categoryStatistics = 'category_statistics';
  static const String monthlyStatistics = 'monthly_statistics';
  static const String yearlyStatistics = 'yearly_statistics';
  static const String budgetsByMonth = 'budgets_by_month';
  static const String savingGoals = 'saving_goals';
  static const String categories = 'categories';
  
  static String generateKey(String base, Map<String, dynamic>? params) {
    if (params == null || params.isEmpty) {
      return base;
    }
    
    final paramsString = params.entries
        .map((entry) => '${entry.key}:${entry.value}')
        .join('&');
    
    return '${base}_$paramsString';  // 修复：使用字符串插值替代字符串拼接
  }
}

/// 缓存条目
class CacheEntry<T> {
  final T data;
  final DateTime createdAt;
  final Duration ttl;
  
  CacheEntry({
    required this.data,
    required this.createdAt,
    required this.ttl,
  });
  
  bool get isExpired {
    return DateTime.now().difference(createdAt) > ttl;
  }
}

/// 查询缓存服务
/// 提供查询结果的缓存管理，支持TTL过期策略和自动清理
class QueryCacheService {
  static final QueryCacheService instance = QueryCacheService._internal();
  
  QueryCacheService._internal();
  
  factory QueryCacheService() {
    return instance;
  }
  
  final Map<String, CacheEntry> _cache = {};
  final EnhancedDataPersistenceService _persistenceService = EnhancedDataPersistenceService();
  
  /// 默认缓存时间（分钟）
  static const Duration defaultTTL = Duration(minutes: 5);
  
  /// 设置缓存
  Future<void> setCache<T>(
    String key, 
    T data, {
    Duration? ttl,
  }) async {
    try {
      _cache[key] = CacheEntry(
        data: data,
        createdAt: DateTime.now(),
        ttl: ttl ?? defaultTTL,
      );
      
      // 持久化重要缓存到本地存储
      if (_shouldPersistToStorage(key)) {
        await _persistenceService.saveData(
          key: key,
          data: data,
        );
      }
      
      developer.log('Cache set: $key', name: 'QueryCacheService');
    } catch (e) {
      developer.log('设置缓存失败: $key, $e', name: 'QueryCacheService');
    }
  }
  
  /// 获取缓存
  T? getCache<T>(String key) {
    try {
      final entry = _cache[key];
      
      if (entry == null) {
        return null;
      }
      
      if (entry.isExpired) {
        _removeCache(key);
        return null;
      }
      
      developer.log('缓存命中: $key', name: 'QueryCacheService');
      return entry.data as T;
    } catch (e) {
      developer.log('获取缓存失败: $key, $e', name: 'QueryCacheService');
      return null;
    }
  }
  
  /// 从持久化存储中恢复缓存
  Future<T?> getPersistedCache<T>(String key) async {
    try {
      // 先检查内存缓存
      T? cachedData = getCache<T>(key);
      if (cachedData != null) {
        return cachedData;
      }
      
      // 从持久化存储中恢复
      final data = await _persistenceService.loadData(
        key: key,
      );
      if (data != null && data is T) {
        // 重新放入内存缓存
        _cache[key] = CacheEntry(
          data: data,
          createdAt: DateTime.now(),
          ttl: defaultTTL,
        );
        developer.log('从持久化存储恢复缓存: $key', name: 'QueryCacheService');
        return data;
      }
      
      return null;
    } catch (e) {
      developer.log('从持久化存储恢复缓存失败: $key, $e', name: 'QueryCacheService');
      return null;
    }
  }
  
  /// 删除缓存
  void _removeCache(String key) {
    try {
      _cache.remove(key);
      developer.log('缓存已删除: $key', name: 'QueryCacheService');
    } catch (e) {
      developer.log('删除缓存失败: $key, $e', name: 'QueryCacheService');
    }
  }
  
  /// 清除过期缓存
  void cleanExpiredCache() {
    try {
      final expiredKeys = <String>[];
      
      _cache.forEach((key, entry) {
        if (DateTime.now().difference(entry.createdAt) > entry.ttl) {
          expiredKeys.add(key);
        }
      });
      
      for (final key in expiredKeys) {
        _cache.remove(key);
      }
      
      if (expiredKeys.isNotEmpty) {
        developer.log('已清理 ${expiredKeys.length} 个过期缓存', name: 'QueryCacheService');
      }
    } catch (e) {
      developer.log('清理过期缓存失败: $e', name: 'QueryCacheService');
    }
  }
  
  /// 清除所有缓存
  void clearAllCache() {
    try {
      _cache.clear();
      developer.log('已清除所有缓存', name: 'QueryCacheService');
    } catch (e) {
      developer.log('清除所有缓存失败: $e', name: 'QueryCacheService');
    }
  }
  
  /// 清除指定模式的缓存
  Future<void> clearCacheByPattern(String pattern) async {
    try {
      final keysToRemove = <String>[];
      
      _cache.forEach((key, entry) {
        if (key.contains(pattern)) {
          keysToRemove.add(key);
        }
      });
      
      for (final key in keysToRemove) {
        _cache.remove(key);
        // 同时从持久化存储中删除
        await _persistenceService.deleteData(key);
      }
      
      if (keysToRemove.isNotEmpty) {
        developer.log('已清除 ${keysToRemove.length} 个匹配模式的缓存: $pattern', name: 'QueryCacheService');
      }
    } catch (e) {
      developer.log('清除缓存失败: $pattern, $e', name: 'QueryCacheService');
    }
  }
  
  /// 获取缓存统计信息
  CacheStatistics getCacheStatistics() {
    final totalEntries = _cache.length;
    final expiredEntries = _cache.values.where((entry) => entry.isExpired).length;
    final memoryUsage = _estimateMemoryUsage();
    final cacheHitRate = _calculateCacheHitRate();
    
    return CacheStatistics(
      totalEntries: totalEntries,
      expiredEntries: expiredEntries,
      activeEntries: totalEntries - expiredEntries,
      memoryUsage: memoryUsage,
      cacheHitRate: cacheHitRate,
    );
  }
  
  /// 判断是否应该持久化到存储
  bool _shouldPersistToStorage(String key) {
    // 重要的统计数据和长期查询结果应该持久化
    return key.startsWith(CacheKey.categoryStatistics) ||
           key.startsWith(CacheKey.monthlyStatistics) ||
           key.startsWith(CacheKey.yearlyStatistics);
  }
  
  /// 估算内存使用量（KB）
  int _estimateMemoryUsage() {
    try {
      // 简单估算：每个字符串约50字节，每个对象约200字节
      final stringSize = _cache.keys.fold<int>(0, (sum, key) => sum + key.length);
      final dataSize = _cache.length * 200; // 估算
      return (stringSize + dataSize) ~/ 1024; // 转换为KB
    } catch (e) {
      return 0;
    }
  }
  
  /// 计算缓存命中率（模拟值）
  double _calculateCacheHitRate() {
    // 这里可以集成真实的命中率统计
    // 目前返回模拟值
    return 0.75; // 75%
  }
}

/// 缓存统计信息
class CacheStatistics {
  final int totalEntries;
  final int expiredEntries;
  final int activeEntries;
  final int memoryUsage; // KB
  final double cacheHitRate;
  
  CacheStatistics({
    required this.totalEntries,
    required this.expiredEntries,
    required this.activeEntries,
    required this.memoryUsage,
    required this.cacheHitRate,
  });
  
  @override
  String toString() {
    return 'CacheStatistics(total: $totalEntries, active: $activeEntries, '
           'expired: $expiredEntries, memory: ${memoryUsage}KB, hitRate: ${(cacheHitRate * 100).toStringAsFixed(1)}%)';
  }
}