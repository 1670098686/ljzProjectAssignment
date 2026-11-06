import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../core/providers/budget_provider.dart';
import '../../core/providers/category_provider.dart';
import '../../data/models/budget_model.dart';
import '../../shared/utils/safe_submit.dart';
import '../../utils/icon_mapper.dart';

class BudgetFormPage extends StatefulWidget {
  const BudgetFormPage({super.key, this.budgetId});

  final int? budgetId;

  @override
  State<BudgetFormPage> createState() => _BudgetFormPageState();
}

class _BudgetFormPageState extends State<BudgetFormPage> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _amountController = TextEditingController();
  final TextEditingController _categoryController = TextEditingController();
  final TextEditingController _budgetNameController = TextEditingController();
  final TextEditingController _remarkController = TextEditingController();

  DateTime _selectedMonth = DateTime(DateTime.now().year, DateTime.now().month);
  bool _isSubmitting = false;
  bool _isEditing = false;
  Budget? _budget;

  @override
  void initState() {
    super.initState();
    _isEditing = widget.budgetId != null;

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final categoryProvider = context.read<CategoryProvider>();
      await categoryProvider.loadCategories(type: 2);

      if (_isEditing && widget.budgetId != null) {
        await _loadBudgetData();
      }
    });
  }

  Future<void> _loadBudgetData() async {
    final provider = context.read<BudgetProvider>();
    await provider.loadBudgets(_selectedMonth.year, _selectedMonth.month);

    final found = provider.budgets
        .where((b) => b.id == widget.budgetId)
        .toList();
    if (found.isEmpty || !mounted) return;

    final budget = found.first;
    setState(() {
      _budget = budget;
      _amountController.text = budget.amount.toString();
      _categoryController.text = budget.categoryName;
      _budgetNameController.text = budget.budgetName ?? '';
      _remarkController.text = ''; // 目前预算模型没有备注字段，暂时设为空
      _selectedMonth = DateTime(budget.year, budget.month);
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _categoryController.dispose();
    _budgetNameController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final monthLabel = DateFormat('yyyy年MM月').format(_selectedMonth);

    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? '编辑预算' : '新增预算'),
        elevation: 0,
        backgroundColor: theme.scaffoldBackgroundColor,
        foregroundColor: theme.colorScheme.onSurface,
        actions: [
          if (_isEditing)
            IconButton(
              onPressed: _showDeleteDialog,
              icon: const Icon(Icons.delete_outlined),
              color: theme.colorScheme.error,
            ),
        ],
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20.0),
            physics: const BouncingScrollPhysics(),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 预算名称
                _buildBudgetNameField(theme),
                const SizedBox(height: 24),

                // 预算分类
                _buildCategoryField(theme),
                const SizedBox(height: 24),

                // 预算金额
                _buildAmountField(theme),
                const SizedBox(height: 24),

                // 月份选择
                _buildMonthPickerField(theme, monthLabel),
                const SizedBox(height: 24),

                // 备注
                _buildRemarkField(theme),
                const SizedBox(height: 32),

                // 提交按钮
                _buildSubmitButton(theme),
                // 添加底部间距，确保提交按钮不会被遮挡
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMonthPickerField(ThemeData theme, String label) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '预算月份',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          readOnly: true,
          controller: TextEditingController(text: label),
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          decoration: InputDecoration(
            filled: true,
            fillColor: const Color(0xFFF8F9FA),
            prefixIcon: Container(
              margin: const EdgeInsets.all(12),
              child: Icon(
                Icons.calendar_month_outlined,
                color: theme.colorScheme.primary,
                size: 24,
              ),
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                width: 2,
                color: theme.colorScheme.primary.withAlpha(204),
              ),
            ),
            hintText: '选择预算月份',
            hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
            suffixIcon: Icon(
              Icons.chevron_right,
              color: Colors.grey[600],
            ),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          ),
          onTap: _pickMonth,
        ),
      ],
    );
  }

  Widget _buildBudgetNameField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '预算名称',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _budgetNameController,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          decoration: InputDecoration(
            filled: true,
            fillColor: const Color(0xFFF8F9FA),
            prefixIcon: Container(
              margin: const EdgeInsets.all(12),
              child: Icon(
                Icons.description_outlined,
                color: theme.colorScheme.primary,
                size: 24,
              ),
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                width: 2,
                color: theme.colorScheme.primary.withAlpha(204),
              ),
            ),
            hintText: '例如：餐饮预算、交通预算',
            hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          ),
          validator: (value) {
            if (value == null || value.trim().isEmpty) {
              return '请输入预算名称';
            }
            if (value.trim().length < 2) {
              return '预算名称至少2个字符';
            }
            if (value.trim().length > 50) {
              return '预算名称不能超过50个字符';
            }
            return null;
          },
        ),
      ],
    );
  }

  Widget _buildCategoryField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '预算分类',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Consumer<CategoryProvider>(
          builder: (context, provider, _) {
            final categories = provider.categories
                .where((c) => 
                  c.type == 2 && // 支出分类
                  !provider.isCompositeCategoryGeneratedByBudgetOrGoal(c.name) // 过滤掉组合分类名
                )
                .toList()
              ..sort((a, b) => a.name.compareTo(b.name));

            return TextFormField(
              controller: _categoryController,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
              decoration: InputDecoration(
                filled: true,
                fillColor: const Color(0xFFF8F9FA),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide(
                    width: 2,
                    color: theme.colorScheme.primary.withAlpha(204),
                  ),
                ),
                hintText: '选择或输入预算分类',
                hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
                prefixIcon: Icon(
                  Icons.category_outlined,
                  color: theme.colorScheme.primary,
                ),
                suffixIcon: categories.isNotEmpty
                    ? PopupMenuButton<String>(
                        icon: Icon(
                          Icons.arrow_drop_down,
                          color: Colors.grey[600],
                        ),
                        onSelected: (String value) {
                          setState(() {
                            _categoryController.text = value;
                          });
                        },
                        itemBuilder: (BuildContext context) {
                          return categories.map((category) {
                            return PopupMenuItem<String>(
                              value: category.name,
                              child: Row(
                                children: [
                                  Icon(
                                    category.icon?.isNotEmpty == true
                                        ? IconMapper.getIconData(category.icon!)
                                        : Icons.category,
                                    size: 20,
                                    color: theme.colorScheme.primary,
                                  ),
                                  const SizedBox(width: 12),
                                  Text(category.name),
                                ],
                              ),
                            );
                          }).toList();
                        },
                      )
                    : null,
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return '请选择或输入预算分类';
                }
                
                final trimmedValue = value.trim();
                final categoryProvider = context.read<CategoryProvider>();
                
                // 检查是否是组合分类名（包含多个分隔符的组合）
                if (categoryProvider.isCompositeCategoryGeneratedByBudgetOrGoal(trimmedValue)) {
                  return '分类只能选单个分类名或输入单个分类名，不允许输入 "计划名称-单分类名" 形式';
                }
                
                // 检查是否包含分隔符
                if (trimmedValue.contains('-')) {
                  return '分类名称不能包含分隔符 "-"，只能是单个分类名';
                }
                
                return null;
              },
            );
          },
        ),
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Text(
            '尚无支出分类，可直接输入名称创建新的分类',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAmountField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '预算金额',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _amountController,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          inputFormatters: [
            FilteringTextInputFormatter.allow(RegExp(r'^[0-9]+(\.[0-9]{0,2})?')),
          ],
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: Color(0xFF2E7D32),
          ),
          decoration: InputDecoration(
            filled: true,
            fillColor: const Color(0xFFF8F9FA),
            prefixIcon: Container(
              margin: const EdgeInsets.all(12),
              child: const Text(
                '¥',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF2E7D32),
                ),
              ),
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                width: 2,
                color: theme.colorScheme.primary.withAlpha(204),
              ),
            ),
            hintText: '0.00',
            hintStyle: TextStyle(
              fontSize: 20,
              color: Colors.grey[400],
              fontWeight: FontWeight.w500,
            ),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          ),
          validator: (value) {
            if (value == null || value.trim().isEmpty) {
              return '请输入预算金额';
            }
            final amount = double.tryParse(value);
            if (amount == null) {
              return '请输入有效的金额';
            }
            if (amount <= 0) {
              return '预算金额必须大于0';
            }
            if (amount > 1000000000) {
              return '预算金额不能超过10亿';
            }
            return null;
          },
        ),
      ],
    );
  }

  Widget _buildRemarkField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '备注',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _remarkController,
          maxLines: 4,
          maxLength: 200,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          decoration: InputDecoration(
            filled: true,
            fillColor: const Color(0xFFF8F9FA),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                width: 2,
                color: theme.colorScheme.primary.withAlpha(204),
              ),
            ),
            hintText: '添加预算备注（可选）',
            hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
            prefixIcon: Icon(
              Icons.note_outlined,
              color: theme.colorScheme.primary,
            ),
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
            counterText: '',
          ),
        ),
      ],
    );
  }

  Widget _buildSubmitButton(ThemeData theme) {
    // 简单检查表单是否已填写基本信息
    final isNameFilled = _budgetNameController.text.trim().isNotEmpty;
    final isAmountFilled = _amountController.text.trim().isNotEmpty;
    final isCategoryFilled = _categoryController.text.trim().isNotEmpty;
    final isFormFilled = isNameFilled && isAmountFilled && isCategoryFilled;

    return Container(
      width: double.infinity,
      height: 56,
      margin: const EdgeInsets.only(top: 20),
      child: ElevatedButton(
        onPressed: (_isSubmitting || !isFormFilled) ? null : _handleSubmit,
        style: ElevatedButton.styleFrom(
          backgroundColor: theme.colorScheme.primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          elevation: _isSubmitting ? 0 : 2,
          shadowColor: theme.colorScheme.primary.withAlpha(77),
        ),
        child: _isSubmitting
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                ),
              )
            : Text(
                _isEditing ? '保存修改' : '创建预算',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
      ),
    );
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    if (_isSubmitting) {
      return;
    }

    final categoryName = _categoryController.text.trim();
    final budgetName = _budgetNameController.text.trim();
    if (categoryName.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('请选择分类')));
      }
      return;
    }
    if (budgetName.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('请输入预算名称')));
      }
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    final provider = context.read<BudgetProvider>();
    final categoryProvider = context.read<CategoryProvider>();

    await safeSubmit<void>(
      context: context,
      popOnSuccess: true,
      action: () async {
        await categoryProvider.ensureCategoryExists(categoryName, 2);
        final amount = double.parse(_amountController.text.trim());
        final budget = Budget(
          id: _budget?.id,
          categoryName: categoryName,
          amount: amount,
          year: _selectedMonth.year,
          month: _selectedMonth.month,
          spent: _budget?.spent ?? 0.0,
          budgetName: budgetName,
        );

        final success = _isEditing
            ? await provider.updateBudget(budget)
            : await provider.addBudget(budget);

        if (!success) {
          throw Exception(provider.errorMessage ?? '保存失败，请稍后再试');
        }
      },
      onError: (error, stackTrace) {
        // 只记录错误日志，不重复设置 provider 错误
        debugPrint('预算保存失败: $error');
      },
    );

    if (!mounted) return;
    setState(() {
      _isSubmitting = false;
    });
  }

  Future<void> _pickMonth() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedMonth,
      firstDate: DateTime(now.year - 5),
      lastDate: DateTime(now.year + 5),
      helpText: '选择预算月份',
    );

    if (picked != null) {
      setState(() {
        _selectedMonth = DateTime(picked.year, picked.month);
      });
    }
  }

  // 删除确认对话框功能
  Future<void> _showDeleteDialog() async {
    final provider = context.read<BudgetProvider>();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          '删除预算',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
        content: Text('确定要删除「${_budgetNameController.text}」的预算吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text(
              '取消',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            style: TextButton.styleFrom(
              foregroundColor: Theme.of(dialogContext).colorScheme.error,
            ),
            child: const Text(
              '删除',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: Colors.red,
              ),
            ),
          ),
        ],
      ),
    );

    if (confirmed == true && _budget != null) {
      setState(() {
        _isSubmitting = true;
      });

      await safeSubmit<void>(
        context: context,
        popOnSuccess: true,
        action: () async {
          final success = await provider.deleteBudget(_budget!.id!);
          if (!success) {
            throw Exception(provider.errorMessage ?? '删除失败，请稍后再试');
          }
        },
        onError: (error, stackTrace) {
          // 只记录错误日志，不重复设置 provider 错误
          debugPrint('预算删除失败: $error');
        },
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _isSubmitting = false;
      });
    }
  }
}