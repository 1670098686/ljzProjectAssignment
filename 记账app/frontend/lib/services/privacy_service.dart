import 'package:crypto/crypto.dart';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// 隐私设置服务
class PrivacyService {
  static final PrivacyService _instance = PrivacyService._internal();
  factory PrivacyService() => _instance;
  PrivacyService._internal();

  /// 获取隐私设置
  Future<PrivacySettings> getPrivacySettings() async {
    final prefs = await SharedPreferences.getInstance();
    return PrivacySettings(
      biometricEnabled: prefs.getBool('biometric_enabled') ?? false,
      dataEncryptionEnabled: prefs.getBool('data_encryption_enabled') ?? false,
      privacyMode: PrivacyMode.values[prefs.getInt('privacy_mode') ?? 0],
      showAmountInList: prefs.getBool('show_amount_in_list') ?? true,
      hideAmountInOverview: prefs.getBool('hide_amount_in_overview') ?? false,
      lockAppAfterMinutes: prefs.getInt('lock_app_after_minutes') ?? 5,
      enableAutoLock: prefs.getBool('enable_auto_lock') ?? true,
      allowAnalytics: prefs.getBool('allow_analytics') ?? false,
      allowCrashReports: prefs.getBool('allow_crash_reports') ?? false,
      allowPersonalization: prefs.getBool('allow_personalization') ?? true,
    );
  }

  /// 保存隐私设置
  Future<void> savePrivacySettings(PrivacySettings settings) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('biometric_enabled', settings.biometricEnabled);
    await prefs.setBool('data_encryption_enabled', settings.dataEncryptionEnabled);
    await prefs.setInt('privacy_mode', settings.privacyMode.index);
    await prefs.setBool('show_amount_in_list', settings.showAmountInList);
    await prefs.setBool('hide_amount_in_overview', settings.hideAmountInOverview);
    await prefs.setInt('lock_app_after_minutes', settings.lockAppAfterMinutes);
    await prefs.setBool('enable_auto_lock', settings.enableAutoLock);
    await prefs.setBool('allow_analytics', settings.allowAnalytics);
    await prefs.setBool('allow_crash_reports', settings.allowCrashReports);
    await prefs.setBool('allow_personalization', settings.allowPersonalization);
  }

  /// 启用生物识别验证
  Future<bool> enableBiometricAuth() async {
    // 这里应该调用具体的生物识别API
    // 暂时模拟实现
    return Future.value(true);
  }

  /// 禁用生物识别验证
  Future<void> disableBiometricAuth() async {
    // 清理生物识别相关的存储数据
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('biometric_token');
  }

  /// 验证生物识别
  Future<bool> verifyBiometric() async {
    // 这里应该调用具体的生物识别验证API
    // 暂时模拟实现
    return Future.value(true);
  }

  /// 启用数据加密
  Future<bool> enableDataEncryption(String password) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      // 使用SHA256对密码进行哈希处理（实际项目中应该使用更安全的方法）
      final bytes = utf8.encode(password);
      final digest = sha256.convert(bytes);
      final hashedPassword = digest.toString();
      
      // 保存加密标记和密码哈希
      await prefs.setBool('data_encryption_enabled', true);
      await prefs.setString('encryption_password_hash', hashedPassword);
      
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 验证加密密码
  Future<bool> verifyEncryptionPassword(String password) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final storedHash = prefs.getString('encryption_password_hash');
      
      if (storedHash == null) {
        return false;
      }
      
      final bytes = utf8.encode(password);
      final digest = sha256.convert(bytes);
      final hashedPassword = digest.toString();
      
      return storedHash == hashedPassword;
    } catch (e) {
      return false;
    }
  }

  /// 禁用数据加密
  Future<void> disableDataEncryption() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('data_encryption_enabled', false);
    await prefs.remove('encryption_password_hash');
  }

  /// 清除所有隐私数据
  Future<void> clearPrivacyData() async {
    final prefs = await SharedPreferences.getInstance();
    
    // 清除隐私相关的所有数据
    await prefs.remove('biometric_token');
    await prefs.remove('encryption_password_hash');
    await prefs.remove('app_locked');
    await prefs.remove('last_access_time');
    
    // 重置隐私设置为默认值
    await savePrivacySettings(PrivacySettings.defaultSettings());
  }

  /// 检查应用是否应该锁定
  Future<bool> shouldLockApp() async {
    final prefs = await SharedPreferences.getInstance();
    final settings = await getPrivacySettings();
    
    if (!settings.enableAutoLock) {
      return false;
    }
    
    final lastAccessTime = prefs.getInt('last_access_time');
    if (lastAccessTime == null) {
      await prefs.setInt('last_access_time', DateTime.now().millisecondsSinceEpoch);
      return false;
    }
    
    final elapsed = DateTime.now().millisecondsSinceEpoch - lastAccessTime;
    final lockTimeout = settings.lockAppAfterMinutes * 60 * 1000;
    
    return elapsed >= lockTimeout;
  }

  /// 更新最后访问时间
  Future<void> updateLastAccessTime() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('last_access_time', DateTime.now().millisecondsSinceEpoch);
  }

  /// 锁定应用
  Future<void> lockApp() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('app_locked', true);
  }

  /// 解锁应用
  Future<void> unlockApp() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('app_locked', false);
    await updateLastAccessTime();
  }

  /// 检查应用是否已锁定
  Future<bool> isAppLocked() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool('app_locked') ?? false;
  }
}

