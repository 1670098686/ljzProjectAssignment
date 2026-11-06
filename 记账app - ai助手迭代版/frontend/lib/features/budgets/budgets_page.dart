import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import '../../core/utils/animation_utils.dart';

import '../../../core/providers/budget_provider.dart';
import '../../../data/models/budget_model.dart';

import '../../app/routes.dart';
import '../../core/router/navigation_result.dart';
import '../../core/providers/saving_goal_provider.dart';
import '../../data/models/saving_goal_model.dart';
import '../../../pages/budget_warning/budget_warning_utils.dart';

class BudgetsPage extends StatefulWidget {
  const BudgetsPage({super.key});

  @override
  State<BudgetsPage> createState() => _BudgetsPageState();
}

class _BudgetsPageState extends State<BudgetsPage>
    with SingleTickerProviderStateMixin, RouteResultMixin<BudgetsPage> {
  late DateTime _currentMonth;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _currentMonth = DateTime(now.year, now.month);
    _tabController = TabController(length: 3, vsync: this);
    print('📱 BudgetsPage initState 被调用');
    WidgetsBinding.instance.addPostFrameCallback((_) {
      print('📱 开始调用 _loadBudgets 和 _loadSavingGoals');
      _loadBudgets();
      _loadSavingGoals();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadBudgets() async {
    if (!mounted) {
      return;
    }
    print('📊 开始加载预算数据...');
    await context.read<BudgetProvider>().loadBudgets(
      _currentMonth.year,
      _currentMonth.month,
    );
    
    final provider = context.read<BudgetProvider>();
    print('📊 预算数据加载完成，共 ${provider.budgets.length} 条预算数据');
    
    // 打印每条预算的详细信息
    for (final budget in provider.budgets) {
      final spentAmount = budget.spent ?? 0.0;
      final usagePercentage = budget.amount > 0 ? (spentAmount / budget.amount) * 100 : 0.0;
      print('📊 预算ID: ${budget.id}, 分类: ${budget.categoryName}, 预算金额: ${budget.amount}, 已支出: $spentAmount, 使用百分比: $usagePercentage%');
    }
    
    // 延迟检查预算预警，确保UI已渲染完成
    Future.delayed(const Duration(seconds: 1), () async {
      if (!mounted) return;
      print('🔍 开始检查预算预警...');
      print('🔍 检查预算预警时的上下文：$context');
      // 重置已显示的预警记录，允许重复显示
      BudgetWarningUtils.resetShownWarnings();
      // 检查并触发预算预警
      await BudgetWarningUtils.checkAndTriggerBudgetWarning(context, context.read<BudgetProvider>());
      print('🔍 预算预警检查完成');
    });
  }

  Future<void> _loadSavingGoals() async {
    if (!mounted) {
      return;
    }
    await context.read<SavingGoalProvider>().loadGoals();
  }

  void _changeMonth(int delta) {
    setState(() {
      _currentMonth = DateTime(_currentMonth.year, _currentMonth.month + delta);
    });
    _loadBudgets();
  }

  Future<void> _openBudgetForm() async {
    await pushForResult<bool>(
      location: AppRoutes.budgetForm,
      onRefresh: _loadBudgets,
    );
  }

  Future<void> _editBudget(Budget budget) async {
    await pushForResult<bool>(
      location: '${AppRoutes.budgetForm}?id=${budget.id}',
      onRefresh: _loadBudgets,
    );
  }

  void _navigateToSavingGoals() {
    context.go(AppRoutes.savingGoals);
  }

  void _navigateToSavingRecords() {
    context.go('${AppRoutes.savingGoals}/records');
  }

  void _navigateToBudgetTemplates() {
    // 暂时不实现，因为BudgetTemplatePage不存在
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('该功能暂未实现')),
    );
  }

  /// 导航到预算建议页面
  void _navigateToBudgetRecommendations() {
    // 暂时不实现，因为BudgetRecommendationPage不存在
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('该功能暂未实现')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      // 使用主题默认的scaffoldBackgroundColor，与首页保持一致
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(context),
            Expanded(
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildBudgetTab(),
                  _buildSavingsTab(),
                  _buildStatisticsTab(),
                ],
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _openBudgetForm,
        child: const Icon(Icons.add),
        backgroundColor: theme.colorScheme.primary,
        foregroundColor: theme.colorScheme.onPrimary,
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 16),
              Text(
                '计划',
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.onBackground,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '预算管理与储蓄目标',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onBackground.withOpacity(0.7),
                ),
              ),
            ],
          ),
        ),
        Card(
          elevation: 0,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(12),
              topRight: Radius.circular(12),
            ),
          ),
          color: theme.colorScheme.surface,
          margin: EdgeInsets.zero,
          child: TabBar(
            controller: _tabController,
            tabs: const [
              Tab(text: '预算管理', icon: Icon(Icons.list_alt)),
              Tab(text: '储蓄管理', icon: Icon(Icons.savings)),
              Tab(text: '统计分析', icon: Icon(Icons.analytics_outlined)),
            ],
            labelColor: theme.colorScheme.primary,
            unselectedLabelColor: theme.colorScheme.onSurface.withOpacity(0.5),
            indicatorColor: theme.colorScheme.primary,
          ),
        ),
      ],
    );
  }

  Widget _buildBudgetTab() {
    return Consumer<BudgetProvider>(
      builder: (context, provider, _) {
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: Column(
                children: [
                  _MonthSelector(
                    month: _currentMonth,
                    onPrevious: () => _changeMonth(-1),
                    onNext: () => _changeMonth(1),
                  ),
                  SizedBox(height: 8),
                ],
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                onRefresh: _loadBudgets,
                child: _buildBudgetBody(context, provider),
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildBudgetBody(BuildContext context, BudgetProvider provider) {
    final theme = Theme.of(context);

    if (provider.isLoading && provider.budgets.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: 200),
          Center(child: CircularProgressIndicator()),
        ],
      );
    }

    if (provider.hasError && provider.budgets.isEmpty) {
      final message = provider.errorMessage ?? '加载失败，请稍后再试';
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
        children: [_buildErrorView(message: message, onRetry: _loadBudgets)],
      );
    }

    if (provider.budgets.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
        children: [
          Icon(
            Icons.calendar_month_outlined,
            size: 72,
            color: theme.colorScheme.outline,
          ),
          const SizedBox(height: 16),
          Text(
            '本月暂无预算',
            textAlign: TextAlign.center,
            style: theme.textTheme.titleMedium,
          ),
          const SizedBox(height: 8),
          Text(
            '点击右上角添加按钮创建预算计划',
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      );
    }

    // 立即检查预算预警，确保用户进入页面时能看到预警
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Future.delayed(const Duration(milliseconds: 300), () async {
        if (!mounted) return;
        print('🔍 预算页面UI渲染完成，立即检查预算预警...');
        print('🔍 当前预算数量：${provider.budgets.length}');
        
        // 打印所有预算的详细信息用于调试
        for (final budget in provider.budgets) {
          final spentAmount = budget.spent ?? 0.0;
          final usagePercentage = budget.amount > 0 ? (spentAmount / budget.amount) * 100 : 0.0;
          final isOverBudget = spentAmount >= budget.amount;
          print('🔍 预算详情 - ID: ${budget.id}, 分类: ${budget.categoryName}, 预算金额: ${budget.amount}, 已支出: $spentAmount, 使用百分比: $usagePercentage%, 是否超支: $isOverBudget');
        }
        
        // 重置已显示的预警记录，允许重复显示
        BudgetWarningUtils.resetShownWarnings();
        // 检查并触发预算预警
        await BudgetWarningUtils.checkAndTriggerBudgetWarning(context, provider);
        print('🔍 预算预警检查完成');
      });
    });

    final budgets = provider.budgets.map((budget) => budget).toList()
      ..sort((a, b) => a.categoryName.compareTo(b.categoryName));

    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
      itemCount: budgets.length,
      itemBuilder: (context, index) {
        final budget = budgets[index];
        return _buildBudgetCard(
          budget: budget,
          onEdit: () => _editBudget(budget),
          index: index,
        );
      },
    );
  }

  Widget _buildSavingsTab() {
    final theme = Theme.of(context);
    return Consumer<SavingGoalProvider>(
      builder: (context, provider, _) {
        return RefreshIndicator(
          onRefresh: _loadSavingGoals,
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              // 储蓄管理功能卡片
              Card(
                elevation: 2,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            Icons.savings,
                            color: theme.colorScheme.primary,
                            size: 28,
                          ),
                          const SizedBox(width: 12),
                          Text(
                            '储蓄目标管理',
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Text(
                        '设置和管理您的储蓄目标，跟踪储蓄进度',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 20),
                      Row(
                        children: [
                          Expanded(
                            child: ElevatedButton.icon(
                              onPressed: _navigateToSavingGoals,
                              icon: const Icon(Icons.flag),
                              label: const Text('管理目标'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: theme.colorScheme.primary,
                                foregroundColor: theme.colorScheme.onPrimary,
                                padding: const EdgeInsets.symmetric(
                                  vertical: 12,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _navigateToSavingRecords,
                              icon: const Icon(Icons.receipt_long),
                              label: const Text('查看记录'),
                              style: OutlinedButton.styleFrom(
                                padding: const EdgeInsets.symmetric(
                                  vertical: 12,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 20),

              // 储蓄目标概览
              if (provider.goals.isNotEmpty) ...[
                Text(
                  '储蓄目标概览',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 12),
                ...provider.goals
                    .take(3)
                    .map((goal) => _buildGoalPreviewCard(goal))
                    .toList(),
                if (provider.goals.length > 3) ...[
                  const SizedBox(height: 8),
                  Center(
                    child: TextButton(
                      onPressed: _navigateToSavingGoals,
                      child: Text('查看全部 ${provider.goals.length} 个目标'),
                    ),
                  ),
                ],
              ] else ...[
                // 空状态
                Card(
                  color: theme.colorScheme.surfaceContainerHighest.withAlpha(
                    (0.5 * 255).round(),
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      children: [
                        Icon(
                          Icons.flag_outlined,
                          size: 48,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          '暂无储蓄目标',
                          style: theme.textTheme.titleMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          '创建您的第一个储蓄目标开始理财规划',
                          textAlign: TextAlign.center,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 16),
                        ElevatedButton.icon(
                          onPressed: _navigateToSavingGoals,
                          icon: const Icon(Icons.add),
                          label: const Text('创建目标'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: theme.colorScheme.primary,
                            foregroundColor: theme.colorScheme.onPrimary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _buildGoalPreviewCard(SavingGoal goal) {
    final theme = Theme.of(context);
    final progress = goal.progress;
    final progressPercentage = (progress * 100).clamp(0, 100);
    final isCompleted = goal.isCompleted;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isCompleted
              ? theme.colorScheme.primary
              : theme.colorScheme.outline.withAlpha((0.2 * 255).round()),
          width: isCompleted ? 2 : 1,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  isCompleted ? Icons.check_circle : Icons.flag_outlined,
                  color: isCompleted
                      ? theme.colorScheme.primary
                      : theme.colorScheme.onSurfaceVariant,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    goal.name,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                if (isCompleted)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      '已完成',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onPrimaryContainer,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: progress.clamp(0.0, 1.0),
              backgroundColor: theme.colorScheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(
                isCompleted
                    ? theme.colorScheme.primary
                    : theme.colorScheme.primary.withAlpha((0.8 * 255).round()),
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '¥${goal.currentAmount.toStringAsFixed(2)} / ¥${goal.targetAmount.toStringAsFixed(2)}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                Text(
                  '${progressPercentage.toStringAsFixed(0)}%',
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: isCompleted
                        ? theme.colorScheme.primary
                        : theme.colorScheme.onSurface,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorView({
    required String message,
    required VoidCallback onRetry,
  }) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline, size: 48, color: theme.colorScheme.error),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('重试'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatisticsTab() {
    final theme = Theme.of(context);
    return Consumer<BudgetProvider>(
      builder: (context, provider, _) {
        if (provider.isLoading || provider.budgets.isEmpty) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(40),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.analytics_outlined,
                    size: 72,
                    color: theme.colorScheme.outline,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    '暂无统计数据',
                    style: theme.textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    provider.isLoading ? '加载中...' : '创建预算后即可查看统计信息',
                    textAlign: TextAlign.center,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          );
        }

        // 计算总预算和总支出
        final totalBudget = provider.budgets.fold(0.0, (sum, budget) => sum + budget.amount);
        final totalSpent = provider.budgets.fold(0.0, (sum, budget) => sum + (budget.spent ?? 0.0));
        final remainingBudget = totalBudget - totalSpent;
        final overallPercentage = totalBudget > 0 ? (totalSpent / totalBudget).clamp(0.0, 1.0) : 0.0;

        return ListView(
          padding: const EdgeInsets.all(20),
          children: [
            // 总体预算概览卡片
            Card(
              elevation: 2,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '总体预算概览',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 20),
                    
                    // 预算总额
                    _buildStatisticItem(
                      label: '预算总额',
                      value: totalBudget,
                      icon: Icons.account_balance_wallet_outlined,
                      color: theme.colorScheme.primary,
                    ),
                    const SizedBox(height: 16),
                    
                    // 已支出
                    _buildStatisticItem(
                      label: '已支出',
                      value: totalSpent,
                      icon: Icons.money_outlined,
                      color: Colors.red,
                    ),
                    const SizedBox(height: 16),
                    
                    // 剩余预算
                    _buildStatisticItem(
                      label: '剩余预算',
                      value: remainingBudget,
                      icon: Icons.savings_outlined,
                      color: Colors.green,
                    ),
                    const SizedBox(height: 20),
                    
                    // 整体使用进度
                    Text(
                      '整体使用进度',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 8),
                    LinearProgressIndicator(
                      value: overallPercentage,
                      backgroundColor: theme.colorScheme.surfaceContainerHighest,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        overallPercentage > 0.8 
                            ? Colors.orange 
                            : (overallPercentage > 1.0 ? theme.colorScheme.error : theme.colorScheme.primary)
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '已使用 ${(overallPercentage * 100).toStringAsFixed(0)}%',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                        Text(
                          '¥${totalSpent.toStringAsFixed(2)} / ¥${totalBudget.toStringAsFixed(2)}',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 20),
            
            // 分类预算使用情况
            Text(
              '分类预算使用情况',
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            
            // 按分类显示预算使用情况
            ...provider.budgets.map((budget) {
              final spent = budget.spent ?? 0.0;
              final percentage = budget.amount > 0 ? (spent / budget.amount).clamp(0.0, 1.0) : 0.0;
              final isOverBudget = spent > budget.amount;
              
              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            budget.categoryName,
                            style: theme.textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Text(
                            '${(percentage * 100).toStringAsFixed(0)}%',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: isOverBudget 
                                  ? theme.colorScheme.error 
                                  : (percentage > 0.8 ? Colors.orange : theme.colorScheme.primary),
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      LinearProgressIndicator(
                        value: percentage,
                        backgroundColor: theme.colorScheme.surfaceContainerHighest,
                        valueColor: AlwaysStoppedAnimation<Color>(
                          isOverBudget 
                              ? theme.colorScheme.error 
                              : (percentage > 0.8 ? Colors.orange : theme.colorScheme.primary),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            '¥${spent.toStringAsFixed(2)} / ¥${budget.amount.toStringAsFixed(2)}',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                          Text(
                            isOverBudget ? '超支' : '正常',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: isOverBudget 
                                  ? theme.colorScheme.error 
                                  : Colors.green,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ],
        );
      },
    );
  }

  // 构建统计项
  Widget _buildStatisticItem({
    required String label,
    required double value,
    required IconData icon,
    required Color color,
  }) {
    final theme = Theme.of(context);
    final formatter = NumberFormat.currency(locale: 'zh_CN', symbol: '¥');
    
    return Row(
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: color, size: 24),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                formatter.format(value),
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildBudgetCard({
    required Budget budget,
    required VoidCallback onEdit,
    int index = 0,
  }) {
    final theme = Theme.of(context);
    final formatter = NumberFormat.currency(locale: 'zh_CN', symbol: '¥');

    final spentAmount = budget.spent ?? 0;
    final percentage = budget.amount > 0
        ? (spentAmount / budget.amount).clamp(0.0, 1.0)
        : 0.0;
    final isOverBudget = spentAmount > budget.amount;
    final usagePercentage = percentage * 100;

    final progressColor = isOverBudget
        ? theme.colorScheme.error
        : (percentage > 0.8 ? Colors.orange : theme.colorScheme.primary);
    
    // 延迟显示预算预警动画，确保卡片已渲染完成
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        // 超过90%或超支时触发预警动画
        if (usagePercentage >= 90 || isOverBudget) {
          // 使用 Future.delayed 避免阻塞UI
          Future.delayed(const Duration(milliseconds: 500), () {
            if (mounted) {
              BudgetWarningUtils.checkSingleBudgetWarning(context, budget);
            }
          });
        }
      }
    });

    final card = Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: isOverBudget
              ? theme.colorScheme.error.withAlpha((0.3 * 255).round())
              : theme.colorScheme.outline.withAlpha((0.1 * 255).round()),
          width: isOverBudget ? 2 : 1,
        ),
      ),
      child: InkWell(
        onTap: () {
          // 点击预算卡片时触发预算预警动画
          print('🖱️ 点击预算卡片，分类=${budget.categoryName}, 使用率=$usagePercentage%, 是否超支=$isOverBudget');
          
          // 立即调用预算预警检查（不受阈值限制）
          BudgetWarningUtils.checkSingleBudgetWarning(context, budget);
        },
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.category_outlined,
                    color: theme.colorScheme.primary,
                    size: 24,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      budget.categoryName,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  PopupMenuButton(
                    icon: Icon(
                      Icons.more_vert,
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                    itemBuilder: (context) => [
                      PopupMenuItem(
                        onTap: () => Future.delayed(
                          const Duration(milliseconds: 200),
                          onEdit,
                        ),
                        child: const Row(
                          children: [
                            Icon(Icons.edit, size: 20),
                            SizedBox(width: 8),
                            Text('编辑'),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '预算金额',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  Text(
                    formatter.format(budget.amount),
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '已使用',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  Text(
                    formatter.format(spentAmount),
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: isOverBudget
                          ? theme.colorScheme.error
                          : theme.colorScheme.onSurface,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              LinearProgressIndicator(
                value: percentage,
                backgroundColor: theme.colorScheme.surfaceContainerHighest,
                valueColor: AlwaysStoppedAnimation<Color>(progressColor),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    isOverBudget ? '超支' : '剩余',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  Text(
                    formatter.format(budget.amount - spentAmount),
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: isOverBudget
                          ? theme.colorScheme.error
                          : theme.colorScheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
    
    // 添加卡片动画效果，参考储蓄模块
    return AnimationUtils.createSlideIn(
      child: AnimationUtils.createScaleIn(
        child: card,
        beginScale: 0.95,
        duration: const Duration(milliseconds: 300),
      ),
      beginOffset: const Offset(0, 20),
      duration: const Duration(milliseconds: 300),
      delay: Duration(milliseconds: index * 100),
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