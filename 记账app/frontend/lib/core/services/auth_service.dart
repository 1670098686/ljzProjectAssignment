import 'package:shared_preferences/shared_preferences.dart';
import '../../data/models/user_model.dart';
import '../network/api_client.dart';
import 'unified_error_handling_service.dart';

/// 用户认证服务类
/// 负责处理登录、注册、登出等认证相关的API调用
class AuthService {
  final ApiClient _apiClient;
  final UnifiedErrorHandlingService _errorHandlingService;

  AuthService({
    required ApiClient apiClient,
    required UnifiedErrorHandlingService errorHandlingService,
  })  : _apiClient = apiClient,
        _errorHandlingService = errorHandlingService;

  /// 用户登录
  /// [email] 用户邮箱
  /// [password] 用户密码
  /// 返回AuthResponse包含用户信息和token
  Future<AuthResponse> login(String email, String password) async {
    try {
      final loginRequest = LoginRequest(
        username: email, // 使用email作为username
        password: password,
        rememberMe: false,
      );
      
      final response = await _apiClient.post(
        '/api/v1/auth/login',
        data: loginRequest.toJson(),
        fromJson: (json) => AuthResponse.fromJson(json as Map<String, dynamic>),
      );

      // 保存token到本地存储
      await _saveAuthToken(response.token);
      
      return response;
    } catch (error) {
      _errorHandlingService.handleError(
        '用户登录',
        error,
        customMessage: '登录失败',
      );
      rethrow;
    }
  }

  /// 用户注册
  /// [email] 用户邮箱
  /// [password] 用户密码
  /// [username] 用户名
  /// 返回AuthResponse包含用户信息和token
  Future<AuthResponse> register(String email, String password, String username) async {
    try {
      final registerRequest = RegisterRequest(
        username: username,
        email: email,
        password: password,
        confirmPassword: password, // 临时使用相同密码，实际应用中应该分开
      );
      
      final response = await _apiClient.post(
        '/api/v1/auth/register',
        data: registerRequest.toJson(),
        fromJson: (json) => AuthResponse.fromJson(json as Map<String, dynamic>),
      );

      // 保存token到本地存储
      await _saveAuthToken(response.token);
      
      return response;
    } catch (error) {
      _errorHandlingService.handleError(
        '用户注册',
        error,
        customMessage: '注册失败',
      );
      rethrow;
    }
  }

  /// 用户登出
  Future<void> logout() async {
    try {
      // 调用登出API（如果后端支持）
      await _apiClient.post(
        '/api/v1/auth/logout',
        fromJson: (json) => null as dynamic, // 登出不需要返回值
      );
    } catch (error) {
      // 登出失败时仍然清除本地token
      _errorHandlingService.handleError(
        '用户登出',
        error,
        customMessage: '登出失败',
      );
    } finally {
      // 清除本地存储的token
      await _clearAuthToken();
    }
  }

  /// 刷新token
  Future<String> refreshToken() async {
    try {
      final newToken = await _apiClient.post(
        '/api/v1/auth/refresh',
        fromJson: (json) => (json as Map<String, dynamic>)['token'] as String,
      );
      
      await _saveAuthToken(newToken);
      
      return newToken;
    } catch (error) {
      _errorHandlingService.handleError(
        '刷新token',
        error,
        customMessage: '刷新token失败',
      );
      rethrow;
    }
  }

  /// 重置密码
  /// [email] 用户邮箱
  Future<void> resetPassword(String email) async {
    try {
      // 实际应用中，应该先发送验证码，然后使用验证码重置密码
      // 这里简化处理，仅调用API
      final resetRequest = ResetPasswordRequest(
        email: email,
        newPassword: '', // 实际应用中应该提供新密码
        confirmNewPassword: '', // 实际应用中应该提供确认新密码
        verificationCode: '', // 实际应用中应该提供验证码
      );
      
      await _apiClient.post(
        '/api/v1/auth/reset-password',
        data: resetRequest.toJson(),
        fromJson: (json) => null as dynamic, // 重置密码不需要返回值
      );
    } catch (error) {
      _errorHandlingService.handleError(
        '重置密码',
        error,
        customMessage: '重置密码失败',
      );
      rethrow;
    }
  }

  /// 获取当前用户信息
  Future<User> getCurrentUser() async {
    try {
      return await _apiClient.get(
        '/api/v1/auth/me',
        fromJson: (json) => User.fromJson(json as Map<String, dynamic>),
      );
    } catch (error) {
      _errorHandlingService.handleError(
        '获取用户信息',
        error,
        customMessage: '获取用户信息失败',
      );
      rethrow;
    }
  }

  /// 检查用户是否已登录
  Future<bool> isLoggedIn() async {
    try {
      final token = await _getAuthToken();
      return token != null && token.isNotEmpty;
    } catch (error) {
      return false;
    }
  }

  /// 获取认证token
  Future<String?> _getAuthToken() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString('auth_token');
    } catch (error) {
      return null;
    }
  }

  /// 保存认证token
  Future<void> _saveAuthToken(String token) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('auth_token', token);
    } catch (error) {
      _errorHandlingService.handleError(
        '保存认证token',
        error,
        customMessage: '保存token失败',
      );
      rethrow;
    }
  }

  /// 清除认证token
  Future<void> _clearAuthToken() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('auth_token');
    } catch (error) {
      _errorHandlingService.handleError(
        '清除认证token',
        error,
        customMessage: '清除token失败',
      );
      rethrow;
    }
  }

  /// 验证token是否有效
  Future<bool> validateToken() async {
    try {
      final token = await _getAuthToken();
      if (token == null || token.isEmpty) {
        return false;
      }

      // 调用验证token的API
      await _apiClient.get(
        '/api/v1/auth/validate',
        fromJson: (json) => null as dynamic, // 验证token不需要返回值
      );
      return true;
    } catch (error) {
      return false;
    }
  }
}