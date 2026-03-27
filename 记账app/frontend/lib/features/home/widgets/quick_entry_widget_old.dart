import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../core/providers/bill_provider.dart';
import '../../../core/providers/category_provider.dart';
import '../../../data/models/bill_model.dart';
import '../../../data/models/category_model.dart';

class HomeQuickEntry extends StatefulWidget {
  final VoidCallback onEntryCreated;

  const HomeQuickEntry({super.key, required this.onEntryCreated});

  @override
  State<HomeQuickEntry> createState() => _HomeQuickEntryState();
}

class _HomeQuickEntryState extends State<HomeQuickEntry> {
  static const List<String> _keypadValues = [
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9',
    '.',
    '0',
    '⌫',
  ];

  int _entryType = 2; // 1=收入, 2=支出
  String _inputValue = '0';
  String? _selectedCategory;
  bool _isSubmitting = false;
  final TextEditingController _remarkController = TextEditingController();
  final TextEditingController _categoryController = TextEditingController(); // 分类输入控制器

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final categoryProvider = context.read<CategoryProvider>();
      if (categoryProvider.categories.isEmpty) {
        await categoryProvider.loadCategories();
      }
    });
  }

  @override
  void dispose() {
    _remarkController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final amount = double.tryParse(_inputValue) ?? 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '快速记一笔',
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Text('仿计算器输入，支持分类与备注，随时随地记录灵感。', style: theme.textTheme.bodySmall),
        const SizedBox(height: 16),
        _buildTypeSelector(theme),
        const SizedBox(height: 16),
        _buildAmountDisplay(theme, amount),
        const SizedBox(height: 16),
        _buildCategorySelector(theme),
        const SizedBox(height: 12),
        _buildAccessoryRow(theme),
        const SizedBox(height: 12),
        _buildRemarkField(theme),
        const SizedBox(height: 24),
        _buildKeypad(theme),
        const SizedBox(height: 24),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            icon: _isSubmitting
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.save),
            label: Text(_entryType == 1 ? '保存收入' : '保存支出'),
            onPressed: _isSubmitting ? null : () => _handleSubmit(amount),
          ),
        ),
      ],
    );
  }

  Widget _buildTypeSelector(ThemeData theme) {
    return SegmentedButton<int>(
      segments: const [
        ButtonSegment(
          value: 2,
          icon: Icon(Icons.remove_circle),
          label: Text('支出'),
        ),
        ButtonSegment(
          value: 1,
          icon: Icon(Icons.add_circle),
          label: Text('收入'),
        ),
      ],
      selected: {_entryType},
      onSelectionChanged: (selection) {
        setState(() {
          _entryType = selection.first;
          // 重置分类以避免类型不匹配
          _selectedCategory = null;
        });
      },
    );
  }

  Widget _buildAmountDisplay(ThemeData theme, double amount) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('金额 (¥)', style: theme.textTheme.labelMedium),
          const SizedBox(height: 8),
          Text(
            amount.toStringAsFixed(2),
            style: theme.textTheme.displaySmall?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategorySelector(ThemeData theme) {
    final subtitle = _selectedCategory ?? '选择分类';
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 8),
      leading: CircleAvatar(
        radius: 22,
        backgroundColor: theme.colorScheme.primaryContainer,
        child: Icon(
          Icons.category,
          color: theme.colorScheme.onPrimaryContainer,
        ),
      ),
      title: const Text('分类'),
      subtitle: Text(subtitle),
      trailing: TextButton(
        onPressed: _showCategorySheet,
        child: const Text('选择'),
      ),
    );
  }

  Widget _buildAccessoryRow(ThemeData theme) {
    return Row(
      children: [
        OutlinedButton.icon(
          onPressed: _selectedCategory == null ? null : _clearCategory,
          icon: const Icon(Icons.clear),
          label: const Text('清除分类'),
        ),
        const SizedBox(width: 12),
        OutlinedButton.icon(
          onPressed: () {
            ScaffoldMessenger.of(
              context,
            ).showSnackBar(const SnackBar(content: Text('图片上传功能即将上线')));
          },
          icon: const Icon(Icons.attachment),
          label: const Text('上传票据'),
        ),
      ],
    );
  }

  Widget _buildRemarkField(ThemeData theme) {
    return TextField(
      controller: _remarkController,
      maxLines: 1,
      decoration: const InputDecoration(
        labelText: '备注 (可选)',
        border: OutlineInputBorder(),
      ),
    );
  }

  Widget _buildKeypad(ThemeData theme) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: _keypadValues.length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 2.2,
      ),
      itemBuilder: (context, index) {
        final value = _keypadValues[index];
        return ElevatedButton(
          onPressed: () => _onKeyPressed(value),
          style: ElevatedButton.styleFrom(
            backgroundColor: value == '⌫'
                ? theme.colorScheme.errorContainer
                : theme.colorScheme.primaryContainer,
            foregroundColor: value == '⌫'
                ? theme.colorScheme.onErrorContainer
                : theme.colorScheme.onPrimaryContainer,
          ),
          child: Text(
            value,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
          ),
        );
      },
    );
  }

  void _onKeyPressed(String value) {
    setState(() {
      if (value == '⌫') {
        if (_inputValue.length == 1) {
          _inputValue = '0';
        } else {
          _inputValue = _inputValue.substring(0, _inputValue.length - 1);
        }
        return;
      }
      if (value == '.') {
        if (_inputValue.contains('.')) {
          return;
        }
        _inputValue += '.';
        return;
      }
      if (_inputValue == '0') {
        _inputValue = value;
      } else {
        _inputValue += value;
      }
    });
  }

  Future<void> _showCategorySheet() async {
    final provider = context.read<CategoryProvider>();
    if (provider.categories.isEmpty) {
      await provider.loadCategories(type: _entryType);
    }
    if (!mounted) {
      return;
    }
    final List<Category> options = provider
        .getCategoriesByType(_entryType)
        .toList();

    final result = await showModalBottomSheet<String>(
      context: context,
      builder: (context) {
        if (options.isEmpty) {
          return const Padding(
            padding: EdgeInsets.all(24),
            child: Text('暂无分类，请先在分类管理中创建'),
          );
        }
        return ListView.separated(
          itemCount: options.length,
          separatorBuilder: (context, _) => const Divider(height: 1),
          itemBuilder: (context, index) {
            final category = options[index];
            return ListTile(
              title: Text(category.name),
              onTap: () => Navigator.of(context).pop(category.name),
            );
          },
        );
      },
    );

    if (result != null && mounted) {
      setState(() => _selectedCategory = result);
    }
  }

  void _clearCategory() {
    setState(() => _selectedCategory = null);
  }

  Future<void> _handleSubmit(double amount) async {
    final messenger = ScaffoldMessenger.of(context);
    if (amount <= 0) {
      messenger.showSnackBar(const SnackBar(content: Text('请输入有效金额')));
      return;
    }
    if (_selectedCategory == null) {
      messenger.showSnackBar(const SnackBar(content: Text('请选择分类')));
      return;
    }

    setState(() => _isSubmitting = true);
    final billProvider = context.read<BillProvider>();
    final bill = Bill(
      type: _entryType,
      categoryName: _selectedCategory!,
      amount: amount,
      transactionDate: DateFormat('yyyy-MM-dd').format(DateTime.now()),
      remark: _remarkController.text.trim().isEmpty
          ? null
          : _remarkController.text.trim(),
    );

    final result = await billProvider.addBill(bill);
    if (!mounted) {
      return;
    }
    setState(() => _isSubmitting = false);

    if (result) {
      messenger.showSnackBar(const SnackBar(content: Text('记账成功')));
      _resetForm();
      widget.onEntryCreated();
    } else {
      messenger.showSnackBar(const SnackBar(content: Text('记账失败，请稍后再试')));
    }
  }

  void _resetForm() {
    setState(() {
      _inputValue = '0';
      _selectedCategory = null;
      _remarkController.clear();
      _entryType = 2;
    });
  }
}
