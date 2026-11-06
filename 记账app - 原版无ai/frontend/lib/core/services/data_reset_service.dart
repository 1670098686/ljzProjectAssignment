import 'package:flutter/foundation.dart';

import '../database/database_service.dart';
import 'enhanced_data_persistence_service.dart';
import 'image_upload_service.dart';

/// Centralized data wipe service that clears SQLite, shared prefs, and media.
class DataResetService {
  DataResetService._internal();

  static final DataResetService _instance = DataResetService._internal();

  factory DataResetService() => _instance;

  final DatabaseService _databaseService = DatabaseService.instance;
  final EnhancedDataPersistenceService _persistenceService =
      EnhancedDataPersistenceService();
  final ImageUploadService _imageUploadService = ImageUploadService();

  /// 清除所有持久化数据并通知各个Provider刷新状态。
  Future<void> clearAllData() async {
    final failures = <String>[];

    try {
      await _databaseService.clearAllData();
    } catch (e, stackTrace) {
      debugPrint('Failed to clear database data: $e\n$stackTrace');
      failures.add('database records');
    }

    try {
      await _persistenceService.initialize();
      final cleared = await _persistenceService.clearAllData();
      if (!cleared) {
        failures.add('local preferences');
      }
    } catch (e, stackTrace) {
      debugPrint('Failed to clear shared preferences: $e\n$stackTrace');
      failures.add('local preferences');
    }

    try {
      await _imageUploadService.clearAllSavedImages();
    } catch (e, stackTrace) {
      debugPrint('Failed to clear bill images: $e\n$stackTrace');
      failures.add('bill images');
    }

    if (failures.isNotEmpty) {
      throw Exception('Unable to fully clear: ${failures.join(', ')}');
    }
  }
}
