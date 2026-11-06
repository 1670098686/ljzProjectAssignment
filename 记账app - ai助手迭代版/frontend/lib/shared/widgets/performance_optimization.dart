import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:intl/intl.dart';
import '../../data/models/bill_model.dart';
import '../../data/models/saving_goal_model.dart';

/// 性能优化工具类
/// 提供列表虚拟化、图片缓存等性能优化组件
class PerformanceOptimization {
  /// 禁用所有性能优化调试日志
  static bool debugPerformance = false;
}

/// 列表虚拟化优化组件
class VirtualizedListWidget<T> extends StatelessWidget {
  final List<T> items;
  final Widget Function(BuildContext context, int index, T item) itemBuilder;
  final double? itemExtent;
  final double? cacheExtent;
  final ScrollPhysics? physics;
  final EdgeInsets? padding;
  final bool shrinkWrap;

  const VirtualizedListWidget({
    super.key,
    required this.items,
    required this.itemBuilder,
    this.itemExtent,
    this.cacheExtent = 1000.0,
    this.physics,
    this.padding,
    this.shrinkWrap = false,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: items.length,
      itemExtent: itemExtent,
      cacheExtent: cacheExtent,
      physics: physics ?? const BouncingScrollPhysics(),
      padding: padding,
      shrinkWrap: shrinkWrap,
      itemBuilder: (context, index) {
        if (index >= items.length) {
          return const SizedBox.shrink();
        }
        return itemBuilder(context, index, items[index]);
      },
    );
  }
}

/// 交易列表虚拟化组件
class VirtualizedTransactionList extends StatelessWidget {
  final List<Bill> transactions;
  final Function(Bill)? onTap;
  final ScrollPhysics? physics;
  final EdgeInsets? padding;

  const VirtualizedTransactionList({
    super.key,
    required this.transactions,
    this.onTap,
    this.physics,
    this.padding,
  });

  @override
  Widget build(BuildContext context) {
    return VirtualizedListWidget<Bill>(
      items: transactions,
      itemExtent: 80.0, // 固定高度提升性能
      cacheExtent: 1000.0, // 缓存范围
      physics: physics,
      padding: padding,
      itemBuilder: (context, index, bill) {
        return _TransactionListTile(bill: bill, onTap: onTap, index: index);
      },
    );
  }
}

/// 储蓄目标列表虚拟化组件
class VirtualizedSavingGoalList extends StatelessWidget {
  final List<SavingGoal> goals;
  final Function(SavingGoal)? onTap;
  final Function(SavingGoal)? onEdit;
  final Function(SavingGoal)? onDelete;
  final ScrollPhysics? physics;
  final EdgeInsets? padding;

  const VirtualizedSavingGoalList({
    super.key,
    required this.goals,
    this.onTap,
    this.onEdit,
    this.onDelete,
    this.physics,
    this.padding,
  });

  @override
  Widget build(BuildContext context) {
    return VirtualizedListWidget<SavingGoal>(
      items: goals,
      itemExtent: 180.0, // 储蓄目标需要更多空间
      cacheExtent: 500.0, // 缓存范围
      physics: physics,
      padding: padding,
      itemBuilder: (context, index, goal) {
        return _SavingGoalListTile(
          goal: goal, 
          onTap: onTap, 
          onEdit: onEdit,
          onDelete: onDelete,
          index: index
        );
      },
    );
  }
}

/// 交易列表项组件
class _TransactionListTile extends StatelessWidget {
  final Bill bill;
  final Function(Bill)? onTap;
  final int index;

