import 'dart:async';
import 'dart:developer' as developer;
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

/// 图片优化服务
/// 提供图片加载、缓存、压缩、预加载等功能
class ImageOptimizationService {
  static final ImageOptimizationService _instance = ImageOptimizationService._internal();
  factory ImageOptimizationService() => _instance;
  ImageOptimizationService._internal();

  // 图片缓存管理
  final Map<String, Uint8List> _memoryCache = {};
  final Map<String, Timer> _preloadTimers = {};
  final Set<String> _loadingImages = {};

  // 配置参数
  static const int _maxMemoryCacheSize = 50; // 最大内存缓存数量
  static const int _maxMemoryCacheBytes = 50 * 1024 * 1024; // 50MB
  static const Duration _cacheExpirationTime = Duration(hours: 24); // 缓存过期时间

  // 当前内存使用统计
  int _currentMemoryUsage = 0;

  /// 初始化图片优化服务
  Future<void> initialize() async {
    developer.log('图片优化服务初始化开始', name: 'ImageOptimizationService');

    // 请求存储权限
    await _requestStoragePermission();

    // 清理过期的本地缓存
    await _cleanExpiredCache();

    // 设置内存监控
    _setupMemoryMonitoring();

    developer.log('图片优化服务初始化完成', name: 'ImageOptimizationService');
  }

  /// 请求存储权限
  Future<void> _requestStoragePermission() async {
    if (!kIsWeb) {
      await [
        Permission.storage,
        Permission.manageExternalStorage,
      ].request();
    }
  }

  /// 清理过期的本地缓存
  Future<void> _cleanExpiredCache() async {
    try {
      final cacheDir = await getTemporaryDirectory();
      final cachePath = '${cacheDir.path}/cached_network_image';
      final directory = Directory(cachePath);

      if (await directory.exists()) {
        final files = await directory.list().toList();
        final now = DateTime.now();

        for (final file in files) {
          if (file is File) {
            final stat = await file.stat();
            final modified = stat.modified;
            if (now.difference(modified) > _cacheExpirationTime) {
              await file.delete();
              developer.log('清理过期缓存文件: ${file.path}', name: 'ImageOptimizationService');
            }
          }
        }
      }
    } catch (e) {
      developer.log('清理缓存失败: $e', name: 'ImageOptimizationService');
    }
  }

  /// 设置内存监控
  void _setupMemoryMonitoring() {
    // 定期清理内存缓存
    Timer.periodic(Duration(minutes: 5), (_) {
      _cleanMemoryCache();
    });
  }

