import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'dart:math' as math;
import '../../data/models/saving_goal_model.dart';

/// 储蓄统计组件（修复版本）
class OptimizedSavingStatisticsWidget extends StatefulWidget {
  final List<SavingGoal> goals;
  final Map<String, double>? monthlySavings;

  const OptimizedSavingStatisticsWidget({
    super.key,
    required this.goals,
    this.monthlySavings,
  });

  @override
  State<OptimizedSavingStatisticsWidget> createState() => _OptimizedSavingStatisticsWidgetState();
}

class _OptimizedSavingStatisticsWidgetState extends State<OptimizedSavingStatisticsWidget> 
    with TickerProviderStateMixin {
  late TabController _tabController;
  Map<String, double>? _monthlySavingsCache;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _monthlySavingsCache = widget.monthlySavings ?? {};
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.goals.isEmpty) {
      return const Center(
        child: Text('暂无储蓄目标'),
      );
    }

    return Column(
      children: [
        // Tab栏
        TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '目标概览'),
            Tab(text: '月度趋势'),
            Tab(text: '成就分析'),
          ],
        ),
        // Tab内容
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              _buildGoalOverviewTab(),
              _buildMonthlyTrendTab(),
              _buildAchievementAnalysisTab(),
            ],
          ),
        ),
      ],
    );
  }

  /// 构建目标概览Tab
  Widget _buildGoalOverviewTab() {
    final totalGoals = widget.goals.length;
    final completedGoals = widget.goals.where((goal) => goal.isCompleted).length;
    final totalTarget = widget.goals.fold(0.0, (sum, goal) => sum + goal.targetAmount);
    final totalSaved = widget.goals.fold(0.0, (sum, goal) => sum + goal.currentAmount);
    final overallProgress = totalTarget > 0 ? totalSaved / totalTarget : 0.0;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 总览卡片
          RepaintBoundary(
            child: _buildOverviewCard(totalGoals, completedGoals, totalTarget, totalSaved, overallProgress),
          ),
          const SizedBox(height: 16),
          
          // 目标进度列表
          Expanded(
            child: RepaintBoundary(
              child: ListView.builder(
                itemCount: widget.goals.length,
                cacheExtent: 200,
                itemBuilder: (context, index) {
                  final goal = widget.goals[index];
                  return RepaintBoundary(
                    child: _buildGoalProgressCard(goal),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 构建月度趋势Tab
  Widget _buildMonthlyTrendTab() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 月度储蓄图表
          Text(
            '月度储蓄趋势',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 16),
          RepaintBoundary(
            child: _buildMonthlySavingsChart(),
          ),
          const SizedBox(height: 16),
          
          // 月度数据列表
          RepaintBoundary(
            child: _buildMonthlyDataList(),
          ),
        ],
      ),
    );
  }

  /// 构建成就分析Tab
  Widget _buildAchievementAnalysisTab() {
    final monthlySavings = _monthlySavingsCache!;
    final totalMonths = monthlySavings.length;
    final averageMonthly = totalMonths > 0 
        ? monthlySavings.values.reduce((a, b) => a + b) / totalMonths 
        : 0.0;
    final maxMonthly = monthlySavings.values.isNotEmpty 
        ? monthlySavings.values.reduce(math.max) 
        : 0.0;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 成就统计卡片
          RepaintBoundary(
            child: _buildAchievementStatsCard(totalMonths, averageMonthly, maxMonthly),
          ),
          const SizedBox(height: 16),
          
          // 成就进度环
          Text(
            '储蓄成就',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 16),
          RepaintBoundary(
            child: _buildAchievementRings(),
          ),
          const SizedBox(height: 16),
          
          // 里程碑
          RepaintBoundary(
            child: _buildMilestonesList(),
          ),
        ],
      ),
    );
  }

  /// 构建概览卡片
  Widget _buildOverviewCard(int totalGoals, int completedGoals, double totalTarget, double totalSaved, double overallProgress) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildStatItem('总目标', totalGoals.toString(), Icons.flag),
                _buildStatItem('已完成', completedGoals.toString(), Icons.check_circle),
                _buildStatItem('储蓄额', '¥${NumberFormat('#,###').format(totalSaved)}', Icons.savings),
              ],
            ),
            const SizedBox(height: 16),
            
            // 总体进度条
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('总体进度', style: Theme.of(context).textTheme.titleSmall),
                    Text('${(overallProgress * 100).toStringAsFixed(1)}%', 
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(color: Colors.blue)),
                  ],
                ),
                const SizedBox(height: 8),
                LinearProgressIndicator(
                  value: overallProgress,
                  backgroundColor: Colors.grey[200],
                  valueColor: const AlwaysStoppedAnimation<Color>(Colors.blue),
                ),
                const SizedBox(height: 4),
                Text(
                  '目标总额: ¥${NumberFormat('#,###').format(totalTarget)}',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 构建统计项目
  Widget _buildStatItem(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: Colors.blue, size: 24),
        const SizedBox(height: 4),
        Text(
          value,
          style: Theme.of(context).textTheme.titleLarge?.copyWith(color: Colors.blue),
        ),
        Text(
          label,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
        ),
      ],
    );
  }

  /// 构建目标进度卡片
  Widget _buildGoalProgressCard(SavingGoal goal) {
    final progress = goal.progress;
    final progressText = '${(progress * 100).toStringAsFixed(1)}%';

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  goal.isCompleted ? Icons.check_circle : Icons.flag,
                  color: goal.isCompleted ? Colors.green : Colors.blue,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    goal.name,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Text(
                  progressText,
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: goal.isCompleted ? Colors.green : Colors.blue,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: progress.clamp(0.0, 1.0),
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation<Color>(
                goal.isCompleted ? Colors.green : Colors.blue,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '当前: ¥${NumberFormat('#,###').format(goal.currentAmount)}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                Text(
                  '目标: ¥${NumberFormat('#,###').format(goal.targetAmount)}',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              '截止日期: ${DateFormat('yyyy-MM-dd').format(goal.deadline)}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[500]),
            ),
          ],
        ),
      ),
    );
  }

  /// 构建月度储蓄图表
  Widget _buildMonthlySavingsChart() {
    if (_monthlySavingsCache!.isEmpty) {
      return const Center(
        child: Text('暂无月度数据'),
      );
    }

    // 简化的图表实现
    return Container(
      height: 200,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Center(
        child: Text(
          '月度储蓄图表\n（图表功能待实现）',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      ),
    );
  }

  /// 构建月度数据列表
  Widget _buildMonthlyDataList() {
    if (_monthlySavingsCache!.isEmpty) {
      return const Center(
        child: Text('暂无月度数据'),
      );
    }

    return ListView.builder(
      shrinkWrap: true,
      itemCount: _monthlySavingsCache!.length,
      itemBuilder: (context, index) {
        final month = _monthlySavingsCache!.keys.elementAt(index);
        final amount = _monthlySavingsCache![month]!;
        return ListTile(
          leading: const Icon(Icons.calendar_today),
          title: Text(month),
          trailing: Text(
            '¥${NumberFormat('#,###').format(amount)}',
            style: const TextStyle(color: Colors.blue),
          ),
        );
      },
    );
  }

  /// 构建成就统计卡片
  Widget _buildAchievementStatsCard(int totalMonths, double averageMonthly, double maxMonthly) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStatItem('记录月数', totalMonths.toString(), Icons.calendar_month),
                _buildStatItem('月均储蓄', '¥${NumberFormat('#,###').format(averageMonthly)}', Icons.trending_up),
                _buildStatItem('最高月储蓄', '¥${NumberFormat('#,###').format(maxMonthly)}', Icons.emoji_events),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 构建成就进度环
  Widget _buildAchievementRings() {
    return Container(
      height: 150,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(8),
      ),
      child: const Center(
        child: Text('成就进度环\n（功能待实现）'),
      ),
    );
  }

  /// 构建里程碑列表
  Widget _buildMilestonesList() {
    return ListView.builder(
      shrinkWrap: true,
      itemCount: 5, // 示例数据
      itemBuilder: (context, index) {
        return ListTile(
          leading: const Icon(Icons.star),
          title: Text('里程碑 ${index + 1}'),
          subtitle: Text('描述信息 ${index + 1}'),
          trailing: Icon(
            index < 2 ? Icons.check_circle : Icons.radio_button_unchecked,
            color: index < 2 ? Colors.green : Colors.grey,
          ),
        );
      },
    );
  }
}