/// 隐私设置数据类
class PrivacySettings {
  final bool biometricEnabled;        // 启用生物识别验证
  final bool dataEncryptionEnabled;   // 启用数据加密
  final PrivacyMode privacyMode;      // 隐私模式
  final bool showAmountInList;        // 在列表中显示金额
  final bool hideAmountInOverview;    // 在概览中隐藏金额
  final int lockAppAfterMinutes;      // 锁定应用时间（分钟）
  final bool enableAutoLock;          // 启用自动锁定
  final bool allowAnalytics;          // 允许分析数据
  final bool allowCrashReports;       // 允许崩溃报告
  final bool allowPersonalization;    // 允许个性化

  const PrivacySettings({
    required this.biometricEnabled,
    required this.dataEncryptionEnabled,
    required this.privacyMode,
    required this.showAmountInList,
    required this.hideAmountInOverview,
    required this.lockAppAfterMinutes,
    required this.enableAutoLock,
    required this.allowAnalytics,
    required this.allowCrashReports,
    required this.allowPersonalization,
  });

  /// 默认设置
  factory PrivacySettings.defaultSettings() {
    return const PrivacySettings(
      biometricEnabled: false,
      dataEncryptionEnabled: false,
      privacyMode: PrivacyMode.normal,
      showAmountInList: true,
      hideAmountInOverview: false,
      lockAppAfterMinutes: 5,
      enableAutoLock: true,
      allowAnalytics: false,
      allowCrashReports: false,
      allowPersonalization: true,
    );
  }

  /// 更新设置
  PrivacySettings copyWith({
    bool? biometricEnabled,
    bool? dataEncryptionEnabled,
    PrivacyMode? privacyMode,
    bool? showAmountInList,
    bool? hideAmountInOverview,
    int? lockAppAfterMinutes,
    bool? enableAutoLock,
    bool? allowAnalytics,
    bool? allowCrashReports,
    bool? allowPersonalization,
  }) {
    return PrivacySettings(
      biometricEnabled: biometricEnabled ?? this.biometricEnabled,
      dataEncryptionEnabled: dataEncryptionEnabled ?? this.dataEncryptionEnabled,
      privacyMode: privacyMode ?? this.privacyMode,
      showAmountInList: showAmountInList ?? this.showAmountInList,
      hideAmountInOverview: hideAmountInOverview ?? this.hideAmountInOverview,
      lockAppAfterMinutes: lockAppAfterMinutes ?? this.lockAppAfterMinutes,
      enableAutoLock: enableAutoLock ?? this.enableAutoLock,
      allowAnalytics: allowAnalytics ?? this.allowAnalytics,
      allowCrashReports: allowCrashReports ?? this.allowCrashReports,
      allowPersonalization: allowPersonalization ?? this.allowPersonalization,
    );
  }

  /// 获取隐私模式显示文本
  String get privacyModeDisplayText {
    switch (privacyMode) {
      case PrivacyMode.strict:
        return '严格模式';
      case PrivacyMode.normal:
        return '正常模式';
      case PrivacyMode.open:
        return '开放模式';
    }
  }

  /// 获取隐私模式描述
  String get privacyModeDescription {
    switch (privacyMode) {
      case PrivacyMode.strict:
        return '最高级别隐私保护，隐藏所有敏感信息';
      case PrivacyMode.normal:
        return '平衡隐私保护，隐藏部分敏感信息';
      case PrivacyMode.open:
        return '最小隐私限制，显示所有信息';
    }
  }
}

/// 隐私模式枚举
enum PrivacyMode {
  strict,    // 严格模式
  normal,    // 正常模式
  open,      // 开放模式
}