  /// 优化网络图片加载
  Widget buildOptimizedNetworkImage({
    required String imageUrl,
    double? width,
    double? height,
    BoxFit fit = BoxFit.cover,
    Widget? placeholder,
    Widget? errorWidget,
    int? memCacheWidth,
    int? memCacheHeight,
    Duration fadeInDuration = const Duration(milliseconds: 300),
  }) {
    if (imageUrl.isEmpty) {
      return errorWidget ?? _buildDefaultErrorWidget(width, height);
    }

    // 计算缓存尺寸（支持高DPI显示）
    final cacheWidth = memCacheWidth ?? (width != null ? (width! * 2).toInt() : null);
    final cacheHeight = memCacheHeight ?? (height != null ? (height! * 2).toInt() : null);

    return CachedNetworkImage(
      imageUrl: imageUrl,
      width: width,
      height: height,
      fit: fit,
      placeholder: (context, url) =>
          placeholder ?? _buildDefaultPlaceholder(width, height),
      errorWidget: (context, url, error) =>
          errorWidget ?? _buildDefaultErrorWidget(width, height),
      fadeInDuration: fadeInDuration,
      memCacheWidth: cacheWidth,
      memCacheHeight: cacheHeight,
      maxWidthDiskCache: cacheWidth,
      maxHeightDiskCache: cacheHeight,
      progressIndicatorBuilder: (context, url, progress) {
        return Center(
          child: SizedBox(
            width: width,
            height: height,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                CircularProgressIndicator(
                  value: progress.progress,
                  strokeWidth: 2,
                ),
                SizedBox(height: 8),
                Text(
                  '${(progress.progress * 100).toInt()}%',
                  style: TextStyle(fontSize: 12),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  /// 构建默认占位符
  Widget _buildDefaultPlaceholder(double? width, double? height) {
    return Container(
      width: width,
      height: height,
      color: Colors.grey[300],
      child: Center(
        child: CircularProgressIndicator(
          strokeWidth: 2,
          valueColor: AlwaysStoppedAnimation<Color>(Colors.blue),
        ),
      ),
    );
  }

  /// 构建默认错误组件
  Widget _buildDefaultErrorWidget(double? width, double? height) {
    return Container(
      width: width,
      height: height,
      color: Colors.grey[200],
      child: Icon(
        Icons.broken_image,
        size: width != null ? width! / 4 : 24,
        color: Colors.grey[600],
      ),
    );
  }

  /// 预加载图片
  Future<void> preloadImage(String imageUrl) async {
    if (_loadingImages.contains(imageUrl)) {
      return; // 已经在加载中
    }

    _loadingImages.add(imageUrl);

    try {
      final image = CachedNetworkImageProvider(imageUrl);
      await image.resolve(ImageConfiguration.empty);

      // 将图片添加到内存缓存
      final bytes = await _getImageBytes(imageUrl);
      if (bytes != null) {
        _addToMemoryCache(imageUrl, bytes);
      }

      developer.log('图片预加载成功: $imageUrl', name: 'ImageOptimizationService');
    } catch (e) {
      developer.log('图片预加载失败: $imageUrl, 错误: $e', name: 'ImageOptimizationService');
    } finally {
      _loadingImages.remove(imageUrl);
    }
  }

  /// 获取图片字节数据
  Future<Uint8List?> _getImageBytes(String imageUrl) async {
    try {
      // 这里可以实现具体的图片下载和缓存逻辑
      // 例如使用 http 包下载图片
      return null;
    } catch (e) {
      developer.log('获取图片字节失败: $imageUrl, 错误: $e', name: 'ImageOptimizationService');
      return null;
    }
  }

  /// 添加到内存缓存
  void _addToMemoryCache(String key, Uint8List data) {
    // 检查缓存大小限制
    if (_memoryCache.length >= _maxMemoryCacheSize) {
      _removeOldestCache();
    }

    // 检查内存使用限制
    if (_currentMemoryUsage + data.length > _maxMemoryCacheBytes) {
      _cleanMemoryCache();
    }

    _memoryCache[key] = data;
    _currentMemoryUsage += data.length;
  }

  /// 移除最旧的缓存
  void _removeOldestCache() {
    if (_memoryCache.isNotEmpty) {
      final oldestKey = _memoryCache.keys.first;
      final data = _memoryCache[oldestKey];
      _memoryCache.remove(oldestKey);
      if (data != null) {
        _currentMemoryUsage -= data.length;
      }
    }
  }

  /// 清理内存缓存
  void _cleanMemoryCache() {
    _memoryCache.clear();
    _currentMemoryUsage = 0;
    developer.log('内存缓存已清理', name: 'ImageOptimizationService');
  }

  /// 批量预加载图片
  Future<void> preloadImages(List<String> imageUrls) async {
    if (imageUrls.isEmpty) return;

    // 分批处理，避免同时加载过多图片
    const batchSize = 5;
    for (int i = 0; i < imageUrls.length; i += batchSize) {
      final batch = imageUrls.sublist(
        i,
        i + batchSize > imageUrls.length ? imageUrls.length : i + batchSize,
      );

      await Future.wait(batch.map((url) => preloadImage(url)));

      // 在批次之间稍作延迟
      if (i + batchSize < imageUrls.length) {
        await Future.delayed(Duration(milliseconds: 100));
      }
    }

    developer.log('批量预加载完成: ${imageUrls.length} 张图片', name: 'ImageOptimizationService');
  }

  /// 智能预加载（根据使用模式）
  void smartPreload({
    required List<String> priorityImages,
    List<String>? secondaryImages,
    Duration delay = const Duration(milliseconds: 500),
  }) {
    // 立即预加载高优先级图片
    for (final imageUrl in priorityImages) {
      preloadImage(imageUrl);
    }

    // 延迟预加载次要图片
    if (secondaryImages != null && secondaryImages.isNotEmpty) {
      final timerKey = 'secondary_${DateTime.now().millisecondsSinceEpoch}';
      _preloadTimers[timerKey] = Timer(delay, () {
        preloadImages(secondaryImages!);
        _preloadTimers.remove(timerKey);
      });
    }
  }

  /// 压缩图片
  Future<Uint8List?> compressImage(
    Uint8List imageData, {
    int quality = 80,
    int? maxWidth,
    int? maxHeight,
  }) async {
    try {
      final codec = await ui.instantiateImageCodec(
        imageData,
        targetWidth: maxWidth,
        targetHeight: maxHeight,
      );
      
      final frame = await codec.getNextFrame();
      final byteData = await frame.image.toByteData(format: ui.ImageByteFormat.png);
      
      if (byteData != null) {
        return byteData.buffer.asUint8List();
      }
    } catch (e) {
      developer.log('图片压缩失败: $e', name: 'ImageOptimizationService');
    }
    
    return null;
  }

  /// 获取缓存统计信息
  Map<String, dynamic> getCacheStatistics() {
    return {
      'memory_cache_count': _memoryCache.length,
      'memory_usage_bytes': _currentMemoryUsage,
      'memory_usage_mb': (_currentMemoryUsage / 1024 / 1024).toStringAsFixed(2),
      'loading_images_count': _loadingImages.length,
      'preload_timers_count': _preloadTimers.length,
    };
  }

  /// 清理所有资源
  void dispose() {
    // 取消所有预加载定时器
    for (final timer in _preloadTimers.values) {
      timer.cancel();
    }
    _preloadTimers.clear();

    // 清理内存缓存
    _cleanMemoryCache();

    _loadingImages.clear();

    developer.log('图片优化服务已释放', name: 'ImageOptimizationService');
  }
}

/// 图片优化相关的工具方法
class ImageOptimizationUtils {
  /// 计算适合缓存的图片尺寸
  static (int? width, int? height) calculateCacheSize({
    required double displayWidth,
    required double displayHeight,
    required double devicePixelRatio,
  }) {
    final cacheWidth = (displayWidth * devicePixelRatio * 2).toInt();
    final cacheHeight = (displayHeight * devicePixelRatio * 2).toInt();
    
    return (cacheWidth, cacheHeight);
  }

  /// 判断是否需要高清图片
  static bool shouldUseHighResolution({
    required double devicePixelRatio,
    required bool isDetailView,
  }) {
    return devicePixelRatio > 2.0 || isDetailView;
  }

  /// 生成图片缩略图URL
  static String generateThumbnailUrl(String originalUrl, {
    int width = 200,
    int height = 200,
  }) {
    // 这里可以根据实际的后端API来生成缩略图URL
    // 例如：添加缩略图参数
    if (originalUrl.contains('?')) {
      return '$originalUrl&thumbnail=${width}x$height';
    } else {
      return '$originalUrl?thumbnail=${width}x$height';
    }
  }
}