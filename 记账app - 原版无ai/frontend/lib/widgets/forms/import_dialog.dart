import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';

import '../../data/models/bill_model.dart';
import '../../core/providers/bill_provider.dart';
import '../../services/import_service.dart';

/// 批量导入对话框
class ImportDialog extends StatefulWidget {
  const ImportDialog({super.key});

  @override
  State<ImportDialog> createState() => _ImportDialogState();
}

class _ImportDialogState extends State<ImportDialog> {
  bool _isImporting = false;
  int _importedCount = 0;
  int _totalCount = 0;
  String _importStatus = '';
  List<Bill> _importedTransactions = [];

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('批量导入记录'),
      content: _buildContent(),
      actions: _buildActions(),
    );
  }

  Widget _buildContent() {
    if (_isImporting) {
      return _buildImportProgress();
    } else if (_importedCount > 0) {
      return _buildImportResult();
    } else {
      return _buildImportInstructions();
    }
  }

  Widget _buildImportInstructions() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '支持CSV格式文件导入，请确保文件包含以下列：',
          style: TextStyle(fontSize: 14, color: Colors.grey),
        ),
        const SizedBox(height: 12),
        _buildInstructionItem('类型', '收入或支出'),
        _buildInstructionItem('分类', '交易分类名称'),
        _buildInstructionItem('金额', '数字格式，支持小数'),
        _buildInstructionItem('日期', 'yyyy-MM-dd格式'),
        _buildInstructionItem('备注', '可选，交易说明'),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: ElevatedButton.icon(
                onPressed: _selectAndImportFile,
                icon: const Icon(Icons.file_upload, size: 18),
                label: const Text('选择文件'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).primaryColor,
                  foregroundColor: Colors.white,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: ElevatedButton.icon(
                onPressed: _downloadTemplate,
                icon: const Icon(Icons.download, size: 18),
                label: const Text('下载模板'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue[50],
                  foregroundColor: Colors.blue[700],
                  side: BorderSide(color: Colors.blue[300]!),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildInstructionItem(String title, String description) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '• $title：',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
          ),
          const SizedBox(width: 4),
          Expanded(
            child: Text(
              description,
              style: TextStyle(fontSize: 13, color: Colors.grey[600]),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildImportProgress() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const CircularProgressIndicator(),
        const SizedBox(height: 16),
        Text(
          _importStatus,
          style: const TextStyle(fontSize: 14),
          textAlign: TextAlign.center,
        ),
        if (_totalCount > 0) ...[
          const SizedBox(height: 8),
          LinearProgressIndicator(
            value: _importedCount / _totalCount,
            backgroundColor: Colors.grey[200],
          ),
          const SizedBox(height: 8),
          Text(
            '已导入 $_importedCount/$_totalCount 条记录',
            style: const TextStyle(fontSize: 12, color: Colors.grey),
          ),
        ],
      ],
    );
  }

  Widget _buildImportResult() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(
              Icons.check_circle,
              color: Colors.green[600],
              size: 24,
            ),
            const SizedBox(width: 8),
            Text(
              '导入完成',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.green[600],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text('成功导入 $_importedCount 条记录'),
        const SizedBox(height: 8),
        if (_importedTransactions.isNotEmpty) ...[
          const Text(
            '导入的记录预览：',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Container(
            height: 120,
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey[300]!),
              borderRadius: BorderRadius.circular(8),
            ),
            child: ListView.builder(
              itemCount: _importedTransactions.length,
              itemBuilder: (context, index) {
                final transaction = _importedTransactions[index];
                return ListTile(
                  leading: Icon(
                    transaction.type == 1 ? Icons.arrow_downward : Icons.arrow_upward,
                    color: transaction.type == 1 ? Colors.green : Colors.red,
                  ),
                  title: Text(transaction.categoryName),
                  subtitle: Text('${transaction.amount}元'),
                  trailing: Text(
                    transaction.transactionDate.split('-').sublist(1).join('-'), // 显示月-日格式
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                );
              },
            ),
          ),
        ],
      ],
    );
  }

  List<Widget> _buildActions() {
    if (_isImporting) {
      return [
        TextButton(
          onPressed: _cancelImport,
          child: const Text('取消'),
        ),
      ];
    } else if (_importedCount > 0) {
      return [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('完成'),
        ),
      ];
    } else {
      return [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('取消'),
        ),
        ElevatedButton(
          onPressed: _selectAndImportFile,
          child: const Text('开始导入'),
        ),
      ];
    }
  }

  Future<void> _downloadTemplate() async {
    try {
      final template = await ImportService.getCsvTemplate();
      final directory = await getApplicationDocumentsDirectory();
      final file = File('${directory.path}/记账模板.csv');
      await file.writeAsString(template, encoding: utf8);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('模板已保存到文档目录'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('下载模板失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _cancelImport() {
    setState(() {
      _isImporting = false;
      _importStatus = '导入已取消';
    });
  }

  Future<void> _selectAndImportFile() async {
    final provider = Provider.of<BillProvider>(context, listen: false);
    
    setState(() {
      _isImporting = true;
      _importStatus = '正在选择文件...';
    });

    try {
      // 导入CSV文件（包含文件选择）
      final transactions = await ImportService.importFromCsv(provider);
      
      if (transactions.isEmpty) {
        setState(() {
          _isImporting = false;
          _importStatus = '未找到有效的交易记录';
        });
        return;
      }
      
      setState(() {
        _totalCount = transactions.length;
        _importStatus = '正在保存记录...';
      });

      // 批量保存
      final successCount = await ImportService.saveImportedTransactions(
        transactions,
        provider,
      );

      setState(() {
        _isImporting = false;
        _importedCount = successCount;
        _importedTransactions = transactions.take(3).toList(); // 只显示前3条预览
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('成功导入 $successCount 条记录'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      setState(() {
        _isImporting = false;
      });
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('导入失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }
}