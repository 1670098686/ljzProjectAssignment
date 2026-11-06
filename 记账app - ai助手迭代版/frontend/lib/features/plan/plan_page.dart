import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../core/utils/animation_utils.dart';

import '../../app/routes.dart';
import '../../core/providers/saving_goal_provider.dart';
import '../../core/providers/budget_provider.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/budget_model.dart';
import '../../data/services/dynamic_statistics_service.dart';

// 导出统计结果类以供内部使用
export '../../data/services/dynamic_statistics_service.dart' 
  show SavingGoalRealTimeStats, BudgetRealTimeStats;
import '../../pages/budget_warning/budget_warning_utils.dart';
import '../../pages/celebration/celebration_utils.dart';
import '../../shared/widgets/error_view.dart';
import '../../core/services/event_bus_service.dart';

enum _PlanTab { budget, goals }

final NumberFormat _compactCurrencyFormatter = NumberFormat.compactCurrency(
  locale: 'zh_CN',
  symbol: '¥',
  decimalDigits: 1,
);

String _formatCurrency(double value) {
  if (value.abs() >= 10000) {
    return _compactCurrencyFormatter.format(value);
  }
  return '¥${value.toStringAsFixed(2)}';
}

class PlanPage extends StatefulWidget {
  const PlanPage({super.key});

  @override
  State<PlanPage> createState() => _PlanPageState();
}

class _PlanPageState extends State<PlanPage> {
  _PlanTab _currentTab = _PlanTab.budget;
  late DateTime _currentMonth;
  final DynamicStatisticsService _dynamicStats = DynamicStatisticsService();
  List<SavingGoalRealTimeStats> _savingGoalRealTimeStats = [];
  List<BudgetRealTimeStats> _budgetRealTimeStats = [];
  List<Budget> _budgets = [];
  
  /// 交易事件订阅
  late StreamSubscription _transactionSubscription;

