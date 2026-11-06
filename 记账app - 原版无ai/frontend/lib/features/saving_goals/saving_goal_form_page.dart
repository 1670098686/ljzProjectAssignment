import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers/saving_goal_provider.dart';
import '../../core/providers/category_provider.dart';
import '../../core/providers/optimized_transaction_provider.dart';
// import '../../core/providers/budget_provider.dart'; // 预算功能已移除
import '../../core/router/route_guards.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/category_model.dart';
import '../../data/services/bill_service.dart';
import '../../shared/widgets/category_selector_field.dart';
import '../../utils/icon_mapper.dart';

class SavingGoalFormPage extends StatefulWidget {
  const SavingGoalFormPage({super.key, this.goalId});

  final int? goalId;

  @override
  State<SavingGoalFormPage> createState() => _SavingGoalFormPageState();
}

class _SavingGoalFormPageState extends State<SavingGoalFormPage> {
  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nameController;
  late TextEditingController _targetAmountController;
  late TextEditingController _currentAmountController;
  late TextEditingController _descriptionController;
  late TextEditingController _categoryController;

  DateTime? _selectedDeadline;
  bool _isEditMode = false;
  int? _editingGoalId;
  bool _isLoading = false;
  
  // 存储原始值，用于检测组合分类名是否需要更新
  String _originalGoalName = '';
  String _originalCategoryName = '';

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController();
    _targetAmountController = TextEditingController();
    _currentAmountController = TextEditingController();
    _descriptionController = TextEditingController();
    _categoryController = TextEditingController();
    
    // 设置默认截止日期（新增模式下）
    _selectedDeadline = DateTime.now().add(const Duration(days: 30));
    
    // 设置默认当前金额为0（新增模式下）
    _currentAmountController.text = '0';

