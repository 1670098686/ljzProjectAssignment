import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/models/user_model.dart';
import '../services/local_database_service.dart';
import 'state_sync_manager.dart';







/// 用户状态管理提供者
class UserProvider with ChangeNotifier {
  final LocalDatabaseService? _databaseService;
  SharedPreferences? _prefs;
  
  /// 用于通知路由刷新的ValueNotifier
  final ValueNotifier<int> _refreshNotifier = ValueNotifier(0);
  
  /// 获取用于路由刷新的ValueNotifier
  ValueNotifier<int> get refreshNotifier => _refreshNotifier;

  User? _user;
  bool _isLoading = false;
  bool _isAuthenticated = false;
  String? _errorMessage;
  String? _token;

  /// 响应StateSyncManager的清除数据通知
  void _onSyncNotification() async {
    print('📢 用户Provider收到StateSyncManager同步通知，开始清除用户数据...');
    await clearUserData();
  }

  /// 清除用户数据
  Future<void> clearUserData() async {
    await _clearAuthState();
    _isLoading = false;
    _errorMessage = null;
    notifyListeners();
    _refreshNotifier.value++;
    print('✅ 用户数据清除完成');
  }

  UserProvider({
    LocalDatabaseService? databaseService,
    SharedPreferences? prefs,
  }) : _databaseService = databaseService,
        _prefs = prefs {
    // 监听StateSyncManager的清除数据通知
    StateSyncManager().addSyncListener('user', _onSyncNotification);
    // 延迟初始化，避免阻塞应用启动
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initialize();
    });
  }

  /// 获取加载状态
  bool get isLoading => _isLoading;

  /// 获取认证状态
  bool get isAuthenticated => _isAuthenticated;

  /// 兼容旧逻辑的登录状态别名
  bool get isLoggedIn => _isAuthenticated;

  /// 获取当前用户
  User? get user => _user;

  /// 获取错误信息
  String? get errorMessage => _errorMessage;

  /// 获取当前token
  String? get token => _token;

  /// 初始化用户状态
  Future<void> _initialize() async {
    try {
      _isLoading = true;
      notifyListeners();

      // 如果_prefs为null，初始化SharedPreferences
      _prefs ??= await SharedPreferences.getInstance();

      // 从本地存储加载token
      _token = _prefs?.getString('auth_token');
      if (_token != null) {
        _isAuthenticated = true;
        // 如果有databaseService，加载用户信息
      if (_databaseService != null) {
        await _loadUserInfo();
      }
      } else {
        _isAuthenticated = false;
        _user = null;
      }

      _isLoading = false;
      _errorMessage = null;
      notifyListeners();
    } catch (error) {
      _isLoading = false;
      _errorMessage = '初始化失败: $error';
      _isAuthenticated = false;
      _user = null;
      _token = null;
      notifyListeners();
    }
  }

  /// 加载用户信息
  Future<void> _loadUserInfo() async {
    try {
      // 从本地存储加载用户信息
      if (_prefs != null) {
        final userInfoStr = _prefs?.getString('user_info');
        if (userInfoStr != null) {
          _user = User.fromJson(jsonDecode(userInfoStr) as Map<String, dynamic>);
        }
      }
    } catch (error) {
      _errorMessage = '加载用户信息失败: $error';
      // 如果加载失败，不清除认证状态，保留本地token
      // await _clearAuthState();
    }
  }

  /// 登录
  Future<bool> login(String email, String password) async {
    try {
      _isLoading = true;
      _errorMessage = null;
      notifyListeners();

      // 如果_prefs为null，初始化SharedPreferences
      _prefs ??= await SharedPreferences.getInstance();

      // 使用真实的LocalDatabaseService进行登录验证
      if (_databaseService != null) {
        final user = await _databaseService!.loginUser(email, password);
        _user = user;
        // 本地认证不需要token，使用email作为token
        _token = email;
        _isAuthenticated = true;
      } else {
        // 如果没有LocalDatabaseService，使用本地验证（仅用于演示）
        // 这里应该调用后端API进行真实验证
        throw Exception('数据库服务未初始化');
      }

      // 保存token到本地存储
      if (_prefs != null && _token != null && _user != null) {
        await _prefs!.setString('auth_token', _token!);
        await _prefs!.setString('user_info', _user!.toJson().toString());
      }

      _isLoading = false;
      _errorMessage = null;
      notifyListeners();
      _refreshNotifier.value++;
      return true;
    } catch (error) {
      _isLoading = false;
      _errorMessage = '登录失败: $error';
      _isAuthenticated = false;
      _user = null;
      _token = null;
      notifyListeners();
      return false;
    }
  }

  /// 登出
  Future<bool> logout() async {
    try {
      _isAuthenticated = false;
      _user = null;
      _token = null;

      // 清除本地存储的token和用户信息
      if (_prefs != null) {
        await _prefs!.remove('auth_token');
        await _prefs!.remove('user_info');
      }

      notifyListeners();
      _refreshNotifier.value++;
      return true;
    } catch (error) {
      return false;
    }
  }

  /// 刷新token
  Future<bool> refreshToken() async {
    try {
      // 对于本地认证，直接从本地存储重新加载token
      if (_prefs != null) {
        _token = _prefs?.getString('auth_token');
        // 如果有databaseService，重新加载用户信息
        if (_databaseService != null) {
          await _loadUserInfo();
        }
      }
      _isAuthenticated = _token != null;
      notifyListeners();
      return _isAuthenticated;
    } catch (error) {
      _errorMessage = '刷新token失败: $error';
      notifyListeners();
      return false;
    }
  }

  /// 清除认证状态
  Future<void> _clearAuthState() async {
    _isAuthenticated = false;
    _user = null;
    _token = null;
    if (_prefs != null) {
      await _prefs!.remove('auth_token');
      await _prefs!.remove('refresh_token');
      await _prefs!.remove('user_info');
    }
  }

  /// 清除错误信息
  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  /// 检查用户权限
  bool hasPermission(String permission) {
    // 如果是已认证用户，默认拥有所有权限
    return _isAuthenticated;
  }

  /// 检查用户角色
  bool hasRole(String role) {
    // 如果是已认证用户，默认拥有所有角色
    return _isAuthenticated;
  }

  /// 注册新用户
  Future<bool> register(String email, String password, String name) async {
    try {
      _isLoading = true;
      _errorMessage = null;
      notifyListeners();

      // 如果_prefs为null，初始化SharedPreferences
      _prefs ??= await SharedPreferences.getInstance();

      // 使用真实的LocalDatabaseService进行注册验证
      if (_databaseService != null) {
        // 检查邮箱是否已注册
        final exists = await _databaseService!.userExists(email);
        if (exists) {
          throw Exception('该邮箱已被注册');
        }
        
        // 注册新用户
        final user = await _databaseService!.registerUser(email, password);
        // 注册成功后不立即设置用户为已认证状态，让用户手动登录
        _user = null;
        _token = null;
        _isAuthenticated = false;
      } else {
        // 如果没有LocalDatabaseService，使用本地验证（仅用于演示）
        // 这里应该调用后端API进行真实验证
        throw Exception('数据库服务未初始化');
      }
      
      // 注册成功后不保存token和用户信息到本地存储，用户需要手动登录

      _isLoading = false;
      _errorMessage = null;
      notifyListeners();
      _refreshNotifier.value++;
      return true;
    } catch (error) {
      _isLoading = false;
      _errorMessage = '注册失败: $error';
      _isAuthenticated = false;
      _user = null;
      _token = null;
      notifyListeners();
      return false;
    }
  }
}