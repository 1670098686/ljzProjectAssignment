import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../../data/models/statistics_model.dart';

/// 优化的分类统计柱状图组件
class OptimizedCategoryBarChart extends StatefulWidget {
  final List<CategoryStatistics> categoryStats;
  final bool isLoading;
  final String emptyMessage;

  const OptimizedCategoryBarChart({
    super.key,
    required this.categoryStats,
    this.isLoading = false,
    this.emptyMessage = '暂无分类数据',
  });

  @override
  State<OptimizedCategoryBarChart> createState() => _OptimizedCategoryBarChartState();
}

class _OptimizedCategoryBarChartState extends State<OptimizedCategoryBarChart>
    with AutomaticKeepAliveClientMixin {
  final ValueNotifier<int> _touchedBarIndex = ValueNotifier<int>(-1);
  List<BarChartGroupData>? _cachedBarGroups;
  double? _cachedMaxY;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _precomputeChartData();
  }

  @override
  void didUpdateWidget(OptimizedCategoryBarChart oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.categoryStats != oldWidget.categoryStats) {
      _precomputeChartData();
    }
  }

  void _precomputeChartData() {
    if (widget.categoryStats.isEmpty) {
      _cachedBarGroups = null;
      _cachedMaxY = null;
      return;
    }

    // 缓存柱状图组数据
    _cachedBarGroups = widget.categoryStats.asMap().entries.map((entry) {
      final index = entry.key;
      final stat = entry.value;
      final color = _getBarColor(index, Theme.of(context));

      return BarChartGroupData(
        x: index,
        barRods: [
          BarChartRodData(
            toY: stat.amount,
            color: color.withAlpha(200),
            width: 20,
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(4),
              topRight: Radius.circular(4),
            ),
            backDrawRodData: BackgroundBarChartRodData(
              show: true,
              color: Theme.of(context).colorScheme.outline.withValues(alpha: 0.1),
            ),
          ),
        ],
      );
    }).toList();

    // 缓存最大值
    _cachedMaxY = widget.categoryStats.map((stat) => stat.amount).reduce((a, b) => a > b ? a : b) * 1.2;
  }

  @override
  void dispose() {
    _touchedBarIndex.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    super.build(context); // AutomaticKeepAliveClientMixin required call

    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    if (widget.isLoading) {
      return RepaintBoundary(
        child: Container(
          height: 250,
          alignment: Alignment.center,
          child: CircularProgressIndicator(
            strokeWidth: 2,
            valueColor: AlwaysStoppedAnimation<Color>(colorScheme.primary),
          ),
        ),
      );
    }

    if (widget.categoryStats.isEmpty) {
      return RepaintBoundary(
        child: Container(
          height: 250,
          alignment: Alignment.center,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.bar_chart_outlined,
                size: 64,
                color: colorScheme.outline,
              ),
              const SizedBox(height: 16),
              Text(
                widget.emptyMessage,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    return RepaintBoundary(
      child: ValueListenableBuilder<int>(
        valueListenable: _touchedBarIndex,
        builder: (context, touchedIndex, child) {
          return SizedBox(
            height: 250,
            child: BarChart(
              BarChartData(
                alignment: BarChartAlignment.spaceAround,
                maxY: _cachedMaxY!,
                barTouchData: BarTouchData(
                  enabled: true,
                  touchTooltipData: BarTouchTooltipData(
                    getTooltipColor: (group) => colorScheme.surface,
                    getTooltipItem: (group, groupIndex, rod, rodIndex) {
                      final stat = widget.categoryStats[groupIndex];
                      return BarTooltipItem(
                        '${stat.categoryName}\n金额: ¥${stat.amount.toStringAsFixed(2)}\n占比: ${stat.percentage.toStringAsFixed(1)}%',
                        TextStyle(
                          color: colorScheme.onSurface,
                          fontWeight: FontWeight.bold,
                        ),
                      );
                    },
                  ),
                  touchCallback: (flTouchEvent, barTouchResponse) {
                    if (barTouchResponse != null &&
                        barTouchResponse.spot != null) {
                      _touchedBarIndex.value = barTouchResponse.spot!.touchedBarGroupIndex;
                    } else {
                      _touchedBarIndex.value = -1;
                    }
                  },
                ),
                titlesData: FlTitlesData(
                  show: true,
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 30,
                      getTitlesWidget: (value, meta) {
                        final index = value.toInt();
                        if (index >= 0 && index < widget.categoryStats.length) {
                          final stat = widget.categoryStats[index];
                          return Padding(
                            padding: const EdgeInsets.only(top: 8.0),
                            child: Text(
                              stat.categoryName.length > 4
                                  ? '${stat.categoryName.substring(0, 4)}...'
                                  : stat.categoryName,
                              style: TextStyle(
                                color: colorScheme.onSurfaceVariant,
                                fontSize: 10,
                                fontWeight: FontWeight.w500,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              textAlign: TextAlign.center,
                            ),
                          );
                        }
                        return const Text('');
                      },
                    ),
                  ),
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 40,
                      getTitlesWidget: (value, meta) {
                        return Text(
                          '¥${value.toInt()}',
                          style: TextStyle(
                            color: colorScheme.onSurfaceVariant,
                            fontSize: 10,
                          ),
                        );
                      },
                    ),
                  ),
                  topTitles: AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  rightTitles: AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                ),
                borderData: FlBorderData(
                  show: false,
                ),
                gridData: FlGridData(
                  show: true,
                  drawVerticalLine: false,
                  getDrawingHorizontalLine: (value) => FlLine(
                    color: colorScheme.outline.withAlpha(51),
                    strokeWidth: 1,
                  ),
                ),
                barGroups: _updateTouchedBarColors(_cachedBarGroups!, touchedIndex),
              ),
            ),
          );
        },
      ),
    );
  }

  /// 更新被触摸柱状图的颜色
  List<BarChartGroupData> _updateTouchedBarColors(List<BarChartGroupData> barGroups, int touchedIndex) {
    return barGroups.asMap().entries.map((entry) {
      final index = entry.key;
      final group = entry.value;
      final isTouched = index == touchedIndex;
      
      if (group.barRods.isNotEmpty) {
        final originalRod = group.barRods.first;
        final rodColor = originalRod.color ?? Colors.blue;
        final updatedRod = originalRod.copyWith(
          color: isTouched ? rodColor.withAlpha(255) : rodColor.withAlpha(200),
        );
        
        return group.copyWith(
          barRods: [updatedRod],
        );
      }
      
      return group;
    }).toList();
  }

  Color _getBarColor(int index, ThemeData theme) {
    final colors = [
      theme.colorScheme.primary,
      theme.colorScheme.secondary,
      theme.colorScheme.tertiary,
      theme.colorScheme.error,
      theme.colorScheme.primaryContainer,
      theme.colorScheme.secondaryContainer,
      theme.colorScheme.tertiaryContainer,
      theme.colorScheme.errorContainer,
    ];
    return colors[index % colors.length];
  }
}