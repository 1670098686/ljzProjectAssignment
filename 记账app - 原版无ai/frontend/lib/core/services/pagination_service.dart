import 'dart:developer' as developer;
import 'package:sqflite/sqflite.dart';

/// 分页查询参数
class PaginationParams {
  final int page;
  final int pageSize;
  final String sortField;
  final String sortOrder; // 'ASC' 或 'DESC'
  
  PaginationParams({
    this.page = 1,
    this.pageSize = 20,
    this.sortField = 'id',
    this.sortOrder = 'DESC',
  }) : assert(page > 0, '页码必须大于0'),
       assert(pageSize > 0, '页面大小必须大于0'),
       assert(pageSize <= 100, '页面大小不能超过100');
}

/// 分页查询结果
class PaginatedResult<T> {
  final List<T> data;
  final int totalCount;
  final int page;
  final int pageSize;
  final int totalPages;
  final bool hasNextPage;
  final bool hasPreviousPage;
  
  PaginatedResult({
    required this.data,
    required this.totalCount,
    required this.page,
    required this.pageSize,
  }) : totalPages = (totalCount / pageSize).ceil(),
       hasNextPage = (page * pageSize) < totalCount,
       hasPreviousPage = page > 1;
}

/// 分页查询服务
/// 提供高性能的分页查询功能，支持大数据量处理
class PaginationService {
  static final PaginationService instance = PaginationService._internal();
  
  PaginationService._internal();
  
  factory PaginationService() {
    return instance;
  }
  
