import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/models/bill_model.dart';
import '../../data/models/category_model.dart';
import '../../core/providers/bill_provider.dart';
import '../../core/providers/category_provider.dart';
import '../../shared/widgets/category_selector_field.dart';
import '../../utils/batch_operations_utils.dart';
import '../../widgets/forms/import_dialog.dart';

/// 批量操作对话框组件
class BatchOperationsDialog extends StatefulWidget {
  final List<Bill> selectedBills;
  final String operation;
  final Function(String? operation, bool? result)? onBatchOperationCompleted;

  const BatchOperationsDialog({
    super.key,
    required this.selectedBills,
    required this.operation,
    this.onBatchOperationCompleted,
  });

  @override
  State<BatchOperationsDialog> createState() => _BatchOperationsDialogState();
}

class _BatchOperationsDialogState extends State<BatchOperationsDialog> {
  final TextEditingController _categoryController = TextEditingController();
  String? _selectedOperation;
  String _bulkRemark = '';
  bool _isProcessing = false;
  List<Category> _categories = [];

  @override
  void initState() {
    super.initState();
    // 根据传入的操作类型预设选中的操作
    _selectedOperation = widget.operation;
    _loadCategories();
  }

  @override
  void dispose() {
    _categoryController.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    try {
      final provider = context.read<CategoryProvider>();
      await provider.loadCategories();
      final categoryType = _selectedBillsType;
      final categories = categoryType == null
          ? provider.categories
          : provider.categories.where((c) => c.type == categoryType).toList();
      setState(() {
        _categories = categories;
      });
    } catch (e) {
      setState(() {
        _categories = [];
      });
    }
  }

  bool get _hasMixedTypes {
    final types = widget.selectedBills.map((bill) => bill.type).toSet();
    return types.length > 1;
  }

  int? get _selectedBillsType {
    if (widget.selectedBills.isEmpty || _hasMixedTypes) {
      return null;
    }
    return widget.selectedBills.first.type;
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('批量操作 (${widget.selectedBills.length}条记录)'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 操作选择
          const Text(
            '选择操作',
            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
          ),
          const SizedBox(height: 8),
          Column(
            children: [
              _buildOperationTile(
                'delete',
                Icons.delete_outline,
                '删除记录',
                '删除选中的${widget.selectedBills.length}条记录',
                Colors.red,
              ),
              _buildOperationTile(
                'change_category',
                Icons.category_outlined,
                '批量分类',
                '将选中记录的分类统一修改',
                Colors.blue,
              ),
              _buildOperationTile(
                'add_remark',
                Icons.note_add_outlined,
                '批量备注',
                '为选中记录添加统一备注',
                Colors.orange,
              ),
              _buildOperationTile(
                'export',
                Icons.download_outlined,
                '导出数据',
                '导出选中记录到文件',
                Colors.green,
              ),
              _buildOperationTile(
                'import',
                Icons.upload_outlined,
                '导入数据',
                '从CSV文件批量导入记录',
                Colors.purple,
              ),
            ],
          ),

          // 分类选择（仅在选择批量分类时显示）
          if (_selectedOperation == 'change_category') ...[
            const SizedBox(height: 16),
            const Text(
              '选择新分类',
              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
            ),
            const SizedBox(height: 8),
            CategorySelectorField(
              controller: _categoryController,
              categories: _categories,
              labelText: '分类',
              hintText: _hasMixedTypes ? '包含不同收支类型，无法自动创建分类' : '请输入或选择分类',
              onChanged: (_) => setState(() {}),
              onCategorySelected: (_) => setState(() {}),
              onClear: () => setState(() {}),
              colorResolver: (category) =>
                  category.type == 1 ? Colors.green : Colors.red,
            ),
            if (_hasMixedTypes)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '选中的记录包含收入和支出，请分批操作以确保分类类型一致。',
                  style: TextStyle(color: Colors.orange.shade700, fontSize: 12),
                ),
              ),
          ],

