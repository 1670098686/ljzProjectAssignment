// 优化的快速记账组件
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class OptimizedQuickEntryWidget extends StatefulWidget {
  const OptimizedQuickEntryWidget({super.key});

  @override
  State<OptimizedQuickEntryWidget> createState() => _OptimizedQuickEntryWidgetState();
}

class _OptimizedQuickEntryWidgetState extends State<OptimizedQuickEntryWidget> {
  final TextEditingController _amountController = TextEditingController();
  final TextEditingController _remarkController = TextEditingController();
  int _selectedType = 2; // 默认支出
  String _selectedCategory = '餐饮';
  DateTime _selectedDate = DateTime.now();
  
  final List<String> _incomeCategories = ['工资', '奖金', '兼职', '理财', '其他'];
  final List<String> _expenseCategories = ['餐饮', '交通', '购物', '娱乐', '医疗', '教育', '其他'];
  final List<String> _quickAmounts = ['10', '20', '50', '100', '200', '500'];

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 交易类型选择
          OptimizedTransactionTypeSection(
            selectedType: _selectedType,
            onTypeChanged: (type) {
              setState(() {
                _selectedType = type;
                _selectedCategory = type == 1 
                    ? _incomeCategories.first 
                    : _expenseCategories.first;
              });
            },
          ),
          
          const SizedBox(height: 16),
          
          // 分类选择
          OptimizedCategoryPicker(
            selectedType: _selectedType,
            selectedCategory: _selectedCategory,
            incomeCategories: _incomeCategories,
            expenseCategories: _expenseCategories,
            onCategoryChanged: (category) {
              setState(() {
                _selectedCategory = category;
              });
            },
          ),
          
          const SizedBox(height: 16),
          
          // 金额输入
          OptimizedAmountInputSection(
            controller: _amountController,
            quickAmounts: _quickAmounts,
            onAmountChanged: (amount) {
              // 处理金额变化
            },
          ),
          
          const SizedBox(height: 16),
          
          // 日期选择
          OptimizedDateSelector(
            selectedDate: _selectedDate,
            onDateChanged: (date) {
              setState(() {
                _selectedDate = date;
              });
            },
          ),
          
          const SizedBox(height: 16),
          
          // 备注输入
          OptimizedRemarkInput(
            controller: _remarkController,
          ),
          
          const SizedBox(height: 24),
          
          // 提交按钮
          OptimizedSubmitButton(
            onPressed: _handleSubmit,
          ),
        ],
      ),
    );
  }

  void _handleSubmit() {
    // 提交逻辑
    Navigator.of(context).pop();
  }

  @override
  void dispose() {
    _amountController.dispose();
    _remarkController.dispose();
    super.dispose();
  }
}

// 优化的交易类型选择组件
class OptimizedTransactionTypeSection extends StatelessWidget {
  final int selectedType;
  final Function(int) onTypeChanged;

  const OptimizedTransactionTypeSection({
    super.key,
    required this.selectedType,
    required this.onTypeChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _OptimizedTypeButton(
            label: '支出',
            isSelected: selectedType == 2,
            onPressed: () => onTypeChanged(2),
            color: Theme.of(context).colorScheme.error,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _OptimizedTypeButton(
            label: '收入',
            isSelected: selectedType == 1,
            onPressed: () => onTypeChanged(1),
            color: Theme.of(context).colorScheme.primary,
          ),
        ),
      ],
    );
  }
}

// 优化的分类选择器
class OptimizedCategoryPicker extends StatelessWidget {
  final int selectedType;
  final String selectedCategory;
  final List<String> incomeCategories;
  final List<String> expenseCategories;
  final Function(String) onCategoryChanged;

  const OptimizedCategoryPicker({
    super.key,
    required this.selectedType,
    required this.selectedCategory,
    required this.incomeCategories,
    required this.expenseCategories,
    required this.onCategoryChanged,
  });

  @override
  Widget build(BuildContext context) {
    final categories = selectedType == 1 ? incomeCategories : expenseCategories;
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '分类',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: categories.map((category) {
            return FilterChip(
              label: Text(category),
              selected: selectedCategory == category,
              onSelected: (selected) {
                if (selected) {
                  onCategoryChanged(category);
                }
              },
            );
          }).toList(),
        ),
      ],
    );
  }
}

/// 优化的金额输入组件
class OptimizedAmountInputSection extends StatefulWidget {
  final TextEditingController controller;
  final List<String> quickAmounts;
  final Function(double) onAmountChanged;

  const OptimizedAmountInputSection({
    super.key,
    required this.controller,
    required this.quickAmounts,
    required this.onAmountChanged,
  });

  @override
  State<OptimizedAmountInputSection> createState() => _OptimizedAmountInputSectionState();
}

class _OptimizedAmountInputSectionState extends State<OptimizedAmountInputSection> {
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '金额',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        TextField(
          controller: widget.controller,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          inputFormatters: [
            FilteringTextInputFormatter.allow(RegExp(r'^\d*\.?\d{0,2}')),
          ],
          decoration: const InputDecoration(
            hintText: '0.00',
            prefixText: '¥ ',
            border: OutlineInputBorder(),
          ),
          onChanged: (value) {
            final amount = double.tryParse(value) ?? 0.0;
            widget.onAmountChanged(amount);
          },
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: widget.quickAmounts.map((amount) {
            return ActionChip(
              label: Text('¥$amount'),
              onPressed: () {
                widget.controller.text = amount;
                widget.onAmountChanged(double.parse(amount));
              },
            );
          }).toList(),
        ),
      ],
    );
  }
}

// 优化的日期选择器
class OptimizedDateSelector extends StatelessWidget {
  final DateTime selectedDate;
  final Function(DateTime) onDateChanged;

  const OptimizedDateSelector({
    super.key,
    required this.selectedDate,
    required this.onDateChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '日期',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        ListTile(
          leading: const Icon(Icons.calendar_today),
          title: Text(
            '${selectedDate.year}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}',
          ),
          onTap: () async {
            final date = await showDatePicker(
              context: context,
              initialDate: selectedDate,
              firstDate: DateTime(2020),
              lastDate: DateTime.now(),
            );
            if (date != null) {
              onDateChanged(date);
            }
          },
        ),
      ],
    );
  }
}

// 优化的备注输入组件
class OptimizedRemarkInput extends StatelessWidget {
  final TextEditingController controller;

  const OptimizedRemarkInput({
    super.key,
    required this.controller,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '备注',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          maxLines: 2,
          decoration: const InputDecoration(
            hintText: '可选：添加备注信息',
            border: OutlineInputBorder(),
          ),
        ),
      ],
    );
  }
}

// 优化的提交按钮
class OptimizedSubmitButton extends StatelessWidget {
  final VoidCallback onPressed;

  const OptimizedSubmitButton({
    super.key,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 48,
      child: ElevatedButton(
        onPressed: onPressed,
        child: const Text(
          '保存记录',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }
}

// 优化的类型按钮
class _OptimizedTypeButton extends StatelessWidget {
  final String label;
  final bool isSelected;
  final VoidCallback onPressed;
  final Color color;

  const _OptimizedTypeButton({
    required this.label,
    required this.isSelected,
    required this.onPressed,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: isSelected ? color : null,
        foregroundColor: isSelected ? Colors.white : null,
        side: BorderSide(
          color: isSelected ? color : Theme.of(context).dividerColor,
        ),
      ),
      child: Text(label),
    );
  }
}