import 'dart:io';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as path;

/// 图片上传服务类
class ImageUploadService {
  static final ImageUploadService _instance = ImageUploadService._internal();
  final ImagePicker _imagePicker = ImagePicker();

  ImageUploadService._internal();

  factory ImageUploadService() {
    return _instance;
  }

  bool _isPhotoPermissionGranted(
    PermissionStatus status, {
    bool allowLimitedAccess = false,
  }) {
    if (status.isGranted) {
      return true;
    }
    if (allowLimitedAccess && status == PermissionStatus.limited) {
      return true;
    }
    return false;
  }

  Future<bool> _requestPhotosPermission({
    bool allowLimitedAccess = false,
  }) async {
    try {
      final status = await Permission.photos.status;
      if (_isPhotoPermissionGranted(
        status,
        allowLimitedAccess: allowLimitedAccess,
      )) {
        return true;
      }

      if (status.isPermanentlyDenied) {
        throw Exception('相册权限被永久拒绝，请在设置中手动授权');
      }

      final newStatus = await Permission.photos.request();
      if (_isPhotoPermissionGranted(
        newStatus,
        allowLimitedAccess: allowLimitedAccess,
      )) {
        return true;
      }

      if (newStatus.isPermanentlyDenied) {
        throw Exception('相册权限被永久拒绝，请在设置中手动授权');
      }

      return false;
    } on UnimplementedError catch (e) {
      print('当前平台不支持 photos 权限: $e');
      return false;
    } catch (e) {
      print('处理相册权限失败: $e');
      rethrow;
    }
  }

  /// 检查并请求相机权限
  Future<bool> _checkCameraPermission() async {
    try {
      final status = await Permission.camera.status;
      if (status.isGranted) {
        return true;
      }
      if (status.isPermanentlyDenied) {
        throw Exception('相机权限被永久拒绝，请在设置中手动授权');
      }
      final newStatus = await Permission.camera.request();
      return newStatus.isGranted;
    } catch (e) {
      print('相机权限检查失败: $e');
      rethrow;
    }
  }

  /// 检查并请求相册权限
  Future<bool> _checkGalleryPermission() async {
    try {
      if (Platform.isIOS) {
        return await _requestPhotosPermission(allowLimitedAccess: true);
      }

      if (Platform.isAndroid) {
        final hasPhotosPermission = await _requestPhotosPermission();
        if (hasPhotosPermission) {
          return true;
        }

        // Android 12 及以下使用存储权限作为兜底方案
        return await _checkStoragePermission();
      }

      return true;
    } catch (e) {
      print('相册权限检查失败: $e');
      rethrow;
    }
  }

  /// 检查并请求存储权限
  Future<bool> _checkStoragePermission() async {
    try {
      if (!Platform.isAndroid) {
        return true; // iOS 不需要存储权限
      }

      final status = await Permission.storage.status;
      if (status.isGranted) {
        return true;
      }
      if (status.isPermanentlyDenied) {
        throw Exception('存储权限被永久拒绝，请在设置中手动授权');
      }
      final newStatus = await Permission.storage.request();
      return newStatus.isGranted;
    } catch (e) {
      print('存储权限检查失败: $e');
      rethrow;
    }
  }

  /// 从相机拍照
  Future<File?> takePhoto() async {
    try {
      final hasCameraPermission = await _checkCameraPermission();
      if (!hasCameraPermission) {
        throw Exception('相机权限被拒绝');
      }

      final hasStoragePermission = await _checkStoragePermission();
      if (!hasStoragePermission) {
        throw Exception('存储权限被拒绝');
      }

      final XFile? pickedFile = await _imagePicker.pickImage(
        source: ImageSource.camera,
        maxWidth: 1920,
        maxHeight: 1080,
        imageQuality: 80,
      );

      if (pickedFile != null) {
        return File(pickedFile.path);
      }
      return null;
    } catch (e) {
      print('拍照失败: $e');
      rethrow;
    }
  }

