import 'dart:developer' as developer;
import 'dart:math' as math;
import 'package:sqflite/sqflite.dart';
import '../database/database_service.dart';

/// 数据库查询优化器
/// 负责查询性能优化、索引管理、查询缓存和执行统计
class DatabaseQueryOptimizer {
  static final DatabaseQueryOptimizer _instance = DatabaseQueryOptimizer._internal();
  factory DatabaseQueryOptimizer() => _instance;
  DatabaseQueryOptimizer._internal();

  final DatabaseService _databaseService = DatabaseService.instance;
  final Map<String, QueryCacheEntry> _queryCache = {};
  final Map<String, QueryExecutionStats> _executionStats = {};
  static const int _maxCacheSize = 100;
  static const Duration _cacheExpiry = Duration(minutes: 10);

  /// 优化的账单查询 - 支持复杂条件
  Future<List<Map<String, dynamic>>> queryBillsOptimized({
    Database? db,
    int? type,
    String? category,
    DateTime? startDate,
    DateTime? endDate,
    double? minAmount,
    double? maxAmount,
    String? keyword,
    String? orderBy,
    int? limit,
    int? offset,
  }) async {
    final database = db ?? await _databaseService.database;
    final queryKey = _buildQueryKey('bills', {
      'type': type,
      'category': category,
      'startDate': startDate?.toIso8601String(),
      'endDate': endDate?.toIso8601String(),
      'minAmount': minAmount,
      'maxAmount': maxAmount,
      'keyword': keyword,
      'orderBy': orderBy,
      'limit': limit,
      'offset': offset,
    });

    // 检查缓存
    final cachedResult = _getCachedResult(queryKey);
    if (cachedResult != null) {
      developer.log('使用缓存查询结果: $queryKey', name: 'DatabaseQueryOptimizer');
      return cachedResult;
    }

    try {
      final startTime = DateTime.now();
      
      // 构建优化的查询条件
      final conditions = <String>[];
      final args = <dynamic>[];

      if (type != null) {
        conditions.add('type = ?');
        args.add(type);
      }

      if (category != null) {
        conditions.add('categoryName LIKE ?');
        args.add('%$category%');
      }

      if (startDate != null) {
        conditions.add('transactionDate >= ?');
        args.add(_formatDate(startDate));
      }

      if (endDate != null) {
        conditions.add('transactionDate <= ?');
        args.add(_formatDate(endDate));
      }

      if (minAmount != null) {
        conditions.add('amount >= ?');
        args.add(minAmount);
      }

      if (maxAmount != null) {
        conditions.add('amount <= ?');
        args.add(maxAmount);
      }

      if (keyword != null && keyword.isNotEmpty) {
        conditions.add('(categoryName LIKE ? OR remark LIKE ?)');
        args.add('%$keyword%');
        args.add('%$keyword%');
      }

      // 构建查询语句
      final whereClause = conditions.isEmpty ? '1=1' : conditions.join(' AND ');
      final orderClause = orderBy ?? 'transactionDate DESC';
      final limitClause = limit != null ? ' LIMIT $limit' : '';
      final offsetClause = offset != null ? ' OFFSET $offset' : '';

      final query = '''
        SELECT * FROM bills 
        WHERE $whereClause 
        ORDER BY $orderClause$limitClause$offsetClause
      ''';

      final results = await database.rawQuery(query, args);
      
      // 记录执行统计
      final endTime = DateTime.now();
      _recordExecutionStats(queryKey, endTime.difference(startTime), results.length);
      
      // 缓存结果
      _cacheResult(queryKey, results);
      
      developer.log(
        '优化查询完成: ${results.length}条记录, 耗时${endTime.difference(startTime).inMilliseconds}ms',
        name: 'DatabaseQueryOptimizer',
      );

      return results;
    } catch (e, stackTrace) {
      developer.log(
        '优化查询失败',
        name: 'DatabaseQueryOptimizer',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 优化的分类查询 - 使用索引优化
  Future<List<Map<String, dynamic>>> queryCategoriesOptimized({
    Database? db,
    int? type,
    String? searchKeyword,
    String? orderBy,
    int? limit,
  }) async {
    final database = db ?? await _databaseService.database;
    final queryKey = _buildQueryKey('categories', {
      'type': type,
      'searchKeyword': searchKeyword,
      'orderBy': orderBy,
      'limit': limit,
    });

    // 检查缓存
    final cachedResult = _getCachedResult(queryKey);
    if (cachedResult != null) {
      return cachedResult;
    }

    try {
      final startTime = DateTime.now();
      
      final conditions = <String>[];
      final args = <dynamic>[];

      if (type != null) {
        conditions.add('type = ?');
        args.add(type);
      }

      if (searchKeyword != null && searchKeyword.isNotEmpty) {
        conditions.add('name LIKE ?');
        args.add('%$searchKeyword%');
      }

      final whereClause = conditions.isEmpty ? '1=1' : conditions.join(' AND ');
      final orderClause = orderBy ?? 'name ASC';
      final limitClause = limit != null ? ' LIMIT $limit' : '';

      // 使用索引优化的查询
      final query = '''
        SELECT * FROM categories 
        WHERE $whereClause 
        ORDER BY $orderClause$limitClause
      ''';

      final results = await database.rawQuery(query, args);
      
      final endTime = DateTime.now();
      _recordExecutionStats(queryKey, endTime.difference(startTime), results.length);
      _cacheResult(queryKey, results);
      
      return results;
    } catch (e, stackTrace) {
      developer.log('分类查询失败', name: 'DatabaseQueryOptimizer', error: e, stackTrace: stackTrace);
      rethrow;
    }
  }

  /// 优化的统计查询 - 直接在数据库计算
  Future<Map<String, dynamic>> queryStatisticsOptimized({
    Database? db,
    required DateTime startDate,
    required DateTime endDate,
    int? type,
  }) async {
    final database = db ?? await _databaseService.database;
    final queryKey = _buildQueryKey('statistics', {
      'startDate': _formatDate(startDate),
      'endDate': _formatDate(endDate),
      'type': type,
    });

    try {
      final startTime = DateTime.now();
      
      // 使用数据库聚合函数进行优化计算
      final conditions = <String>['transactionDate >= ?', 'transactionDate <= ?'];
      final args = [_formatDate(startDate), _formatDate(endDate)];

      if (type != null) {
        conditions.add('type = ?');
        args.add(type);
      }

      final whereClause = conditions.join(' AND ');

      // 分类统计查询
      final categoryQuery = '''
        SELECT categoryName, SUM(amount) as total, COUNT(*) as count
        FROM bills 
        WHERE $whereClause 
        GROUP BY categoryName 
        ORDER BY total DESC
      ''';

      // 月度统计查询
      final monthlyQuery = '''
        SELECT 
          strftime('%Y-%m', transactionDate) as month,
          type,
          SUM(amount) as total,
          COUNT(*) as count
        FROM bills 
        WHERE $whereClause 
        GROUP BY strftime('%Y-%m', transactionDate), type
        ORDER BY month DESC
      ''';

      // 总额统计查询
      final totalQuery = '''
        SELECT 
          SUM(amount) as totalAmount,
          COUNT(*) as totalCount,
          AVG(amount) as avgAmount,
          MIN(amount) as minAmount,
          MAX(amount) as maxAmount
        FROM bills 
        WHERE $whereClause
      ''';

      final categoryResults = await database.rawQuery(categoryQuery, args);
      final monthlyResults = await database.rawQuery(monthlyQuery, args);
      final totalResults = await database.rawQuery(totalQuery, args);

      final statistics = {
        'categoryStats': categoryResults,
        'monthlyStats': monthlyResults,
        'totalStats': totalResults.isNotEmpty ? totalResults.first : {},
        'period': {
          'startDate': _formatDate(startDate),
          'endDate': _formatDate(endDate),
        },
        'queryTime': DateTime.now().difference(startTime).inMilliseconds,
      };

      final endTime = DateTime.now();
      _recordExecutionStats(queryKey, endTime.difference(startTime), 
        categoryResults.length + monthlyResults.length + totalResults.length);
      
      return statistics;
    } catch (e, stackTrace) {
      developer.log('统计查询失败', name: 'DatabaseQueryOptimizer', error: e, stackTrace: stackTrace);
      rethrow;
    }
  }

  /// 添加复合索引优化
  Future<void> addCompoundIndexes(Database db) async {
    try {
      developer.log('开始添加复合索引优化...', name: 'DatabaseQueryOptimizer');
      
      // 账单表复合索引
      await _addIndexIfNotExists(db, 'idx_bills_type_date', 
        'CREATE INDEX idx_bills_type_date ON bills(type, transactionDate)');
      await _addIndexIfNotExists(db, 'idx_bills_category_date', 
        'CREATE INDEX idx_bills_category_date ON bills(categoryName, transactionDate)');
      await _addIndexIfNotExists(db, 'idx_bills_type_category', 
        'CREATE INDEX idx_bills_type_category ON bills(type, categoryName)');
      await _addIndexIfNotExists(db, 'idx_bills_amount_range', 
        'CREATE INDEX idx_bills_amount_range ON bills(amount, type)');
      
      // 分类表复合索引
      await _addIndexIfNotExists(db, 'idx_categories_type_name', 
        'CREATE INDEX idx_categories_type_name ON categories(type, name)');
      
      // 预算表复合索引
      await _addIndexIfNotExists(db, 'idx_budgets_category_month', 
        'CREATE INDEX idx_budgets_category_month ON budgets(categoryName, year, month)');
      
      developer.log('复合索引添加完成', name: 'DatabaseQueryOptimizer');
    } catch (e, stackTrace) {
      developer.log('添加复合索引失败', name: 'DatabaseQueryOptimizer', error: e, stackTrace: stackTrace);
    }
  }

  /// 获取查询性能分析
  Future<Map<String, dynamic>> getQueryPerformanceAnalysis() async {
    final analysis = <String, dynamic>{};
    
    // 分析缓存命中率
    final cacheHitRate = _calculateCacheHitRate();
    analysis['cacheHitRate'] = cacheHitRate;
    
    // 分析执行统计
    final avgExecutionTime = _calculateAverageExecutionTime();
    analysis['averageExecutionTime'] = avgExecutionTime;
    
    // 最慢的查询
    final slowestQueries = _getSlowestQueries(5);
    analysis['slowestQueries'] = slowestQueries;
    
    // 最频繁的查询
    final frequentQueries = _getFrequentQueries(5);
    analysis['frequentQueries'] = frequentQueries;
    
    // 缓存使用情况
    analysis['cacheUsage'] = {
      'size': _queryCache.length,
      'maxSize': _maxCacheSize,
      'utilization': _queryCache.length / _maxCacheSize,
    };

    return analysis;
  }

  /// 清理过期缓存
  void clearExpiredCache() {
    final now = DateTime.now();
    final expiredKeys = <String>[];
    
    _queryCache.forEach((key, entry) {
      if (now.difference(entry.timestamp) > _cacheExpiry) {
        expiredKeys.add(key);
      }
    });
    
    for (final key in expiredKeys) {
      _queryCache.remove(key);
    }
    
    developer.log('清理了${expiredKeys.length}个过期缓存项', name: 'DatabaseQueryOptimizer');
  }

  /// 清空所有缓存
  void clearAllCache() {
    _queryCache.clear();
    developer.log('清空了所有查询缓存', name: 'DatabaseQueryOptimizer');
  }

  /// 关闭优化器（清理资源）
  void dispose() {
    clearAllCache();
    _executionStats.clear();
  }

  // 私有辅助方法
  String _buildQueryKey(String table, Map<String, dynamic> params) {
    final sortedParams = params.entries
        .where((e) => e.value != null)
        .map((e) => '${e.key}:${e.value}')
        .toList()
      ..sort();
    return '$table:${sortedParams.join('|')}';
  }

  List<Map<String, dynamic>>? _getCachedResult(String queryKey) {
    final entry = _queryCache[queryKey];
    if (entry == null) return null;
    
    if (DateTime.now().difference(entry.timestamp) > _cacheExpiry) {
      _queryCache.remove(queryKey);
      return null;
    }
    
    entry.lastAccessed = DateTime.now();
    return entry.results;
  }

  void _cacheResult(String queryKey, List<Map<String, dynamic>> results) {
    if (_queryCache.length >= _maxCacheSize) {
      // 移除最久未访问的缓存项
      var oldestKey = '';
      var oldestTime = DateTime.now();
      
      _queryCache.forEach((key, entry) {
        if (entry.lastAccessed.isBefore(oldestTime)) {
          oldestKey = key;
          oldestTime = entry.lastAccessed;
        }
      });
      
      if (oldestKey.isNotEmpty) {
        _queryCache.remove(oldestKey);
      }
    }
    
    _queryCache[queryKey] = QueryCacheEntry(
      results: results,
      timestamp: DateTime.now(),
      lastAccessed: DateTime.now(),
    );
  }

  void _recordExecutionStats(String queryKey, Duration duration, int resultCount) {
    final stats = _executionStats[queryKey] ?? QueryExecutionStats();
    stats.recordExecution(duration, resultCount);
    _executionStats[queryKey] = stats;
  }

  double _calculateCacheHitRate() {
    if (_queryCache.isEmpty) return 0.0;
    
    final totalAccesses = _queryCache.values
        .fold<int>(0, (sum, entry) => sum + entry.accessCount);
    
    return totalAccesses > 0 ? (totalAccesses - _queryCache.length) / totalAccesses : 0.0;
  }

  Duration _calculateAverageExecutionTime() {
    if (_executionStats.isEmpty) return Duration.zero;
    
    final totalTime = _executionStats.values
        .fold<int>(0, (sum, stats) => sum + stats.totalExecutionTime.inMilliseconds);
    
    return Duration(milliseconds: (totalTime / _executionStats.length).round());
  }

  List<Map<String, dynamic>> _getSlowestQueries(int limit) {
    final sortedStats = _executionStats.entries.toList()
      ..sort((a, b) => b.value.averageExecutionTime.inMilliseconds
          .compareTo(a.value.averageExecutionTime.inMilliseconds));
    
    return sortedStats.take(limit).map((entry) => {
      'query': entry.key,
      'averageTime': entry.value.averageExecutionTime.inMilliseconds,
      'totalExecutions': entry.value.executionCount,
    }).toList();
  }

  List<Map<String, dynamic>> _getFrequentQueries(int limit) {
    final sortedStats = _executionStats.entries.toList()
      ..sort((a, b) => b.value.executionCount.compareTo(a.value.executionCount));
    
    return sortedStats.take(limit).map((entry) => {
      'query': entry.key,
      'executionCount': entry.value.executionCount,
      'totalTime': entry.value.totalExecutionTime.inMilliseconds,
    }).toList();
  }

  String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  Future<void> _addIndexIfNotExists(Database db, String indexName, String indexSQL) async {
    try {
      final result = await db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
        [indexName]
      );
      
      if (result.isEmpty) {
        await db.execute(indexSQL);
        developer.log('已创建索引: $indexName', name: 'DatabaseQueryOptimizer');
      }
    } catch (e) {
      developer.log('创建索引 $indexName 时出错: $e', name: 'DatabaseQueryOptimizer');
    }
  }
}

/// 查询缓存条目
class QueryCacheEntry {
  final List<Map<String, dynamic>> results;
  final DateTime timestamp;
  DateTime lastAccessed;
  int accessCount = 1;

  QueryCacheEntry({
    required this.results,
    required this.timestamp,
    DateTime? lastAccessed,
  }) : lastAccessed = lastAccessed ?? timestamp;
}

/// 查询执行统计
class QueryExecutionStats {
  int executionCount = 0;
  int totalExecutionTime = 0; // 毫秒
  int totalResultCount = 0;

  Duration get averageExecutionTime {
    return Duration(milliseconds: 
      executionCount > 0 ? (totalExecutionTime / executionCount).round() : 0);
  }

  double get averageResultCount {
    return executionCount > 0 ? totalResultCount / executionCount : 0.0;
  }

  void recordExecution(Duration duration, int resultCount) {
    executionCount++;
    totalExecutionTime += duration.inMilliseconds;
    totalResultCount += resultCount;
  }
}