import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/providers/bill_provider.dart';
import '../../../core/providers/category_provider.dart';
import '../../../data/models/bill_model.dart';
import '../../../data/models/category_model.dart';

/// 快速交易对话框组件
/// 提供简单的收入/支出记录功能
class QuickTransactionDialog extends StatefulWidget {
  const QuickTransactionDialog({
    super.key,
    required this.type,
    required this.title,
  });

  final int type; // 1=收入, 2=支出
  final String title;

  @override
  State<QuickTransactionDialog> createState() => _QuickTransactionDialogState();
}

class _QuickTransactionDialogState extends State<QuickTransactionDialog> {
  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController();
  final _remarkController = TextEditingController();
  
  String _selectedCategory = '其他';
  bool _isLoading = false;
  List<Category> _userCategories = [];

  @override
  void initState() {
    super.initState();
    _loadUserCategories();
  }

  Future<void> _loadUserCategories() async {
    try {
      final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
      await categoryProvider.loadCategories(type: widget.type);
      
      if (mounted) {
        setState(() {
          _userCategories = categoryProvider.categories
              .where((category) => category.type == widget.type)
              .toList();
          
          if (_userCategories.isNotEmpty) {
            _selectedCategory = _userCategories.first.name;
          }
        });
      }
    } catch (e) {
      debugPrint('加载用户分类失败: $e');
    }
  }

  @override
  void dispose() {
    _amountController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  Future<void> _saveTransaction() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      
      final bill = Bill(
        id: null, // 新增记录
        type: widget.type,
        categoryName: _selectedCategory,
        amount: double.parse(_amountController.text),
        remark: _remarkController.text.trim(),
        transactionDate: DateTime.now().toIso8601String(),
      );

      await billProvider.addBill(bill);

      if (mounted) {
        // 显示成功提示
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(widget.type == 1 ? '收入记录添加成功！' : '支出记录添加成功！'),
            backgroundColor: Colors.green,
          ),
        );
        
        // 关闭对话框
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('添加失败：$e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  // 获取分类图标 - 未使用
  // String _getCategoryIcon(String category) {
  //   final iconMap = {
  //     '工资': 'work',
  //     '奖金': 'card_giftcard',
  //     '投资': 'trending_up',
  //     '兼职': 'business_center',
  //     '餐饮': 'restaurant',
  //     '交通': 'directions_car',
  //     '购物': 'shopping_cart',
  //     '娱乐': 'sports_esports',
  //     '医疗': 'local_hospital',
  //     '教育': 'school',
  //     '住房': 'home',
  //   };
  //   return iconMap[category] ?? 'category';
  // }

  // 获取分类颜色 - 未使用
  // Color _getCategoryColor(String category) {
  //   if (widget.type == 1) {
  //     // 收入颜色
  //     final colorMap = {
  //       '工资': Colors.green,
  //       '奖金': Colors.orange,
  //       '投资': Colors.blue,
  //       '兼职': Colors.purple,
  //     };
  //     return colorMap[category] ?? Colors.green;
  //   } else {
  //     // 支出颜色
  //     final colorMap = {
  //       '餐饮': Colors.red,
  //       '交通': Colors.blue,
  //       '购物': Colors.pink,
  //       '娱乐': Colors.orange,
  //       '医疗': Colors.red,
  //       '教育': Colors.purple,
  //       '住房': Colors.brown,
  //     };
  //     return colorMap[category] ?? Colors.red;
  //   }
  // }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // final colorScheme = theme.colorScheme; // 未使用的颜色方案

    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: Row(
        children: [
          Icon(
            widget.type == 1 ? Icons.trending_up : Icons.trending_down,
            color: widget.type == 1 ? Colors.green : Colors.red,
          ),
          const SizedBox(width: 8),
          Text(
            widget.title,
            style: theme.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // 金额输入
              TextFormField(
                controller: _amountController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: '金额',
                  prefixText: '¥ ',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  prefixIcon: const Icon(Icons.attach_money),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return '请输入金额';
                  }
                  if (double.tryParse(value) == null) {
                    return '请输入有效的金额';
                  }
                  if (double.parse(value) <= 0) {
                    return '金额必须大于0';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),

              // 分类选择
              DropdownButtonFormField<String>(
                initialValue: _selectedCategory,
                decoration: InputDecoration(
                  labelText: '分类',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  prefixIcon: const Icon(Icons.category),
                ),
                items: _userCategories
                    .map((category) => DropdownMenuItem(
                          value: category.name,
                          child: Text(category.name),
                        ))
                    .toList(),
                onChanged: (value) {
                  if (value != null) {
                    setState(() {
                      _selectedCategory = value;
                    });
                  }
                },
              ),
              const SizedBox(height: 16),

              // 备注输入
              TextFormField(
                controller: _remarkController,
                decoration: InputDecoration(
                  labelText: '备注（可选）',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  prefixIcon: const Icon(Icons.note),
                ),
                maxLines: 2,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isLoading ? null : () => Navigator.of(context).pop(),
          child: const Text('取消'),
        ),
        ElevatedButton(
          onPressed: _isLoading ? null : _saveTransaction,
          style: ElevatedButton.styleFrom(
            backgroundColor: widget.type == 1 ? Colors.green : Colors.red,
            foregroundColor: Colors.white,
          ),
          child: _isLoading
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(widget.type == 1 ? '确认收入' : '确认支出'),
        ),
      ],
    );
  }
}