import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api_response.dart';
import 'app_exception.dart';

class ApiClient {
  static const String _envBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: '',
  );

  late final String _baseUrl;
  late final Dio _dio;

  ApiClient({String? baseUrl}) {
    _baseUrl = _resolveBaseUrl(baseUrl);
    _dio = Dio(
      BaseOptions(
        baseUrl: _baseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
          'Accept': 'application/json',
          'User-Agent': 'FinanceApp/1.0.0',
        },
      ),
    );

    // 添加请求中间件
    _setupInterceptors();
  }

  /// 设置统一的请求和响应中间件
  void _setupInterceptors() {
    if (kDebugMode) {
      _dio.interceptors.add(
        LogInterceptor(
          requestBody: true,
          responseBody: true,
          logPrint: (obj) => debugPrint('API: $obj'),
        ),
      );
    }

    // 请求拦截器 - 添加认证头
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // 添加认证token（如果有）
          final token = await _getAuthToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }

          // 添加请求ID用于跟踪
          options.headers['X-Request-ID'] = DateTime.now().millisecondsSinceEpoch.toString();

          // 添加时间戳
          options.headers['X-Timestamp'] = DateTime.now().toIso8601String();

          handler.next(options);
        },
        onResponse: (response, handler) {
          // 统一响应处理
          handler.next(response);
        },
        onError: (DioException e, handler) async {
          // 统一错误处理
          if (e.response?.statusCode == 401) {
            // 自动处理未授权错误
            await _handleUnauthorized();
          }
          handler.next(_handleError(e));
        },
      ),
    );
  }

  /// 获取认证token
  Future<String?> _getAuthToken() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString('auth_token');
    } catch (e) {
      debugPrint('获取认证token失败: $e');
      return null;
    }
  }

  DioException _handleError(DioException error) {
    // We can log the error here if needed
    return error;
  }

  /// 处理未授权错误
  Future<void> _handleUnauthorized() async {
    try {
      // 清除本地存储的token
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('auth_token');
      await prefs.remove('refresh_token');
      
      // 可以在这里添加事件通知，让应用知道用户已登出
      // 例如使用event_bus或provider通知
      debugPrint('用户未授权，已清除本地token');
    } catch (e) {
      debugPrint('处理未授权错误失败: $e');
    }
  }

  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(dynamic json) fromJson,
  }) async {
    try {
      final response = await _dio.get(path, queryParameters: queryParameters);
      return _handleResponse(response, fromJson);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<T> post<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    required T Function(dynamic json) fromJson,
  }) async {
    try {
      final response = await _dio.post(
        path,
        data: data,
        queryParameters: queryParameters,
      );
      return _handleResponse(response, fromJson);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<T> put<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    required T Function(dynamic json) fromJson,
  }) async {
    try {
      final response = await _dio.put(
        path,
        data: data,
        queryParameters: queryParameters,
      );
      return _handleResponse(response, fromJson);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<T> delete<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    required T Function(dynamic json) fromJson,
  }) async {
    try {
      final response = await _dio.delete(
        path,
        data: data,
        queryParameters: queryParameters,
      );
      return _handleResponse(response, fromJson);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<T> patch<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    required T Function(dynamic json) fromJson,
  }) async {
    try {
      final response = await _dio.patch(
        path,
        data: data,
        queryParameters: queryParameters,
      );
      return _handleResponse(response, fromJson);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  T _handleResponse<T>(Response response, T Function(dynamic json) fromJson) {
    final apiResponse = ApiResponse<T>.fromJson(response.data, fromJson);

    if (apiResponse.isSuccess) {
      // For all types, return the data or null if nullable
      return apiResponse.data as T;
    } else {
      throw AppException(apiResponse.message, apiResponse.code);
    }
  }

  AppException _mapDioException(DioException e) {
    String message = e.message ?? 'Unknown error';
    int? code = e.response?.statusCode;

    // Handle ApiResponse format for error responses
    if (e.response?.data is Map<String, dynamic>) {
      final data = e.response!.data as Map<String, dynamic>;
      if (data.containsKey('message')) {
        message = data['message'];
      }
      if (data.containsKey('code')) {
        code = data['code'] as int;
      }
    }

    switch (code) {
      case 400:
        return BadRequestException(message, code);
      case 401:
        return UnauthorizedException(message, code);
      case 404:
        return NotFoundException(message, code);
      case 500:
        return ServerException(message, code);
      default:
        return NetworkException(message, code);
    }
  }

  String _resolveBaseUrl(String? override) {
    final candidate = override ?? _envBaseUrl;

    if (candidate.isEmpty) {
      return _normalizeBaseUrl('http://localhost:8081');
    }

    // 归一化URL并设置默认值
    final uri = Uri.tryParse(candidate);
    if (uri == null) {
      return _normalizeBaseUrl('http://localhost:8081');
    }

    // 根据平台设置默认地址
    if (candidate.contains('10.0.2.2') || candidate.contains('localhost')) {
      if (candidate.contains('android')) {
        return 'http://10.0.2.2:8081';
      }
      return 'http://localhost:8081';
    }

    return _normalizeBaseUrl(candidate);
  }

  String _normalizeBaseUrl(String value) {
    try {
      final uri = Uri.parse(value);
      if (uri.path.isEmpty || uri.path == '/') {
        return uri.replace(path: '/api/v1').toString();
      }
      return uri.toString();
    } catch (_) {
      return value;
    }
  }
}
