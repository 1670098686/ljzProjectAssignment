import 'package:json_annotation/json_annotation.dart';

part 'user_model.g.dart';

/// 用户模型
/// 表示应用中的用户信息
@JsonSerializable(explicitToJson: true)
class User {
  /// 用户ID
  final int id;

  /// 用户名
  final String username;

  /// 邮箱地址
  final String email;

  /// 手机号
  final String? phone;

  /// 头像URL
  final String? avatar;

  /// 昵称
  final String? nickname;

  /// 创建时间
  final DateTime createdAt;

  /// 最后登录时间
  final DateTime? lastLoginAt;

  /// 是否启用
  final bool enabled;

  /// 用户角色
  final List<String> roles;

  /// 用户权限
  final List<String> permissions;

  User({
    required this.id,
    required this.username,
    required this.email,
    this.phone,
    this.avatar,
    this.nickname,
    required this.createdAt,
    this.lastLoginAt,
    required this.enabled,
    this.roles = const [],
    this.permissions = const [],
  });

  /// 从JSON创建User对象
  factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);

  /// 将User对象转换为JSON
  Map<String, dynamic> toJson() => _$UserToJson(this);

  /// 获取显示名称（优先使用昵称，其次使用用户名）
  String get displayName => nickname ?? username;

  /// 检查用户是否具有指定角色
  bool hasRole(String role) => roles.contains(role);

  /// 检查用户是否具有指定权限
  bool hasPermission(String permission) => permissions.contains(permission);

  /// 检查用户是否具有管理员角色
  bool get isAdmin => hasRole('ADMIN');

  /// 检查用户是否具有普通用户角色
  bool get isUser => hasRole('USER');

  /// 创建匿名用户
  static User get anonymous => User(
        id: 0,
        username: 'anonymous',
        email: 'anonymous@example.com',
        createdAt: DateTime.now(),
        enabled: false,
      );

  @override
  String toString() {
    return 'User(id: $id, username: $username, email: $email, displayName: $displayName)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is User && other.id == id;
  }

  @override
  int get hashCode => id.hashCode;
}

/// 认证响应模型
/// 包含登录/注册成功后返回的用户信息和token
@JsonSerializable(explicitToJson: true)
class AuthResponse {
  /// 用户信息
  final User user;

  /// 认证token
  final String token;

  /// token过期时间（秒）
  final int expiresIn;

  /// 刷新token
  final String? refreshToken;

  AuthResponse({
    required this.user,
    required this.token,
    required this.expiresIn,
    this.refreshToken,
  });

  /// 从JSON创建AuthResponse对象
  factory AuthResponse.fromJson(Map<String, dynamic> json) =>
      _$AuthResponseFromJson(json);

  /// 将AuthResponse对象转换为JSON
  Map<String, dynamic> toJson() => _$AuthResponseToJson(this);

  /// 计算token过期时间
  DateTime get expiresAt => DateTime.now().add(Duration(seconds: expiresIn));

  /// 检查token是否即将过期（在5分钟内过期）
  bool get isTokenExpiringSoon {
    final now = DateTime.now();
    final expiresAt = this.expiresAt;
    return expiresAt.difference(now).inMinutes < 5;
  }

  /// 检查token是否已过期
  bool get isTokenExpired => DateTime.now().isAfter(expiresAt);
}

/// 登录请求模型
@JsonSerializable()
class LoginRequest {
  /// 用户名或邮箱
  final String username;

  /// 密码
  final String password;

  /// 是否记住登录状态
  final bool rememberMe;

  LoginRequest({
    required this.username,
    required this.password,
    this.rememberMe = false,
  });

  /// 从JSON创建LoginRequest对象
  factory LoginRequest.fromJson(Map<String, dynamic> json) =>
      _$LoginRequestFromJson(json);

  /// 将LoginRequest对象转换为JSON
  Map<String, dynamic> toJson() => _$LoginRequestToJson(this);
}

/// 注册请求模型
@JsonSerializable()
class RegisterRequest {
  /// 用户名
  final String username;

  /// 邮箱地址
  final String email;

  /// 密码
  final String password;

  /// 确认密码
  final String confirmPassword;

  /// 手机号（可选）
  final String? phone;

  /// 昵称（可选）
  final String? nickname;

  RegisterRequest({
    required this.username,
    required this.email,
    required this.password,
    required this.confirmPassword,
    this.phone,
    this.nickname,
  });

  /// 从JSON创建RegisterRequest对象
  factory RegisterRequest.fromJson(Map<String, dynamic> json) =>
      _$RegisterRequestFromJson(json);

  /// 将RegisterRequest对象转换为JSON
  Map<String, dynamic> toJson() => _$RegisterRequestToJson(this);

  /// 验证密码是否匹配
  bool get isPasswordMatch => password == confirmPassword;
}

/// 密码重置请求模型
@JsonSerializable()
class ResetPasswordRequest {
  /// 邮箱地址
  final String email;

  /// 新密码
  final String newPassword;

  /// 确认新密码
  final String confirmNewPassword;

  /// 验证码
  final String verificationCode;

  ResetPasswordRequest({
    required this.email,
    required this.newPassword,
    required this.confirmNewPassword,
    required this.verificationCode,
  });

  /// 从JSON创建ResetPasswordRequest对象
  factory ResetPasswordRequest.fromJson(Map<String, dynamic> json) =>
      _$ResetPasswordRequestFromJson(json);

  /// 将ResetPasswordRequest对象转换为JSON
  Map<String, dynamic> toJson() => _$ResetPasswordRequestToJson(this);

  /// 验证密码是否匹配
  bool get isPasswordMatch => newPassword == confirmNewPassword;
}

/// 用户偏好设置模型
@JsonSerializable()
class UserPreferences {
  /// 主题设置
  final String theme;

  /// 语言设置
  final String language;

  /// 货币单位
  final String currency;

  /// 日期格式
  final String dateFormat;

  /// 时间格式
  final String timeFormat;

  /// 是否启用通知
  final bool notificationsEnabled;

  /// 是否启用生物识别认证
  final bool biometricAuthEnabled;

  /// 默认首页
  final String defaultHomePage;

  /// 数据备份频率
  final String backupFrequency;

  /// 导出格式偏好
  final String exportFormat;

  UserPreferences({
    this.theme = 'light',
    this.language = 'zh-CN',
    this.currency = 'CNY',
    this.dateFormat = 'yyyy-MM-dd',
    this.timeFormat = 'HH:mm',
    this.notificationsEnabled = true,
    this.biometricAuthEnabled = false,
    this.defaultHomePage = 'home',
    this.backupFrequency = 'weekly',
    this.exportFormat = 'excel',
  });

  /// 从JSON创建UserPreferences对象
  factory UserPreferences.fromJson(Map<String, dynamic> json) =>
      _$UserPreferencesFromJson(json);

  /// 将UserPreferences对象转换为JSON
  Map<String, dynamic> toJson() => _$UserPreferencesToJson(this);

  /// 创建默认偏好设置
  static UserPreferences get defaultPreferences => UserPreferences();
}