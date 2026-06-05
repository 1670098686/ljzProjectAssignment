import 'package:dio/dio.dart';
import '../data/models/transaction_model.dart';
import '../data/models/category_model.dart';
import '../data/models/saving_goal_model.dart';
import '../data/models/budget_model.dart';
import '../models/api_response.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8080/api';
  final Dio _dio;

  ApiService() : _dio = Dio(BaseOptions(baseUrl: baseUrl)) {
    // 添加请求拦截器
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        // 添加认证token
        // options.headers['Authorization'] = 'Bearer $token';
        return handler.next(options);
      },
      onResponse: (response, handler) {
        return handler.next(response);
      },
      onError: (DioException e, handler) {
        // 处理错误
        print('API Error: ${e.message}');
        return handler.next(e);
      },
    ));
  }

  // 统一的API请求封装
  Future<ApiResponse<T>> _apiRequest<T>(
    Future<Response> Function() request,
    T Function(dynamic) fromJsonT) async {
    try {
      final response = await request();
      return ApiResponse.fromJson(response.data, fromJsonT);
    } catch (e) {
      if (e is DioException) {
        if (e.response != null) {
          return ApiResponse(
            code: e.response!.statusCode ?? 500,
            message: e.response!.data['message'] ?? '请求失败',
            success: false,
          );
        } else {
          return ApiResponse(
            code: 500,
            message: e.message ?? '网络错误',
            success: false,
          );
        }
      } else {
        return ApiResponse(
          code: 500,
          message: e.toString(),
          success: false,
        );
      }
    }
  }



  // 交易相关API
  Future<ApiResponse<List<Transaction>>> getTransactions() async {
    return await _apiRequest(
      () => _dio.get('/transactions'),
      (data) => (data as List)
          .map((item) => Transaction.fromJson(item))
          .toList(),
    );
  }

  Future<ApiResponse<Transaction>> createTransaction(Transaction transaction) async {
    return await _apiRequest(
      () => _dio.post('/transactions', data: transaction.toJson()),
      (data) => Transaction.fromJson(data),
    );
  }

  // 分类相关API
  Future<ApiResponse<List<Category>>> getCategories() async {
    return await _apiRequest(
      () => _dio.get('/categories'),
      (data) => (data as List)
          .map((item) => Category.fromJson(item))
          .toList(),
    );
  }



  // 储蓄目标相关API
  Future<ApiResponse<List<SavingGoal>>> getSavingGoals() async {
    return await _apiRequest(
      () => _dio.get('/saving-goals'),
      (data) => (data as List)
          .map((item) => SavingGoal.fromJson(item))
          .toList(),
    );
  }

  // 预算预警相关API
  
  /// 获取预算预警列表
  Future<ApiResponse<List<BudgetAlert>>> getBudgetAlerts() async {
    return await _apiRequest(
      () => _dio.get('/api/v1/budget/alert'),
      (data) => (data as List)
          .map((item) => BudgetAlert.fromJson(item))
          .toList(),
    );
  }

  /// 检查预算预警
  Future<ApiResponse<List<BudgetAlert>>> checkBudgetAlerts() async {
    return await _apiRequest(
      () => _dio.get('/api/v1/budget/alert/check'),
      (data) => (data as List)
          .map((item) => BudgetAlert.fromJson(item))
          .toList(),
    );
  }
}
