import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'unified_error_handling_service.dart';

/// 数据导出服务类
/// 负责与后端API对接，实现数据导出功能
class DataExportService {
  static const String _defaultBaseUrl = 'http://localhost:8080/api/export';

  final UnifiedErrorHandlingService _errorHandler;
  final http.Client _client;
  final String _baseUrl;

  DataExportService(
    this._errorHandler, {
    http.Client? httpClient,
    String? baseUrl,
  }) : _client = httpClient ?? http.Client(),
       _baseUrl = baseUrl ?? _defaultBaseUrl;

  /// 获取认证token
  Future<String?> _getAuthToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('auth_token');
  }

  /// 构建请求头
  Future<Map<String, String>> _buildHeaders() async {
    final token = await _getAuthToken();
    final headers = {'Content-Type': 'application/json'};

    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }

    return headers;
  }

  /// 导出交易数据
  Future<ExportResult> exportTransactions({
    String format = 'CSV',
    DateTime? startDate,
    DateTime? endDate,
    int? type,
    String? category,
  }) async {
    try {
      final headers = await _buildHeaders();
      final params = <String, String>{'format': format.toUpperCase()};

      if (startDate != null) {
        params['startDate'] = _formatDate(startDate);
      }
      if (endDate != null) {
        params['endDate'] = _formatDate(endDate);
      }
      if (type != null) {
        params['type'] = type.toString();
      }
      if (category != null) {
        params['category'] = category;
      }

      final uri = _buildExportUri('transactions', params);
      final response = await _client.get(uri, headers: headers);

      if (response.statusCode == 200) {
        return ExportResult(
          success: true,
          data: response.bodyBytes,
          fileName:
              'transactions_${DateTime.now().millisecondsSinceEpoch}.${format.toLowerCase()}',
          message: '交易数据导出成功',
        );
      } else {
        throw Exception('导出失败: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      _errorHandler.handleError('导出交易数据', e);
      return ExportResult(success: false, message: '导出失败: $e');
    }
  }

  /// 导出预算数据
  Future<ExportResult> exportBudgets({
    String format = 'CSV',
    int? year,
    int? month,
  }) async {
    try {
      final headers = await _buildHeaders();
      final params = <String, String>{'format': format.toUpperCase()};

      if (year != null) {
        params['year'] = year.toString();
      }
      if (month != null) {
        params['month'] = month.toString();
      }

      final uri = _buildExportUri('budgets', params);
      final response = await _client.get(uri, headers: headers);

      if (response.statusCode == 200) {
        return ExportResult(
          success: true,
          data: response.bodyBytes,
          fileName: 'budgets_${DateTime.now().millisecondsSinceEpoch}.${format.toLowerCase()}',
          message: '预算数据导出成功',
        );
      } else {
        throw Exception('导出失败: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      _errorHandler.handleError('导出预算数据', e);
      return ExportResult(success: false, message: '导出失败: $e');
    }
  }

  /// 导出储蓄目标数据
  Future<ExportResult> exportSavingGoals({String format = 'CSV'}) async {
    try {
      final headers = await _buildHeaders();
      final params = <String, String>{'format': format.toUpperCase()};

      final uri = _buildExportUri('saving-goals', params);
      final response = await _client.get(uri, headers: headers);

      if (response.statusCode == 200) {
        return ExportResult(
          success: true,
          data: response.bodyBytes,
          fileName:
              'saving_goals_${DateTime.now().millisecondsSinceEpoch}.${format.toLowerCase()}',
          message: '储蓄目标数据导出成功',
        );
      } else {
        throw Exception('导出失败: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      _errorHandler.handleError('导出储蓄目标数据', e);
      return ExportResult(success: false, message: '导出失败: $e');
    }
  }

  /// 导出统计报表数据
  Future<ExportResult> exportStatistics({
    String format = 'CSV',
    DateTime? startDate,
    DateTime? endDate,
    String granularity = 'daily',
  }) async {
    try {
      final headers = await _buildHeaders();
      final params = <String, String>{
        'format': format.toUpperCase(),
        'granularity': granularity,
      };

      if (startDate != null) {
        params['startDate'] = _formatDate(startDate);
      }
      if (endDate != null) {
        params['endDate'] = _formatDate(endDate);
      }

      final uri = _buildExportUri('statistics', params);
      final response = await _client.get(uri, headers: headers);

      if (response.statusCode == 200) {
        return ExportResult(
          success: true,
          data: response.bodyBytes,
          fileName:
              'statistics_${DateTime.now().millisecondsSinceEpoch}.${format.toLowerCase()}',
          message: '统计报表数据导出成功',
        );
      } else {
        throw Exception('导出失败: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      _errorHandler.handleError('导出统计报表数据', e);
      return ExportResult(success: false, message: '导出失败: $e');
    }
  }

  /// 批量导出所有数据
  Future<ExportResult> exportAllData({String format = 'ZIP'}) async {
    try {
      final headers = await _buildHeaders();
      final params = <String, String>{'format': format.toUpperCase()};

      final uri = _buildExportUri('all', params);
      final response = await _client.get(uri, headers: headers);

      if (response.statusCode == 200) {
        return ExportResult(
          success: true,
          data: response.bodyBytes,
          fileName: 'finance_data_${DateTime.now().millisecondsSinceEpoch}.zip',
          message: '全部数据导出成功',
        );
      } else {
        throw Exception('导出失败: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      _errorHandler.handleError('导出全部数据', e);
      return ExportResult(success: false, message: '导出失败: $e');
    }
  }

  /// 格式化日期为字符串
  String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  /// 构建导出接口 URI
  Uri _buildExportUri(String path, Map<String, String> params) {
    return Uri.parse('$_baseUrl/$path').replace(queryParameters: params);
  }
}

/// 导出结果类
class ExportResult {
  final bool success;
  final Uint8List? data;
  final String? fileName;
  final String message;

  ExportResult({
    required this.success,
    this.data,
    this.fileName,
    required this.message,
  });
}