    final goalId = widget.goalId;
    if (goalId != null) {
      _initializeEditMode(goalId);
    } else {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) {
          return;
        }
        final state = GoRouterState.of(context);
        final parsedId = RouteGuards.parseOptionalInt(state, key: 'id');
        if (parsedId != null) {
          _initializeEditMode(parsedId);
        } else {
          _loadCategories();
        }
      });
    }
  }

  Future<void> _loadCategories() async {
    try {
      final categoryProvider = context.read<CategoryProvider>();
      await categoryProvider.loadCategories(type: 1); // 储蓄目标分类都是收入类型
      if (mounted) {
        setState(() {});
      }
    } catch (e) {
      debugPrint('加载分类失败: $e');
      if (mounted) {
        setState(() {});
      }
    }
  }

  void _initializeEditMode(int goalId) {
    _editingGoalId = goalId;
    _isEditMode = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      _loadGoalForEdit(goalId);
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _targetAmountController.dispose();
    _currentAmountController.dispose();
    _descriptionController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  Future<void> _loadGoalForEdit(int id) async {
    setState(() {
      _isLoading = true;
    });

    final provider = context.read<SavingGoalProvider>();
    final categoryProvider = context.read<CategoryProvider>();
    if (provider.goals.isEmpty) {
      await provider.loadGoals();
    }
    
    // 加载分类数据
    await categoryProvider.loadCategories(type: 1);

    SavingGoal? goal;
    try {
      goal = provider.goals.firstWhere((g) => g.id == id);
    } catch (_) {
      goal = null;
    }

    if (!mounted) {
      return;
    }

    if (goal == null) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('未找到对应的储蓄目标')));
      context.pop();
      return;
    }

    final resolvedGoal = goal;

    setState(() {
      _nameController.text = resolvedGoal.name;
      _targetAmountController.text = resolvedGoal.targetAmount.toString();
      _currentAmountController.text = resolvedGoal.currentAmount.toString();
      _descriptionController.text = resolvedGoal.description ?? '';
      _categoryController.text = resolvedGoal.categoryName;
      _selectedDeadline = resolvedGoal.deadline;
      _originalGoalName = resolvedGoal.name;
      _originalCategoryName = resolvedGoal.categoryName;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditMode ? '编辑储蓄目标' : '新增储蓄目标'),
        centerTitle: true,
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        elevation: 0,
        scrolledUnderElevation: 1,
        actions: [
          if (_isEditMode)
            IconButton(
              onPressed: _isLoading ? null : () => _showDeleteDialog(context),
              icon: const Icon(Icons.delete_outlined),
              color: Colors.red[400],
            ),
        ],
      ),
      body: Consumer<SavingGoalProvider>(
        builder: (context, provider, child) {
          return _isLoading
              ? const Center(child: CircularProgressIndicator())
              : Form(
                  key: _formKey,
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(20.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 目标名称输入
                        _buildNameInput(),
                        const SizedBox(height: 24),

                        // 目标分类选择
                        _buildCategorySelector(),
                        const SizedBox(height: 24),

                        // 目标金额输入
                        _buildTargetAmountInput(),
                        const SizedBox(height: 24),

                        // 当前金额输入
                        _buildCurrentAmountInput(),
                        const SizedBox(height: 24),

                        // 截止日期选择
                        _buildDeadlineSelector(),
                        const SizedBox(height: 24),

                        // 目标描述输入
                        _buildDescriptionInput(),
                        const SizedBox(height: 32),

                        // 提交按钮
                        _buildSubmitButton(provider),
                      ],
                    ),
                  ),
                );
        },
      ),
    );
  }

  Widget _buildNameInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '目标名称',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _nameController,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          decoration: InputDecoration(
            filled: true,
            fillColor: const Color(0xFFF8F9FA),
            prefixIcon: Container(
              margin: const EdgeInsets.all(12),
              child: Icon(
                Icons.savings_outlined,
                color: Theme.of(context).primaryColor,
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
                color: Theme.of(context).primaryColor.withAlpha(204),
              ),
            ),
            hintText: '例如：购买新手机',
            hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 16,
            ),
            errorMaxLines: 2,
          ),
          validator: (value) {
            if (value == null || value.trim().isEmpty) {
              return '请输入目标名称';
            }
            if (value.trim().length < 2) {
              return '目标名称至少2个字符';
            }
            if (value.trim().length > 50) {
              return '目标名称不能超过50个字符';
            }
            return null;
          },
        ),
      ],
    );
  }

  Widget _buildTargetAmountInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '目标金额',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _targetAmountController,
          keyboardType: TextInputType.numberWithOptions(decimal: true),
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
                color: Theme.of(context).primaryColor.withAlpha(204),
              ),
            ),
            hintText: '0.00',
            hintStyle: TextStyle(
              fontSize: 20,
              color: Colors.grey[400],
              fontWeight: FontWeight.w500,
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 16,
            ),
            errorMaxLines: 2,
          ),
          validator: (value) {
            if (value == null || value.trim().isEmpty) {
              return '请输入目标金额';
            }
            final amount = double.tryParse(value);
            if (amount == null) {
              return '请输入有效的金额';
            }
            if (amount <= 0) {
              return '目标金额必须大于0';
            }
            if (amount > 1000000000) {
              return '目标金额不能超过10亿';
            }
            return null;
          },
        ),
      ],
    );
  }

  Widget _buildCurrentAmountInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '当前金额',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _currentAmountController,
          keyboardType: TextInputType.numberWithOptions(decimal: true),
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: Color(0xFF1976D2),
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
                  color: Color(0xFF1976D2),
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
                color: Theme.of(context).primaryColor.withAlpha(204),
              ),
            ),
            hintText: '0.00',
            hintStyle: TextStyle(
              fontSize: 20,
              color: Colors.grey[400],
              fontWeight: FontWeight.w500,
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 16,
            ),
            errorMaxLines: 2,
          ),
          validator: (value) {
            if (value == null || value.trim().isEmpty) {
              return '请输入当前金额';
            }
            final amount = double.tryParse(value);
            if (amount == null) {
              return '请输入有效的金额';
            }
            if (amount < 0) {
              return '当前金额不能小于0';
            }
            if (amount > 1000000000) {
              return '当前金额不能超过10亿';
            }
            return null;
          },
        ),
      ],
    );
  }

  Widget _buildDeadlineSelector() {
    final formattedDate = _selectedDeadline != null
        ? DateFormat('yyyy年MM月dd日').format(_selectedDeadline!)
        : null;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '截止日期',
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
          controller: TextEditingController(text: formattedDate),
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          decoration: InputDecoration(
            filled: true,
            fillColor: const Color(0xFFF8F9FA),
            prefixIcon: Container(
              margin: const EdgeInsets.all(12),
              child: Icon(
                Icons.calendar_today_outlined,
                color: Theme.of(context).primaryColor,
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
                color: Theme.of(context).primaryColor.withAlpha(204),
              ),
            ),
            hintText: '选择截止日期',
            hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 16,
            ),
          ),
          onTap: () => _showDatePicker(context),
        ),
      ],
    );
  }

  /// 检查是否为组合分类名
  bool _isCompositeCategoryName(String categoryName) {
    // 简化逻辑，只过滤明显是预算或目标生成的组合分类名
    // 不再因为包含分隔符就过滤正常分类名
    return categoryName.contains('计划') || 
           categoryName.contains('目标') ||
           categoryName.contains('预算') ||
           // 只有明确包含多个分类名的组合才过滤（如：分类A-分类B）
           (categoryName.contains('-') && _hasMultipleCategoryParts(categoryName));
  }

  /// 检查是否包含多个分类部分
  bool _hasMultipleCategoryParts(String categoryName) {
    // 尝试按常见分隔符分割，看是否包含多个有意义的分类名部分
    final separators = ['-', '_', '·', '•', '+'];
    for (final separator in separators) {
      if (categoryName.contains(separator)) {
        final parts = categoryName.split(separator)
            .map((part) => part.trim())
            .where((part) => part.isNotEmpty)
            .toList();
        // 如果分割后有多个部分，且都是可能的分类名，则认为是组合分类名
        if (parts.length >= 2 && parts.every((part) => part.length >= 2)) {
          return true;
        }
      }
    }
    return false;
  }

  Widget _buildCategorySelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '目标分类',
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
            // 过滤掉组合分类名，只保留普通分类（使用和预算页面相同的过滤逻辑）
            final categories = provider.categories
                .where((c) => c.type == 1 && !provider.isCompositeCategoryGeneratedByBudgetOrGoal(c.name))
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
                    color: Color(0xFF4CAF50),
                  ),
                ),
                hintText: '选择或输入目标分类',
                hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
                prefixIcon: Icon(
                  Icons.category,
                  color: Theme.of(context).primaryColor,
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
                          return categories.map((Category category) {
                            return PopupMenuItem<String>(
                              value: category.name,
                              child: Row(
                                children: [
                                  Icon(
                                    category.icon?.isNotEmpty == true
                                        ? IconMapper.getIconData(category.icon!)
                                        : Icons.trending_up,
                                    size: 20,
                                    color: Theme.of(context).primaryColor,
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
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 16,
                ),
              ),
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return '请选择或输入目标分类';
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
      ],
    );
  }

  Widget _buildDescriptionInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: const Text(
            '目标描述',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF1A1A1A),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _descriptionController,
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
                color: Theme.of(context).primaryColor.withAlpha(204),
              ),
            ),
            hintText: '描述你的储蓄目标和计划（可选）',
            hintStyle: TextStyle(color: Colors.grey[500], fontSize: 16),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 16,
            ),
            counterText: '',
          ),
        ),
      ],
    );
  }

  Widget _buildSubmitButton(SavingGoalProvider provider) {
    final isBusy = provider.isLoading;
    
    // 简单检查表单是否已填写基本信息，避免过于复杂的预验证
    // 详细验证交给 _formKey.currentState!.validate() 在提交时执行
    final isNameFilled = _nameController.text.trim().isNotEmpty;
    final isTargetAmountFilled = _targetAmountController.text.trim().isNotEmpty;
    final isCurrentAmountFilled = _currentAmountController.text.trim().isNotEmpty;
    final isDeadlineSelected = _selectedDeadline != null;
    
    final isFormFilled = isNameFilled && isTargetAmountFilled && isCurrentAmountFilled && isDeadlineSelected;

    return Container(
      width: double.infinity,
      height: 56,
      margin: const EdgeInsets.only(top: 20),
      child: ElevatedButton(
        onPressed: (isBusy || !isFormFilled) ? null : () => _handleSubmit(provider),
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).primaryColor,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          elevation: isBusy ? 0 : 2,
          shadowColor: Theme.of(context).primaryColor.withAlpha(77),
        ),
        child: isBusy
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                ),
              )
            : Text(
                _isEditMode ? '保存修改' : '创建目标',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
      ),
    );
  }

  void _handleSubmit(SavingGoalProvider provider) async {
    if (_selectedDeadline == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请选择截止日期'), backgroundColor: Colors.red),
      );
      return;
    }

    if (_formKey.currentState!.validate()) {
      try {
        final name = _nameController.text.trim();
        final targetAmount = double.parse(_targetAmountController.text);
        final currentAmount = double.parse(_currentAmountController.text);
        final description = _descriptionController.text.trim();
        final categoryName = _categoryController.text.trim();

        // 验证分类名称
        if (categoryName.isEmpty) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('请选择分类'), backgroundColor: Colors.red),
            );
          }
          return;
        }

        // 确保分类存在
        final categoryProvider = context.read<CategoryProvider>();
        await categoryProvider.ensureCategoryExists(categoryName, 1);

        // 打印调试信息
        print('创建储蓄目标：');
        print('名称: $name');
        print('目标金额: $targetAmount');
        print('当前金额: $currentAmount');
        print('截止日期: $_selectedDeadline');
        print('描述: $description');
        print('分类: $categoryName');

        if (_isEditMode && _editingGoalId != null) {
          // 检查是否需要更新组合分类名（在调用Provider之前）
          if (name != _originalGoalName || categoryName != _originalCategoryName) {
            await _updateCompositeCategoryName(context, categoryName, name);
          }
          
          // 更新现有目标
          final updatedGoal = SavingGoal(
            id: _editingGoalId,
            name: name,
            targetAmount: targetAmount,
            currentAmount: currentAmount,
            deadline: _selectedDeadline!,
            description: description.isEmpty ? null : description,
            categoryName: categoryName,
          );
          print('更新目标: $updatedGoal');
          final success = await provider.updateGoal(updatedGoal);
          print('更新结果: $success');
          if (success && mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('储蓄目标已更新'),
                backgroundColor: Colors.green,
              ),
            );
            context.pop(true);
          }
        } else {
          // 创建新目标
          final newGoal = SavingGoal(
            name: name,
            targetAmount: targetAmount,
            currentAmount: currentAmount,
            deadline: _selectedDeadline!,
            description: description.isEmpty ? null : description,
            categoryName: categoryName,
          );
          print('创建新目标: $newGoal');
          final success = await provider.addGoal(newGoal);
          print('创建结果: $success');
          if (success && mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('储蓄目标已创建'),
                backgroundColor: Colors.green,
              ),
            );
            context.pop(true);
          } else {
            print('创建失败，返回false');
            // 如果创建失败，显示provider中的具体错误信息
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(provider.errorMessage ?? '储蓄目标创建失败，请重试'),
                  backgroundColor: Colors.red,
                ),
              );
            }
          }
        }
      } catch (e, stackTrace) {
        print('创建储蓄目标异常: $e');
        print('堆栈跟踪: $stackTrace');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('操作失败：${e.toString()}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    }
  }

  Future<void> _showDatePicker(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate:
          _selectedDeadline ?? DateTime.now().add(const Duration(days: 30)),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 3650)), // 10年后
      locale: const Locale('zh', 'CN'),
    );

    if (picked != null && picked != _selectedDeadline) {
      setState(() {
        _selectedDeadline = picked;
      });
    }
  }



  void _showDeleteDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          '删除储蓄目标',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
        content: const Text(
          '确定要删除这个储蓄目标吗？此操作不可恢复。',
          style: TextStyle(fontSize: 16, color: Color(0xFF666666)),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text(
              '取消',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
            ),
          ),
          TextButton(
            onPressed: () => _handleDelete(context),
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
  }

  Future<void> _handleDelete(BuildContext context) async {
    // 先关闭删除确认对话框
    Navigator.pop(context);
    
    if (_editingGoalId != null) {
      final provider = context.read<SavingGoalProvider>();
      try {
        final success = await provider.deleteGoal(_editingGoalId!);
        if (success) {
          // 直接关闭表单页面，让上一页显示消息
          if (context.mounted) {
            Navigator.of(context).pop(true);
          }
        } else {
          // 显示错误消息
          if (context.mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('删除失败，请重试'),
                backgroundColor: Colors.red,
              ),
            );
          }
        }
      } catch (e) {
        // 显示错误消息
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('删除失败：${e.toString()}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    }
  }

  /// 更新组合分类名（储蓄目标专用）
  Future<void> _updateCompositeCategoryName(BuildContext context, String newCategoryName, String goalName) async {
    try {
      final categoryProvider = context.read<CategoryProvider>();
      final transactionProvider = context.read<OptimizedTransactionProvider>();
      
      // 构建新的组合分类名（使用与预算页面相同的格式：目标名称-分类名称）
      final newCompositeCategoryName = '$goalName-$newCategoryName';
      final oldCompositeCategoryName = '$_originalGoalName-$_originalCategoryName';
      
      print('更新组合分类名: $oldCompositeCategoryName -> $newCompositeCategoryName');
      
      // 1. 创建新的组合分类
      await categoryProvider.ensureCategoryExists(newCompositeCategoryName, 1);
      
      // 2. 更新使用旧组合分类名的交易记录
      await _updateBillsWithCompositeCategory(
        context, 
        oldCompositeCategoryName, 
        newCompositeCategoryName
      );
      
      // 3. 检查并删除未使用的旧分类
      if (oldCompositeCategoryName != newCompositeCategoryName) {
        await _deleteUnusedCompositeCategory(context, oldCompositeCategoryName);
      }
      
      print('组合分类名更新完成');
    } catch (e, stackTrace) {
      print('更新组合分类名失败: $e');
      print('堆栈跟踪: $stackTrace');
    }
  }

  /// 更新使用旧组合分类名的交易记录
  Future<void> _updateBillsWithCompositeCategory(
    BuildContext context,
    String oldCompositeCategoryName,
    String newCompositeCategoryName,
  ) async {
    try {
      final billService = BillService();
      final transactionProvider = context.read<OptimizedTransactionProvider>();
      
      print('开始批量更新交易记录分类名: $oldCompositeCategoryName -> $newCompositeCategoryName');
      
      // 首先获取所有交易记录
      await transactionProvider.loadBills();
      
      // 找到使用旧组合分类名的交易记录ID列表
      final billsToUpdate = transactionProvider.bills
          .where((bill) => bill.categoryName == oldCompositeCategoryName)
          .toList();
          
      if (billsToUpdate.isEmpty) {
        print('没有找到使用旧分类名的交易记录');
        return;
      }
      
      final billIds = billsToUpdate.map((bill) => bill.id!).toList();
      print('找到 ${billIds.length} 条需要更新的交易记录');
      
      // 使用BillService的批量更新方法
      await billService.updateBillsCategory(
        billIds, 
        newCompositeCategoryName
      );
      
      print('交易记录批量更新完成');
      
      // 刷新交易记录Provider状态
      await transactionProvider.loadBills();
    } catch (e, stackTrace) {
      print('更新交易记录失败: $e');
      print('堆栈跟踪: $stackTrace');
    }
  }

  /// 删除未使用的旧组合分类
  Future<void> _deleteUnusedCompositeCategory(
    BuildContext context,
    String compositeCategoryName,
  ) async {
    try {
      final categoryProvider = context.read<CategoryProvider>();
      final transactionProvider = context.read<OptimizedTransactionProvider>();
      final savingGoalProvider = context.read<SavingGoalProvider>();
      
      // 检查是否还有其他地方使用这个组合分类名
      bool isStillInUse = false;
      
      // 检查储蓄目标
      await savingGoalProvider.loadGoals();
      final goalsUsingCategory = savingGoalProvider.goals
          .where((g) => g.categoryName == compositeCategoryName)
          .toList();
      
      // 检查交易记录
      await transactionProvider.loadBills();
      final transactionsUsingCategory = transactionProvider.bills
          .where((t) => t.categoryName == compositeCategoryName)
          .toList();
      
      isStillInUse = goalsUsingCategory.isNotEmpty || 
                    transactionsUsingCategory.isNotEmpty;
      
      if (!isStillInUse) {
        print('删除未使用的组合分类: $compositeCategoryName');
        
        // 查找组合分类
        final categoryToDelete = await categoryProvider.findCategoryByName(compositeCategoryName);
        if (categoryToDelete != null) {
          await categoryProvider.deleteCategory(categoryToDelete.id!);
          print('组合分类删除完成');
        } else {
          print('未找到要删除的组合分类: $compositeCategoryName');
        }
      } else {
        print('组合分类仍在使用，跳过删除: $compositeCategoryName');
      }
    } catch (e, stackTrace) {
      print('删除组合分类失败: $e');
      print('堆栈跟踪: $stackTrace');
    }
  }
}
