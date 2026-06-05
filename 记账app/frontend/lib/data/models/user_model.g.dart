// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

User _$UserFromJson(Map<String, dynamic> json) => User(
  id: (json['id'] as num).toInt(),
  username: json['username'] as String,
  email: json['email'] as String,
  phone: json['phone'] as String?,
  avatar: json['avatar'] as String?,
  nickname: json['nickname'] as String?,
  createdAt: DateTime.parse(json['createdAt'] as String),
  lastLoginAt: json['lastLoginAt'] == null
      ? null
      : DateTime.parse(json['lastLoginAt'] as String),
  enabled: json['enabled'] as bool,
  roles:
      (json['roles'] as List<dynamic>?)?.map((e) => e as String).toList() ??
      const [],
  permissions:
      (json['permissions'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList() ??
      const [],
);

Map<String, dynamic> _$UserToJson(User instance) => <String, dynamic>{
  'id': instance.id,
  'username': instance.username,
  'email': instance.email,
  'phone': instance.phone,
  'avatar': instance.avatar,
  'nickname': instance.nickname,
  'createdAt': instance.createdAt.toIso8601String(),
  'lastLoginAt': instance.lastLoginAt?.toIso8601String(),
  'enabled': instance.enabled,
  'roles': instance.roles,
  'permissions': instance.permissions,
};

AuthResponse _$AuthResponseFromJson(Map<String, dynamic> json) => AuthResponse(
  user: User.fromJson(json['user'] as Map<String, dynamic>),
  token: json['token'] as String,
  expiresIn: (json['expiresIn'] as num).toInt(),
  refreshToken: json['refreshToken'] as String?,
);

Map<String, dynamic> _$AuthResponseToJson(AuthResponse instance) =>
    <String, dynamic>{
      'user': instance.user.toJson(),
      'token': instance.token,
      'expiresIn': instance.expiresIn,
      'refreshToken': instance.refreshToken,
    };

LoginRequest _$LoginRequestFromJson(Map<String, dynamic> json) => LoginRequest(
  username: json['username'] as String,
  password: json['password'] as String,
  rememberMe: json['rememberMe'] as bool? ?? false,
);

Map<String, dynamic> _$LoginRequestToJson(LoginRequest instance) =>
    <String, dynamic>{
      'username': instance.username,
      'password': instance.password,
      'rememberMe': instance.rememberMe,
    };

RegisterRequest _$RegisterRequestFromJson(Map<String, dynamic> json) =>
    RegisterRequest(
      username: json['username'] as String,
      email: json['email'] as String,
      password: json['password'] as String,
      confirmPassword: json['confirmPassword'] as String,
      phone: json['phone'] as String?,
      nickname: json['nickname'] as String?,
    );

Map<String, dynamic> _$RegisterRequestToJson(RegisterRequest instance) =>
    <String, dynamic>{
      'username': instance.username,
      'email': instance.email,
      'password': instance.password,
      'confirmPassword': instance.confirmPassword,
      'phone': instance.phone,
      'nickname': instance.nickname,
    };

ResetPasswordRequest _$ResetPasswordRequestFromJson(
  Map<String, dynamic> json,
) => ResetPasswordRequest(
  email: json['email'] as String,
  newPassword: json['newPassword'] as String,
  confirmNewPassword: json['confirmNewPassword'] as String,
  verificationCode: json['verificationCode'] as String,
);

Map<String, dynamic> _$ResetPasswordRequestToJson(
  ResetPasswordRequest instance,
) => <String, dynamic>{
  'email': instance.email,
  'newPassword': instance.newPassword,
  'confirmNewPassword': instance.confirmNewPassword,
  'verificationCode': instance.verificationCode,
};

UserPreferences _$UserPreferencesFromJson(Map<String, dynamic> json) =>
    UserPreferences(
      theme: json['theme'] as String? ?? 'light',
      language: json['language'] as String? ?? 'zh-CN',
      currency: json['currency'] as String? ?? 'CNY',
      dateFormat: json['dateFormat'] as String? ?? 'yyyy-MM-dd',
      timeFormat: json['timeFormat'] as String? ?? 'HH:mm',
      notificationsEnabled: json['notificationsEnabled'] as bool? ?? true,
      biometricAuthEnabled: json['biometricAuthEnabled'] as bool? ?? false,
      defaultHomePage: json['defaultHomePage'] as String? ?? 'home',
      backupFrequency: json['backupFrequency'] as String? ?? 'weekly',
      exportFormat: json['exportFormat'] as String? ?? 'excel',
    );

Map<String, dynamic> _$UserPreferencesToJson(UserPreferences instance) =>
    <String, dynamic>{
      'theme': instance.theme,
      'language': instance.language,
      'currency': instance.currency,
      'dateFormat': instance.dateFormat,
      'timeFormat': instance.timeFormat,
      'notificationsEnabled': instance.notificationsEnabled,
      'biometricAuthEnabled': instance.biometricAuthEnabled,
      'defaultHomePage': instance.defaultHomePage,
      'backupFrequency': instance.backupFrequency,
      'exportFormat': instance.exportFormat,
    };
