import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../../app/routes.dart';
import '../../core/providers/bill_provider.dart';
import '../../core/providers/category_provider.dart';
import '../../core/utils/animation_utils.dart';
import '../../data/models/bill_model.dart';

class TransactionsPage extends StatefulWidget {
  const TransactionsPage({super.key});

  @override
  State<TransactionsPage> createState() => _TransactionsPageState();
}

class _TransactionsPageState extends State<TransactionsPage>
    with SingleTickerProviderStateMixin {
  int _selectedFilterIndex = 0; // 0: 全部, 1: 收入, 2: 支出
  String _selectedCategory = '全部';
  final List<String> _dateFilters = ['全部', '本周', '本月', '自定义'];
  String _selectedDateFilter = '全部';
  DateTimeRange? _customDateRange;

  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late List<Animation<double>> _slideAnimations;

  @override
  void initState() {
    super.initState();

    // 添加动画控制器
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeIn),
    );

    _slideAnimations = List.generate(
      2,
      (index) => Tween<double>(begin: 50.0, end: 0.0).animate(
        CurvedAnimation(
          parent: _animationController,
          curve: Interval(index * 0.1, 1.0, curve: Curves.easeOut),
        ),
      ),
    );

    // 在build方法执行后启动动画
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _animationController.forward();
      }
    });
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: AnimatedBuilder(
          animation: _animationController,
          builder: (context, child) {
            return FadeTransition(
              opacity: _fadeAnimation,
              child: CustomScrollView(
                physics: const BouncingScrollPhysics(),
                slivers: [
                  SliverToBoxAdapter(
                    child: Transform.translate(
                      offset: Offset(0, _slideAnimations[0].value),
                      child: _buildPageHeader(theme),
                    ),
                  ),
                  SliverToBoxAdapter(
                    child: Transform.translate(
                      offset: Offset(0, _slideAnimations[1].value),
                      child: _buildFilterSection(context),
                    ),
                  ),
                  _buildTransactionSliver(context),
                  const SliverToBoxAdapter(child: SizedBox(height: 80)),
                ],
              ),
            );
          },
        ),
      ),
      // 移除了添加交易记录的浮动按钮，所有收支记录统一通过首页记账模块进行
    );
  }

  Widget _buildFilterSection(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 8.0),
      child: AnimationUtils.createSlideIn(
        duration: const Duration(milliseconds: 400),
        beginOffset: const Offset(0, 30),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AnimationUtils.createSlideIn(
              duration: const Duration(milliseconds: 400),
              delay: const Duration(milliseconds: 100),
              beginOffset: const Offset(-30, 0),
              child: _buildPrimaryFilterTabs(context),
            ),
            const SizedBox(height: 16),
            AnimationUtils.createSlideIn(
              duration: const Duration(milliseconds: 400),
              delay: const Duration(milliseconds: 200),
              beginOffset: const Offset(-30, 0),
              child: _buildCategoryFilterRow(context),
            ),
            const SizedBox(height: 16),
            AnimationUtils.createSlideIn(
              duration: const Duration(milliseconds: 400),
              delay: const Duration(milliseconds: 300),
              beginOffset: const Offset(-30, 0),
              child: _buildDateFilterChips(context),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPageHeader(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '明细',
            style: theme.textTheme.headlineMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: theme.colorScheme.onSurface,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '查看所有交易记录',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPrimaryFilterTabs(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '收支类型',
          style: theme.textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        SegmentedButton<int>(
          segments: const [
            ButtonSegment(value: 0, label: Text('全部')),
            ButtonSegment(value: 1, label: Text('收入')),
            ButtonSegment(value: 2, label: Text('支出')),
          ],
          selected: {_selectedFilterIndex},
          onSelectionChanged: (selection) {
            setState(() {
              _selectedFilterIndex = selection.first;
              _selectedCategory = '全部';
            });
          },
        ),
      ],
    );
  }

  Widget _buildCategoryFilterRow(BuildContext context) {
    final theme = Theme.of(context);
    return Consumer2<CategoryProvider, BillProvider>(
      builder: (context, categoryProvider, billProvider, child) {
        // 从分类列表获取可用分类，而不是从账单中提取
        List<String> categories = [];
        
        // 根据当前筛选类型获取对应类型的分类
        int? typeFilter;
        if (_selectedFilterIndex == 1) {
          typeFilter = 1; // 收入
        } else if (_selectedFilterIndex == 2) {
          typeFilter = 2; // 支出
        }
        
        // 获取所有可用分类
        final allCategories = typeFilter != null 
            ? categoryProvider.categories.where((cat) => cat.type == typeFilter).toList()
            : categoryProvider.categories;
        
        // 从分类列表中提取分类名称
        categories = allCategories.map((cat) => cat.name).toList();
        
        // 去重并排序
        categories = categories.toSet().toList()..sort();
        
        // 在排序后的列表前面添加"全部"选项
        categories.insert(0, '全部');
        
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '分类筛选',
              style: theme.textTheme.labelLarge?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.only(right: 12.0),
              child: Row(
                children: categories.map((category) {
                  final isSelected = _selectedCategory == category;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: ChoiceChip(
                      label: Text(category),
                      selected: isSelected,
                      onSelected: (_) {
                        setState(() {
                          _selectedCategory = category;
                        });
                      },
                    ),
                  );
                }).toList(),
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildDateFilterChips(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '时间范围',
          style: theme.textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: _dateFilters.map((filter) {
            final isSelected = _selectedDateFilter == filter;
            final label = filter == '自定义' && _customDateRange != null
                ? '自定义 ${_formatRangeLabel(_customDateRange!)}'
                : filter;
            return ChoiceChip(
              label: Text(label),
              selected: isSelected,
              onSelected: (_) {
                if (filter == '自定义') {
                  _pickCustomDateRange(context);
                } else {
                  setState(() {
                    _selectedDateFilter = filter;
                    _customDateRange = null;
                  });
                }
              },
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildTransactionSliver(BuildContext context) {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      sliver: Consumer<BillProvider>(
        builder: (context, provider, child) {
          final filteredBills = _getFilteredBills(provider.bills);

          if (filteredBills.isEmpty) {
            return SliverToBoxAdapter(child: _buildEmptyState(context));
          }

          return SliverList(
            delegate: SliverChildBuilderDelegate((context, index) {
              final bill = filteredBills[index];
              return Padding(
                padding: EdgeInsets.only(
                  bottom: index == filteredBills.length - 1 ? 0 : 12,
                ),
                child: _buildTransactionCard(context, bill, index),
              );
            }, childCount: filteredBills.length),
          );
        },
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 80, 20, 0),
      child: Column(
        children: [
          Container(
            width: 120,
            height: 120,
            decoration: BoxDecoration(
              color: theme.colorScheme.primaryContainer,
              shape: BoxShape.circle,
            ),
            child: Icon(
              Icons.receipt_long,
              size: 60,
              color: theme.colorScheme.onPrimaryContainer,
            ),
          ),
          const SizedBox(height: 24),
          Text(
            '暂无交易记录',
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.bold,
              color: theme.colorScheme.onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '尝试调整筛选条件或添加新记录',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTransactionCard(BuildContext context, Bill bill, int index) {
    final theme = Theme.of(context);
    final isIncome = bill.type == 1;
    final amountColor = isIncome ? Colors.green : Colors.red;
    final remark = (bill.remark?.isNotEmpty ?? false) ? bill.remark! : '无备注';
    final dateLabel = _formatDisplayDate(bill.transactionDate);

    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 500),
      delay: Duration(milliseconds: 400 + index * 100),
      beginOffset: const Offset(0, 50),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () {
          // 使用GoRouter的push方法，将bill.id作为path参数传递
          GoRouter.of(context).push('${AppRoutes.transactionDetail}?id=${bill.id}');
        },
        onLongPress: bill.id == null
            ? null
            : () => _confirmDeleteBill(context, bill),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: theme.colorScheme.surface,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: theme.colorScheme.outlineVariant),
            boxShadow: [
              const BoxShadow(
                color: Color(0x05000000),
                blurRadius: 8,
                offset: Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            children: [
              AnimationUtils.createScaleIn(
                duration: const Duration(milliseconds: 300),
                delay: Duration(milliseconds: 500 + index * 100),
                child: Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: _colorWithAlpha(amountColor, 0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    isIncome ? Icons.arrow_downward : Icons.arrow_upward,
                    color: amountColor,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AnimationUtils.createSlideIn(
                  duration: const Duration(milliseconds: 400),
                  delay: Duration(milliseconds: 600 + index * 100),
                  beginOffset: const Offset(-20, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        bill.categoryName,
                        style: theme.textTheme.bodyLarge?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        remark,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        dateLabel,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              AnimationUtils.createSlideIn(
                duration: const Duration(milliseconds: 400),
                delay: Duration(milliseconds: 700 + index * 100),
                beginOffset: const Offset(20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      '${isIncome ? '+' : '-'}¥${bill.amount.toStringAsFixed(2)}',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: amountColor,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      isIncome ? '收入' : '支出',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Color _colorWithAlpha(Color color, double opacity) {
    final alpha = (opacity * 255).round().clamp(0, 255);
    return color.withAlpha(alpha.toInt());
  }

  List<Bill> _getFilteredBills(List<Bill> bills) {
    Iterable<Bill> filtered = bills;

    if (_selectedFilterIndex == 1) {
      filtered = filtered.where((bill) => bill.type == 1);
    } else if (_selectedFilterIndex == 2) {
      filtered = filtered.where((bill) => bill.type == 2);
    }

    if (_selectedCategory != '全部') {
      filtered = filtered.where(
        (bill) => bill.categoryName == _selectedCategory,
      );
    }

    final range = _getSelectedDateRange();
    if (range != null) {
      filtered = filtered.where((bill) {
        final billDate = _parseBillDate(bill.transactionDate);
        if (billDate == null) {
          return false;
        }
        return _isWithinRange(billDate, range);
      });
    }

    final result = filtered.toList()
      ..sort((a, b) => b.transactionDate.compareTo(a.transactionDate));
    return result;
  }

  List<String> _extractCategoriesForSelectedTab(List<Bill> bills) {
    Iterable<Bill> filtered = bills;
    if (_selectedFilterIndex == 1) {
      filtered = filtered.where((bill) => bill.type == 1);
    } else if (_selectedFilterIndex == 2) {
      filtered = filtered.where((bill) => bill.type == 2);
    }

    final categories = <String>{};
    for (final bill in filtered) {
      categories.add(bill.categoryName);
    }

    final sorted = categories.toList()..sort();
    return ['全部', ...sorted];
  }

  DateTimeRange? _getSelectedDateRange() {
    final now = DateTime.now();
    switch (_selectedDateFilter) {
      case '本周':
        final start = now.subtract(Duration(days: now.weekday - 1));
        final end = start.add(const Duration(days: 6));
        return DateTimeRange(start: _startOfDay(start), end: _endOfDay(end));
      case '本月':
        final start = DateTime(now.year, now.month, 1);
        final end = DateTime(now.year, now.month + 1, 0);
        return DateTimeRange(start: _startOfDay(start), end: _endOfDay(end));
      case '自定义':
        if (_customDateRange != null) {
          return DateTimeRange(
            start: _startOfDay(_customDateRange!.start),
            end: _endOfDay(_customDateRange!.end),
          );
        }
        return null;
      default:
        return null;
    }
  }

  DateTime _startOfDay(DateTime date) =>
      DateTime(date.year, date.month, date.day);

  DateTime _endOfDay(DateTime date) =>
      DateTime(date.year, date.month, date.day, 23, 59, 59);

  bool _isWithinRange(DateTime date, DateTimeRange range) {
    return !date.isBefore(range.start) && !date.isAfter(range.end);
  }

  DateTime? _parseBillDate(String value) {
    try {
      return DateTime.parse(value);
    } catch (_) {
      return null;
    }
  }

  String _formatDisplayDate(String raw) {
    final parsed = _parseBillDate(raw);
    if (parsed == null) {
      return raw;
    }
    return DateFormat('yyyy/MM/dd').format(parsed);
  }

  String _formatRangeLabel(DateTimeRange range) {
    final formatter = DateFormat('MM/dd');
    return '${formatter.format(range.start)}-${formatter.format(range.end)}';
  }

  Future<void> _pickCustomDateRange(BuildContext context) async {
    final now = DateTime.now();
    final initialRange =
        _customDateRange ??
        DateTimeRange(start: now.subtract(const Duration(days: 6)), end: now);

    final picked = await showDateRangePicker(
      context: context,
      initialDateRange: initialRange,
      firstDate: DateTime(now.year - 5),
      lastDate: DateTime(now.year + 1),
    );

    DateTimeRange? nextRange;
    String? nextFilter;

    if (picked != null) {
      nextRange = picked;
      nextFilter = '自定义';
    } else if (_customDateRange == null) {
      nextFilter = '全部';
    }

    if (!mounted) {
      return;
    }
    if (nextFilter == null && nextRange == null) {
      return;
    }

    setState(() {
      if (nextRange != null) {
        _customDateRange = nextRange;
      }
      if (nextFilter != null) {
        _selectedDateFilter = nextFilter;
      }
    });
  }

  Future<void> _confirmDeleteBill(BuildContext context, Bill bill) async {    
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: const Text('删除记录'),
          content: const Text('确定要删除这条交易记录吗？操作不可撤销。'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error,
              ),
              child: const Text('删除'),
            ),
          ],
        );
      },
    );

    if (!mounted) {
      return;
    }
    if (confirmed != true || bill.id == null) {
      return;
    }

    // 调用异步删除方法
    await _handleDeleteBill(bill);
  }

  Future<void> _handleDeleteBill(Bill bill) async {
    // 获取provider但不在异步函数中使用BuildContext
    final provider = context.read<BillProvider>();
    final success = await provider.deleteBill(bill.id!);
    
    // 在mounted条件下显示删除结果
    if (mounted) {
      _showDeleteResult(success);
    }
  }

  void _showDeleteResult(bool success) {
    // 在mounted条件下显示删除结果
    if (!mounted) return;
    
    if (success) {
      // 使用成功动画组件显示反馈
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle, color: Colors.white),
              const SizedBox(width: 8),
              const Expanded(child: Text('记录已删除')),
            ],
          ),
          backgroundColor: Theme.of(context).colorScheme.primary,
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      );
    } else {
      // 显示删除失败提示
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error, color: Colors.white),
              const SizedBox(width: 8),
              const Expanded(child: Text('删除失败，请重试')),
            ],
          ),
          backgroundColor: Theme.of(context).colorScheme.error,
          duration: const Duration(seconds: 3),
        ),
      );
    }
  }
}