  @override
  void initState() {
    super.initState();
    _currentMonth = DateTime(DateTime.now().year, DateTime.now().month);
    // 页面初始化时加载所有必要数据
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      // 先加载当前标签的数据
      await _loadData();
    });
    
    // 监听交易创建事件，当有新交易时检查预算
    _transactionSubscription = EventBusService.instance.eventBus.on<TransactionCreatedEvent>().listen((event) {
      print('📢 PlanPage收到交易创建事件，开始检查预算...');
      _checkBudgetAfterTransaction();
    });
  }

  Future<void> _loadData() async {
    if (_currentTab == _PlanTab.budget) {
      await _loadBudgets();
    } else {
      await _loadSavingGoals();
    }
  }

  Future<void> _loadSavingGoals() async {
    if (!mounted) return;
    final provider = context.read<SavingGoalProvider>();
    final previouslyCompletedGoalIds = provider.goals
        .where((goal) => goal.isCompleted)
        .map((goal) => goal.id)
        .toSet();

    await provider.loadGoals();

    if (!mounted) return;

    // 计算实时统计
    final goals = provider.goals;
    print('🎯 储蓄目标总数: ${goals.length}');
    
    for (var goal in goals) {
      print('🎯 目标: ${goal.name}, 当前金额: ${goal.currentAmount}, 目标金额: ${goal.targetAmount}, 模型计算完成: ${goal.isCompleted}');
    }
    
    _savingGoalRealTimeStats = await _dynamicStats.getSavingGoalsRealTimeStats(
      goals: goals,
      context: context,
    );
    
    print('📊 实时统计结果数: ${_savingGoalRealTimeStats.length}');
    for (var stat in _savingGoalRealTimeStats) {
      print('📊 目标: ${stat.goal.name}, 实际储蓄: ${stat.actualSaved}, 进度: ${stat.progress}, 动态计算完成: ${stat.isCompleted}');
    }

    // 使用动态计算的实时统计数据找出所有已完成的目标
    final completedGoals = _savingGoalRealTimeStats.where((stat) {
      return stat.isCompleted;
    }).map((stat) => stat.goal).toList();
    
    print('🎉 已完成目标数: ${completedGoals.length}');
    for (var goal in completedGoals) {
      print('🎉 已完成目标: ${goal.name}');
    }

    // 对所有已完成的目标，每次进入储蓄目标模块时都播放动画
    if (completedGoals.isNotEmpty) {
      print('🚀 准备播放完成动画');
      // 延迟播放动画，确保UI已渲染完成
      Future.delayed(const Duration(seconds: 1), () {
        if (!mounted) return;
        print('🚀 开始播放完成动画');
        // 播放所有已完成目标的动画
        for (int i = 0; i < completedGoals.length; i++) {
          final goal = completedGoals[i];
          print('🚀 播放目标 ${goal.name} 的动画');
          // 每个目标间隔播放动画
          Future.delayed(Duration(milliseconds: i * 800), () {
            if (!mounted) return;
            print('🚀 执行目标 ${goal.name} 的动画');
            CelebrationUtils.showFullCelebration(
              context,
              goal.name,
              goal.targetAmount,
            );
          });
        }
      });
    } else {
      print('🎯 没有已完成的目标，不播放动画');
    }
    
    setState(() {}); // 刷新UI，确保实时统计数据更新到UI
  }

  Future<void> _loadBudgets() async {
    if (!mounted) return;
    final provider = context.read<BudgetProvider>();
    await provider.loadBudgets(_currentMonth.year, _currentMonth.month);

    if (!mounted) return;

    // 计算实时统计
    final budgets = provider.budgets;
    _budgets = budgets;
    _budgetRealTimeStats = await _dynamicStats.getBudgetsRealTimeStats(
      budgets: budgets,
      context: context,
    );
    
    // 延迟检查预算预警，确保UI已渲染完成
    Future.delayed(const Duration(seconds: 1), () async {
      if (!mounted) return;
      print('🔍 开始检查预算预警...');
      // 重置已显示的预警记录，允许重复显示
      BudgetWarningUtils.resetShownWarnings();
      // 检查并触发预算预警
      await BudgetWarningUtils.checkAndTriggerBudgetWarning(context, provider);
    });
    
    setState(() {}); // 刷新UI，确保实时统计数据更新到UI
  }

  void _changeMonth(int delta) {
    setState(() {
      _currentMonth = DateTime(_currentMonth.year, _currentMonth.month + delta);
    });
  }
  
  @override
  void dispose() {
    // 取消交易事件订阅
    _transactionSubscription.cancel();
    super.dispose();
  }
  
  /// 交易创建后检查预算超支情况
  Future<void> _checkBudgetAfterTransaction() async {
    if (!mounted) return;
    
    print('🔍 开始执行交易后预算检查...');
    
    // 重新加载最新的预算数据
    final provider = context.read<BudgetProvider>();
    await provider.loadBudgets(_currentMonth.year, _currentMonth.month);
    
    if (!mounted) return;
    
    // 检查并触发预算预警
    await BudgetWarningUtils.checkAndTriggerBudgetWarning(context, provider);
    
    print('✅ 交易后预算检查完成');
  }



  Future<void> _openSavingGoalForm() async {
    final result = await context.push(AppRoutes.savingGoalForm);
    if (result is bool && result) {
      _loadSavingGoals();
    }
  }

  Future<void> _editSavingGoal(SavingGoal goal) async {
    final result = await context.push('${AppRoutes.savingGoals}/form/${goal.id}');
    if (result is bool && result) {
      _loadSavingGoals();
    }
  }

  Future<void> _openBudgetForm() async {
    final result = await context.push(AppRoutes.budgetForm);
    if (result is bool && result) {
      _loadBudgets();
    }
  }

  Future<void> _editBudget(Budget budget) async {
    final result = await context.push('${AppRoutes.budgetForm}?id=${budget.id}');
    if (result is bool && result) {
      _loadBudgets();
    }
  }

  Future<void> _confirmDeleteBudget(Budget budget) async {
    if (budget.id == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除预算'),
        content: Text('确定删除预算【${budget.budgetName ?? budget.categoryName}】吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed != true || !mounted) {
      return;
    }

    final success = await context.read<BudgetProvider>().deleteBudget(
      budget.id!,
    );
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(success ? '预算已删除' : '删除预算失败')));
  }

  Future<void> _confirmDeleteGoal(SavingGoal goal) async {
    if (goal.id == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除目标'),
        content: Text('确定删除目标【${goal.name}】吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed != true || !mounted) {
      return;
    }

    final success = await context.read<SavingGoalProvider>().deleteGoal(
      goal.id!,
    );
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(success ? '目标已删除' : '删除目标失败')));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _buildPageHeader(theme),
            _buildSegmentedControl(theme),
            Expanded(
              child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: KeyedSubtree(
                    key: ValueKey(_currentTab),
                    child: _currentTab == _PlanTab.budget
                        ? _buildBudgetView()
                        : _buildSavingGoalsView(),
                  ),
                ),
            ),
          ],
        ),
      ),
      floatingActionButton: _buildFloatingActionButton(),
    );
  }

  Widget _buildPageHeader(ThemeData theme) {
    final monthLabel = DateFormat('yyyy年MM月').format(_currentMonth);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '计划',
                  style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '统筹本月预算与储蓄目标',
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: theme.colorScheme.primaryContainer,
              borderRadius: BorderRadius.circular(24),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.calendar_month,
                  size: 18,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
                const SizedBox(width: 6),
                Text(
                  monthLabel,
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: theme.colorScheme.onPrimaryContainer,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSegmentedControl(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: SegmentedButton<_PlanTab>(
        selected: {_currentTab},
        onSelectionChanged: (newSelection) {
          setState(() {
            _currentTab = newSelection.first;
          });
          // 切换标签页时重新加载数据
          _loadData();
        },
        segments: const [
            ButtonSegment(
              value: _PlanTab.budget,
              label: Text('预算'),
              icon: Icon(Icons.attach_money),
            ),
            ButtonSegment(
              value: _PlanTab.goals,
              label: Text('储蓄目标'),
              icon: Icon(Icons.savings),
            ),
          ],
        style: SegmentedButton.styleFrom(
          backgroundColor: theme.colorScheme.surface,
          side: BorderSide(color: theme.colorScheme.outline),
          selectedBackgroundColor: theme.colorScheme.primaryContainer,
          selectedForegroundColor: theme.colorScheme.onPrimaryContainer,
        ),
      ),
    );
  }



  Widget _buildSavingGoalsView() {
    return Consumer<SavingGoalProvider>(
      builder: (context, provider, _) {
        final goals = provider.goals.map((goal) => goal).toList()
          ..sort((a, b) {
            if (a.isCompleted != b.isCompleted) {
              return a.isCompleted ? 1 : -1;
            }
            return a.deadline.compareTo(b.deadline);
          });

        return RefreshIndicator(
          onRefresh: _loadSavingGoals,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 120),
            children: [
              _buildSavingSummaryCard(context, goals),
              const SizedBox(height: 16),
              if (provider.isLoading && goals.isEmpty)
                _buildLoadingState(context, '正在加载储蓄目标…')
              else if (provider.hasError && goals.isEmpty)
                ErrorView(
                  message: provider.errorMessage ?? '加载失败，请稍后重试',
                  onRetry: _loadSavingGoals,
                )
              else if (goals.isEmpty)
                _buildEmptyState(
                  context,
                  icon: Icons.savings_outlined,
                  title: '暂未创建储蓄目标',
                  subtitle: '用一个新目标开启你的储蓄计划',
                )
              else
                ...goals.asMap().entries.map(
                  (entry) {
                    final index = entry.key;
                    final goal = entry.value;
                    final realTimeStats = _savingGoalRealTimeStats.firstWhere(
                      (stat) => stat.goal.id == goal.id,
                      orElse: () => SavingGoalRealTimeStats(
                        goal: goal,
                        actualSaved: goal.currentAmount,
                        progress: goal.progress,
                        remaining: goal.targetAmount - goal.currentAmount,
                        isCompleted: goal.isCompleted,
                        daysRemaining: goal.deadline.difference(DateTime.now()).inDays,
                        isOverdue: goal.isOverdue,
                        status: goal.isCompleted ? SavingGoalStatus.completed : 
                               goal.isOverdue ? SavingGoalStatus.overdue : 
                               goal.deadline.difference(DateTime.now()).inDays <= 7 ? SavingGoalStatus.urgent : SavingGoalStatus.inProgress,
                      ),
                    );
                    
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: _SavingGoalPlanCard(
                        goal: goal,
                        realTimeStats: realTimeStats,
                        onTap: () => _editSavingGoal(goal),
                        onDelete: () => _confirmDeleteGoal(goal),
                      ),
                    );
                  },
                ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildBudgetSummaryCard(BuildContext context, List<Budget> budgets) {
    final theme = Theme.of(context);
    final totalBudget = budgets.fold<double>(0, (sum, b) => sum + b.amount);
    
    // 使用实时统计数据计算总支出
    final totalSpent = _budgetRealTimeStats.fold<double>(0, (sum, stat) => sum + stat.actualSpent);
    
    final usage = totalBudget == 0
        ? 0.0
        : (totalSpent / totalBudget).clamp(0.0, 1.0);
    
    // 使用实时统计数据计算预警和超支数量
    final warningCount = _budgetRealTimeStats.where((stat) => stat.usage >= 0.8 && stat.usage < 1.0).length;
    final overCount = _budgetRealTimeStats.where((stat) => stat.usage >= 1.0).length;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('本月预算概览', style: theme.textTheme.titleMedium),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _SummaryStat(
                    label: '总预算',
                    value: _formatCurrency(totalBudget),
                  ),
                ),
                Expanded(
                  child: _SummaryStat(
                    label: '已使用',
                    value: _formatCurrency(totalSpent),
                  ),
                ),
                Expanded(
                  child: _SummaryStat(
                    label: '剩余额度',
                    value: _formatCurrency(totalBudget - totalSpent),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('总体进度', style: theme.textTheme.labelMedium),
                Text(
                  '${double.parse((usage * 100).toStringAsFixed(1))}%',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.primary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(value: usage, minHeight: 8),
            const SizedBox(height: 8),
            Text(
              '预警 $warningCount · 超支 $overCount',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSavingSummaryCard(BuildContext context, List<SavingGoal> goals) {
    final theme = Theme.of(context);
    
    // 使用实时统计数据计算总储蓄
    final totalSaved = _savingGoalRealTimeStats.fold<double>(0.0, (sum, stat) => sum + stat.actualSaved);
    final totalTarget = goals.fold<double>(0.0, (sum, g) => sum + g.targetAmount);
    
    // 使用实时统计数据统计完成数量
    final completed = _savingGoalRealTimeStats.where((stat) {
        return stat.progress >= 1.0;
      }).length;
    
    // 使用实时统计数据计算平均完成度
    final avgProgress = _savingGoalRealTimeStats.isNotEmpty
        ? _savingGoalRealTimeStats.map((stat) => stat.progress).reduce((a, b) => a + b) / _savingGoalRealTimeStats.length
        : 0.0;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('储蓄目标概览', style: theme.textTheme.titleMedium),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _SummaryStat(
                    label: '总储蓄',
                    value: _formatCurrency(totalSaved),
                    color: theme.colorScheme.primary,
                  ),
                ),
                Expanded(
                  child: _SummaryStat(
                    label: '目标总额',
                    value: _formatCurrency(totalTarget),
                  ),
                ),
                Expanded(
                  child: _SummaryStat(
                    label: '已完成',
                    value: '$completed/${goals.length}',
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('平均完成度', style: theme.textTheme.labelMedium),
                Text(
                  '${double.parse((avgProgress * 100).toStringAsFixed(1))}%',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.primary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(value: avgProgress, minHeight: 8),
            const SizedBox(height: 8),
            Text(
              goals.isEmpty
                  ? '尚未创建目标'
                  : '继续努力，距离目标还有 ${_formatCurrency((totalTarget - totalSaved).clamp(0, double.infinity))}',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingState(BuildContext context, String label) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 16),
          Text(label, style: theme.textTheme.bodyMedium),
        ],
      ),
    );
  }

  Widget _buildEmptyState(
    BuildContext context, {
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        children: [
          Icon(icon, size: 72, color: theme.colorScheme.outline),
          const SizedBox(height: 16),
          Text(title, style: theme.textTheme.titleMedium),
          const SizedBox(height: 8),
          Text(
            subtitle,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBudgetView() {
    return Consumer<BudgetProvider>(
      builder: (context, provider, _) {
        final budgets = provider.budgets.map((budget) => budget).toList()
          ..sort((a, b) {
            final statA = _budgetRealTimeStats.firstWhere((stat) => stat.budgetId == a.id, orElse: () => BudgetRealTimeStats(budgetId: a.id, actualSpent: 0, budgetedAmount: a.amount, usage: 0));
            final statB = _budgetRealTimeStats.firstWhere((stat) => stat.budgetId == b.id, orElse: () => BudgetRealTimeStats(budgetId: b.id, actualSpent: 0, budgetedAmount: b.amount, usage: 0));
            final usageA = statA.usage;
            final usageB = statB.usage;
            if (usageA >= 1 && usageB < 1) return -1;
            if (usageA < 1 && usageB >= 1) return 1;
            if (usageA >= 0.8 && usageB < 0.8) return -1;
            if (usageA < 0.8 && usageB >= 0.8) return 1;
            return a.categoryName.compareTo(b.categoryName);
          });

        return RefreshIndicator(
          onRefresh: _loadBudgets,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 120),
            children: [
              _buildBudgetSummaryCard(context, budgets),
              const SizedBox(height: 8),
              const SizedBox(height: 16),
              if (provider.isLoading && budgets.isEmpty)
                _buildLoadingState(context, '正在加载预算数据…')
              else if (provider.hasError && budgets.isEmpty)
                ErrorView(
                  message: provider.errorMessage ?? '加载失败，请稍后重试',
                  onRetry: _loadBudgets,
                )
              else if (budgets.isEmpty)
                _buildEmptyState(
                  context,
                  icon: Icons.money_off_outlined,
                  title: '本月暂无预算',
                  subtitle: '创建一个预算计划，合理管理您的支出',
                )
              else
                ...budgets.asMap().entries.map(
                  (entry) {
                    final index = entry.key;
                    final budget = entry.value;
                    final realTimeStats = _budgetRealTimeStats.firstWhere((stat) => stat.budgetId == budget.id, orElse: () => BudgetRealTimeStats(budgetId: budget.id, actualSpent: 0, budgetedAmount: budget.amount, usage: 0));
                    
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: AnimationUtils.createSlideIn(
                        child: AnimationUtils.createScaleIn(
                          child: _BudgetPlanCard(
                            budget: budget,
                            realTimeStats: realTimeStats,
                            onTap: () => _editBudget(budget),
                            onDelete: () => _confirmDeleteBudget(budget),
                          ),
                          beginScale: 0.95,
                          duration: const Duration(milliseconds: 300),
                        ),
                        beginOffset: const Offset(0, 20),
                        duration: const Duration(milliseconds: 300),
                        delay: Duration(milliseconds: index * 100),
                      ),
                    );
                  },
                ),
            ],
          ),
        );
      },
    );
  }
  
  /// 异步更新预算实时统计数据
  Future<void> _updateBudgetRealTimeStats(List<Budget> budgets) async {
    if (budgets.isEmpty) {
      setState(() {
        _budgetRealTimeStats = [];
      });
      return;
    }
    
    final stats = await _dynamicStats.getBudgetsRealTimeStats(
      budgets: budgets,
      context: context,
    );
    
    if (!mounted) return;
    
    setState(() {
      _budgetRealTimeStats = stats;
    });
  }
  
  /// 异步更新储蓄目标实时统计数据
  Future<void> _updateSavingGoalRealTimeStats(List<SavingGoal> goals) async {
    if (goals.isEmpty) {
      setState(() {
        _savingGoalRealTimeStats = [];
      });
      return;
    }
    
    final stats = await _dynamicStats.getSavingGoalsRealTimeStats(
      goals: goals,
      context: context,
    );
    
    if (!mounted) return;
    
    setState(() {
      _savingGoalRealTimeStats = stats;
    });
  }

  Widget _buildFloatingActionButton() {
    return FloatingActionButton.extended(
      onPressed: _currentTab == _PlanTab.budget ? _openBudgetForm : _openSavingGoalForm,
      label: Text(_currentTab == _PlanTab.budget ? '新建预算' : '新建目标'),
      icon: Icon(_currentTab == _PlanTab.budget ? Icons.attach_money : Icons.savings_outlined),
    );
  }
}

class _MonthSelector extends StatelessWidget {
  const _MonthSelector({
    required this.month,
    required this.onPrevious,
    required this.onNext,
  });

  final DateTime month;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final label = DateFormat('yyyy年MM月').format(month);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            IconButton(
              onPressed: onPrevious,
              icon: const Icon(Icons.chevron_left),
              tooltip: '上一月',
            ),
            Row(
              children: [
                Icon(Icons.calendar_month, color: theme.colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  label,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
            IconButton(
              onPressed: onNext,
              icon: const Icon(Icons.chevron_right),
              tooltip: '下一月',
            ),
          ],
        ),
      ),
    );
  }
}

class _BudgetPlanCard extends StatelessWidget {
  const _BudgetPlanCard({
    required this.budget, 
    this.onTap, 
    this.onDelete,
    this.realTimeStats,
  });

  final Budget budget;
  final VoidCallback? onTap;
  final VoidCallback? onDelete;
  final BudgetRealTimeStats? realTimeStats;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    // 使用实时统计数据
    final spent = realTimeStats?.actualSpent ?? 0.0;
    final usage = realTimeStats?.usage ?? 0.0;
    
    final statusColor = usage >= 1
        ? theme.colorScheme.error
        : usage >= 0.8
        ? theme.colorScheme.tertiary
        : theme.colorScheme.primary;
    final statusLabel = usage >= 1
        ? '超支'
        : usage >= 0.8
        ? '预警'
        : '正常';

    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      onLongPress: onDelete,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: theme.colorScheme.outlineVariant),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        budget.budgetName ?? budget.categoryName,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${budget.year}年${budget.month.toString().padLeft(2, '0')}月',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha((0.12 * 255).round()),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    statusLabel,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: statusColor,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            LinearProgressIndicator(
              value: usage.clamp(0.0, 1.0),
              minHeight: 8,
              backgroundColor: theme.colorScheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(statusColor),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '已用 ${_formatCurrency(spent)}',
                  style: theme.textTheme.bodyMedium,
                ),
                Text(
                  '预算 ${_formatCurrency(budget.amount)}',
                  style: theme.textTheme.bodyMedium,
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  usage >= 1
                      ? '超支 ${_formatCurrency(spent - budget.amount)}'
                      : '剩余 ${_formatCurrency((budget.amount - spent).clamp(0, double.infinity))}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: usage >= 1
                        ? theme.colorScheme.error
                        : theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                Row(
                  children: [
                    TextButton.icon(
                      onPressed: onTap,
                      icon: const Icon(Icons.edit_outlined, size: 18),
                      label: const Text('编辑'),
                    ),
                    IconButton(
                      onPressed: onDelete,
                      tooltip: '删除预算',
                      icon: const Icon(Icons.delete_outline),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _SavingGoalPlanCard extends StatelessWidget {
  const _SavingGoalPlanCard({
    required this.goal,
    this.onTap,
    this.onDelete,
    this.realTimeStats,
  });

  final SavingGoal goal;
  final VoidCallback? onTap;
  final VoidCallback? onDelete;
  final SavingGoalRealTimeStats? realTimeStats;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    // 使用实时统计数据
    final progress = (realTimeStats?.progress ?? goal.progress).clamp(0.0, 1.0);
    final actualSaved = realTimeStats?.actualSaved ?? goal.currentAmount;
    final daysLeft = goal.deadline.difference(DateTime.now()).inDays;
    
    final statusColor = progress >= 1
        ? theme.colorScheme.primary
        : daysLeft < 0
        ? theme.colorScheme.error
        : theme.colorScheme.tertiary;
    final statusLabel = progress >= 1
        ? '已完成'
        : daysLeft < 0
        ? '已到期'
        : '进行中';

    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      onLongPress: onDelete,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: theme.colorScheme.outlineVariant),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        goal.name,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if ((goal.description ?? '').isNotEmpty) ...[
                        const SizedBox(height: 4),
                        Text(
                          goal.description!,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha((0.12 * 255).round()),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    statusLabel,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: statusColor,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            LinearProgressIndicator(
              value: progress,
              minHeight: 8,
              backgroundColor: theme.colorScheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(statusColor),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '已存 ${_formatCurrency(actualSaved)}',
                  style: theme.textTheme.bodyMedium,
                ),
                Text(
                  '目标 ${_formatCurrency(goal.targetAmount)}',
                  style: theme.textTheme.bodyMedium,
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  progress >= 1
                      ? '目标已完成'
                      : daysLeft >= 0
                      ? '剩余 $daysLeft 天 · 仍需 ${_formatCurrency((goal.targetAmount - actualSaved).clamp(0, double.infinity))}'
                      : '已逾期 · 仍需 ${_formatCurrency((goal.targetAmount - actualSaved).clamp(0, double.infinity))}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: progress >= 1
                        ? theme.colorScheme.primary
                        : theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                Row(
                  children: [
                    TextButton.icon(
                      onPressed: onTap,
                      icon: const Icon(Icons.edit_outlined, size: 18),
                      label: const Text('编辑'),
                    ),
                    IconButton(
                      onPressed: onDelete,
                      tooltip: '删除目标',
                      icon: const Icon(Icons.delete_outline),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _SummaryStat extends StatelessWidget {
  const _SummaryStat({required this.label, required this.value, this.color});

  final String label;
  final String value;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: theme.textTheme.titleMedium?.copyWith(
            color: color ?? theme.colorScheme.onSurface,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}
