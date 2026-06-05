import 'dart:io';
import 'dart:convert';
import 'package:csv/csv.dart';
import 'package:file_picker/file_picker.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';

import '../data/models/bill_model.dart';
import '../core/providers/bill_provider.dart';

/// 批量导入服务类
class ImportService {
  /// 选择并导入CSV文件
  static Future<List<Bill>> importFromCsv(
    BillProvider provider,
  ) async {
    try {
      // 选择文件
      FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['csv'],
        allowMultiple: false,
      );

      if (result == null || result.files.isEmpty) {
        throw Exception('未选择文件');
      }

      final file = File(result.files.single.path!);
      final csvString = await file.readAsString();
      
      // 解析CSV
      final csvTable = const CsvToListConverter().convert(csvString);
      
      if (csvTable.isEmpty || csvTable.length < 2) {
        throw Exception('CSV文件格式错误：缺少数据行');
      }

      // 解析数据行
      final transactions = <Bill>[];
      final headers = csvTable[0].map((e) => e.toString().toLowerCase()).toList();
      
      for (int i = 1; i < csvTable.length; i++) {
        try {
          final row = csvTable[i];
          final transaction = _parseCsvRow(row, headers);
          if (transaction != null) {
            transactions.add(transaction);
          }
        } catch (e) {
          // 解析失败，跳过此行
        }
      }

      return transactions;
    } catch (e) {
      // 只抛出异常，不在service层使用BuildContext
      throw Exception('导入CSV文件失败: $e');
    }
  }

  /// 解析CSV行数据
  static Bill? _parseCsvRow(List<dynamic> row, List<String> headers) {
    try {
      // 构建字段映射
      final fieldMap = <String, dynamic>{};
      for (int j = 0; j < headers.length && j < row.length; j++) {
        fieldMap[headers[j]] = row[j];
      }

      // 解析必填字段
      final typeText = _getFieldValue(fieldMap, ['type', '类型']);
      final category = _getFieldValue(fieldMap, ['category', '分类', 'categoryname', 'categoryname']);
      final amountText = _getFieldValue(fieldMap, ['amount', '金额', 'money']);
      final dateText = _getFieldValue(fieldMap, ['date', '日期', 'transactiondate']);

      if (typeText == null || category == null || amountText == null || dateText == null) {
        return null;
      }

      // 解析类型
      final type = _parseType(typeText);
      
      // 解析金额
      final amount = _parseAmount(amountText);
      
      // 解析日期（转换为字符串格式）
      final date = _parseDate(dateText);
      final dateString = DateFormat('yyyy-MM-dd').format(date);
      
      // 解析备注
      final remark = _getFieldValue(fieldMap, ['remark', '备注', 'description']);

      return Bill(
        type: type,
        categoryName: category,
        amount: amount,
        transactionDate: dateString,
        remark: remark ?? '',
      );
    } catch (e) {
      // 解析CSV行数据失败
      return null;
    }
  }

  /// 获取字段值（支持多个可能的字段名）
  static String? _getFieldValue(Map<String, dynamic> fieldMap, List<String> fieldNames) {
    for (final fieldName in fieldNames) {
      final value = fieldMap[fieldName]?.toString().trim();
      if (value != null && value.isNotEmpty) {
        return value;
      }
    }
    return null;
  }

  /// 解析类型
  static int _parseType(String typeText) {
    final text = typeText.toLowerCase();
    if (text.contains('收入') || text.contains('income') || text == '1') {
      return 1;
    } else if (text.contains('支出') || text.contains('expense') || text == '2') {
      return 2;
    }
    throw Exception('未知的交易类型: $typeText');
  }

  /// 解析金额
  static double _parseAmount(String amountText) {
    try {
      // 移除货币符号和逗号
      final cleanText = amountText
          .replaceAll(RegExp(r'[^\d.-]'), '')
          .replaceAll(',', '');
      
      final amount = double.tryParse(cleanText);
      if (amount == null || amount <= 0) {
        throw Exception('无效的金额: $amountText');
      }
      return amount;
    } catch (e) {
      throw Exception('金额解析失败: $amountText');
    }
  }

  /// 解析日期
  static DateTime _parseDate(String dateText) {
    try {
      // 尝试多种日期格式
      final formats = [
        'yyyy-MM-dd',
        'yyyy/MM/dd',
        'yyyy.MM.dd',
        'MM/dd/yyyy',
        'dd/MM/yyyy',
        'yyyy-MM-dd HH:mm:ss',
        'yyyy/MM/dd HH:mm:ss',
      ];

      for (final format in formats) {
        try {
          final date = DateFormat(format).parse(dateText);
          return date;
        } catch (e) {
          continue;
        }
      }

      // 尝试解析时间戳
      final timestamp = int.tryParse(dateText);
      if (timestamp != null) {
        return DateTime.fromMillisecondsSinceEpoch(timestamp);
      }

      throw Exception('无法解析日期格式: $dateText');
    } catch (e) {
      throw Exception('日期解析失败: $dateText');
    }
  }

  /// 批量保存导入的交易记录
  static Future<int> saveImportedTransactions(
    List<Bill> transactions,
    BillProvider provider,
  ) async {
    try {
      int successCount = 0;
      
      for (final transaction in transactions) {
        try {
          final success = await provider.addBill(transaction);
          if (success) {
            successCount++;
          }
        } catch (e) {
          // 保存交易记录失败，继续处理其他记录
        }
      }
      
      return successCount;
    } catch (e) {
      throw Exception('批量保存失败: $e');
    }
  }

  /// 获取CSV导入模板
  static Future<String> getCsvTemplate() async {
    final template = '''类型,分类,金额,日期,备注
收入,工资,5000.00,2024-01-15,月工资收入
支出,餐饮,35.50,2024-01-15,午餐
支出,交通,8.00,2024-01-15,地铁
收入,奖金,1000.00,2024-01-20,季度奖金
支出,购物,299.00,2024-01-25,购买衣服

说明：
1. 类型：收入或支出
2. 分类：交易分类名称
3. 金额：数字格式，支持小数
4. 日期：yyyy-MM-dd格式
5. 备注：可选，交易说明''';
    
    return template;
  }

  /// 下载CSV模板
  static Future<void> downloadCsvTemplate() async {
    try {
      final template = await getCsvTemplate();
      
      // 在移动端，我们可以将模板保存到下载目录
      if (Platform.isAndroid || Platform.isIOS) {
        final directory = await getDownloadsDirectory();
        if (directory != null) {
          final file = File('${directory.path}/记账模板.csv');
          await file.writeAsString(template, encoding: utf8);
        }
      } else {
        // Web端处理 - 简化版本，仅返回模板内容
        // 在实际应用中，可以通过其他方式实现文件下载
        // CSV模板内容已生成
      }
    } catch (e) {
      throw Exception('下载模板失败: $e');
    }
  }
}

// 辅助函数：获取下载目录（移动端）
Future<Directory?> getDownloadsDirectory() async {
  if (Platform.isAndroid || Platform.isIOS) {
    return await getExternalStorageDirectory();
  }
  return null;
}