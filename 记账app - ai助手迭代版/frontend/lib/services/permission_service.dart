import 'dart:io';

import 'package:permission_handler/permission_handler.dart';

/// 权限管理服务类
class PermissionService {
  /// 申请所有必要权限
  static Future<Map<String, bool>> requestAllPermissions() async {
    final results = <String, bool>{};

    try {
      final storageGranted = await _ensureStoragePermission();
      results['storage'] = storageGranted;

      // 日期权限不需要特殊申请，直接返回true
      results['date'] = true;

      // 文件权限使用存储权限替代
      results['file'] = results['storage']!;

      print('权限请求结果: $results');
    } catch (e) {
      print('申请权限失败: $e');
      results['storage'] = false;
      results['date'] = true;
      results['file'] = false;
    }

    return results;
  }

  static Future<bool> _ensureStoragePermission() async {
    try {
      print('开始检查存储权限...');
      if (!Platform.isAndroid) {
        print('非Android平台，直接返回true');
        return true;
      }

      // 直接请求存储权限，不进行复杂的Android版本检查
      // permission_handler库会自动处理不同Android版本的权限映射
      print('请求存储权限...');
      final status = await Permission.storage.request();
      print('存储权限请求结果: $status');
      
      // 直接返回权限状态
      return status.isGranted;
    } catch (e) {
      print('存储权限检查失败: $e');
      return false;
    }
  }

  /// 打开应用设置页面
  static Future<void> openAppSettingsPage() async {
    try {
      await openAppSettings();
      print('已打开应用设置页面');
    } catch (e) {
      print('打开应用设置页面失败: $e');
    }
  }
}
