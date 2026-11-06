import 'dart:math' as math;
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/providers/statistics_provider.dart';
import '../../data/models/statistics_model.dart';
import '../../core/utils/animation_utils.dart';

class StatisticsPage extends StatefulWidget {
  final String? timeRange;
  final bool hasTodayData;
  
  const StatisticsPage({super.key, this.timeRange, this.hasTodayData = false});

  @override
  State<StatisticsPage> createState() => _StatisticsPageState();
}

class _StatisticsPageState extends State<StatisticsPage>
    with SingleTickerProviderStateMixin {
  static const List<String> _timeRanges = ['本日', '本月', '上月', '自定义'];

  String _selectedTimeRange = '本月';
  DateTimeRange? _customDateRange;
  Map<int, List<CategoryStatistics>> _categoryStatsByType = {
    1: <CategoryStatistics>[],
    2: <CategoryStatistics>[],
  };
  int _categoryDetailType = 2; // 1=收入，2=支出

  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late List<Animation<double>> _slideAnimations;

  @override
  void initState() {
    super.initState();

    // 设置初始时间范围
    _selectedTimeRange = widget.timeRange ?? '本月';
    
    print('📊 统计页面: 接收到的参数 - timeRange: ${widget.timeRange}, hasTodayData: ${widget.hasTodayData}');

    // 添加动画控制器
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeIn),
    );

    _slideAnimations = List.generate(
      4,
      (index) => Tween<double>(begin: 50.0, end: 0.0).animate(
        CurvedAnimation(
          parent: _animationController,
          curve: Interval(index * 0.1, 1.0, curve: Curves.easeOut),
        ),
      ),
    );

    // 在build方法执行后启动动画和加载数据
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _animationController.forward();
        _loadData();
      }
    });
  }

  Future<void> _loadData({bool includeSummary = true}) async {
    final provider = context.read<StatisticsProvider>();
    final range = _resolveDateRange(_selectedTimeRange);
    final start = _formatDate(range.start);
    final end = _formatDate(range.end);

    try {
      // 串行加载数据，避免状态冲突
      if (includeSummary) {
        final summaryDate = range.end;
        await provider.loadSummary(
          summaryDate.year,
          summaryDate.month,
          preferLocal: true,
        );
      }

      await _loadCategoryBreakdown(provider, start, end, preferLocal: true);
      await provider.loadTrendStats(
        startDate: start,
        endDate: end,
        preferLocal: true,
      );

      // 如果是从今日统计跳转过来但没有今日数据，显示提示
      if (widget.hasTodayData && _selectedTimeRange == '本日') {
        final hasTodayData = (provider.summary?.totalIncome ?? 0) > 0 || 
                           (provider.summary?.totalExpense ?? 0) > 0;
        if (!hasTodayData && mounted) {
          Future.microtask(() => _showNoTodayDataSnackBar());
        }
      }
    } catch (e) {
      // 忽略单个数据加载失败，允许部分数据展示
      print('📊 统计页面: 数据加载失败 - $e');
    }
  }
  
  void _showNoTodayDataSnackBar() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('今日暂无收支记录'),
        backgroundColor: Theme.of(context).colorScheme.outline,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  Future<void> _loadCategoryBreakdown(
    StatisticsProvider provider,
    String start,
    String end, {
    bool preferLocal = false,
  }) async {
    // 串行加载收入和支出分类统计数据，减少notifyListeners调用次数
    final List<CategoryStatistics> incomeStats = [];
    final List<CategoryStatistics> expenseStats = [];

    // 加载收入分类统计
    await provider.loadCategoryStats(
      startDate: start,
      endDate: end,
      type: 1,
      preferLocal: preferLocal,
    );
    if (!provider.hasError) {
      incomeStats.addAll(List<CategoryStatistics>.from(provider.categoryStats));
    }

    // 加载支出分类统计
    await provider.loadCategoryStats(
      startDate: start,
      endDate: end,
      type: 2,
      preferLocal: preferLocal,
    );
    if (!provider.hasError) {
      expenseStats.addAll(List<CategoryStatistics>.from(provider.categoryStats));
    }

    if (provider.hasError || !mounted) {
      return;
    }

    // 将收入和支出分类统计数据分别存储
    setState(() {
      _categoryStatsByType = {
        1: incomeStats,
        2: expenseStats,
      };
    });
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // 使用context.read获取StatisticsProvider实例，避免自动重新构建
    final provider = context.read<StatisticsProvider>();
    
    return Scaffold(
      // 使用主题默认的scaffoldBackgroundColor，与首页保持一致
      body: SafeArea(
        child: Stack(
          children: [
            _buildBody(context, provider),
            if (provider.isLoading)
              const Positioned(
                left: 0,
                right: 0,
                top: 0,
                child: LinearProgressIndicator(minHeight: 3),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildBody(BuildContext context, StatisticsProvider provider) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return RefreshIndicator(
      onRefresh: _loadData,
      color: colorScheme.primary,
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        child: SafeArea(
          child: AnimatedBuilder(
            animation: _animationController,
            builder: (context, child) {
              return FadeTransition(
                opacity: _fadeAnimation,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 16),

                    // 欢迎文字
                    Transform.translate(
                      offset: Offset(0, _slideAnimations[0].value),
                      child: Padding(
                        padding: const EdgeInsets.all(20.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '统计分析',
                              style: theme.textTheme.headlineMedium
                                  ?.copyWith(
                                    fontWeight: FontWeight.bold,
                                    color: colorScheme.onSurface,
                                  ),
                            ),
                            Text(
                              '全面了解您的收支情况',
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: colorScheme.onSurface.withValues(alpha:
                                  0.7,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),

                    // 时间范围选择器
                    Transform.translate(
                      offset: Offset(0, _slideAnimations[1].value),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20.0,
                        ),
                        child: _buildTimeRangeSelector(context),
                      ),
                    ),
                    const SizedBox(height: 16),

                    // 收支汇总卡片
                    Transform.translate(
                      offset: Offset(0, _slideAnimations[2].value),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20.0,
                        ),
                        child: _buildSummaryCard(context, provider.summary),
                      ),
                    ),
                    const SizedBox(height: 16),

                    // 分类占比分析
                    Transform.translate(
                      offset: Offset(0, _slideAnimations[3].value),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20.0,
                        ),
                        child: _buildCategoryOverview(context),
                      ),
                    ),
                    const SizedBox(height: 16),

                    // 收支趋势图
                    Transform.translate(
                      offset: Offset(0, _slideAnimations[3].value),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20.0,
                        ),
                        child: _buildTrendChart(
                          context,
                          provider.trendStats,
                        ),
                      ),
                    ),
                    const SizedBox(height: 32),
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }



  Widget _buildTimeRangeSelector(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AnimationUtils.createSlideIn(
          duration: const Duration(milliseconds: 400),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _timeRanges.map((range) {
                final isSelected = _selectedTimeRange == range;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(range),
                    selected: isSelected,
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _selectedTimeRange = range;
                          if (range != '自定义') {
                            _customDateRange = null;
                          }
                        });
                        _loadData();
                      }
                    },
                    selectedColor: colorScheme.primary.withAlpha(
                      51,
                    ), // 0.2 * 255 = 51
                    backgroundColor: colorScheme.surfaceContainerHighest,
                    side: BorderSide(
                      color: isSelected
                          ? colorScheme.primary
                          : Colors.transparent,
                      width: 1,
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ),
        // 自定义日期范围选择器
        if (_selectedTimeRange == '自定义') ...[
          const SizedBox(height: 12),
          AnimationUtils.createSlideIn(
            duration: const Duration(milliseconds: 500),
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: colorScheme.outline),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '选择自定义时间范围',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(
                        context,
                      ).colorScheme.onSurface.withAlpha(204), // 0.8 * 255 = 204
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (_customDateRange != null)
                    AnimationUtils.createFadeIn(
                      duration: const Duration(milliseconds: 300),
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 8,
                        ),
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary.withAlpha(
                            26,
                          ), // 0.1 * 255 = 25.5 ≈ 26
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: Theme.of(context).colorScheme.primary,
                          ),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              Icons.calendar_today,
                              size: 16,
                              color: colorScheme.primary,
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                '${_formatDate(_customDateRange!.start)} - ${_formatDate(_customDateRange!.end)}',
                                style: Theme.of(context).textTheme.bodySmall
                                    ?.copyWith(
                                  color: colorScheme.primary,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ),
                            IconButton(
                              icon: const Icon(Icons.edit, size: 16),
                              onPressed: () => _selectCustomDateRange(),
                              constraints: const BoxConstraints(
                                minWidth: 32,
                                minHeight: 32,
                              ),
                              padding: EdgeInsets.zero,
                              color: colorScheme.primary,
                            ),
                            IconButton(
                              icon: const Icon(Icons.clear, size: 16),
                              onPressed: () {
                                setState(() {
                                  _customDateRange = null;
                                });
                                _loadData();
                              },
                              constraints: const BoxConstraints(
                                minWidth: 32,
                                minHeight: 32,
                              ),
                              padding: EdgeInsets.zero,
                              color: colorScheme.outline,
                            ),
                          ],
                        ),
                      ),
                    ),
                  if (_customDateRange == null)
                    AnimationUtils.createFadeIn(
                      duration: const Duration(milliseconds: 300),
                      child: SizedBox(
                        width: double.infinity,
                        child: OutlinedButton.icon(
                          onPressed: () => _selectCustomDateRange(),
                          icon: const Icon(Icons.add),
                          label: const Text('点击选择日期范围'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: colorScheme.primary,
                            side: BorderSide(color: colorScheme.primary),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildSummaryCard(BuildContext context, StatisticsSummary? summary) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    final income = summary?.totalIncome ?? 0;
    final expense = summary?.totalExpense ?? 0;
    final balance = summary?.balance ?? (income - expense);

    final monthLabel =
        '${DateTime.now().year}-'
        '${DateTime.now().month.toString().padLeft(2, '0')}';

    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 600),
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: colorScheme.outline.withValues(alpha: 0.15)),
        ),
        color: colorScheme.surface,
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 标题和月份
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // 标题
                  AnimationUtils.createSlideIn(
                    duration: const Duration(milliseconds: 400),
                    delay: const Duration(milliseconds: 100),
                    child: Text(
                      '收支汇总',
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: colorScheme.onSurface,
                      ),
                    ),
                  ),
                  // 月份标签
                  AnimationUtils.createFadeIn(
                    duration: const Duration(milliseconds: 400),
                    delay: const Duration(milliseconds: 200),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 6,
                      ),
                      decoration: BoxDecoration(
                        color: colorScheme.primaryContainer,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Text(
                        monthLabel,
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: colorScheme.onPrimaryContainer,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              // 收支汇总卡片
              Row(
                children: [
                  // 收入卡片
                  Expanded(
                    child: AnimationUtils.createSlideIn(
                      duration: const Duration(milliseconds: 500),
                      delay: const Duration(milliseconds: 300),
                      child: _buildSummaryItemCard(
                        context,
                        '收入',
                        income,
                        colorScheme.primary,
                        Icons.trending_up,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  // 支出卡片
                  Expanded(
                    child: AnimationUtils.createSlideIn(
                      duration: const Duration(milliseconds: 500),
                      delay: const Duration(milliseconds: 400),
                      child: _buildSummaryItemCard(
                        context,
                        '支出',
                        expense,
                        colorScheme.error,
                        Icons.trending_down,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  // 结余卡片
                  Expanded(
                    child: AnimationUtils.createSlideIn(
                      duration: const Duration(milliseconds: 500),
                      delay: const Duration(milliseconds: 500),
                      child: _buildSummaryItemCard(
                        context,
                        '结余',
                        balance,
                        colorScheme.tertiary,
                        Icons.account_balance_wallet,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 构建汇总项卡片
  Widget _buildSummaryItemCard(
    BuildContext context,
    String label,
    double amount,
    Color color,
    IconData icon,
  ) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          // 图标
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: color, size: 16),
          ),
          const SizedBox(height: 6),

          // 标签
          Flexible(
            child: Text(
              label,
              style: theme.textTheme.labelMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
                fontWeight: FontWeight.w500,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(height: 4),

          // 金额
          Flexible(
            child: Text(
              '¥${amount.toStringAsFixed(2)}',
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: color,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryOverview(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final incomeStats = _categoryStatsByType[1] ?? const <CategoryStatistics>[];
    final expenseStats =
        _categoryStatsByType[2] ?? const <CategoryStatistics>[];

    return AnimationUtils.createSlideIn(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AnimationUtils.createSlideIn(
            duration: const Duration(milliseconds: 500),
            delay: Duration(milliseconds: 600),
            child: Text(
              '分类占比分析',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
                color: colorScheme.onSurface,
              ),
            ),
          ),
          const SizedBox(height: 8),
          AnimationUtils.createFadeIn(
            duration: const Duration(milliseconds: 400),
            delay: Duration(milliseconds: 700),
            child: Text(
              '收入与支出分类的占比、排名一目了然',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          const SizedBox(height: 16),
          AnimationUtils.createSlideIn(
            duration: const Duration(milliseconds: 600),
            delay: Duration(milliseconds: 800),
            child: _buildCategorySplitCharts(context, incomeStats, expenseStats),
          ),
          const SizedBox(height: 24),
          AnimationUtils.createSlideIn(
            duration: const Duration(milliseconds: 600),
            delay: Duration(milliseconds: 900),
            child: _buildCategoryDetailSection(context),
          ),
        ],
      ),
    );
  }

  Widget _buildCategorySplitCharts(
    BuildContext context,
    List<CategoryStatistics> incomeStats,
    List<CategoryStatistics> expenseStats,
  ) {
    final theme = Theme.of(context);
    final cards = [
      _buildCategoryChartCard(
        context,
        title: '收入分类',
        icon: Icons.trending_up,
        accent: theme.colorScheme.primary,
        stats: incomeStats,
      ),
      _buildCategoryChartCard(
        context,
        title: '支出分类',
        icon: Icons.trending_down,
        accent: theme.colorScheme.error,
        stats: expenseStats,
      ),
    ];

    return LayoutBuilder(
      builder: (context, constraints) {
        final isNarrow = constraints.maxWidth < 600;
        if (isNarrow) {
          return Column(
            children: [cards[0], const SizedBox(height: 12), cards[1]],
          );
        }
        return Row(
          children: [
            Expanded(child: cards[0]),
            const SizedBox(width: 12),
            Expanded(child: cards[1]),
          ],
        );
      },
    );
  }

  Widget _buildCategoryChartCard(
    BuildContext context, {
    required String title,
    required IconData icon,
    required Color accent,
    required List<CategoryStatistics> stats,
  }) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final total = stats.fold<double>(0, (sum, item) => sum + item.amount);

    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 600),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: colorScheme.surface,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: colorScheme.outlineVariant),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.04),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AnimationUtils.createSlideIn(
              duration: const Duration(milliseconds: 500),
              child: Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: accent.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(icon, color: accent),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        total == 0 ? '暂无数据' : '合计 ¥${total.toStringAsFixed(2)}',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.textTheme.bodySmall?.color?.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            if (stats.isEmpty)
              AnimationUtils.createFadeIn(
                duration: const Duration(milliseconds: 500),
                child: _buildEmptyCategoryState(context),
              )
            else ...[
              AnimationUtils.createSlideIn(
                duration: const Duration(milliseconds: 700),
                child: SizedBox(height: 200, child: _buildAnimatedDonutChart(context, stats)),
              ),
              const SizedBox(height: 16),
              AnimationUtils.createSlideIn(
                duration: const Duration(milliseconds: 600),
                child: _buildCategoryTopList(context, stats),
              ),
            ],
          ],
        ),
      ),
    );
  }

  /// 构建顶级分类列表（只显示前5名）
  Widget _buildCategoryTopList(
    BuildContext context,
    List<CategoryStatistics> stats,
  ) {
    if (stats.isEmpty) {
      return _buildEmptyCategoryState(context);
    }

    // 只显示前5名
    final topStats = stats.take(5).toList();
    final colors = _categoryPalette(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '分类排名 TOP5',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        
        for (final entry in topStats.asMap().entries)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _buildTopCategoryItem(
              context,
              entry.value,
              colors[entry.key % colors.length],
              entry.key + 1, // 排名
            ),
          ),
      ],
    );
  }

  /// 构建顶级分类单项
  Widget _buildTopCategoryItem(
    BuildContext context,
    CategoryStatistics stat,
    Color color,
    int rank,
  ) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final colorScheme = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: colorScheme.outlineVariant, width: 1),
      ),
      child: Row(
        children: [
          // 排名徽章
          Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                rank.toString(),
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          
          // 分类名称
          Expanded(
            child: Text(
              stat.categoryName,
              style: textTheme.bodyMedium?.copyWith(
                fontWeight: FontWeight.w500,
                color: colorScheme.onSurface,
              ),
            ),
          ),
          
          // 金额和百分比
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '¥${stat.amount.toStringAsFixed(2)}',
                style: textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: colorScheme.onSurface,
                ),
              ),
              Text(
                '${(stat.percentage * 100).toStringAsFixed(1)}%',
                style: textTheme.bodySmall?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAnimatedDonutChart(
    BuildContext context,
    List<CategoryStatistics> stats,
  ) {
    final colors = _categoryPalette(context);
    
    return LayoutBuilder(
      builder: (context, constraints) {
        final radius = math.min(constraints.maxWidth, constraints.maxHeight) * 0.35;
        
        return Center(
          child: TweenAnimationBuilder<double>(
            duration: const Duration(milliseconds: 1200),
            curve: Curves.elasticOut,
            tween: Tween<double>(begin: 0, end: 1),
            builder: (context, value, child) {
              return Transform.scale(
                scale: value,
                child: PieChart(
                  PieChartData(
                    sectionsSpace: 3,
                    centerSpaceRadius: radius * 0.6,
                    sections: [
                      for (final entry in stats.asMap().entries)
                        PieChartSectionData(
                          value: entry.value.amount * value,
                          title: '${(entry.value.percentage * 100).toStringAsFixed(0)}%',
                          color: colors[entry.key % colors.length],
                          radius: radius * 0.3,
                          titleStyle: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                    ],
                    centerSpaceColor: Theme.of(context).colorScheme.surface,
                  ),
                ),
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildEmptyCategoryState(BuildContext context) {
    return Container(
      height: 180,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: const [
          Icon(Icons.pie_chart_outline, size: 48, color: Colors.grey),
          SizedBox(height: 8),
          Text('暂无分类数据'),
        ],
      ),
    );
  }

  Widget _buildCategoryDetailSection(BuildContext context) {
    final theme = Theme.of(context);
    final stats =
        _categoryStatsByType[_categoryDetailType] ??
        const <CategoryStatistics>[];
    final label = _categoryDetailType == 1 ? '收入' : '支出';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '分类明细',
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            SegmentedButton<int>(
              segments: const [
                ButtonSegment(value: 1, label: Text('收入')),
                ButtonSegment(value: 2, label: Text('支出')),
              ],
              selected: {_categoryDetailType},
              onSelectionChanged: (selection) {
                setState(() => _categoryDetailType = selection.first);
              },
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (stats.isEmpty)
          _buildEmptyCategoryState(context)
        else
          _buildCategoryList(context, stats, label),
      ],
    );
  }

  Widget _buildCategoryList(
    BuildContext context,
    List<CategoryStatistics> categoryStats,
    String label,
  ) {
    final colors = _categoryPalette(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '$label分类排名',
          style: Theme.of(
            context,
          ).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 12),

        // 分类列表
        for (final entry in categoryStats.asMap().entries)
          _buildCategoryItem(
            context,
            entry.value,
            colors[entry.key % colors.length],
          ),
      ],
    );
  }

  Widget _buildCategoryItem(
    BuildContext context,
    CategoryStatistics stat,
    Color color,
  ) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final colorScheme = theme.colorScheme;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 6,
            offset: const Offset(0, 1),
          ),
        ],
        border: Border.all(color: colorScheme.outlineVariant, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 分类名称和金额
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // 分类名称
              Row(
                children: [
                  Container(
                    width: 12,
                    height: 12,
                    decoration: BoxDecoration(
                      color: color,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    stat.categoryName,
                    style: textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: colorScheme.onSurface,
                    ),
                  ),
                ],
              ),

              // 金额和占比
              Row(
                children: [
                  Text(
                    '¥${stat.amount.toStringAsFixed(2)}',
                    style: textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: colorScheme.onSurface,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      '${stat.percentage.toStringAsFixed(1)}%',
                      style: textTheme.labelMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: color,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),

          // 进度条
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: (stat.percentage / 100).clamp(0.0, 1.0),
              minHeight: 8,
              valueColor: AlwaysStoppedAnimation<Color>(color.withValues(alpha: 0.8)),
            backgroundColor: color.withValues(alpha: 0.15),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTrendChart(
    BuildContext context,
    List<TrendStatistics> trendStats,
  ) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 800),
      delay: const Duration(milliseconds: 1000),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题和副标题
          AnimationUtils.createSlideIn(
            duration: const Duration(milliseconds: 600),
            delay: const Duration(milliseconds: 1100),
            child: Text(
              '收支趋势图',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
                color: colorScheme.onSurface,
              ),
            ),
          ),
          const SizedBox(height: 8),
          AnimationUtils.createFadeIn(
            duration: const Duration(milliseconds: 500),
            delay: const Duration(milliseconds: 1200),
            child: Text(
              '对比所选时间范围内的收入与支出走势',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 内容容器
          AnimationUtils.createSlideIn(
            duration: const Duration(milliseconds: 700),
            delay: const Duration(milliseconds: 1300),
            child: Container(
              decoration: BoxDecoration(
                color: colorScheme.surface,
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.05),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              padding: const EdgeInsets.all(20),
              child: trendStats.isEmpty
                  ? const Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.show_chart_outlined,
                            size: 64,
                            color: Colors.grey,
                          ),
                          SizedBox(height: 16),
                          Text('暂无趋势数据'),
                          SizedBox(height: 8),
                          Text('该时间范围内暂无曲线可展示。', textAlign: TextAlign.center),
                        ],
                      ),
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        AnimationUtils.createSlideIn(
                          duration: const Duration(milliseconds: 800),
                          delay: const Duration(milliseconds: 1400),
                          child: SizedBox(
                            height: 240,
                            child: _buildAnimatedLineChart(context, trendStats),
                          ),
                        ),
                        const SizedBox(height: 20),
                        AnimationUtils.createSlideIn(
                          duration: const Duration(milliseconds: 600),
                          delay: const Duration(milliseconds: 1500),
                          child: _buildTrendLegend(context),
                        ),
                        const SizedBox(height: 20),
                        AnimationUtils.createSlideIn(
                          duration: const Duration(milliseconds: 600),
                          delay: const Duration(milliseconds: 1600),
                          child: _buildSavingTrendSection(context, trendStats),
                        ),
                        const SizedBox(height: 8),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAnimatedLineChart(
    BuildContext context,
    List<TrendStatistics> trendStats,
  ) {
    final scheme = Theme.of(context).colorScheme;
    final labelStyle = Theme.of(context).textTheme.labelSmall;

    // 直接生成收入和支出数据点，不使用缓存
    final incomeSpots = trendStats
        .asMap()
        .entries
        .map((entry) => FlSpot(entry.key.toDouble(), entry.value.income))
        .toList();

    final expenseSpots = trendStats
        .asMap()
        .entries
        .map((entry) => FlSpot(entry.key.toDouble(), entry.value.expense))
        .toList();

    return TweenAnimationBuilder<double>(
      duration: const Duration(milliseconds: 1500),
      curve: Curves.easeInOutCubic,
      tween: Tween<double>(begin: 0, end: 1),
      builder: (context, value, child) {
        return LineChart(
          LineChartData(
            minY: 0,
            gridData: FlGridData(
              drawVerticalLine: false,
              horizontalInterval: _resolveGridInterval(trendStats),
              getDrawingHorizontalLine: (value) => FlLine(
                color: scheme.outlineVariant,
                strokeWidth: 0.6,
                dashArray: const [4, 4],
              ),
            ),
            titlesData: FlTitlesData(
              leftTitles: AxisTitles(
                sideTitles: SideTitles(
                  reservedSize: 48,
                  showTitles: true,
                  getTitlesWidget: (value, _) => Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Text(
                      value.toStringAsFixed(0),
                      style: labelStyle,
                      textAlign: TextAlign.right,
                    ),
                  ),
                ),
              ),
              bottomTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  interval: 1,
                  getTitlesWidget: (value, _) {
                    final index = value.toInt();
                    if (index < 0 || index >= trendStats.length) {
                      return const SizedBox.shrink();
                    }
                    final label = trendStats[index].date.substring(5);
                    return Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(label, style: labelStyle),
                    );
                  },
                ),
              ),
              rightTitles: const AxisTitles(
                sideTitles: SideTitles(showTitles: false),
              ),
              topTitles: const AxisTitles(
                sideTitles: SideTitles(showTitles: false),
              ),
            ),
            borderData: FlBorderData(
              show: true,
              border: Border(
                bottom: BorderSide(
                  color: scheme.outline,
                  width: 1,
                ),
                left: BorderSide(
                  color: scheme.outline,
                  width: 1,
                ),
                right: const BorderSide(
                  color: Colors.transparent,
                  width: 1,
                ),
                top: const BorderSide(
                  color: Colors.transparent,
                  width: 1,
                ),
              ),
            ),
            maxY: _resolveMaxY(trendStats),
            lineTouchData: LineTouchData(
              enabled: true,
              touchCallback: (FlTouchEvent event, LineTouchResponse? touchInput) {
                // 处理触摸事件
              },
              touchTooltipData: LineTouchTooltipData(
                getTooltipColor: (_) => scheme.primaryContainer,
                getTooltipItems: (touchedSpots) {
                  return touchedSpots.map((LineBarSpot touchedSpot) {
                    final textStyle = TextStyle(
                      color: scheme.onPrimaryContainer,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    );
                    final value = touchedSpot.y;
                    final date = trendStats[touchedSpot.x.toInt()].date;
                    return LineTooltipItem(
                      '${touchedSpot.bar.color == scheme.primary ? '收入' : '支出'}\n¥${value.toStringAsFixed(2)}\n$date',
                      textStyle,
                    );
                  }).toList();
                },
              ),
            ),
            lineBarsData: [
              LineChartBarData(
                spots: incomeSpots,
                isCurved: true,
                color: scheme.primary,
                barWidth: 3,
                isStrokeCapRound: true,
                dotData: FlDotData(
                  show: true,
                  getDotPainter: (spot, percent, barData, index) {
                    return FlDotCirclePainter(
                      radius: 4,
                      color: scheme.primary,
                      strokeWidth: 2,
                      strokeColor: scheme.onPrimary,
                    );
                  },
                ),
                belowBarData: BarAreaData(
                  show: true,
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      scheme.primary.withValues(alpha: 0.3 * value),
              scheme.primary.withValues(alpha: 0.05 * value),
                    ],
                  ),
                ),
              ),
              LineChartBarData(
                spots: expenseSpots,
                isCurved: true,
                color: scheme.error,
                barWidth: 3,
                isStrokeCapRound: true,
                dotData: FlDotData(
                  show: true,
                  getDotPainter: (spot, percent, barData, index) {
                    return FlDotCirclePainter(
                      radius: 4,
                      color: scheme.error,
                      strokeWidth: 2,
                      strokeColor: scheme.onError,
                    );
                  },
                ),
                belowBarData: BarAreaData(
                  show: true,
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      scheme.error.withValues(alpha: 0.3 * value),
              scheme.error.withValues(alpha: 0.05 * value),
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }



  Widget _buildTrendLegend(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final entries = [('收入', scheme.primary), ('支出', scheme.error)];

    return Wrap(
      spacing: 16,
      runSpacing: 8,
      children: [
        for (final entry in entries)
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 10,
                height: 10,
                decoration: BoxDecoration(
                  color: entry.$2,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Text(entry.$1, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
      ],
    );
  }

  double _resolveGridInterval(List<TrendStatistics> data) {
    if (data.isEmpty) {
      return 100;
    }

    final maxValue = data
        .expand((trend) => [trend.income, trend.expense])
        .fold<double>(0, (prev, value) => value > prev ? value : prev);

    if (maxValue <= 500) {
      return 100;
    }
    if (maxValue <= 2000) {
      return 200;
    }
    if (maxValue <= 10000) {
      return 1000;
    }
    return (maxValue / 10).ceilToDouble();
  }

  /// 计算图表的最大Y值
  double _resolveMaxY(List<TrendStatistics> data) {
    if (data.isEmpty) {
      return 1000;
    }

    final maxValue = data
        .expand((trend) => [trend.income, trend.expense])
        .fold<double>(0, (prev, value) => value > prev ? value : prev);

    // 确保有适当的边距
    return maxValue * 1.2;
  }

  /// 构建储蓄趋势显示区域 - 移除储蓄趋势内容，使UI更简洁
  Widget _buildSavingTrendSection(
    BuildContext context,
    List<TrendStatistics> trendStats,
  ) {
    // 移除储蓄趋势显示，使UI更简洁适合手机
    return const SizedBox.shrink();
  }

  List<Color> _categoryPalette(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return [
      scheme.primary,
      scheme.secondary,
      scheme.tertiary,
      scheme.error,
      scheme.primaryContainer,
      scheme.secondaryContainer,
      scheme.tertiaryContainer,
      scheme.errorContainer,
    ].map((color) => color.withAlpha(230)).toList(growable: false);
  }

  DateTimeRange _resolveDateRange(String rangeLabel) {
    final now = DateTime.now();

    switch (rangeLabel) {
      case '自定义':
        // 如果有自定义日期范围则使用，否则返回本月
        if (_customDateRange != null) {
          return _customDateRange!;
        } else {
          // 默认设置为最近30天
          final start = now.subtract(const Duration(days: 30));
          final end = now;
          return DateTimeRange(start: start, end: end);
        }
      case '上月':
        final start = DateTime(now.year, now.month - 1, 1);
        final end = DateTime(now.year, now.month, 0);
        return DateTimeRange(start: start, end: end);
      case '本季度':
        final quarterStartMonth = ((now.month - 1) ~/ 3) * 3 + 1;
        final start = DateTime(now.year, quarterStartMonth, 1);
        final rawEnd = DateTime(now.year, quarterStartMonth + 3, 0);
        final end = rawEnd.isAfter(now) ? now : rawEnd;
        return DateTimeRange(start: start, end: end);
      case '本年':
        final start = DateTime(now.year, 1, 1);
        final rawEnd = DateTime(now.year, 12, 31);
        final end = rawEnd.isAfter(now) ? now : rawEnd;
        return DateTimeRange(start: start, end: end);
      case '本月':
      default:
        final start = DateTime(now.year, now.month, 1);
        final end = DateTime(now.year, now.month + 1, 0);
        return DateTimeRange(start: start, end: end.isAfter(now) ? now : end);
    }
  }

  String _formatDate(DateTime date) {
    final year = date.year.toString().padLeft(4, '0');
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  /// 选择自定义日期范围
  Future<void> _selectCustomDateRange() async {
    final DateTimeRange? result = await showDateRangePicker(
      context: context,
      firstDate: DateTime.now().subtract(const Duration(days: 365)), // 一年前
      lastDate: DateTime.now().add(const Duration(days: 365)), // 一年后
      initialDateRange: _customDateRange,
      helpText: '选择统计时间范围',
      cancelText: '取消',
      confirmText: '确认',
    );

    if (result != null) {
      setState(() {
        _customDateRange = result;
      });
      _loadData();
    }
  }
}
