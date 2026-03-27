import 'package:flutter/material.dart';

/// 预算预警对话框组件
class BudgetWarningDialog extends StatefulWidget {
  final String categoryName;
  final String? budgetName;
  final double spentAmount;
  final double budgetAmount;
  final double usagePercentage;

  const BudgetWarningDialog({
    super.key,
    required this.categoryName,
    this.budgetName,
    required this.spentAmount,
    required this.budgetAmount,
    required this.usagePercentage,
  });

  @override
  State<BudgetWarningDialog> createState() => _BudgetWarningDialogState();
}

class _BudgetWarningDialogState extends State<BudgetWarningDialog> 
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;
  late Animation<double> _fadeAnimation;
  late Animation<Color?> _colorAnimation;
  late Animation<Color?> _textColorAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    );

    _scaleAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.elasticOut,
      reverseCurve: Curves.elasticIn,
    ));

    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeIn,
    ));

    _colorAnimation = ColorTween(
      begin: Colors.orange[50],
      end: Colors.red[50],
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOut,
    ));

    _textColorAnimation = ColorTween(
      begin: Colors.orange[700],
      end: Colors.red[700],
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOut,
    ));

    // 启动动画控制器，确保动画能够正确播放
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_controller.status == AnimationStatus.dismissed) {
        _controller.forward();
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isOverBudget = widget.spentAmount >= widget.budgetAmount;
    
    return Dialog(
      backgroundColor: Colors.transparent,
      elevation: 0,
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, child) {
          return Transform.scale(
            scale: _scaleAnimation.value,
            child: Opacity(
              opacity: _fadeAnimation.value,
              child: Container(
                width: 300,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      _colorAnimation.value ?? Colors.orange[50]!,
                      Colors.white,
                    ],
                  ),
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(30),
                      blurRadius: 15,
                      offset: const Offset(0, 8),
                    ),
                  ],
                  border: Border.all(
                    color: _textColorAnimation.value?.withAlpha(100) ?? Colors.orange.withAlpha(100),
                    width: 1,
                  ),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // 警告图标
                    Transform.scale(
                      scale: _scaleAnimation.value,
                      child: Container(
                        width: 80,
                        height: 80,
                        decoration: BoxDecoration(
                          color: _textColorAnimation.value?.withAlpha(100) ?? Colors.orange.withAlpha(100),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.warning,
                          size: 40,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    
                    const SizedBox(height: 16),
                    
                    // 标题
                    Text(
                      isOverBudget ? '⚠️ 预算超支！' : '⚠️ 预算预警',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: _textColorAnimation.value,
                      ),
                    ),
                     
                    const SizedBox(height: 8),
                     
                    // 计划名称（优先显示计划名称，如果不存在则显示分类名称）
                    Text(
                      widget.budgetName ?? widget.categoryName,
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: Colors.black87,
                      ),
                      textAlign: TextAlign.center,
                    ),
                     
                    const SizedBox(height: 8),
                     
                    // 金额信息
                    Text(
                      '¥${widget.spentAmount.toStringAsFixed(2)} / ¥${widget.budgetAmount.toStringAsFixed(2)}',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: _textColorAnimation.value,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                     
                    const SizedBox(height: 8),
                     
                    // 百分比
                    Text(
                      '已使用 ${widget.usagePercentage.toStringAsFixed(0)}%',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: _textColorAnimation.value,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    
                    const SizedBox(height: 16),
                    
                    // 警告文字
                    Text(
                      isOverBudget 
                        ? '您的${widget.budgetName ?? widget.categoryName}计划本月已超支，请控制支出！' 
                        : '您的${widget.budgetName ?? widget.categoryName}计划本月预算已使用超过${widget.usagePercentage.toStringAsFixed(0)}%，请注意控制支出！',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: Colors.grey[600],
                      ),
                      textAlign: TextAlign.center,
                    ),
                    
                    const SizedBox(height: 20),
                    
                    // 确认按钮
                    ElevatedButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _textColorAnimation.value,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(20),
                        ),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 48,
                          vertical: 14,
                        ),
                        elevation: 5,
                        shadowColor: _textColorAnimation.value?.withAlpha(100),
                      ),
                      child: const Text(
                        '知道了',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}