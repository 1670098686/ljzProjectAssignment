import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:csv/csv.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../providers/bill_provider.dart';
import '../providers/saving_goal_provider.dart';

/// 本地数据导出服务类
/// 负责生成并导出本地数据到各种格式
class LocalDataExportService {
  final BillProvider _billProvider;
  final SavingGoalProvider _savingGoalProvider;

  LocalDataExportService(
    this._billProvider,
    this._savingGoalProvider,
  );

  /// 导出交易数据到CSV
  Future<ExportResult> exportTransactionsToCsv({
    DateTime? startDate,
    DateTime? endDate,
    int? type,
    String? category,
  }) async {
    try {
      // 获取交易数据
      final allTransactions = _billProvider.getAllBills();
      var transactions = allTransactions;

      // 应用筛选条件
      if (startDate != null) {
        transactions = transactions.where((t) =>
            DateTime.parse(t.transactionDate).isAfter(startDate)).toList();
      }
      if (endDate != null) {
        transactions = transactions.where((t) =>
            DateTime.parse(t.transactionDate).isBefore(endDate)).toList();
      }
      if (type != null) {
        transactions = transactions.where((t) => t.type == type).toList();
      }
      if (category != null && category.isNotEmpty) {
        transactions = transactions.where((t) =>
            t.categoryName == category).toList();
      }

      if (transactions.isEmpty) {
        return ExportResult(
          success: false,
          message: '没有找到符合条件的交易记录',
        );
      }

      // 生成CSV内容
      final List<List<dynamic>> csvData = [
        ['类型', '分类', '金额', '日期', '备注'],
        ...transactions.map((t) => [
          t.type == 1 ? '收入' : '支出',
          t.categoryName,
          t.amount.toStringAsFixed(2),
          t.transactionDate,
          t.remark ?? '',
        ]),
      ];

      final csv = const ListToCsvConverter().convert(csvData);
      final fileName = 'transactions_${DateFormat('yyyyMMdd_HHmmss').format(DateTime.now())}.csv';

      return ExportResult(
        success: true,
        data: utf8.encode(csv),
        fileName: fileName,
        message: '成功导出 ${transactions.length} 条交易记录',
      );
    } catch (e) {
      return ExportResult(
        success: false,
        message: '导出失败: $e',
      );
    }
  }

  // 预算功能已移除，预算导出方法已废弃
  Future<ExportResult> exportBudgetsToCsv({
    int? year,
    int? month,
  }) async {
    return ExportResult(
      success: false,
      message: '预算功能已移除，无法导出预算数据',
    );
  }

  /// 导出储蓄目标数据到CSV
  Future<ExportResult> exportSavingGoalsToCsv() async {
    try {
      final goals = await _savingGoalProvider.getAllSavingGoals();

      if (goals.isEmpty) {
        return ExportResult(
          success: false,
          message: '没有找到储蓄目标记录',
        );
      }

      final List<List<dynamic>> csvData = [
        ['目标名称', '目标金额', '当前金额', '截止日期', '描述', '完成进度'],
        ...goals.map((g) => [
          g.name,
          g.targetAmount.toStringAsFixed(2),
          g.currentAmount.toStringAsFixed(2),
          DateFormat('yyyy-MM-dd').format(g.deadline),
          g.description ?? '',
          '${(g.progress * 100).toStringAsFixed(1)}%',
        ]),
      ];

      final csv = const ListToCsvConverter().convert(csvData);
      final fileName = 'saving_goals_${DateFormat('yyyyMMdd_HHmmss').format(DateTime.now())}.csv';

      return ExportResult(
        success: true,
        data: utf8.encode(csv),
        fileName: fileName,
        message: '成功导出 ${goals.length} 条储蓄目标',
      );
    } catch (e) {
      return ExportResult(
        success: false,
        message: '导出失败: $e',
      );
    }
  }

