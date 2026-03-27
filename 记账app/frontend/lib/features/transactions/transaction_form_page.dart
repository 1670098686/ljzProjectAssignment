import 'dart:io';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/providers/bill_provider.dart';
import '../../../core/providers/category_provider.dart';
import '../../../core/services/image_upload_service.dart';
import '../../../data/models/bill_model.dart';
import '../../../shared/utils/safe_submit.dart';
import '../../../shared/widgets/category_selector_field.dart';
import '../../../shared/widgets/loading_animations.dart';
import '../../../shared/widgets/micro_interactions.dart';
import '../../../utils/icon_mapper.dart';

class TransactionFormPage extends StatefulWidget {
  final Bill? bill;
  final int? billId;

  const TransactionFormPage({super.key, this.bill, this.billId});

  @override
  State<TransactionFormPage> createState() => _TransactionFormPageState();
}

class _TransactionFormPageState extends State<TransactionFormPage> {
  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController();
  final _remarkController = TextEditingController();
  final _categoryController = TextEditingController();

  int _selectedType = 2; // 1=收入, 2=支出，默认支出
  DateTime _selectedDate = DateTime.now();
  Bill? _editingBill;
  bool _isInitializing = false;
  bool _isSubmitting = false;
  String? _selectedImagePath;
  bool _isUploadingImage = false;
  final ImageUploadService _imageUploadService = ImageUploadService();

