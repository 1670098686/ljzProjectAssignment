import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/api_response.dart';
import '../data/models/user_model.dart';

class AuthService {
  static const String baseUrl = 'http://localhost:8080/api';
  final Dio _dio;

  AuthService() : _dio = Dio(BaseOptions(baseUrl: baseUrl)) {
    // 添加认证拦截器
    _setupAuthInterceptor();
  }

  // 添加认证拦截器
  void _setupAuthInterceptor() {
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        // 从shared_preferences获取令牌
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString('auth_token');
        
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        
        return handler.next(options);
      },
    ));
  }

  // 登录功能
  Future<ApiResponse<User>> login(String username, String password) async {
    try {
      final response = await _dio.post('/auth/login', data: {
        'username': username,
        'password': password,
      });
      
      // 保存令牌
      final token = response.data['data']['token'];
      await _saveToken(token);
      
      return ApiResponse.fromJson(response.data, (data) => User.fromJson(data['user']));
    } catch (e) {
      if (e is DioException) {
        if (e.response != null) {
          return ApiResponse(
            code: e.response!.statusCode ?? 500,
            message: e.response!.data['message'] ?? '登录失败',
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

  // 注册功能
  Future<ApiResponse<User>> register(String username, String password, String email) async {
    try {
      final response = await _dio.post('/auth/register', data: {
        'username': username,
        'password': password,
        'email': email,
      });
      
      // 保存令牌
      final token = response.data['data']['token'];
      await _saveToken(token);
      
      return ApiResponse.fromJson(response.data, (data) => User.fromJson(data['user']));
    } catch (e) {
      if (e is DioException) {
        if (e.response != null) {
          return ApiResponse(
            code: e.response!.statusCode ?? 500,
            message: e.response!.data['message'] ?? '注册失败',
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

  // 保存令牌
  Future<void> _saveToken(String token) async {
    // 使用shared_preferences保存令牌
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('auth_token', token);
  }

  // 获取令牌
  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('auth_token');
  }

  // 清除令牌
  Future<void> clearToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('auth_token');
  }

  // 检查是否已登录
  Future<bool> isLoggedIn() async {
    final token = await getToken();
    return token != null;
  }

  // 登出功能
  Future<void> logout() async {
    await clearToken();
  }
}
