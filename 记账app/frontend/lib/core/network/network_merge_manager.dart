import 'dart:async';
import 'dart:developer' as developer;
import 'dart:collection';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

/// 网络请求合并管理器
/// 用于将多个相关请求合并为批量请求，提高网络效率
class NetworkMergeManager {
  static final NetworkMergeManager _instance = NetworkMergeManager._internal();
  factory NetworkMergeManager() => _instance;
  NetworkMergeManager._internal();

  late final Dio _dio;

  // 请求合并配置
  static const int _defaultMergeDelay = 100; // 合并延迟时间（毫秒）
  static const int _defaultMaxBatchSize = 10; // 最大批量大小
  static const int _defaultMaxWaitTime = 1000; // 最大等待时间（毫秒）

  // 合并队列
  final Queue<BatchRequest> _mergeQueue = Queue<BatchRequest>();
  final Map<String, Timer> _mergeTimers = {};
  final Map<String, List<PendingRequest>> _pendingRequests = {};

  // 配置参数
  int _mergeDelay = _defaultMergeDelay;
  int _maxBatchSize = _defaultMaxBatchSize;
  int _maxWaitTime = _defaultMaxWaitTime;

  /// 初始化网络请求合并管理器
  void initialize() {
    _dio = Dio(BaseOptions(
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'Content-Type': 'application/json; charset=UTF-8',
        'Accept': 'application/json',
      },
    ));