  @override
  void initState() {
    super.initState();
    _editingBill = widget.bill;
    if (_editingBill != null) {
      _hydrateBill(_editingBill!);
    } else if (widget.billId != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _loadBillById(widget.billId!);
      });
    }
    // 加载分类数据
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await context.read<CategoryProvider>().loadCategories();
    });
  }

  Future<void> _loadBillById(int id) async {
    setState(() {
      _isInitializing = true;
    });

    final provider = context.read<BillProvider>();
    if (provider.bills.isEmpty) {
      await provider.loadBills();
    }

    Bill? bill;
    try {
      bill = provider.bills.firstWhere((b) => b.id == id);
    } catch (_) {
      bill = null;
    }

    if (!mounted) {
      return;
    }

    if (bill == null) {
      setState(() => _isInitializing = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('未找到对应的交易记录')));
      Navigator.of(context).pop();
      return;
    }

    _editingBill = bill;
    _hydrateBill(bill);
    setState(() => _isInitializing = false);
  }

  void _hydrateBill(Bill bill) {
    setState(() {
      _selectedType = bill.type;
      _amountController.text = bill.amount.toString();
      _selectedDate = DateTime.parse(bill.transactionDate);
      _remarkController.text = bill.remark ?? '';
      _categoryController.text = bill.categoryName;
      _selectedImagePath = bill.imagePath; // 加载现有图片路径
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _remarkController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isEditing = _editingBill != null || widget.billId != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? '编辑交易' : '新增交易'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: theme.colorScheme.surface,
        foregroundColor: theme.colorScheme.onSurface,
        actions: [
          if (_editingBill?.id != null)
            IconButton(
              onPressed: _showDeleteDialog,
              icon: const Icon(Icons.delete_outline),
            ),
        ],
      ),
      body: _isInitializing
          ? const LoadingStateWidget(type: LoadingType.pulse, message: '加载中...')
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(20),
                physics: const BouncingScrollPhysics(),
                children: [
                  _buildTypeSelector(theme),
                  const SizedBox(height: 24),
                  _buildAmountField(theme),
                  const SizedBox(height: 24),
                  _buildCategoryField(theme),
                  const SizedBox(height: 24),
                  _buildDateField(theme),
                  const SizedBox(height: 24),
                  _buildRemarkField(theme),
                  const SizedBox(height: 24),
                  _buildImageField(theme),
                  const SizedBox(height: 32),
                  _buildSubmitButton(theme),
                ],
              ),
            ),
    );
  }

  Widget _buildSubmitButton(ThemeData theme) {
    final isEditing = _editingBill != null || widget.billId != null;

    return BounceAnimatedButton(
      onPressed: _isSubmitting ? null : _handleSubmit,
      width: double.infinity,
      height: 56,
      backgroundColor: theme.primaryColor,
      label: isEditing ? '保存修改' : '保存交易',
      child: _isSubmitting
          ? const LoadingStateWidget(type: LoadingType.spinner, size: 20)
          : null,
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
    if (categoryName.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('请选择分类')));
      }
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    final billProvider = context.read<BillProvider>();
    final categoryProvider = context.read<CategoryProvider>();

    await safeSubmit<void>(
      context: context,
      popOnSuccess: true,
      action: () async {
        final amount = double.parse(_amountController.text.trim());

        await categoryProvider.ensureCategoryExists(
          categoryName,
          _selectedType,
        );

        final bill = Bill(
          id: _editingBill?.id ?? widget.billId,
          type: _selectedType,
          categoryName: categoryName,
          amount: amount,
          transactionDate: DateFormat('yyyy-MM-dd').format(_selectedDate),
          remark: _remarkController.text.trim().isEmpty
              ? null
              : _remarkController.text.trim(),
          imagePath: _selectedImagePath, // 保存图片路径
        );

        print('📋 创建账单对象:');
        print('  - ID: ${bill.id}');
        print('  - 类型: ${bill.type}');
        print('  - 分类: ${bill.categoryName}');
        print('  - 金额: ${bill.amount}');
        print('  - 日期: ${bill.transactionDate}');
        print('  - 备注: ${bill.remark}');
        print('  - 图片路径: ${bill.imagePath}');
        print('💾 准备保存到数据库...');

        if (bill.id != null) {
          print('🔄 更新现有账单 ID: ${bill.id}');
          await billProvider.updateBill(bill.id!, bill);
          print('✅ 账单更新完成');
        } else {
          print('🔄 创建新账单');
          await billProvider.addBill(bill);
          print('✅ 账单创建完成');
        }
      },
      onError: (error, stackTrace) {
        billProvider.setError(error);
      },
    );

    if (!mounted) return;
    setState(() {
      _isSubmitting = false;
    });
  }

  Widget _buildTypeSelector(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '交易类型',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _buildTypeButton(
                theme,
                '支出',
                _selectedType == 2,
                Colors.red,
                Icons.arrow_upward,
                () => _updateTransactionType(2),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildTypeButton(
                theme,
                '收入',
                _selectedType == 1,
                Colors.green,
                Icons.arrow_downward,
                () => _updateTransactionType(1),
              ),
            ),
          ],
        ),
      ],
    );
  }

  void _updateTransactionType(int type) {
    if (_selectedType == type) {
      return;
    }
    setState(() {
      _selectedType = type;
      _categoryController.clear();
    });
  }

  Widget _buildTypeButton(
    ThemeData theme,
    String title,
    bool isSelected,
    Color color,
    IconData icon,
    VoidCallback onTap,
  ) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          border: Border.all(
            color: isSelected ? color : theme.colorScheme.outline,
            width: isSelected ? 2 : 1,
          ),
          borderRadius: BorderRadius.circular(12),
          color: isSelected ? color.withAlpha(26) : null,
        ),
        child: Column(
          children: [
            Icon(
              icon,
              color: isSelected ? color : theme.colorScheme.onSurfaceVariant,
              size: 32,
            ),
            const SizedBox(height: 8),
            Text(
              title,
              style: theme.textTheme.titleMedium?.copyWith(
                color: isSelected ? color : theme.colorScheme.onSurface,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAmountField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '金额',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _amountController,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          style: theme.textTheme.headlineSmall?.copyWith(
            fontWeight: FontWeight.w700,
            color: _selectedType == 1 ? Colors.green : Colors.red,
          ),
          decoration: InputDecoration(
            hintText: '0.00',
            prefixIcon: Container(
              padding: const EdgeInsets.all(16),
              child: Text(
                '¥',
                style: theme.textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.primary,
                ),
              ),
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: theme.colorScheme.outline),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                color: theme.colorScheme.primary,
                width: 2,
              ),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: theme.colorScheme.error),
            ),
            filled: true,
            fillColor: theme.colorScheme.surface,
          ),
          validator: (value) {
            if (value == null || value.isEmpty) {
              return '请输入金额';
            }
            final amount = double.tryParse(value);
            if (amount == null || amount <= 0) {
              return '请输入有效金额';
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
        Text(
          '分类',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        Consumer<CategoryProvider>(
          builder: (context, provider, _) {
            final categories = provider.getAllAvailableCategories(type: _selectedType);

            return FormField<String>(
              validator: (_) {
                if (_categoryController.text.trim().isEmpty) {
                  return '请输入分类';
                }
                return null;
              },
              builder: (field) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    CategorySelectorField(
                      controller: _categoryController,
                      categories: categories,
                      hintText: '请输入或选择分类',
                      onChanged: (value) {
                        field.didChange(value.trim());
                        setState(() {});
                      },
                      onCategorySelected: (_) {
                        field.didChange(_categoryController.text.trim());
                        setState(() {});
                      },
                      onClear: () {
                        field.didChange('');
                        setState(() {});
                      },
                      colorResolver: (category) =>
                          category.type == 1 ? Colors.green : Colors.red,
                      iconResolver: (category) {
                        final iconName = category.icon;
                        if (iconName != null && iconName.isNotEmpty) {
                          return IconMapper.getIconData(iconName);
                        }
                        return Icons.category;
                      },
                    ),
                    if (categories.isEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          '暂无可用分类，可直接输入名称创建新分类',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ),
                    if (field.hasError)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          field.errorText ?? '',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.error,
                          ),
                        ),
                      ),
                  ],
                );
              },
            );
          },
        ),
      ],
    );
  }

  Widget _buildDateField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '日期',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        InkWell(
          onTap: () => _showDatePicker(),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
            decoration: BoxDecoration(
              border: Border.all(color: theme.colorScheme.outline),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.calendar_today_outlined,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
                const SizedBox(width: 12),
                Text(
                  DateFormat('yyyy年MM月dd日').format(_selectedDate),
                  style: theme.textTheme.titleMedium,
                ),
                const Spacer(),
                Icon(
                  Icons.arrow_drop_down,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildRemarkField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '备注',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _remarkController,
          maxLines: 3,
          decoration: InputDecoration(
            hintText: '添加备注信息（可选）',
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            prefixIcon: const Icon(Icons.note_alt_outlined),
          ),
        ),
      ],
    );
  }

  Widget _buildImageField(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '关联图片',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        _selectedImagePath != null
            ? _buildImagePreview(theme)
            : _buildImageUploadButton(theme),
      ],
    );
  }

  Widget _buildImagePreview(ThemeData theme) {
    return Container(
      width: double.infinity,
      height: 200,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: theme.colorScheme.outline.withAlpha(51)),
      ),
      child: Stack(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Image.file(
              File(_selectedImagePath!),
              width: double.infinity,
              height: double.infinity,
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) {
                return Container(
                  width: double.infinity,
                  height: double.infinity,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: theme.colorScheme.surfaceVariant.withOpacity(0.5),
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.broken_image,
                        size: 48,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '图片加载失败',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
          Positioned(
            top: 8,
            right: 8,
            child: Row(
              children: [
                FloatingActionButton.small(
                  onPressed: _changeImage,
                  heroTag: 'change_image',
                  child: const Icon(Icons.edit),
                ),
                const SizedBox(width: 8),
                FloatingActionButton.small(
                  onPressed: _removeImage,
                  heroTag: 'remove_image',
                  backgroundColor: Colors.red,
                  child: const Icon(Icons.delete),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildImageUploadButton(ThemeData theme) {
    return InkWell(
      onTap: _isUploadingImage ? null : _pickImage,
      child: Container(
        width: double.infinity,
        height: 120,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: theme.colorScheme.outline.withAlpha(51)),
          color: theme.colorScheme.surfaceVariant.withOpacity(0.3),
        ),
        child: _isUploadingImage
            ? Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      theme.colorScheme.primary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '上传中...',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.add_photo_alternate,
                    size: 48,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '点击添加图片',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Future<void> _pickImage() async {
    if (_isUploadingImage) return;

    setState(() => _isUploadingImage = true);

    try {
      print('🔄 开始选择图片...');
      final imageFile = await _imageUploadService.pickImageFromGallery();
      
      if (imageFile != null && mounted) {
        print('✅ 图片选择成功: ${imageFile.path}');
        // 保存图片到应用目录并获取路径
        final imagePath = await _imageUploadService.saveImageToAppDirectory(imageFile);
        print('📁 图片保存到: $imagePath');
        setState(() {
          _selectedImagePath = imagePath;
        });
        print('💾 设置图片路径到表单: $_selectedImagePath');
        
        // 显示成功提示
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('图片选择并保存成功！'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } else {
        print('⚠️ 图片选择被取消或失败');
        // 显示取消提示
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('图片选择已取消，请重试'),
              backgroundColor: Colors.orange,
            ),
          );
        }
      }
    } catch (e) {
      print('❌ 选择图片失败: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('选择图片失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isUploadingImage = false);
      }
    }
  }

  Future<void> _changeImage() async {
    if (_isUploadingImage) return;

    setState(() => _isUploadingImage = true);

    try {
      final imageFile = await _imageUploadService.pickImageFromGallery();
      if (imageFile != null && mounted) {
        // 删除旧图片（如果存在）
        if (_selectedImagePath != null && _selectedImagePath!.isNotEmpty) {
          await _imageUploadService.deleteImageFile(_selectedImagePath);
        }
        // 保存新图片到应用目录并获取路径
        final imagePath = await _imageUploadService.saveImageToAppDirectory(imageFile);
        setState(() {
          _selectedImagePath = imagePath;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('更换图片失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isUploadingImage = false);
      }
    }
  }

  Future<void> _removeImage() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除这张图片吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      // 删除图片文件
      if (_selectedImagePath != null && _selectedImagePath!.isNotEmpty) {
        await _imageUploadService.deleteImageFile(_selectedImagePath);
      }
      setState(() {
        _selectedImagePath = null;
      });
    }
  }

  void _showDatePicker() async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
    );

    if (picked != null && picked != _selectedDate) {
      setState(() => _selectedDate = picked);
    }
  }

  void _showDeleteDialog() {
    if (_editingBill?.id == null) {
      return;
    }
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('删除交易'),
        content: const Text('确定要删除这笔交易吗？此操作不可撤销。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(dialogContext); // 关闭对话框
              final billProvider = context.read<BillProvider>();
              final navigator = Navigator.of(context);
              final messenger = ScaffoldMessenger.of(context);

              await billProvider.deleteBill(_editingBill!.id!);

              if (!mounted) {
                return;
              }

              messenger.showSnackBar(
                const SnackBar(
                  content: Text('交易删除成功'),
                  backgroundColor: Colors.green,
                ),
              );
              navigator.pop(true); // 返回列表页面
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );
  }
}
