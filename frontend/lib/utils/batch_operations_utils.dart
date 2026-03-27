import 'dart:convert';
import '../data/models/bill_model.dart';

/// 批量操作工具类
class BatchOperationsUtils {
  /// 批量删除记录
  static Future<List<Bill>> batchDeleteBills(
    List<Bill> bills,
  ) async {
    // 这里实际删除逻辑需要在Provider或Service层处理
    // 这里只返回处理后的列表（已删除的记录）
    return bills;
  }

  /// 批量修改分类
  static List<Bill> batchChangeCategory(
    List<Bill> bills,
    String newCategory,
  ) {
    return bills.map((bill) {
      return bill.copyWith(categoryName: newCategory);
    }).toList();
  }

  /// 批量添加备注
  static List<Bill> batchAddRemark(
    List<Bill> bills,
    String remark,
  ) {
    return bills.map((bill) {
      final currentRemark = bill.remark ?? '';
      final newRemark = remark.isEmpty 
        ? currentRemark 
        : '$currentRemark${currentRemark.isEmpty ? '' : '\n'}$remark';
      
      return bill.copyWith(remark: newRemark);
    }).toList();
  }

  /// 导出数据为CSV格式
  static Future<String> exportToCsv(List<Bill> bills) async {
    final buffer = StringBuffer();
    
    // CSV头部
    buffer.writeln('ID,类型,分类,金额,备注,日期');
    
    // 数据行
    for (final bill in bills) {
      final typeText = bill.type == 1 ? '收入' : '支出';
      final amountText = bill.amount.toStringAsFixed(2);
      
      // 处理备注中的逗号和换行符
      final remarkText = (bill.remark ?? '')
          .replaceAll(',', '，')
          .replaceAll('\n', ' ')
          .replaceAll('\r', ' ');
      
      buffer.writeln(
        '${bill.id},$typeText,${bill.categoryName},$amountText,$remarkText,${bill.transactionDate}'
      );
    }
    
    return buffer.toString();
  }

  /// 导出数据为JSON格式
  static String exportToJson(List<Bill> bills) {
    final jsonList = bills.map((bill) => bill.toJson()).toList();
    return jsonEncode(jsonList);
  }

  /// 导出发票信息（用于Excel等）
  static Future<String> generateInvoiceReport(List<Bill> bills) async {
    final buffer = StringBuffer();
    
    // 报表头部
    buffer.writeln('个人收支记账报告');
    buffer.writeln('导出时间: ${DateTime.now().toLocal().toString().split(' ')[0]}');
    buffer.writeln('记录数量: ${bills.length}条');
    buffer.writeln('=' * 50);
    buffer.writeln();
    
    // 统计信息
    final totalIncome = bills.where((t) => t.type == 1).fold(0.0, (sum, t) => sum + t.amount);
    final totalExpense = bills.where((t) => t.type == 2).fold(0.0, (sum, t) => sum + t.amount);
    final netIncome = totalIncome - totalExpense;
    
    buffer.writeln('收入总计: ¥${totalIncome.toStringAsFixed(2)}');
    buffer.writeln('支出总计: ¥${totalExpense.toStringAsFixed(2)}');
    buffer.writeln('净收入: ¥${netIncome.toStringAsFixed(2)}');
    buffer.writeln();
    buffer.writeln('=' * 50);
    buffer.writeln();
    
    // 详细记录
    buffer.writeln('详细记录列表:');
    buffer.writeln('类型\t分类\t金额\t备注\t日期');
    buffer.writeln('-' * 30);
    
    for (final bill in bills) {
      final typeText = bill.type == 1 ? '收入' : '支出';
      final amountText = '¥${bill.amount.toStringAsFixed(2)}';
      
      buffer.writeln(
        '$typeText\t${bill.categoryName}\t$amountText\t${bill.remark ?? ''}\t${bill.transactionDate}'
      );
    }
    
    return buffer.toString();
  }

  /// 按分类统计
  static Map<String, double> getCategoryStatistics(List<Bill> bills, {int? type}) {
    final filteredBills = type != null 
      ? bills.where((t) => t.type == type).toList()
      : bills;
    
    final Map<String, double> statistics = {};
    
    for (final bill in filteredBills) {
      if (statistics.containsKey(bill.categoryName)) {
        statistics[bill.categoryName] = statistics[bill.categoryName]! + bill.amount;
      } else {
        statistics[bill.categoryName] = bill.amount;
      }
    }
    
    return statistics;
  }

  /// 按月份统计
  static Map<String, double> getMonthlyStatistics(List<Bill> bills, {int? type}) {
    final filteredBills = type != null 
      ? bills.where((t) => t.type == type).toList()
      : bills;
    
    final Map<String, double> statistics = {};
    
    for (final bill in filteredBills) {
      final monthKey = bill.transactionDate.substring(0, 7); // yyyy-MM
      
      if (statistics.containsKey(monthKey)) {
        statistics[monthKey] = statistics[monthKey]! + bill.amount;
      } else {
        statistics[monthKey] = bill.amount;
      }
    }
    
    return statistics;
  }

  /// 生成数据摘要
  static String generateDataSummary(List<Bill> bills) {
    final totalBills = bills.length;
    final incomeBills = bills.where((t) => t.type == 1).length;
    final expenseBills = bills.where((t) => t.type == 2).length;
    
    final totalIncome = bills.where((t) => t.type == 1).fold(0.0, (sum, t) => sum + t.amount);
    final totalExpense = bills.where((t) => t.type == 2).fold(0.0, (sum, t) => sum + t.amount);
    final netIncome = totalIncome - totalExpense;
    
    final categories = bills.map((t) => t.categoryName).toSet().length;
    final dateRange = bills.isEmpty 
      ? '无数据'
      : '${bills.map((t) => t.transactionDate).reduce((a, b) => a.compareTo(b) < 0 ? a : b)} 至 ${bills.map((t) => t.transactionDate).reduce((a, b) => a.compareTo(b) > 0 ? a : b)}';
    
    return '''
数据摘要报告
============
记录总数: $totalBills 条
收入记录: $incomeBills 条
支出记录: $expenseBills 条

收入总计: ¥${totalIncome.toStringAsFixed(2)}
支出总计: ¥${totalExpense.toStringAsFixed(2)}
净收入: ¥${netIncome.toStringAsFixed(2)}

涉及分类: $categories 个
数据范围: $dateRange

导出时间: ${DateTime.now().toLocal().toString()}
''';
  }

  /// 验证批量操作数据
  static String? validateBatchOperation(List<Bill> bills, String operation) {
    if (bills.isEmpty) {
      return '请选择要操作的记录';
    }
    
    switch (operation) {
      case 'delete':
        if (bills.length > 50) {
          return '单次删除记录数量不能超过50条';
        }
        break;
      case 'change_category':
        final categories = bills.map((t) => t.categoryName).toSet();
        if (categories.length == 1) {
          return '所有选中记录已经是相同分类，无需批量修改';
        }
        break;
      case 'add_remark':
        if (bills.every((t) => (t.remark ?? '').isNotEmpty)) {
          return '所有选中记录已有备注，请确认是否继续';
        }
        break;
    }
    
    return null;
  }

  /// 格式化文件大小
  static String formatFileSize(int bytes) {
    if (bytes < 1024) {
      return '$bytes B';
    } else if (bytes < 1024 * 1024) {
      return '${(bytes / 1024).toStringAsFixed(2)} KB';
    } else if (bytes < 1024 * 1024 * 1024) {
      return '${(bytes / (1024 * 1024)).toStringAsFixed(2)} MB';
    } else {
      return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB';
    }
  }
}