import 'dart:io';
import 'dart:convert';
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';

/// 备份文件信息类
class BackupFileInfo {
  final String fileName;
  final DateTime createdAt;
  final int fileSize;
  final String filePath;

  BackupFileInfo({
    required this.fileName,
    required this.createdAt,
    required this.fileSize,
    required this.filePath,
  });

  Map<String, dynamic> toJson() {
    return {
      'fileName': fileName,
      'createdAt': createdAt.millisecondsSinceEpoch,
      'fileSize': fileSize,
      'filePath': filePath,
    };
  }

  factory BackupFileInfo.fromJson(Map<String, dynamic> json) {
    return BackupFileInfo(
      fileName: json['fileName'] as String,
      createdAt: DateTime.fromMillisecondsSinceEpoch(json['createdAt'] as int),
      fileSize: json['fileSize'] as int,
      filePath: json['filePath'] as String,
    );
  }

  @override
  String toString() {
    return 'BackupFileInfo{name: $fileName, size: ${(fileSize / 1024).toStringAsFixed(2)}KB}';
  }
}

/// 备份恢复操作结果类
class BackupResult {
  final bool success;
  final String message;
  final String? filePath;

  BackupResult({
    required this.success,
    required this.message,
    this.filePath,
  });
}

/// 本地备份服务类
class LocalBackupService {
  static const String _backupDirName = 'backups';

  /// 获取备份目录路径
  Future<String> get _backupDirectoryPath async {
    final directory = await getApplicationDocumentsDirectory();
    return path.join(directory.path, _backupDirName);
  }

  /// 确保备份目录存在
  Future<void> _ensureBackupDirectoryExists() async {
    final backupPath = await _backupDirectoryPath;
    final directory = Directory(backupPath);
    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }
  }

  /// 创建备份
  Future<BackupResult> createBackup(Map<String, dynamic> data, {
    String? customFileName,
  }) async {
    try {
      await _ensureBackupDirectoryExists();

      final fileName = customFileName ?? _generateBackupFileName();
      final backupPath = await _backupDirectoryPath;
      final filePath = path.join(backupPath, fileName);

      // 准备备份数据并写入文件
      {
        final backupData = {
          'version': '1.0',
          'createdAt': DateTime.now().millisecondsSinceEpoch,
          'data': data,
        };

        // 转换为JSON并写入文件
        final jsonString = jsonEncode(backupData);
        final file = File(filePath);
        await file.writeAsString(jsonString);
        
        return BackupResult(
          success: true,
          message: '备份创建成功',
          filePath: filePath,
        );
      }
    } catch (e) {
      return BackupResult(
        success: false,
        message: '创建备份失败: $e',
      );
    }
  }

  /// 恢复备份
  Future<BackupResult> restoreBackup(String filePath) async {
    try {
      final file = File(filePath);
      if (!await file.exists()) {
        return BackupResult(
          success: false,
          message: '备份文件不存在',
        );
      }

      final content = await file.readAsString();
      jsonDecode(content); // 解析JSON数据以验证格式

      return BackupResult(
        success: true,
        message: '备份恢复成功',
      );
    } catch (e) {
      return BackupResult(
        success: false,
        message: '恢复备份失败: $e',
      );
    }
  }

  /// 获取所有备份文件列表
  Future<List<BackupFileInfo>> getBackupFiles() async {
    await _ensureBackupDirectoryExists();

    final backupPath = await _backupDirectoryPath;
    final directory = Directory(backupPath);

    if (!await directory.exists()) {
      return [];
    }

    final files = await directory.list().where((entity) => 
      entity is File && entity.path.endsWith('.backup')
    ).toList();

    final backupFiles = <BackupFileInfo>[];

    for (final file in files) {
      if (file is File) {
        final stat = await file.stat();
        final fileName = path.basename(file.path);
        
        backupFiles.add(BackupFileInfo(
          fileName: fileName,
          createdAt: stat.modified,
          fileSize: stat.size,
          filePath: file.path,
        ));
      }
    }

    // 按创建时间倒序排列
    backupFiles.sort((a, b) => b.createdAt.compareTo(a.createdAt));
    return backupFiles;
  }

  /// 删除备份文件
  Future<BackupResult> deleteBackupFile(String filePath) async {
    try {
      final file = File(filePath);
      if (await file.exists()) {
        await file.delete();
        return BackupResult(
          success: true,
          message: '备份文件删除成功',
        );
      } else {
        return BackupResult(
          success: false,
          message: '备份文件不存在',
        );
      }
    } catch (e) {
      return BackupResult(
        success: false,
        message: '删除备份文件失败: $e',
      );
    }
  }

  /// 生成备份文件名
  String _generateBackupFileName() {
    final now = DateTime.now();
    final timestamp = '${now.year}${now.month.toString().padLeft(2, '0')}${now.day.toString().padLeft(2, '0')}_${now.hour.toString().padLeft(2, '0')}${now.minute.toString().padLeft(2, '0')}${now.second.toString().padLeft(2, '0')}';
    return 'backup_$timestamp.backup';
  }
}