  const _TransactionListTile({
    required this.bill,
    this.onTap,
    required this.index,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final amountColor = bill.type == 1 ? Colors.green : Colors.red;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap != null ? () => onTap!(bill) : null,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(12),
              boxShadow: [
                BoxShadow(
                  color: theme.shadowColor.withAlpha((0.1 * 255).round()),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Row(
              children: [
                // 分类图标
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: amountColor.withAlpha((0.1 * 255).round()),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(
                    bill.type == 1 ? Icons.arrow_upward : Icons.arrow_downward,
                    color: amountColor,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                // 交易信息
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        bill.remark ?? '无备注',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w500,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        bill.categoryName,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurface.withAlpha((0.6 * 255).round()),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                // 金额
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      '${bill.type == 1 ? '+' : '-'}${bill.amount.toStringAsFixed(2)}',
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: amountColor,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _formatDate(bill.transactionDate),
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurface.withAlpha((0.5 * 255).round()),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _formatDate(String? dateString) {
    if (dateString == null || dateString.isEmpty) {
      return '';
    }

    DateTime? date;
    try {
      date = DateTime.parse(dateString);
    } catch (_) {
      return dateString;
    }
    final now = DateTime.now();
    final difference = now.difference(date);

    if (difference.inDays == 0) {
      return '今天';
    } else if (difference.inDays == 1) {
      return '昨天';
    } else if (difference.inDays < 7) {
      return '${difference.inDays}天前';
    } else {
      return '${date.month}/${date.day}';
    }
  }
}

/// 储蓄目标列表项组件
class _SavingGoalListTile extends StatelessWidget {
  final SavingGoal goal;
  final Function(SavingGoal)? onTap;
  final Function(SavingGoal)? onEdit;
  final Function(SavingGoal)? onDelete;
  final int index;

  const _SavingGoalListTile({
    required this.goal,
    this.onTap,
    this.onEdit,
    this.onDelete,
    required this.index,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final progress = goal.currentAmount / goal.targetAmount;
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

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap != null ? () => onTap!(goal) : null,
        onLongPress: onDelete != null ? () => onDelete!(goal) : null,
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
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if ((goal.description ?? '').isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text(
                            goal.description!,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
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
              // 进度条
              LinearProgressIndicator(
                value: progress.clamp(0.0, 1.0),
                minHeight: 8,
                backgroundColor: theme.colorScheme.surfaceContainerHighest,
                valueColor: AlwaysStoppedAnimation<Color>(statusColor),
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '已存 ${_formatCurrency(goal.currentAmount)}',
                    style: theme.textTheme.bodyMedium,
                  ),
                  Text(
                    '目标 ${_formatCurrency(goal.targetAmount)}',
                    style: theme.textTheme.bodyMedium,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                progress >= 1
                    ? '目标已完成'
                    : daysLeft >= 0
                    ? '剩余 $daysLeft 天 · 仍需 ${_formatCurrency((goal.targetAmount - goal.currentAmount).clamp(0, double.infinity))}'
                    : '已逾期 · 仍需 ${_formatCurrency((goal.targetAmount - goal.currentAmount).clamp(0, double.infinity))}',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(
                    Icons.calendar_today,
                    size: 16,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _formatDate(DateFormat('yyyy-MM-dd').format(goal.deadline)),
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const Spacer(),
                  if (onEdit != null)
                    TextButton(
                      onPressed: () => onEdit!(goal),
                      child: const Text('编辑'),
                    ),
                  if (onDelete != null)
                    IconButton(
                      onPressed: () => onDelete!(goal),
                      tooltip: '删除目标',
                      icon: const Icon(Icons.delete_outline),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatCurrency(double amount) {
    return '¥${amount.toStringAsFixed(2)}';
  }

  String _formatDate(String dateString) {
    if (dateString.isEmpty) {
      return '';
    }

    DateTime? date;
    try {
      date = DateTime.parse(dateString);
    } catch (_) {
      return dateString;
    }
    
    final now = DateTime.now();
    final difference = now.difference(date);

    if (difference.inDays == 0) {
      return '今天';
    } else if (difference.inDays == 1) {
      return '昨天';
    } else if (difference.inDays < 7) {
      return '${difference.inDays}天前';
    } else {
      return dateString;
    }
  }
}

/// 图片缓存优化组件
class OptimizedImageWidget extends StatelessWidget {
  final String imageUrl;
  final double? width;
  final double? height;
  final BoxFit fit;
  final Widget? placeholder;
  final Widget? errorWidget;
  final Duration fadeInDuration;

  const OptimizedImageWidget({
    super.key,
    required this.imageUrl,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
    this.placeholder,
    this.errorWidget,
    this.fadeInDuration = const Duration(milliseconds: 300),
  });

  @override
  Widget build(BuildContext context) {
    if (imageUrl.isEmpty) {
      return errorWidget ?? _defaultErrorWidget(context);
    }

    return CachedNetworkImage(
      imageUrl: imageUrl,
      width: width,
      height: height,
      fit: fit,
      placeholder: (context, url) =>
          placeholder ?? _defaultPlaceholderWidget(context),
      errorWidget: (context, url, error) =>
          errorWidget ?? _defaultErrorWidget(context),
      fadeInDuration: fadeInDuration,
      memCacheWidth: width != null
          ? (width! * 2).toInt()
          : null, // 2x缓存以支持高DPI显示
      memCacheHeight: height != null ? (height! * 2).toInt() : null,
    );
  }

  Widget _defaultPlaceholderWidget(BuildContext context) {
    return Container(
      width: width,
      height: height,
      color: Theme.of(context).colorScheme.surface.withAlpha((0.3 * 255).round()),
      child: Center(
        child: CircularProgressIndicator(
          strokeWidth: 2,
          valueColor: AlwaysStoppedAnimation<Color>(
            Theme.of(context).colorScheme.primary,
          ),
        ),
      ),
    );
  }

  Widget _defaultErrorWidget(BuildContext context) {
    return Container(
      width: width,
      height: height,
      color: Theme.of(context).colorScheme.errorContainer,
      child: Icon(
        Icons.broken_image,
        size: width != null ? width! / 4 : 24,
        color: Theme.of(context).colorScheme.onErrorContainer,
      ),
    );
  }
}

/// 性能监控组件
class PerformanceMonitor extends StatelessWidget {
  final Widget child;
  final String componentName;
  final bool enableDebug;

  const PerformanceMonitor({
    super.key,
    required this.child,
    required this.componentName,
    this.enableDebug = false,
  });

  @override
  Widget build(BuildContext context) {
    if (PerformanceOptimization.debugPerformance && enableDebug) {
      return _PerformanceDebugWrapper(
        componentName: componentName,
        child: child,
      );
    }
    return child;
  }
}

/// 性能调试包装器
class _PerformanceDebugWrapper extends StatefulWidget {
  final Widget child;
  final String componentName;

  const _PerformanceDebugWrapper({
    required this.child,
    required this.componentName,
  });

  @override
  State<_PerformanceDebugWrapper> createState() =>
      _PerformanceDebugWrapperState();
}

class _PerformanceDebugWrapperState extends State<_PerformanceDebugWrapper>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;
  bool _isExpanded = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: 1.05,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.elasticOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => _controller.forward(),
      onExit: (_) => _controller.reverse(),
      child: GestureDetector(
        onTap: () {
          setState(() {
            _isExpanded = !_isExpanded;
          });
        },
        child: AnimatedBuilder(
          animation: _scaleAnimation,
          builder: (context, child) {
            return Transform.scale(
              scale: _scaleAnimation.value,
              child: widget.child,
            );
          },
        ),
      ),
    );
  }
}

/// 延迟加载组件
class LazyLoadWidget extends StatefulWidget {
  final Widget Function() builder;
  final Duration delay;

  const LazyLoadWidget({
    super.key,
    required this.builder,
    this.delay = const Duration(milliseconds: 100),
  });

  @override
  State<LazyLoadWidget> createState() => _LazyLoadWidgetState();
}

class _LazyLoadWidgetState extends State<LazyLoadWidget> {
  Widget? _child;

  @override
  void initState() {
    super.initState();
    _loadWidget();
  }

  Future<void> _loadWidget() async {
    await Future.delayed(widget.delay);
    if (mounted) {
      setState(() {
        _child = widget.builder();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return _child ?? const SizedBox.shrink();
  }
}