  /// 执行分页查询（账单数据）
  Future<PaginatedResult<Map<String, dynamic>>> queryBillsPaginated(
    Database db,
    PaginationParams params, {
    String? whereClause,
    List<dynamic>? whereArgs,
  }) async {
    try {
      developer.log('开始执行账单分页查询: 页码=${params.page}, 大小=${params.pageSize}', name: 'PaginationService');
      
      // 构建查询条件
      String queryWhere = whereClause ?? '1=1';
      final queryArgs = whereArgs ?? <dynamic>[];
      
      // 获取总数
      final countQuery = 'SELECT COUNT(*) as total FROM bills WHERE $queryWhere';
      final countResult = await db.rawQuery(countQuery, queryArgs);
      final totalCount = Sqflite.firstIntValue(countResult) ?? 0;
      
      // 构建分页查询
      final offset = (params.page - 1) * params.pageSize;
      final dataQuery = '''
        SELECT * FROM bills 
        WHERE $queryWhere
        ORDER BY ${params.sortField} ${params.sortOrder}
        LIMIT ${params.pageSize} OFFSET $offset
      ''';
      
      final dataResult = await db.rawQuery(dataQuery, queryArgs);
      
      developer.log(
        '账单分页查询完成: 总数=$totalCount, 当前页=${params.page}, 共${(totalCount / params.pageSize).ceil()}页',
        name: 'PaginationService'
      );
      
      return PaginatedResult<Map<String, dynamic>>(
        data: dataResult,
        totalCount: totalCount,
        page: params.page,
        pageSize: params.pageSize,
      );
    } catch (e, stackTrace) {
      developer.log(
        '账单分页查询失败',
        name: 'PaginationService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }
  
  /// 执行分页查询（分类数据）
  Future<PaginatedResult<Map<String, dynamic>>> queryCategoriesPaginated(
    Database db,
    PaginationParams params, {
    String? whereClause,
    List<dynamic>? whereArgs,
  }) async {
    try {
      developer.log('开始执行分类分页查询: 页码=${params.page}, 大小=${params.pageSize}', name: 'PaginationService');
      
      // 构建查询条件
      String queryWhere = whereClause ?? '1=1';
      final queryArgs = whereArgs ?? <dynamic>[];
      
      // 获取总数
      final countQuery = 'SELECT COUNT(*) as total FROM categories WHERE $queryWhere';
      final countResult = await db.rawQuery(countQuery, queryArgs);
      final totalCount = Sqflite.firstIntValue(countResult) ?? 0;
      
      // 构建分页查询
      final offset = (params.page - 1) * params.pageSize;
      final dataQuery = '''
        SELECT * FROM categories 
        WHERE $queryWhere
        ORDER BY ${params.sortField} ${params.sortOrder}
        LIMIT ${params.pageSize} OFFSET $offset
      ''';
      
      final dataResult = await db.rawQuery(dataQuery, queryArgs);
      
      developer.log(
        '分类分页查询完成: 总数=$totalCount, 当前页=${params.page}, 共${(totalCount / params.pageSize).ceil()}页',
        name: 'PaginationService'
      );
      
      return PaginatedResult<Map<String, dynamic>>(
        data: dataResult,
        totalCount: totalCount,
        page: params.page,
        pageSize: params.pageSize,
      );
    } catch (e, stackTrace) {
      developer.log(
        '分类分页查询失败',
        name: 'PaginationService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }
  
  /// 执行分页查询（预算数据）
  Future<PaginatedResult<Map<String, dynamic>>> queryBudgetsPaginated(
    Database db,
    PaginationParams params, {
    String? whereClause,
    List<dynamic>? whereArgs,
  }) async {
    try {
      developer.log('开始执行预算分页查询: 页码=${params.page}, 大小=${params.pageSize}', name: 'PaginationService');
      
      // 构建查询条件
      String queryWhere = whereClause ?? '1=1';
      final queryArgs = whereArgs ?? <dynamic>[];
      
      // 获取总数
      final countQuery = 'SELECT COUNT(*) as total FROM budgets WHERE $queryWhere';
      final countResult = await db.rawQuery(countQuery, queryArgs);
      final totalCount = Sqflite.firstIntValue(countResult) ?? 0;
      
      // 构建分页查询
      final offset = (params.page - 1) * params.pageSize;
      final dataQuery = '''
        SELECT * FROM budgets 
        WHERE $queryWhere
        ORDER BY ${params.sortField} ${params.sortOrder}
        LIMIT ${params.pageSize} OFFSET $offset
      ''';
      
      final dataResult = await db.rawQuery(dataQuery, queryArgs);
      
      developer.log(
        '预算分页查询完成: 总数=$totalCount, 当前页=${params.page}, 共${(totalCount / params.pageSize).ceil()}页',
        name: 'PaginationService'
      );
      
      return PaginatedResult<Map<String, dynamic>>(
        data: dataResult,
        totalCount: totalCount,
        page: params.page,
        pageSize: params.pageSize,
      );
    } catch (e, stackTrace) {
      developer.log(
        '预算分页查询失败',
        name: 'PaginationService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }
  
  /// 执行分页查询（储蓄目标数据）
  Future<PaginatedResult<Map<String, dynamic>>> querySavingGoalsPaginated(
    Database db,
    PaginationParams params, {
    String? whereClause,
    List<dynamic>? whereArgs,
  }) async {
    try {
      developer.log('开始执行储蓄目标分页查询: 页码=${params.page}, 大小=${params.pageSize}', name: 'PaginationService');
      
      // 构建查询条件
      String queryWhere = whereClause ?? '1=1';
      final queryArgs = whereArgs ?? <dynamic>[];
      
      // 获取总数
      final countQuery = 'SELECT COUNT(*) as total FROM saving_goals WHERE $queryWhere';
      final countResult = await db.rawQuery(countQuery, queryArgs);
      final totalCount = Sqflite.firstIntValue(countResult) ?? 0;
      
      // 构建分页查询
      final offset = (params.page - 1) * params.pageSize;
      final dataQuery = '''
        SELECT * FROM saving_goals 
        WHERE $queryWhere
        ORDER BY ${params.sortField} ${params.sortOrder}
        LIMIT ${params.pageSize} OFFSET $offset
      ''';
      
      final dataResult = await db.rawQuery(dataQuery, queryArgs);
      
      developer.log(
        '储蓄目标分页查询完成: 总数=$totalCount, 当前页=${params.page}, 共${(totalCount / params.pageSize).ceil()}页',
        name: 'PaginationService'
      );
      
      return PaginatedResult<Map<String, dynamic>>(
        data: dataResult,
        totalCount: totalCount,
        page: params.page,
        pageSize: params.pageSize,
      );
    } catch (e, stackTrace) {
      developer.log(
        '储蓄目标分页查询失败',
        name: 'PaginationService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }
  
  /// 预加载下一页数据（提升用户体验）
  Future<void> preloadNextPage(
    Database db,
    String tableName,
    PaginationParams currentParams,
  ) async {
    try {
      final nextPageParams = PaginationParams(
        page: currentParams.page + 1,
        pageSize: currentParams.pageSize,
        sortField: currentParams.sortField,
        sortOrder: currentParams.sortOrder,
      );
      
      // 简化版本：只预加载一页，不传入具体查询条件
      // 在实际应用中，可以根据具体情况定制预加载策略
      developer.log('开始预加载下一页数据: $tableName, 页码=${nextPageParams.page}', name: 'PaginationService');
      
      // 这里可以实现具体的预加载逻辑
      // 为了避免影响主查询性能，实际实现时应该使用后台任务
      
    } catch (e) {
      developer.log('预加载下一页数据失败: $e', name: 'PaginationService');
    }
  }
  
  /// 获取数据库性能统计信息
  Future<PaginationStatistics> getPaginationStatistics(Database db) async {
    try {
      // 获取各个表的数据量
      final tables = ['bills', 'categories', 'budgets', 'saving_goals'];
      final Map<String, int> tableCounts = {};
      
      for (final table in tables) {
        final result = await db.rawQuery('SELECT COUNT(*) as count FROM $table');
        tableCounts[table] = Sqflite.firstIntValue(result) ?? 0;
      }
      
      // 计算总数据量
      final totalRecords = tableCounts.values.fold<int>(0, (sum, count) => sum + count);
      
      return PaginationStatistics(
        tableCounts: tableCounts,
        totalRecords: totalRecords,
        estimatedMemoryUsage: _estimateMemoryUsage(totalRecords),
      );
    } catch (e) {
      developer.log('获取分页统计信息失败: $e', name: 'PaginationService');
      rethrow;
    }
  }
  
  /// 估算内存使用量（MB）
  int _estimateMemoryUsage(int totalRecords) {
    // 估算每个记录平均占用100字节
    return (totalRecords * 100) ~/ (1024 * 1024);
  }
}

/// 分页查询统计信息
class PaginationStatistics {
  final Map<String, int> tableCounts;
  final int totalRecords;
  final int estimatedMemoryUsage; // MB
  
  PaginationStatistics({
    required this.tableCounts,
    required this.totalRecords,
    required this.estimatedMemoryUsage,
  });
  
  @override
  String toString() {
    final countsString = tableCounts.entries.map((e) => '${e.key}: ${e.value}').join(', ');
    return 'PaginationStatistics(total: $totalRecords, tables: $countsString, memory: ${estimatedMemoryUsage}MB)';
  }
}