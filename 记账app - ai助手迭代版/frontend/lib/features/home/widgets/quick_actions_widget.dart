import 'package:flutter/material.dart';

import '../../../shared/utils/constants.dart';

class HomeQuickActions extends StatelessWidget {
  final double todayIncome;
  final double todayExpense;
  final Function(String? timeRange)? onNavigateStatistics;

  const HomeQuickActions({
    super.key,
    required this.todayIncome,
    required this.todayExpense,
    required this.onNavigateStatistics,
  });

  @override
  Widget build(BuildContext context) {
    // 只保留今日统计卡片
    final card = _QuickActionConfig(
      title: '今日统计',
      subtitle: '收入 ¥${todayIncome.toStringAsFixed(2)} · 支出 ¥${todayExpense.toStringAsFixed(2)}',
      icon: Icons.today,
      accent: AppColors.primary,
      onTap: () {
        onNavigateStatistics?.call('today');
      },
    );

    // 只显示单个卡片，全宽显示
    return Container(
      width: double.infinity,
      child: _QuickActionCard(config: card),
    );
  }
}

class _QuickActionConfig {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color accent;
  final VoidCallback onTap;

  const _QuickActionConfig({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.accent,
    required this.onTap,
  });
}

class _QuickActionCard extends StatelessWidget {
  final _QuickActionConfig config;

  const _QuickActionCard({required this.config});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surface,
      borderRadius: BorderRadius.circular(16),
      elevation: 1,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: config.onTap,
        child: LayoutBuilder(
          builder: (context, constraints) {
            final isNarrow = constraints.maxWidth < 200;
            
            return Padding(
              padding: isNarrow 
                  ? const EdgeInsets.all(8) // 减少内边距以适应更小的高度
                  : const EdgeInsets.all(12),
              child: Row(
                children: [
                  Container(
                    width: isNarrow ? 32 : 36, // 进一步缩小图标容器
                    height: isNarrow ? 32 : 36,
                    decoration: BoxDecoration(
                      color: config.accent.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(isNarrow ? 8 : 10),
                    ),
                    child: Icon(
                      config.icon, 
                      color: config.accent,
                      size: isNarrow ? 16 : 20, // 缩小图标大小
                    ),
                  ),
                  SizedBox(width: isNarrow ? 6 : 8), // 减少间距
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          config.title,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                            fontSize: isNarrow ? 12 : 14, // 缩小字体大小
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 1), // 减少间距
                        Text(
                          config.subtitle,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.textTheme.bodySmall?.color?.withValues(alpha: 0.7),
                            fontSize: isNarrow ? 9 : 10, // 缩小字体大小
                          ),
                          maxLines: 1, // 限制为单行显示
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  if (!isNarrow) 
                    Icon(
                      Icons.chevron_right, 
                      color: theme.dividerColor,
                      size: 18, // 缩小箭头图标
                    ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