    // 添加拦截器
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          options.headers['X-Request-ID'] = DateTime.now().millisecondsSinceEpoch.toString();
          handler.next(options);
        },
        onResponse: (response, handler) {
          handler.next(response);
        },
        onError: (error, handler) {
          developer.log('批量请求错误: ${error.message}', name: 'NetworkMergeManager');
          handler.next(error);
        },
      ),
    );

    if (kDebugMode) {
      _dio.interceptors.add(
        LogInterceptor(
          logPrint: (obj) => developer.log('API: $obj', name: 'NetworkMergeManager'),
        ),
      );
    }

    developer.log('网络请求合并管理器初始化完成', name: 'NetworkMergeManager');
  }

  /// 设置合并参数
  void configure({
    int? mergeDelay,
    int? maxBatchSize,
    int? maxWaitTime,
  }) {
    if (mergeDelay != null) _mergeDelay = mergeDelay;
    if (maxBatchSize != null) _maxBatchSize = maxBatchSize;
    if (maxWaitTime != null) _maxWaitTime = maxWaitTime;
  }

  /// 发起可合并的请求
  Future<T> request<T>(
    String key,
    String path, {
    required String method,
    Map<String, dynamic>? queryParameters,
    dynamic data,
    required T Function(dynamic json) fromJson,
    String? mergeGroup, // 合并组标识
  }) async {
    final request = PendingRequest<T>(
      path: path,
      method: method,
      queryParameters: queryParameters,
      data: data,
      fromJson: fromJson,
      completer: Completer<T>(),
    );

    // 添加到待合并队列
    final groupKey = mergeGroup ?? 'default';
    if (!_pendingRequests.containsKey(groupKey)) {
      _pendingRequests[groupKey] = [];
    }
    _pendingRequests[groupKey]!.add(request);

    // 设置合并定时器
    _scheduleMerge(key, groupKey);

    return request.completer.future;
  }

  /// 调度请求合并
  void _scheduleMerge(String key, String groupKey) {
    // 清除现有定时器
    if (_mergeTimers.containsKey(groupKey)) {
      _mergeTimers[groupKey]!.cancel();
    }

    // 创建新的合并定时器
    _mergeTimers[groupKey] = Timer(Duration(milliseconds: _mergeDelay), () {
      _executeMerge(groupKey);
    });
  }

  /// 执行批量合并请求
  Future<void> _executeMerge(String groupKey) async {
    final requests = _pendingRequests[groupKey];
    if (requests == null || requests.isEmpty) {
      return;
    }

    // 清空待处理列表
    _pendingRequests[groupKey] = [];

    // 如果只有一个请求，直接执行
    if (requests.length == 1) {
      await _executeSingleRequest(requests.first);
      return;
    }

    // 批量处理多个请求
    await _executeBatchRequest(requests, groupKey);
  }

  /// 执行单个请求
  Future<void> _executeSingleRequest<T>(PendingRequest<T> request) async {
    try {
      final response = await _dio.request(
        request.path,
        queryParameters: request.queryParameters,
        data: request.data,
        options: Options(method: request.method),
      );

      final result = _parseResponse(response.data, request.fromJson);
      request.completer.complete(result);
    } catch (e) {
      request.completer.completeError(e);
    }
  }

  /// 执行批量请求
  Future<void> _executeBatchRequest<T>(
    List<PendingRequest<T>> requests,
    String groupKey,
  ) async {
    // 模拟批量请求（实际项目中可能需要后端支持）
    final batchRequest = BatchRequest(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      groupKey: groupKey,
      requests: requests,
    );

    try {
      developer.log(
        '执行批量请求: ${requests.length} 个请求',
        name: 'NetworkMergeManager',
      );

      // 这里实现具体的批量请求逻辑
      // 例如：将多个请求合并为一个HTTP请求
      await _processBatchRequest(batchRequest);

      developer.log(
        '批量请求完成: ${requests.length} 个请求',
        name: 'NetworkMergeManager',
      );
    } catch (e) {
      developer.log(
        '批量请求失败: $e',
        name: 'NetworkMergeManager',
      );

      // 所有请求都失败
      for (final request in requests) {
        request.completer.completeError(e);
      }
    }
  }

  /// 处理批量请求（示例实现）
  Future<void> _processBatchRequest(BatchRequest batchRequest) async {
    // 这里可以根据实际需求实现具体的批量请求逻辑
    // 例如：使用GraphQL批量查询、REST批量API等

    for (final request in batchRequest.requests) {
      try {
        final response = await _dio.request(
          request.path,
          queryParameters: request.queryParameters,
          data: request.data,
          options: Options(method: request.method),
        );

        final result = _parseResponse(response.data, request.fromJson);
        request.completer.complete(result);
      } catch (e) {
        request.completer.completeError(e);
      }
    }
  }

  /// 解析响应数据
  T _parseResponse<T>(dynamic data, T Function(dynamic json) fromJson) {
    try {
      return fromJson(data);
    } catch (e) {
      developer.log('响应数据解析失败: $e', name: 'NetworkMergeManager');
      rethrow;
    }
  }

  /// 强制刷新指定组的待合并请求
  Future<void> flushPendingRequests(String groupKey) async {
    if (_mergeTimers.containsKey(groupKey)) {
      _mergeTimers[groupKey]!.cancel();
      _mergeTimers.remove(groupKey);
    }

    if (_pendingRequests.containsKey(groupKey) &&
        _pendingRequests[groupKey]!.isNotEmpty) {
      await _executeMerge(groupKey);
    }
  }

  /// 获取待处理请求数量
  int getPendingRequestCount(String groupKey) {
    return _pendingRequests[groupKey]?.length ?? 0;
  }

  /// 获取所有待处理请求总数
  int getTotalPendingRequestCount() {
    return _pendingRequests.values
        .fold(0, (sum, requests) => sum + requests.length);
  }

  /// 清理资源
  void dispose() {
    // 取消所有定时器
    for (final timer in _mergeTimers.values) {
      timer.cancel();
    }
    _mergeTimers.clear();

    // 完成所有待处理的请求（失败）
    for (final requests in _pendingRequests.values) {
      for (final request in requests) {
        if (!request.completer.isCompleted) {
          request.completer.completeError('请求已取消');
        }
      }
    }
    _pendingRequests.clear();

    // 关闭Dio
    _dio.close();

    developer.log('网络请求合并管理器已释放', name: 'NetworkMergeManager');
  }
}

/// 待处理请求
class PendingRequest<T> {
  final String path;
  final String method;
  final Map<String, dynamic>? queryParameters;
  final dynamic data;
  final T Function(dynamic json) fromJson;
  final Completer<T> completer;

  PendingRequest({
    required this.path,
    required this.method,
    required this.queryParameters,
    required this.data,
    required this.fromJson,
    required this.completer,
  });
}

/// 批量请求
class BatchRequest {
  final String id;
  final String groupKey;
  final List<PendingRequest> requests;

  BatchRequest({
    required this.id,
    required this.groupKey,
    required this.requests,
  });
}