  /// 从相册选择图片
  Future<File?> pickImageFromGallery() async {
    try {
      print('开始检查相册权限...');
      final hasGalleryPermission = await _checkGalleryPermission();
      if (!hasGalleryPermission) {
        throw Exception('相册权限被拒绝，无法访问相册');
      }

      print('开始检查存储权限...');
      final hasStoragePermission = await _checkStoragePermission();
      if (!hasStoragePermission) {
        throw Exception('存储权限被拒绝，无法保存图片');
      }

      print('权限检查通过，开始选择图片...');
      final XFile? pickedFile = await _imagePicker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1920,
        maxHeight: 1080,
        imageQuality: 80,
      );

      if (pickedFile != null) {
        print('图片选择成功: ${pickedFile.path}');
        return File(pickedFile.path);
      }
      print('用户取消了图片选择');
      return null;
    } catch (e) {
      print('选择图片失败: $e');

      // 提供更友好的错误信息
      if (e.toString().contains('永久拒绝')) {
        throw Exception('权限被永久拒绝，请在手机设置中为应用授权相册和存储权限');
      } else if (e.toString().contains('权限被拒绝')) {
        throw Exception('权限被拒绝，请授权应用访问相册和存储');
      }

      rethrow;
    }
  }

  /// 保存图片到应用目录
  Future<String> saveImageToAppDirectory(File imageFile) async {
    try {
      final appDir = await getApplicationDocumentsDirectory();
      final imagesDir = Directory(path.join(appDir.path, 'bill_images'));

      if (!await imagesDir.exists()) {
        await imagesDir.create(recursive: true);
      }

      // 生成唯一文件名
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final extension = path.extension(imageFile.path);
      final fileName = 'bill_$timestamp$extension';

      final newPath = path.join(imagesDir.path, fileName);
      final newFile = await imageFile.copy(newPath);

      return newFile.path;
    } catch (e) {
      print('保存图片失败: $e');
      rethrow;
    }
  }

  /// 删除图片文件
  Future<void> deleteImageFile(String? imagePath) async {
    if (imagePath == null || imagePath.isEmpty) return;

    try {
      final file = File(imagePath);
      if (await file.exists()) {
        await file.delete();
      }
    } catch (e) {
      print('删除图片文件失败: $e');
      // 不抛出异常，避免影响主要业务逻辑
    }
  }

  /// 获取图片文件
  Future<File?> getImageFile(String? imagePath) async {
    if (imagePath == null || imagePath.isEmpty) return null;

    try {
      final file = File(imagePath);
      if (await file.exists()) {
        return file;
      }
      return null;
    } catch (e) {
      print('获取图片文件失败: $e');
      return null;
    }
  }

  /// 检查图片文件是否存在
  Future<bool> imageFileExists(String? imagePath) async {
    if (imagePath == null || imagePath.isEmpty) return false;

    try {
      final file = File(imagePath);
      return await file.exists();
    } catch (e) {
      print('检查图片文件失败: $e');
      return false;
    }
  }

  /// 清除所有已保存的账单图片
  Future<void> clearAllSavedImages() async {
    try {
      final appDir = await getApplicationDocumentsDirectory();
      final imagesDir = Directory(path.join(appDir.path, 'bill_images'));
      if (await imagesDir.exists()) {
        await imagesDir.delete(recursive: true);
        print('已清除所有账单图片文件');
      }
    } catch (e) {
      print('清除账单图片文件失败: $e');
    }
  }

  /// 清理临时图片文件
  Future<void> cleanupTempFiles() async {
    try {
      final tempDir = await getTemporaryDirectory();
      final files = tempDir.listSync();

      for (final file in files) {
        if (file is File) {
          final fileName = path.basename(file.path);
          if (fileName.startsWith('image_picker_')) {
            await file.delete();
          }
        }
      }
    } catch (e) {
      print('清理临时文件失败: $e');
      // 不抛出异常
    }
  }
}
