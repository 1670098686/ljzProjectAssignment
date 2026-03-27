import 'dart:convert';
import 'package:dio/dio.dart';
import '../network/app_exception.dart';

/// API响应解析结果
class ApiResponseResult<T> {
  final bool success;
  final T? data;
  final String? message;
  final int? code;
  final dynamic rawResponse;
  final Exception? error;

  ApiResponseResult({
    required this.success,
    this.data,
    this.message,
    this.code,
    this.rawResponse,
    this.error,
  });

  /// 创建成功结果
  factory ApiResponseResult.success(T data, {String? message, int? code}) {
    return ApiResponseResult<T>(
      success: true,
      data: data,
      message: message ?? '操作成功',
      code: code ?? 200,
    );
  }

  /// 创建失败结果
  factory ApiResponseResult.failure(String message, {int? code, Exception? error}) {
    return ApiResponseResult<T>(
      success: false,
      message: message,
      code: code ?? 500,
      error: error,
    );
  }

  /// 从Dio响应创建结果
  factory ApiResponseResult.fromDioResponse(dynamic response) {
    try {
      if (response == null) {
        return ApiResponseResult<T>.failure('响应为空');
      }

      // 检查响应状态码
      final statusCode = response.statusCode;
      if (statusCode == null) {
        return ApiResponseResult<T>.failure('无效的响应状态码');
      }

      // 处理成功响应
      if (statusCode >= 200 && statusCode < 300) {
        final responseData = response.data;
        
        // 解析标准API响应格式
        if (responseData is Map<String, dynamic>) {
          final code = responseData['code'] as int? ?? 200;
          final message = responseData['message'] as String? ?? '操作成功';
          final data = responseData['data'] as dynamic;

          if (code == 200) {
            return ApiResponseResult<T>.success(
              data as T,
              message: message,
              code: code,
            );
          } else {
            // 后端返回业务错误
            return ApiResponseResult<T>.failure(
              message,
              code: code,
              error: BusinessException(message, code),
            );
          }
        } else {
          // 非标准格式，直接返回数据
          return ApiResponseResult<T>.success(
            responseData as T,
            code: statusCode,
          );
        }
      } else {
        // 处理HTTP错误
        final errorMessage = _getHttpErrorMessage(statusCode, response.data);
        return ApiResponseResult<T>.failure(
          errorMessage,
          code: statusCode,
          error: _createAppException(statusCode, errorMessage),
        );
      }
    } catch (e) {
      return ApiResponseResult<T>.failure(
        '响应解析失败: ${e.toString()}',
        error: e is Exception ? e : Exception(e.toString()),
      );
    }
  }

  /// 获取HTTP错误消息
  static String _getHttpErrorMessage(int statusCode, dynamic responseData) {
    if (responseData is Map<String, dynamic>) {
      return responseData['message'] as String? ?? _getDefaultHttpErrorMessage(statusCode);
    } else if (responseData is String) {
      try {
        final jsonData = json.decode(responseData);
        if (jsonData is Map<String, dynamic>) {
          return jsonData['message'] as String? ?? _getDefaultHttpErrorMessage(statusCode);
        }
      } catch (e) {
        // 忽略JSON解析错误
      }
      return responseData;
    }
    return _getDefaultHttpErrorMessage(statusCode);
  }

  /// 获取默认HTTP错误消息
  static String _getDefaultHttpErrorMessage(int statusCode) {
    switch (statusCode) {
      case 400:
        return '请求参数错误';
      case 401:
        return '身份验证失效，请重新登录';
      case 403:
        return '权限不足，无法访问该资源';
      case 404:
        return '资源不存在或已删除';
      case 429:
        return '请求频率超限，请稍后重试';
      case 500:
        return '服务器内部错误，请稍后重试';
      case 502:
        return '网关错误，请稍后重试';
      case 503:
        return '服务暂时不可用，请稍后重试';
      default:
        return '网络错误: $statusCode';
    }
  }

  /// 创建应用异常
  static Exception _createAppException(int statusCode, String message) {
    switch (statusCode) {
      case 400:
        return BadRequestException(message, statusCode);
      case 401:
        return UnauthorizedException(message, statusCode);
      case 403:
        return UnauthorizedException(message, statusCode); // 使用相同的异常类型
      case 404:
        return NotFoundException(message, statusCode);
      case 429:
        return NetworkException(message, statusCode);
      case 500:
      case 502:
      case 503:
        return ServerException(message, statusCode);
      default:
        return AppException(message, statusCode);
    }
  }

