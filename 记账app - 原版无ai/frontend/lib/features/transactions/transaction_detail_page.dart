import 'dart:io';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../core/router/navigation_result.dart';
import '../../../core/services/image_upload_service.dart';
import '../../../data/models/bill_model.dart';
import '../../../data/models/category_model.dart';
import '../../../utils/icon_mapper.dart';
import '../../../core/providers/category_provider.dart';
import '../../../core/providers/bill_provider.dart';

class TransactionDetailPage extends StatefulWidget {
  final int? billId;

  const TransactionDetailPage({super.key, required this.billId});

  @override
  State<TransactionDetailPage> createState() => _TransactionDetailPageState();
}

class _TransactionDetailPageState extends State<TransactionDetailPage>
    with RouteResultMixin<TransactionDetailPage> {
  Bill? _bill;
  final ImageUploadService _imageUploadService = ImageUploadService();
  bool _isUploading = false;

  @override
  void initState() {
    super.initState();
    _loadBill();
  }

  void _loadBill() {
    print('📋 _loadBill: 开始加载账单数据');
    print('  - 账单ID: ${widget.billId}');
    
    if (widget.billId != null) {
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      try {
        _bill = billProvider.bills.firstWhere(
          (bill) => bill.id == widget.billId,
        );
        print('✅ 账单加载成功');
        print('  - 账单ID: ${_bill?.id}');
        print('  - 账单类型: ${_bill?.type}');
        print('  - 账单金额: ${_bill?.amount}');
        print('  - 账单分类: ${_bill?.categoryName}');
        print('  - 账单图片路径: ${_bill?.imagePath ?? "null"}');
      } catch (e) {
        print('❌ 账单加载失败: $e');
        print('  - 错误信息: 找不到交易记录 ID: ${widget.billId}');
        // 如果找不到对应的账单，显示错误信息
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('找不到交易记录 ID: ${widget.billId}'),
              backgroundColor: Colors.red,
            ),
          );
          // 延迟返回上一页
          Future.delayed(const Duration(seconds: 2), () {
            if (mounted) {
              Navigator.of(context).pop();
            }
          });
        }
      }
    } else {
      print('⚠️ 没有账单ID，初始化空账单');
    }
  }

  Future<void> _changeImage() async {
    if (_isUploading || _bill == null || _bill!.id == null) return;
    
    setState(() => _isUploading = true);
    
    try {
        final imageFile = await _imageUploadService.pickImageFromGallery();
        if (imageFile != null && mounted) {
          // 保存图片到应用目录并获取路径
          final imagePath = await _imageUploadService.saveImageToAppDirectory(imageFile);
          // 更新账单的图片路径
          final updatedBill = _bill!.copyWith(imagePath: imagePath);
        final billProvider = Provider.of<BillProvider>(context, listen: false);
        
        // 保存到数据库
        final success = await billProvider.updateBill(_bill!.id!, updatedBill);
        
        if (success && mounted) {
          setState(() {
            _bill = updatedBill;
          });
          
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('图片更新成功'),
              backgroundColor: Colors.green,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('图片更新失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isUploading = false);
      }
    }
  }

  Future<void> _removeImage() async {
    if (_bill == null || _bill!.id == null) return;
    
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
      // 更新账单，移除图片路径
      final updatedBill = _bill!.copyWith(imagePath: null);
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      
      final success = await billProvider.updateBill(_bill!.id!, updatedBill);
      
      if (success && mounted) {
        setState(() {
          _bill = updatedBill;
        });
        
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('图片删除成功'),
            backgroundColor: Colors.green,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_bill == null) {
      return _buildLoadingScaffold();
    }

    return _buildScaffold(context);
  }

  Widget _buildLoadingScaffold() {
    final theme = Theme.of(context);
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('交易详情'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: theme.colorScheme.surface,
        foregroundColor: theme.colorScheme.onSurface,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(
              color: theme.colorScheme.primary,
              strokeWidth: 3,
            ),
            const SizedBox(height: 16),
            Text(
              '加载交易信息中...',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildScaffold(BuildContext context) {
    final theme = Theme.of(context);
    final isIncome = _bill!.type == 1;
    final amountColor = isIncome ? Colors.green : Colors.red;

    return Scaffold(
      appBar: AppBar(
        title: const Text('交易详情'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: theme.colorScheme.surface,
        foregroundColor: theme.colorScheme.onSurface,
        actions: [
          IconButton(
            onPressed: () => _showEditDialog(context),
            icon: const Icon(Icons.edit),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 金额显示
            _buildAmountCard(context, amountColor),
            const SizedBox(height: 24),

            // 交易信息
            _buildInfoCard(context, amountColor),
            const SizedBox(height: 24),

            // 分类信息
            _buildCategoryCard(context),
            const SizedBox(height: 24),

            // 备注信息
            _buildRemarkCard(context),
            const SizedBox(height: 24),

            // 操作按钮
            _buildActionButtons(context),
          ],
        ),
      ),
    );
  }

  Widget _buildAmountCard(BuildContext context, Color amountColor) {
    final theme = Theme.of(context);
    final isIncome = _bill!.type == 1;

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      shadowColor: Colors.transparent,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: theme.colorScheme.surface,
          border: Border.all(
            color: theme.colorScheme.outline.withOpacity(0.2),
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 日期显示在顶部右侧
              Align(
                alignment: Alignment.topRight,
                child: Text(
                  DateFormat('yyyy-MM-dd').format(DateTime.parse(_bill!.transactionDate)),
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(height: 8),
              // 金额和类型显示在下方
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      color: amountColor.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(
                      isIncome ? Icons.arrow_downward : Icons.arrow_upward,
                      size: 28,
                      color: amountColor,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        isIncome ? '收入' : '支出',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${isIncome ? '+' : '-'}¥${_bill!.amount.toStringAsFixed(2)}',
                        style: theme.textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: amountColor,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoCard(BuildContext context, Color amountColor) {
    final theme = Theme.of(context);
    final isIncome = _bill!.type == 1;

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: theme.colorScheme.outline.withAlpha(30)),
      ),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          color: theme.colorScheme.surface,
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '交易信息',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: theme.colorScheme.onSurface,
                ),
              ),
              const SizedBox(height: 20),
              _buildInfoRow(
                context,
                isIncome ? Icons.trending_up : Icons.trending_down,
                '交易类型',
                isIncome ? '收入' : '支出',
                amountColor,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCategoryCard(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: theme.colorScheme.outline.withAlpha(30)),
      ),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          color: theme.colorScheme.surface,
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '分类信息',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: theme.colorScheme.onSurface,
                ),
              ),
              const SizedBox(height: 20),
              Consumer<CategoryProvider>(
                builder: (context, provider, _) {
                  // 优化分类查找逻辑
                  Category? category;
                  
                  // 首先尝试精确匹配（名称和类型都匹配）
                  final exactMatch = provider.categories.where(
                    (cat) => cat.name == _bill!.categoryName && cat.type == _bill!.type
                  ).toList();
                  
                  if (exactMatch.isNotEmpty) {
                    category = exactMatch.first;
                  } else {
                    // 如果没有精确匹配，尝试仅匹配名称
                    final nameMatch = provider.categories.where(
                      (cat) => cat.name == _bill!.categoryName
                    ).toList();
                    
                    if (nameMatch.isNotEmpty) {
                      category = nameMatch.first;
                    } else {
                      // 如果没有名称匹配，使用默认值
                      category = null;
                    }
                  }

                  return _buildInfoRow(
                    context,
                    category != null ? IconMapper.getIconData(category.icon ?? '') : Icons.category,
                    '分类名称',
                    category?.name ?? _bill!.categoryName,
                    theme.colorScheme.primary,
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRemarkCard(BuildContext context) {
    final theme = Theme.of(context);
    final hasRemark = _bill?.remark != null && (_bill?.remark?.isNotEmpty ?? false);
    final hasImage = _bill?.imagePath != null && (_bill?.imagePath?.isNotEmpty ?? false);

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: theme.colorScheme.outline.withAlpha(30)),
      ),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          color: theme.colorScheme.surface,
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 备注信息标题
              Text(
                '备注信息',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: theme.colorScheme.onSurface,
                ),
              ),
              const SizedBox(height: 16),
              // 备注内容
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: theme.colorScheme.surfaceVariant.withOpacity(0.5),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      margin: const EdgeInsets.only(top: 2),
                      child: Icon(
                        Icons.sticky_note_2_outlined,
                        size: 20,
                        color: theme.colorScheme.primary,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        hasRemark ? _bill!.remark! : '无备注',
                        style: theme.textTheme.bodyLarge?.copyWith(
                          color: hasRemark ? theme.colorScheme.onSurface : theme.colorScheme.onSurfaceVariant,
                          fontStyle: hasRemark ? null : FontStyle.italic,
                        ),
                        textAlign: TextAlign.justify,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              // 图片区域
              _buildImageSection(context),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildImageSection(BuildContext context) {
    final theme = Theme.of(context);
    
    print('🔍 _buildImageSection: 开始构建图片区域');
    print('  - 账单对象: ${_bill != null ? "存在" : "为空"}');
    print('  - 图片路径: ${_bill?.imagePath ?? "null"}');
    print('  - 上传状态: ${_isUploading ? "上传中" : "非上传"}');
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '关联图片',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 16),
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: theme.colorScheme.outline.withAlpha(51)),
          ),
          child: Column(
            children: [
              if (_isUploading)
                Container(
                  height: 200,
                  alignment: Alignment.center,
                  child: const CircularProgressIndicator(),
                )
              else if (_bill?.imagePath != null)
                Stack(
                  children: [
                    Container(
                      height: 200,
                      width: double.infinity,
                      child: Image.file(
                        File(_bill!.imagePath!),
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          print('❌ 图片加载失败: ${_bill!.imagePath}');
                          return Container(
                            height: 200,
                            alignment: Alignment.center,
                            child: Icon(
                              Icons.broken_image,
                              size: 48,
                              color: theme.colorScheme.onSurfaceVariant,
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
                )
              else
                Container(
                  padding: const EdgeInsets.all(20),
                  alignment: Alignment.center,
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.image,
                        size: 48,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '暂无图片',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton.icon(
                        onPressed: () {
                          print('🔍 添加图片按钮被点击');
                          print('  - 当前账单ID: ${_bill?.id ?? "null"}');
                          print('  - 当前图片路径: ${_bill?.imagePath ?? "null"}');
                          _changeImage();
                        },
                        icon: const Icon(Icons.add_photo_alternate),
                        label: const Text('添加图片'),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildActionButtons(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      children: [
        Expanded(
          child: FilledButton.tonal(
            onPressed: () => _showEditDialog(context),
            style: FilledButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              elevation: 2,
            ),
            child: Text(
              '编辑',
              style: theme.textTheme.labelLarge?.copyWith(
                fontWeight: FontWeight.w600,
                letterSpacing: 0.5,
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: OutlinedButton(
            onPressed: () => _confirmDelete(context),
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              side: BorderSide(color: theme.colorScheme.error, width: 2),
              elevation: 1,
            ),
            child: Text(
              '删除',
              style: theme.textTheme.labelLarge?.copyWith(
                fontWeight: FontWeight.w600,
                color: theme.colorScheme.error,
                letterSpacing: 0.5,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInfoRow(
    BuildContext context,
    IconData icon,
    String label,
    String value,
    Color iconColor,
  ) {
    final theme = Theme.of(context);

    return Row(
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: iconColor.withOpacity(0.15),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Icon(
            icon,
            color: iconColor,
            size: 24,
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.2,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                value,
                style: theme.textTheme.bodyLarge?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: theme.colorScheme.onSurface,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _showEditDialog(BuildContext context) {
    if (_bill == null) return;

    GoRouter.of(context).push(
      '${AppRoutes.transactionForm}?id=${_bill!.id}',
    ).then((_) {
      // 编辑完成后刷新数据
      _loadBill();
      setState(() {});
    });
  }

  void _confirmDelete(BuildContext context) {
    if (_bill == null) return;

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('确认删除'),
          content: Text('确定要删除这笔${_bill!.type == 1 ? '收入' : '支出'}记录吗？'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () => _deleteBill(context),
              child: const Text('删除'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _deleteBill(BuildContext context) async {
    if (_bill == null || _bill!.id == null) return;

    Navigator.of(context).pop(); // 关闭对话框

    try {
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      final success = await billProvider.deleteBill(_bill!.id!);

      if (success) {
        if (mounted) {
          Navigator.of(context).pop(true); // 返回并通知刷新
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('删除成功'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('删除失败，请重试'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('删除失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
}