import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';
import '../data/models/saving_goal_model.dart';
import '../data/models/saving_record_model.dart';

/// 储蓄统计组件
class SavingStatisticsWidget extends StatefulWidget {
  final List<SavingGoal> goals;
  final List<SavingRecord> records;

  const SavingStatisticsWidget({
    super.key,
    required this.goals,
    required this.records,
  });

  @override
  State<SavingStatisticsWidget> createState() => _SavingStatisticsWidgetState();
}

class _SavingStatisticsWidgetState extends State<SavingStatisticsWidget>
    with TickerProviderStateMixin {
  late TabController _tabController;
  int touchedIndex = -1;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // TabBar
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          decoration: BoxDecoration(
            color: Colors.grey[200],
            borderRadius: BorderRadius.circular(8),
          ),
          child: TabBar(
            controller: _tabController,
            labelColor: Colors.blue,
            unselectedLabelColor: Colors.grey[600],
            indicatorColor: Colors.blue,
            indicatorWeight: 2,
            indicator: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(6),
            ),
            tabs: const [
              Tab(text: '目标概览'),
              Tab(text: '月度趋势'),
              Tab(text: '成就分析'),
            ],
          ),
        ),
        
        // TabBarView
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
          _buildOverviewCard(totalGoals, completedGoals, totalTarget, totalSaved, overallProgress),
          const SizedBox(height: 16),
          
          // 目标进度列表
          Expanded(
            child: ListView.builder(
              itemCount: widget.goals.length,
              itemBuilder: (context, index) {
                final goal = widget.goals[index];
                return _buildGoalProgressCard(goal);
              },
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
          _buildMonthlySavingsChart(),
          const SizedBox(height: 16),
          
          // 月度数据列表
          _buildMonthlyDataList(),
        ],
      ),
    );
  }

  /// 构建成就分析Tab
  Widget _buildAchievementAnalysisTab() {
    final monthlySavings = _calculateMonthlySavings();
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
          _buildAchievementStatsCard(totalMonths, averageMonthly, maxMonthly),
          const SizedBox(height: 16),
          
          // 成就进度环
          Text(
            '储蓄成就',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 16),
          _buildAchievementRings(),
          const SizedBox(height: 16),
          
          // 里程碑
          _buildMilestonesList(),
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
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.blue),
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
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                ),
                Text(
                  '${(goal.progress * 100).toStringAsFixed(1)}%',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: goal.isCompleted ? Colors.green : Colors.blue,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: goal.progress,
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation<Color>(
                goal.isCompleted ? Colors.green : Colors.blue,
              ),
            ),
            const SizedBox(height: 4),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '已储蓄: ¥${NumberFormat('#,###').format(goal.currentAmount)}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                Text(
                  '目标: ¥${NumberFormat('#,###').format(goal.targetAmount)}',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 构建月度储蓄图表
  Widget _buildMonthlySavingsChart() {
    final monthlyData = _getMonthlySavingsData();
    
    return SizedBox(
      height: 200,
      child: LineChart(
        LineChartData(
          titlesData: FlTitlesData(
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 22,
                getTitlesWidget: (value, meta) {
                  final month = DateFormat('MM月').format(DateTime(2024, value.toInt()));
                  return Text(month, style: const TextStyle(fontSize: 12));
                },
              ),
            ),
            leftTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 30,
                getTitlesWidget: (value, meta) {
                  return Text(
                    '¥${(value / 1000).toStringAsFixed(0)}k',
                    style: const TextStyle(fontSize: 10),
                  );
                },
              ),
            ),
          ),
          borderData: FlBorderData(show: false),
          gridData: const FlGridData(show: false),
          lineBarsData: [
            LineChartBarData(
              spots: monthlyData,
              isCurved: true,
              color: Colors.blue,
              barWidth: 3,
              isStrokeCapRound: true,
              dotData: const FlDotData(show: true),
              belowBarData: BarAreaData(
                show: true,
                color: Colors.blue.withAlpha(26), // alpha(255*0.1 = 25.5 ≈ 26)
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 构建月度数据列表
  Widget _buildMonthlyDataList() {
    final monthlyData = _calculateMonthlySavings();
    
    return Expanded(
      child: ListView.builder(
        itemCount: monthlyData.length,
        itemBuilder: (context, index) {
          final entry = monthlyData.entries.elementAt(index);
          final month = '${entry.key.substring(0, 4)}年${entry.key.substring(5, 7)}月';
          final amount = entry.value;
          
          return Card(
            child: ListTile(
              leading: const Icon(Icons.calendar_today, color: Colors.blue),
              title: Text(month),
              trailing: Text(
                '¥${NumberFormat('#,###').format(amount)}',
                style: const TextStyle(color: Colors.blue, fontWeight: FontWeight.bold),
              ),
            ),
          );
        },
      ),
    );
  }

  /// 构建成就统计卡片
  Widget _buildAchievementStatsCard(int totalMonths, double averageMonthly, double maxMonthly) {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text(
              '储蓄成就分析',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildAchievementStat('储蓄月数', '$totalMonths 月'),
                _buildAchievementStat('月均储蓄', '¥${NumberFormat('#,###').format(averageMonthly)}'),
                _buildAchievementStat('最高月储蓄', '¥${NumberFormat('#,###').format(maxMonthly)}'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 构建成就统计项目
  Widget _buildAchievementStat(String label, String value) {
    return Column(
      children: [
        Text(
          value,
          style: Theme.of(context).textTheme.titleLarge?.copyWith(color: Colors.green),
        ),
        Text(
          label,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
        ),
      ],
    );
  }

  /// 构建成就环
  Widget _buildAchievementRings() {
    final achievements = _getAchievements();
    
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: achievements.map((achievement) {
        return _buildAchievementRing(achievement);
      }).toList(),
    );
  }

  /// 构建单个成就环
  Widget _buildAchievementRing(Map<String, dynamic> achievement) {
    final progress = achievement['progress'] as double;
    final icon = achievement['icon'] as IconData;
    final color = achievement['color'] as Color;
    final label = achievement['label'] as String;
    
    return Column(
      children: [
        SizedBox(
          width: 60,
          height: 60,
          child: CircularProgressIndicator(
            value: progress,
            backgroundColor: Colors.grey[200],
            valueColor: AlwaysStoppedAnimation<Color>(color),
            strokeWidth: 4,
          ),
        ),
        const SizedBox(height: 8),
        Icon(icon, color: color, size: 24),
        const SizedBox(height: 4),
        Text(
          label,
          style: Theme.of(context).textTheme.bodySmall,
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  /// 构建里程碑列表
  Widget _buildMilestonesList() {
    final milestones = _getMilestones();
    
    return Expanded(
      child: ListView.builder(
        itemCount: milestones.length,
        itemBuilder: (context, index) {
          final milestone = milestones[index];
          final isUnlocked = milestone['unlocked'] as bool;
          final icon = milestone['icon'] as IconData;
          final title = milestone['title'] as String;
          final description = milestone['description'] as String;
          
          return Card(
            color: isUnlocked ? Colors.green[50] : Colors.grey[50],
            child: ListTile(
              leading: Icon(
                icon,
                color: isUnlocked ? Colors.green : Colors.grey,
              ),
              title: Text(
                title,
                style: TextStyle(
                  color: isUnlocked ? Colors.green[800] : Colors.grey[600],
                  fontWeight: isUnlocked ? FontWeight.bold : FontWeight.normal,
                ),
              ),
              subtitle: Text(description),
              trailing: isUnlocked 
                ? const Icon(Icons.check_circle, color: Colors.green)
                : const Icon(Icons.lock, color: Colors.grey),
            ),
          );
        },
      ),
    );
  }

  // 辅助方法
  List<FlSpot> _getMonthlySavingsData() {
    final monthlyData = _calculateMonthlySavings();
    final sortedMonths = monthlyData.keys.toList()..sort();
    
    return sortedMonths.asMap().entries.map((entry) {
      final x = entry.key + 1.0;
      final y = (entry.value as double);
      return FlSpot(x, y);
    }).toList();
  }

  /// 计算月度储蓄数据
  Map<String, double> _calculateMonthlySavings() {
    final Map<String, double> monthlySavings = {};
    
    for (final record in widget.records) {
      if (record.date.year < 2024) continue;
      
      final monthKey = '${record.date.year}-${record.date.month.toString().padLeft(2, '0')}';
      monthlySavings[monthKey] = (monthlySavings[monthKey] ?? 0.0) + record.amount;
    }
    
    return monthlySavings;
  }

  List<Map<String, dynamic>> _getAchievements() {
    final totalSaved = widget.records.fold(0.0, (sum, record) => sum + record.amount);
    final monthlyGoalAmount = 1000.0; // 月储蓄目标
    final monthlyData = _calculateMonthlySavings();
    final monthsWithGoal = monthlyData.values.where((amount) => amount >= monthlyGoalAmount).length;
    
    return [
      {
        'progress': math.min(1.0, totalSaved / 10000), // 1万元里程碑
        'icon': Icons.military_tech,
        'color': Colors.amber,
        'label': '储蓄新手',
      },
      {
        'progress': math.min(1.0, monthsWithGoal / 12), // 12个月达标
        'icon': Icons.schedule,
        'color': Colors.blue,
        'label': '持续储蓄',
      },
      {
        'progress': math.min(1.0, totalSaved / 50000), // 5万元里程碑
        'icon': Icons.workspace_premium,
        'color': Colors.purple,
        'label': '储蓄达人',
      },
    ];
  }

  List<Map<String, dynamic>> _getMilestones() {
    final totalSaved = widget.records.fold(0.0, (sum, record) => sum + record.amount);
    
    return [
      {
        'unlocked': totalSaved >= 1000,
        'icon': Icons.savings,
        'title': '首次储蓄',
        'description': '完成第一次储蓄',
      },
      {
        'unlocked': widget.goals.where((goal) => goal.isCompleted).isNotEmpty,
        'icon': Icons.flag,
        'title': '目标达成',
        'description': '完成第一个储蓄目标',
      },
      {
        'unlocked': totalSaved >= 10000,
        'icon': Icons.military_tech,
        'title': '万元储蓄',
        'description': '累计储蓄超过1万元',
      },
      {
        'unlocked': widget.records.length >= 50,
        'icon': Icons.list_alt,
        'title': '记录达人',
        'description': '储蓄记录超过50次',
      },
    ];
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }
}