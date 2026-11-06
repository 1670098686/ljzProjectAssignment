import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../../../data/models/statistics_model.dart';

/// 分类统计柱状图组件
class CategoryBarChart extends StatefulWidget {
  final List<CategoryStatistics> categoryStats;
  final bool isLoading;
  final String emptyMessage;

  const CategoryBarChart({
    super.key,
    required this.categoryStats,
    this.isLoading = false,
    this.emptyMessage = '暂无分类数据',
  });

  @override
  State<CategoryBarChart> createState() => _CategoryBarChartState();
}

class _CategoryBarChartState extends State<CategoryBarChart> {
  int _touchedBarIndex = -1;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    if (widget.isLoading) {
      return Container(
        height: 250,
        alignment: Alignment.center,
        child: CircularProgressIndicator(
          strokeWidth: 2,
          valueColor: AlwaysStoppedAnimation<Color>(colorScheme.primary),
        ),
      );
    }

    if (widget.categoryStats.isEmpty) {
      return Container(
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
      );
    }

    return SizedBox(
      height: 250,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: widget.categoryStats.map((stat) => stat.amount).reduce((a, b) => a > b ? a : b) * 1.2,
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
              setState(() {
                if (barTouchResponse != null &&
                    barTouchResponse.spot != null) {
                  _touchedBarIndex = barTouchResponse.spot!.touchedBarGroupIndex;
                } else {
                  _touchedBarIndex = -1;
                }
              });
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
              color: colorScheme.outline.withValues(alpha: 0.2),
              strokeWidth: 1,
            ),
          ),
          barGroups: widget.categoryStats.asMap().entries.map((entry) {
            final index = entry.key;
            final stat = entry.value;
            final isTouched = index == _touchedBarIndex;
            final color = _getBarColor(index, theme);

            return BarChartGroupData(
              x: index,
              barRods: [
                BarChartRodData(
                  toY: stat.amount,
                  color: isTouched ? color.withAlpha(255) : color.withAlpha(200),
                  width: 20,
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(4),
                    topRight: Radius.circular(4),
                  ),
                  backDrawRodData: BackgroundBarChartRodData(
                    show: true,
                    color: colorScheme.outline.withValues(alpha: 0.1),
                  ),
                ),
              ],
            );
          }).toList(),
        ),
      ),
    );
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