  /// 导出交易数据到JSON
  Future<ExportResult> exportTransactionsToJson({
    DateTime? startDate,
    DateTime? endDate,
    int? type,
    String? category,
  }) async {
    try {
      final allTransactions = _billProvider.getAllBills();
      var transactions = allTransactions;

      // 应用筛选条件
      if (startDate != null) {
        transactions = transactions.where((t) =>
            DateTime.parse(t.transactionDate).isAfter(startDate)).toList();
      }
      if (endDate != null) {
        transactions = transactions.where((t) =>
            DateTime.parse(t.transactionDate).isBefore(endDate)).toList();
      }
      if (type != null) {
        transactions = transactions.where((t) => t.type == type).toList();
      }
      if (category != null && category.isNotEmpty) {
        transactions = transactions.where((t) =>
            t.categoryName == category).toList();
      }

      final jsonData = {
        'exportDate': DateTime.now().toIso8601String(),
        'totalCount': transactions.length,
        'filters': {
          'startDate': startDate?.toIso8601String(),
          'endDate': endDate?.toIso8601String(),
          'type': type,
          'category': category,
        },
        'transactions': transactions.map((t) => {
          'id': t.id,
          'type': t.type,
          'typeText': t.type == 1 ? '收入' : '支出',
          'categoryName': t.categoryName,
          'amount': t.amount,
          'transactionDate': t.transactionDate,
          'remark': t.remark,
        }).toList(),
      };

      final json = JsonEncoder.withIndent('  ').convert(jsonData);
      final fileName = 'transactions_${DateFormat('yyyyMMdd_HHmmss').format(DateTime.now())}.json';

      return ExportResult(
        success: true,
        data: utf8.encode(json),
        fileName: fileName,
        message: '成功导出 ${transactions.length} 条交易记录',
      );
    } catch (e) {
      return ExportResult(
        success: false,
        message: '导出失败: $e',
      );
    }
  }

  /// 批量导出所有数据到JSON
  Future<ExportResult> exportAllDataToJson() async {
    try {
      final transactions = _billProvider.getAllBills();
      final savingGoals = await _savingGoalProvider.getAllSavingGoals();

      final jsonData = {
        'exportDate': DateTime.now().toIso8601String(),
        'version': '1.0',
        'data': {
          'transactions': transactions.map((t) => {
            'id': t.id,
            'type': t.type,
            'categoryName': t.categoryName,
            'amount': t.amount,
            'transactionDate': t.transactionDate,
            'remark': t.remark,
          }).toList(),
          'savingGoals': savingGoals.map((g) => {
            'id': g.id,
            'name': g.name,
            'targetAmount': g.targetAmount,
            'currentAmount': g.currentAmount,
            'deadline': g.deadline.toIso8601String(),
            'description': g.description,
            'progress': g.progress,
          }).toList(),
        },
      };

      final json = JsonEncoder.withIndent('  ').convert(jsonData);
      final fileName = 'all_data_${DateFormat('yyyyMMdd_HHmmss').format(DateTime.now())}.json';

      return ExportResult(
        success: true,
        data: utf8.encode(json),
        fileName: fileName,
        message: '成功导出 ${transactions.length} 条交易、${savingGoals.length} 个目标',
      );
    } catch (e) {
      return ExportResult(
        success: false,
        message: '导出失败: $e',
      );
    }
  }

  /// 保存并分享文件
  Future<void> saveAndShareFile(ExportResult result) async {
    try {
      // 获取应用文档目录
      final directory = await getApplicationDocumentsDirectory();
      final filePath = '${directory.path}/${result.fileName}';
      final file = File(filePath);
      
      // 写入文件
      await file.writeAsBytes(result.data!);
      
      // 分享文件
      await SharePlus.instance.share(ShareParams(text: '分享导出的数据', files: [XFile(filePath)]));
      
      return;
    } catch (e) {
      throw Exception('保存或分享文件失败: $e');
    }
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