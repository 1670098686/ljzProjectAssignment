import 'package:flutter/material.dart';

/// 收支概览卡片组件
/// 显示本月的收入、支出和结余信息
class OverviewCardWidget extends StatelessWidget {
  final double income;
  final double expense;
  final double balance;

  const OverviewCardWidget({
    super.key,
    required this.income,
    required this.expense,
    required this.balance,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final monthLabel =
        '${DateTime.now().year}-${DateTime.now().month.toString().padLeft(2, '0')}';

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(13),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题栏
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                Icon(
                  Icons.analytics,
                  color: colorScheme.primary,
                  size: 24,
                ),
                const SizedBox(width: 12),
                Text(
                  '本月概览',
                  style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    monthLabel,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: colorScheme.onPrimaryContainer,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),
          // 数据展示区
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              children: [
                Expanded(
                  child: _buildOverviewItem(
                    context,
                    '收入',
                    '¥${income.toStringAsFixed(2)}',
                    colorScheme.primary,
                  ),
                ),
                Container(
                  height: 64,
                  width: 1,
                  color: theme.colorScheme.outlineVariant,
                ),
                Expanded(
                  child: _buildOverviewItem(
                    context,
                    '支出',
                    '¥${expense.toStringAsFixed(2)}',
                    colorScheme.error,
                  ),
                ),
                Container(
                  height: 64,
                  width: 1,
                  color: theme.colorScheme.outlineVariant,
                ),
                Expanded(
                  child: _buildOverviewItem(
                    context,
                    '结余',
                    '¥${balance.toStringAsFixed(2)}',
                    colorScheme.tertiary,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildOverviewItem(
    BuildContext context,
    String title,
    String amount,
    Color color,
  ) {
    return Column(
      children: [
        Text(
          title,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: Theme.of(context).colorScheme.onSurface.withAlpha(179),
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          amount,
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
            fontWeight: FontWeight.bold,
            color: color,
            letterSpacing: -0.5,
          ),
        ),
        const SizedBox(height: 4),
        Container(
          width: 4,
          height: 4,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
      ],
    );
  }
}