          // 备注输入（仅在选择批量备注时显示）
          if (_selectedOperation == 'add_remark') ...[
            const SizedBox(height: 16),
            const Text(
              '备注内容',
              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
            ),
            const SizedBox(height: 8),
            TextField(
              decoration: const InputDecoration(
                hintText: '请输入备注内容',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
              onChanged: (value) {
                setState(() {
                  _bulkRemark = value;
                });
              },
            ),
          ],

          const SizedBox(height: 16),

          // 操作确认
          if (_selectedOperation != null) ...[
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey.shade200),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '确认操作：',
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      color: _getOperationColor(_selectedOperation!),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _getOperationDescription(),
                    style: TextStyle(color: Colors.grey.shade700),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: _isProcessing ? null : () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        ElevatedButton(
          onPressed: _canConfirm() ? _handleConfirm : null,
          style: ElevatedButton.styleFrom(
            backgroundColor: _selectedOperation != null
                ? _getOperationColor(_selectedOperation!)
                : Colors.grey,
          ),
          child: _isProcessing
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text('确认${_getOperationName()}'),
        ),
      ],
    );
  }

  Widget _buildOperationTile(
    String value,
    IconData icon,
    String title,
    String description,
    Color color,
  ) {
    final isSelected = _selectedOperation == value;

    return InkWell(
      onTap: () {
        setState(() {
          _selectedOperation = value;
          _categoryController.clear();
          _bulkRemark = '';
        });
      },
      borderRadius: BorderRadius.circular(8),
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          border: Border.all(
            color: isSelected ? color : Colors.grey.shade300,
            width: isSelected ? 2 : 1,
          ),
          borderRadius: BorderRadius.circular(8),
          color: isSelected ? color.withAlpha((0.1 * 255).round()) : null,
        ),
        // ignore: deprecated_member_use
        child: RadioListTile<String>(
          // ignore: deprecated_member_use
          value: value,
          // ignore: deprecated_member_use
          groupValue: _selectedOperation,
          // ignore: deprecated_member_use
          onChanged: (String? newValue) {
            setState(() {
              _selectedOperation = newValue;
              _categoryController.clear();
              _bulkRemark = '';
            });
          },
          activeColor: color,
          title: Text(
            title,
            style: TextStyle(
              fontWeight: FontWeight.w600,
              color: isSelected ? color : null,
            ),
          ),
          subtitle: Text(
            description,
            style: TextStyle(fontSize: 12, color: Colors.grey[600]),
          ),
          secondary: Icon(icon, color: isSelected ? color : Colors.grey),
          contentPadding: EdgeInsets.zero,
          controlAffinity: ListTileControlAffinity.leading,
        ),
      ),
    );
  }

  String _getOperationName() {
    switch (_selectedOperation) {
      case 'delete':
        return '删除';
      case 'change_category':
        return '修改分类';
      case 'add_remark':
        return '添加备注';
      case 'export':
        return '导出';
      case 'import':
        return '导入';
      default:
        return '';
    }
  }

  String _getOperationDescription() {
    switch (_selectedOperation) {
      case 'delete':
        return '此操作不可撤销，建议先备份数据。';
      case 'change_category':
        final categoryName = _categoryController.text.trim();
        final summary = categoryName.isEmpty ? '未输入' : categoryName;
        return '将${widget.selectedBills.length}条记录分类修改为：$summary';
      case 'add_remark':
        return '为${widget.selectedBills.length}条记录添加备注：${_bulkRemark.isEmpty ? "未输入" : _bulkRemark}';
      case 'export':
        return '将${widget.selectedBills.length}条记录导出为文件。';
      case 'import':
        return '从CSV文件批量导入交易记录。';
      default:
        return '';
    }
  }

  Color _getOperationColor(String operation) {
    switch (operation) {
      case 'delete':
        return Colors.red;
      case 'change_category':
        return Colors.blue;
      case 'add_remark':
        return Colors.orange;
      case 'export':
        return Colors.green;
      case 'import':
        return Colors.purple;
      default:
        return Colors.grey;
    }
  }

  bool _canConfirm() {
    if (_selectedOperation == null || _isProcessing) return false;

    switch (_selectedOperation) {
      case 'change_category':
        return _categoryController.text.trim().isNotEmpty && !_hasMixedTypes;
      case 'add_remark':
        return _bulkRemark.trim().isNotEmpty;
      case 'delete':
      case 'export':
      case 'import':
        return true;
      default:
        return false;
    }
  }

  Future<void> _handleConfirm() async {
    if (_selectedOperation == null) return;

    setState(() {
      _isProcessing = true;
    });

    try {
      bool success = false;
      switch (_selectedOperation) {
        case 'delete':
          success = await _performBatchDelete();
          break;
        case 'change_category':
          success = await _performBatchCategoryChange();
          break;
        case 'add_remark':
          success = await _performBatchAddRemark();
          break;
        case 'export':
          success = await _performExport();
          break;
        case 'import':
          success = await _performImport();
          break;
      }

      if (mounted) {
        setState(() {
          _isProcessing = false;
        });

        if (success) {
          Navigator.of(context).pop();
          widget.onBatchOperationCompleted?.call(_selectedOperation!, true);
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
        _showErrorMessage('操作失败：$e');
      }
    }
  }

  /// 显示成功提示
  void _showSuccessMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  /// 显示错误提示
  void _showErrorMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  /// 执行批量删除
  Future<bool> _performBatchDelete() async {
    try {
      final error = BatchOperationsUtils.validateBatchOperation(
        widget.selectedBills,
        'delete',
      );
      if (error != null) {
        _showErrorMessage(error);
        return false;
      }

      // 提取账单ID列表
      final billIds = widget.selectedBills
          .where((bill) => bill.id != null)
          .map((bill) => bill.id!)
          .toList();

      // 使用Provider批量删除
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      final success = await billProvider.deleteBills(billIds);

      if (success) {
        _showSuccessMessage('成功删除 ${widget.selectedBills.length} 条记录');
        return true;
      } else {
        _showErrorMessage('删除失败，请重试');
        return false;
      }
    } catch (e) {
      _showErrorMessage('删除失败: $e');
      return false;
    }
  }

  /// 执行批量分类更改
  Future<bool> _performBatchCategoryChange() async {
    try {
      final categoryName = _categoryController.text.trim();
      if (categoryName.isEmpty) {
        _showErrorMessage('请输入分类');
        return false;
      }

      final categoryType = _selectedBillsType;
      if (categoryType == null) {
        _showErrorMessage('选中记录包含不同的收支类型，请分批处理');
        return false;
      }

      // 提取账单ID列表
      final billIds = widget.selectedBills
          .where((bill) => bill.id != null)
          .map((bill) => bill.id!)
          .toList();

      // 使用Provider批量更新分类
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      final categoryProvider = Provider.of<CategoryProvider>(
        context,
        listen: false,
      );

      await categoryProvider.ensureCategoryExists(categoryName, categoryType);

      final success = await billProvider.updateBillsCategory(
        billIds,
        categoryName,
      );

      if (success) {
        _showSuccessMessage('成功更改 ${widget.selectedBills.length} 条记录的分类');
        return true;
      } else {
        _showErrorMessage('更改分类失败，请重试');
        return false;
      }
    } catch (e) {
      _showErrorMessage('更改分类失败: $e');
      return false;
    }
  }

  /// 执行批量添加备注
  Future<bool> _performBatchAddRemark() async {
    try {
      // 提取账单ID列表
      final billIds = widget.selectedBills
          .where((bill) => bill.id != null)
          .map((bill) => bill.id!)
          .toList();

      // 使用Provider批量添加备注
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      final success = await billProvider.addBillsRemark(
        billIds,
        _bulkRemark.trim(),
      );

      if (success) {
        _showSuccessMessage('成功为 ${widget.selectedBills.length} 条记录添加备注');
        return true;
      } else {
        _showErrorMessage('添加备注失败，请重试');
        return false;
      }
    } catch (e) {
      _showErrorMessage('添加备注失败: $e');
      return false;
    }
  }

  /// 执行导出操作
  Future<bool> _performExport() async {
    try {
      await BatchOperationsUtils.exportToCsv(widget.selectedBills);

      if (mounted) {
        _showSuccessMessage('CSV导出功能已触发，数据准备完成');
        return true;
      }
      return false;
    } catch (e) {
      _showErrorMessage('导出失败: $e');
      return false;
    }
  }

  /// 执行导入操作
  Future<bool> _performImport() async {
    try {
      // 关闭当前对话框
      Navigator.of(context).pop();

      // 延迟一小段时间后显示导入对话框
      await Future.delayed(const Duration(milliseconds: 300));

      // 显示导入对话框
      if (mounted) {
        showDialog(
          context: context,
          builder: (context) => const ImportDialog(),
        ).then((value) {
          // 导入完成后回调
          widget.onBatchOperationCompleted?.call('import', true);
        });
        return true;
      }
      return false;
    } catch (e) {
      _showErrorMessage('导入失败: $e');
      return false;
    }
  }
}

/// 批量操作信息类
class BatchOperationInfo {
  final String operation;
  final String? selectedCategory;
  final String bulkRemark;

  BatchOperationInfo({
    required this.operation,
    this.selectedCategory,
    this.bulkRemark = '',
  });
}
