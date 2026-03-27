import 'package:flutter/material.dart';

import '../data/models/saving_goal_model.dart';

/// 储蓄目标进度可视化组件
class SavingGoalVisualization extends StatefulWidget {
  final SavingGoal goal;
  final bool showDetailed;
  final bool compactMode;
  final VoidCallback? onTap;

  const SavingGoalVisualization({
    super.key,
    required this.goal,
    this.showDetailed = false,
    this.compactMode = false,
    this.onTap,
  });

  @override
  State<SavingGoalVisualization> createState() => _SavingGoalVisualizationState();
}

class _SavingGoalVisualizationState extends State<SavingGoalVisualization> with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _scaleAnimation;
  late Animation<double> _rotationAnimation;
  late Animation<double> _fadeAnimation;
  bool _hasShownCompletionAnimation = false;
  bool _isCompleted = false;

  @override
  void initState() {
    super.initState();
    
    _isCompleted = widget.goal.isCompleted;
    
    // 初始化动画控制器
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    
    // 缩放动画
    _scaleAnimation = Tween<double>(begin: 0.8, end: 1.2)
        .chain(CurveTween(curve: Curves.elasticOut))
        .animate(_animationController);
    
    // 旋转动画
    _rotationAnimation = Tween<double>(begin: 0, end: 2 * 3.14159)
        .chain(CurveTween(curve: Curves.easeInOut))
        .animate(_animationController);
    
    // 淡出动画
    _fadeAnimation = Tween<double>(begin: 1.0, end: 0.0)
        .chain(CurveTween(curve: Curves.easeIn))
        .animate(_animationController);
    
    // 如果目标已完成且未显示过动画，播放完成动画
    if (_isCompleted && !_hasShownCompletionAnimation) {
      // 添加延迟，确保UI已渲染完成
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          _playCompletionAnimation();
        }
      });
    }
  }

  @override
  void didUpdateWidget(covariant SavingGoalVisualization oldWidget) {
    super.didUpdateWidget(oldWidget);
    
    final wasCompleted = oldWidget.goal.isCompleted;
    final isNowCompleted = widget.goal.isCompleted;
    
    // 如果从非完成状态变为完成状态，播放动画
    if (!wasCompleted && isNowCompleted && !_hasShownCompletionAnimation) {
      setState(() {
        _isCompleted = true;
      });
      _playCompletionAnimation();
    }
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  void _playCompletionAnimation() {
    _animationController.forward();
    _hasShownCompletionAnimation = true;
  }

  @override
  Widget build(BuildContext context) {
    final progress = widget.goal.progress;
    
    // 根据模式选择不同的视图
    if (widget.compactMode) {
      return _buildCompactView(context, progress);
    } else if (widget.showDetailed) {
      return _buildDetailedView(context, progress);
    } else {
      // 默认紧凑视图
      return _buildCompactView(context, progress);
    }
  }

  Widget _buildCompactView(BuildContext context, double progress) {
    final theme = Theme.of(context);
    
    return GestureDetector(
      onTap: widget.onTap,
      child: Container(
        width: 120,
        height: 120,
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(
            color: theme.colorScheme.outline.withAlpha(77), // 0.3 * 255 = 76.5 ≈ 77
            width: 1,
          ),
        ),
        child: Stack(
          alignment: Alignment.center,
          children: [
            CircularProgressIndicator(
              value: progress > 1.0 ? 1.0 : progress,
              backgroundColor: theme.colorScheme.surfaceContainerHighest,
              color: _isCompleted 
                  ? Colors.green[600] 
                  : theme.colorScheme.primary,
              strokeWidth: 6,
            ),
            Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (_isCompleted)
                  AnimatedBuilder(
                    animation: _animationController,
                    builder: (context, child) {
                      return ScaleTransition(
                        scale: _scaleAnimation,
                        child: Icon(
                          Icons.check_circle,
                          color: Colors.green[600],
                          size: 20 + _scaleAnimation.value * 5,
                        ),
                      );
                    },
                  ),
                Text(
                  '${(progress * 100).toStringAsFixed(0)}%',
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: _isCompleted 
                        ? Colors.green[600] 
                        : theme.colorScheme.primary,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailedView(BuildContext context, double progress) {
    final theme = Theme.of(context);
    
    return Container(
      height: 200,
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          // 左侧：圆形进度图
          SizedBox(
            width: 100,
            height: 100,
            child: Stack(
              alignment: Alignment.center,
              children: [
                CircularProgressIndicator(
                  value: progress > 1.0 ? 1.0 : progress,
                  backgroundColor: theme.colorScheme.surfaceContainerHighest,
                  color: _isCompleted 
                      ? Colors.green[600] 
                      : theme.colorScheme.primary,
                  strokeWidth: 8,
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (_isCompleted)
                      AnimatedBuilder(
                        animation: _animationController,
                        builder: (context, child) {
                          return ScaleTransition(
                            scale: _scaleAnimation,
                            child: RotationTransition(
                              turns: _rotationAnimation,
                              child: Icon(
                                Icons.check_circle,
                                color: Colors.green[600],
                                size: 24 + _scaleAnimation.value * 8,
                              ),
                            ),
                          );
                        },
                      ),
                    Text(
                      '${(progress * 100).toStringAsFixed(0)}%',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: _isCompleted 
                            ? Colors.green[600] 
                            : theme.colorScheme.primary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          
          const SizedBox(width: 24),
          
          // 右侧：详细信息
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  widget.goal.name,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Text(
                      '¥${widget.goal.currentAmount.toStringAsFixed(0)}',
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: _isCompleted 
                            ? Colors.green[600] 
                            : theme.colorScheme.primary,
                      ),
                    ),
                    Text(
                      ' / ¥${widget.goal.targetAmount.toStringAsFixed(0)}',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  _getStatusText(),
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: _getStatusColor(theme),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _getStatusText() {
    if (widget.goal.isCompleted) {
      return '🎉 目标已完成';
    }
    
    if (widget.goal.isOverdue) {
      return '⚠️ 已逾期，请加快进度';
    }
    
    final remainingDays = widget.goal.remainingDays;
    if (remainingDays <= 7) {
      return '🔥 即将截止，还剩 $remainingDays 天';
    } else if (remainingDays <= 30) {
      return '⏰ 时间不多了，还剩 $remainingDays 天';
    } else {
      return '📅 还剩 $remainingDays 天';
    }
  }

  Color _getStatusColor(ThemeData theme) {
    if (widget.goal.isCompleted) {
      return Colors.green[600]!;
    }
    
    if (widget.goal.isOverdue) {
      return Colors.red[600]!;
    }
    
    final remainingDays = widget.goal.remainingDays;
    if (remainingDays <= 7) {
      return Colors.orange[600]!;
    } else {
      return theme.colorScheme.onSurface;
    }
  }
}

/// 储蓄目标进度条组件
class SavingGoalProgressBar extends StatefulWidget {
  final SavingGoal goal;
  final double? width;

  const SavingGoalProgressBar({
    super.key,
    required this.goal,
    this.width,
  });

  @override
  State<SavingGoalProgressBar> createState() => _SavingGoalProgressBarState();
}

class _SavingGoalProgressBarState extends State<SavingGoalProgressBar> with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _scaleAnimation;
  late Animation<Color?> _colorAnimation;
  bool _hasShownCompletionAnimation = false;
  bool _isCompleted = false;

  @override
  void initState() {
    super.initState();
    
    _isCompleted = widget.goal.isCompleted;
    
    // 初始化动画控制器
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    
    // 缩放动画
    _scaleAnimation = Tween<double>(begin: 1.0, end: 1.2)
        .chain(CurveTween(curve: Curves.elasticOut))
        .animate(_animationController);
    
    // 颜色动画
    _colorAnimation = ColorTween(
      begin: Colors.green[500],
      end: Colors.green[700],
    ).chain(CurveTween(curve: Curves.easeInOut))
      .animate(_animationController);
    
    // 如果目标已完成且未显示过动画，播放完成动画
    if (_isCompleted && !_hasShownCompletionAnimation) {
      // 添加延迟，确保UI已渲染完成
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          _playCompletionAnimation();
        }
      });
    }
  }

  @override
  void didUpdateWidget(covariant SavingGoalProgressBar oldWidget) {
    super.didUpdateWidget(oldWidget);
    
    final wasCompleted = oldWidget.goal.isCompleted;
    final isNowCompleted = widget.goal.isCompleted;
    
    // 如果从非完成状态变为完成状态，播放动画
    if (!wasCompleted && isNowCompleted && !_hasShownCompletionAnimation) {
      setState(() {
        _isCompleted = true;
      });
      _playCompletionAnimation();
    }
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  void _playCompletionAnimation() {
    _animationController.forward();
    _hasShownCompletionAnimation = true;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final progress = widget.goal.progress;
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            AnimatedBuilder(
              animation: _animationController,
              builder: (context, child) {
                return ScaleTransition(
                  scale: _scaleAnimation,
                  child: Text(
                    '${(progress * 100).toStringAsFixed(1)}%',
                    style: theme.textTheme.bodySmall?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: _isCompleted 
                          ? _colorAnimation.value ?? Colors.green[600] 
                          : theme.colorScheme.primary,
                    ),
                  ),
                );
              },
            ),
            Text(
              '¥${widget.goal.currentAmount.toStringAsFixed(0)} / ¥${widget.goal.targetAmount.toStringAsFixed(0)}',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Container(
          width: widget.width ?? double.infinity,
          height: 8,
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(4),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: Stack(
              children: [
                LinearProgressIndicator(
                  value: progress > 1.0 ? 1.0 : progress,
                  backgroundColor: Colors.transparent,
                  color: _isCompleted 
                      ? _colorAnimation.value ?? Colors.green[600] 
                      : theme.colorScheme.primary,
                ),
                if (_isCompleted)
                  AnimatedBuilder(
                    animation: _animationController,
                    builder: (context, child) {
                      return Opacity(
                        opacity: 1.0 - _animationController.value,
                        child: Transform.scale(
                          scale: 1.0 + _animationController.value * 0.5,
                          child: Container(
                            width: widget.width ?? double.infinity,
                            height: 8,
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.6),
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}