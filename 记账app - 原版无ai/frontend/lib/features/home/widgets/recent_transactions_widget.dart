import 'package:flutter/material.dart';

import '../../../../data/models/bill_model.dart';

/// 最近交易记录组件
/// 显示最近的几笔交易记录
class RecentTransactionsWidget extends StatelessWidget {
  final List<Bill> transactions;
  final VoidCallback? onViewAll;
  final Function(Bill)? onItemTap;

  const RecentTransactionsWidget({
    super.key,
    required this.transactions,
    this.onViewAll,
    this.onItemTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Card(
      elevation: 0,
      color: colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: colorScheme.outline.withAlpha(30)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: colorScheme.surfaceContainerHighest.withAlpha(80),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    Icons.history,
                    size: 20,
                    color: colorScheme.primary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '最近交易',
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '保持收支节奏，随时掌控',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                TextButton.icon(
                  onPressed: onViewAll,
                  icon: Icon(
                    Icons.arrow_forward_ios,
                    size: 14,
                    color: colorScheme.primary,
                  ),
                  label: Text(
                    '查看全部',
                    style: TextStyle(color: colorScheme.primary),
                  ),
                  style: TextButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                ),
              ],
            ),
          ),
          Divider(height: 1, color: colorScheme.outline.withAlpha(80)),
          if (transactions.isEmpty)
            Padding(
              padding: const EdgeInsets.all(20),
              child: _buildEmptyState(context),
            )
          else
            _buildTransactionsList(context, transactions),
        ],
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final screenWidth = MediaQuery.of(context).size.width;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(
          Icons.inbox_outlined,
          size: screenWidth < 380 ? 32 : 40,
          color: colorScheme.onSurfaceVariant,
        ),
        const SizedBox(height: 12),
        Text(
          '暂无交易记录',
          style: theme.textTheme.titleMedium?.copyWith(
            color: colorScheme.onSurface,
            fontWeight: FontWeight.w600,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 4),
        Text(
          '开始记录你的第一笔收支信息',
          style: theme.textTheme.bodySmall?.copyWith(
            color: colorScheme.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: screenWidth < 360 ? double.infinity : 180,
          child: FilledButton.icon(
            onPressed: onViewAll,
            icon: const Icon(Icons.add),
            label: const Text('立即添加'),
          ),
        ),
      ],
    );
  }

  Widget _buildTransactionsList(BuildContext context, List<Bill> transactions) {
    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: transactions.length,
      itemBuilder: (context, index) => _buildTransactionItem(
        context,
        transactions[index],
      ),
      separatorBuilder: (context, index) => Divider(
        indent: 20,
        endIndent: 20,
        color: Theme.of(context).colorScheme.outline.withAlpha(60),
      ),
    );
  }

  Widget _buildTransactionItem(BuildContext context, Bill bill) {
    final bool isIncome = bill.type == 1; // 1=收入, 2=支出
    final colorScheme = Theme.of(context).colorScheme;
    final Color accentColor = isIncome ? Colors.green : Colors.red;

    return InkWell(
      onTap: onItemTap != null ? () => onItemTap!(bill) : null,
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                color: accentColor.withAlpha(28),
              ),
              child: Icon(
                isIncome ? Icons.trending_up : Icons.trending_down,
                color: accentColor,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    bill.categoryName,
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w600,
                        ) ??
                        const TextStyle(fontWeight: FontWeight.w600),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(
                        Icons.schedule,
                        size: 12,
                        color: colorScheme.onSurfaceVariant,
                      ),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          bill.transactionDate,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: colorScheme.onSurfaceVariant,
                                fontSize: 11,
                              ) ??
                              TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 11),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: accentColor.withAlpha(28),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                '${isIncome ? '+' : '-'}¥${bill.amount.toStringAsFixed(2)}',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: accentColor,
                    ) ??
                    TextStyle(fontWeight: FontWeight.bold, color: accentColor),
              ),
            ),
          ],
        ),
      ),
    );
  }
}