  @override
  String toString() {
    return 'ApiResponseResult(success: $success, code: $code, message: $message, data: $data)';
  }
}

/// API响应解析服务
class ApiResponseParser {
  static final ApiResponseParser _instance = ApiResponseParser._internal();
  
  factory ApiResponseParser() => _instance;
  
  ApiResponseParser._internal();

  /// 解析Dio响应
  ApiResponseResult<T> parseDioResponse<T>(dynamic response) {
    return ApiResponseResult<T>.fromDioResponse(response);
  }

  /// 解析JSON响应
  ApiResponseResult<T> parseJsonResponse<T>(String jsonString, {T Function(dynamic)? fromJson}) {
    try {
      final jsonData = json.decode(jsonString);
      
      if (jsonData is Map<String, dynamic>) {
        final code = jsonData['code'] as int? ?? 200;
        final message = jsonData['message'] as String? ?? '操作成功';
        final data = jsonData['data'] as dynamic;

        if (code == 200) {
          T parsedData;
          if (fromJson != null) {
            parsedData = fromJson(data);
          } else {
            parsedData = data as T;
          }
          
          return ApiResponseResult<T>.success(
            parsedData,
            message: message,
            code: code,
          );
        } else {
          return ApiResponseResult<T>.failure(
            message,
            code: code,
            error: BusinessException(message, code),
          );
        }
      } else {
        return ApiResponseResult<T>.failure('无效的响应格式');
      }
    } catch (e) {
      return ApiResponseResult<T>.failure(
        'JSON解析失败: ${e.toString()}',
        error: e is Exception ? e : Exception(e.toString()),
      );
    }
  }

  /// 验证响应数据格式
  bool validateResponseFormat(dynamic response) {
    if (response is! Map<String, dynamic>) {
      return false;
    }

    final hasCode = response.containsKey('code');
    final hasMessage = response.containsKey('message');
    final hasData = response.containsKey('data');

    return hasCode && hasMessage && hasData;
  }

  /// 提取错误信息
  String extractErrorMessage(dynamic error) {
    if (error is ApiResponseResult) {
      return error.message ?? '未知错误';
    } else if (error is AppException) {
      return error.message;
    } else if (error is Map<String, dynamic>) {
      return error['message'] as String? ?? error.toString();
    } else if (error is String) {
      return error;
    } else {
      return error.toString();
    }
  }

  /// 提取错误码
  int? extractErrorCode(dynamic error) {
    if (error is ApiResponseResult) {
      return error.code;
    } else if (error is AppException) {
      return error.code;
    } else if (error is Map<String, dynamic>) {
      return error['code'] as int?;
    }
    return null;
  }

  /// 检查是否为网络错误
  bool isNetworkError(dynamic error) {
    if (error is DioException) {
      return error.type == DioExceptionType.connectionTimeout ||
             error.type == DioExceptionType.sendTimeout ||
             error.type == DioExceptionType.receiveTimeout ||
             error.type == DioExceptionType.unknown;
    }
    return false;
  }

  /// 检查是否为服务器错误
  bool isServerError(dynamic error) {
    if (error is DioException && error.response != null) {
      final statusCode = error.response!.statusCode;
      return statusCode != null && statusCode >= 500;
    }
    return error is ServerException;
  }

  /// 检查是否为认证错误
  bool isAuthenticationError(dynamic error) {
    if (error is DioException && error.response != null) {
      final statusCode = error.response!.statusCode;
      return statusCode == 401 || statusCode == 403;
    }
    return error is UnauthorizedException;
  }

  /// 检查是否为业务错误
  bool isBusinessError(dynamic error) {
    if (error is DioException && error.response != null) {
      final statusCode = error.response!.statusCode;
      return statusCode == 400 || statusCode == 422;
    }
    return error is BusinessException || error is BadRequestException;
  }

  /// 获取建议的重试策略
  RetryStrategy getRetryStrategy(dynamic error) {
    if (isNetworkError(error)) {
      return RetryStrategy.immediate;
    } else if (isServerError(error)) {
      return RetryStrategy.delayed;
    } else if (isAuthenticationError(error)) {
      return RetryStrategy.requireUserAction;
    } else if (isBusinessError(error)) {
      return RetryStrategy.noRetry;
    }
    return RetryStrategy.noRetry;
  }
}

/// 重试策略枚举
enum RetryStrategy {
  immediate,          // 立即重试
  delayed,            // 延迟重试
  requireUserAction,  // 需要用户操作
  noRetry,            // 不重试
}