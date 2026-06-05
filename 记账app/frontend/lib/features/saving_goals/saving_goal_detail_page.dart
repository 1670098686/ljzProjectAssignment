import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers/saving_goal_provider.dart';

import '../../data/models/saving_goal_model.dart';


import '../../core/router/route_guards.dart';
import '../../widgets/saving_goal_visualization.dart';

class SavingGoalDetailPage extends StatefulWidget {
  final int? goalId;
  final SavingGoal? goal;

  const SavingGoalDetailPage({super.key, this.goalId, this.goal});

  @override
  State<SavingGoalDetailPage> createState() => _SavingGoalDetailPageState();
}

class _SavingGoalDetailPageState extends State<SavingGoalDetailPage> {
  SavingGoal? _goal;
  int? _goalId;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _initializeGoalId();
  }

  void _initializeGoalId() {
    // 从widget参数或路由参数获取ID
    if (widget.goal != null) {
      _goalId = widget.goal!.id;
    } else if (widget.goalId != null) {
      _goalId = widget.goalId;
    } else {
      // 从路由参数获取ID
      try {
        final state = GoRouterState.of(context);
        _goalId = RouteGuards.parseOptionalInt(state, key: 'id');
      } catch (e) {
        // 如果无法获取路由参数，尝试从历史记录中获取
        final lastPath = GoRouterState.of(context).uri.path;
        final pathSegments = lastPath.split('/').where((s) => s.isNotEmpty).toList();
        if (pathSegments.isNotEmpty) {
          final lastSegment = pathSegments.last;
          _goalId = int.tryParse(lastSegment);
        }
      }
      
      // 如果没有ID，则返回上一页
      if (_goalId == null) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            context.pop();
          }
        });
      }
    }
  }

  Future<void> _loadGoal(int goalId) async {
    setState(() {
      _isLoading = true;
    });

    try {
      final provider = context.read<SavingGoalProvider>();
      
      // 确保数据已加载
      if (provider.goals.isEmpty) {
        await provider.loadGoals();
      }
      
      // 查找指定ID的目标
      final goal = provider.goals.firstWhere(
        (g) => g.id == goalId,
        orElse: () => throw Exception('未找到指定的储蓄目标'),
      );
      
      if (mounted) {
        setState(() {
          _goal = goal;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载目标失败: $e')),
        );
        
        // 加载失败，返回上一页
        if (_goalId != null) {
          context.pop();
        }
      }
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    
    // 在依赖项变化后，确保ID已正确初始化
    if (_goalId == null) {
      _initializeGoalId();
    }
    
    // 如果有ID但目标为空，加载数据
    if (_goalId != null && _goal == null) {
      _loadGoal(_goalId!);
    }
  }

  Future<void> _refreshGoal() async {
    if (_goalId == null) return;
    
    setState(() {
      _isLoading = true;
    });

    try {
      final provider = context.read<SavingGoalProvider>();
      await provider.loadGoals();
      
      // 查找更新后的目标
      final updatedGoal = provider.goals.firstWhere(
        (goal) => goal.id == _goalId,
        orElse: () => throw Exception('未找到指定的储蓄目标'),
      );
      
      if (mounted) {
        setState(() {
          _goal = updatedGoal;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('刷新失败: $e')),
        );
      }
    }
  }

  // 移除了添加储蓄记录的功能，所有收支记录统一通过首页记账模块进行

  String _getStatusText() {
    final goal = _goal;
    if (goal == null) return '未知状态';
    
    if (goal.isCompleted) {
      return '已完成';
    } else if (goal.isOverdue) {
      return '已逾期';
    } else {
      final daysLeft = goal.remainingDays;
      if (daysLeft <= 0) {
        return '今天到期';
      } else if (daysLeft == 1) {
        return '明天到期';
      } else {
        return '剩余 $daysLeft 天';
      }
    }
  }

  Color _getStatusColor() {
    final goal = _goal;
    if (goal == null) return Colors.grey;
    
    if (goal.isCompleted) {
      return Colors.green;
    } else if (goal.isOverdue) {
      return Colors.red;
    } else if (goal.remainingDays <= 30) {
      return Colors.orange;
    } else {
      return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final formatter = NumberFormat('#,##0.00');
    
    return Scaffold(
      appBar: AppBar(
        title: Text(_goal?.name ?? '储蓄目标详情'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _refreshGoal,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 进度可视化
                  if (_goal != null)
                    SavingGoalVisualization(
                      goal: _goal!,
                      compactMode: false,
                    ),
                  const SizedBox(height: 24),
                  
                  // 目标基本信息
                  Card(
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                      side: BorderSide(color: theme.colorScheme.outlineVariant),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                '目标信息',
                                style: theme.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 6,
                                ),
                                decoration: BoxDecoration(
                                  color: _getStatusColor().withAlpha(26),
                                  borderRadius: BorderRadius.circular(16),
                                ),
                                child: Text(
                                  _getStatusText(),
                                  style: TextStyle(
                                    color: _getStatusColor(),
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),
                          _buildInfoRow(
                            '目标金额',
                            '¥${formatter.format(_goal?.targetAmount ?? 0)}',
                            theme,
                          ),
                          const SizedBox(height: 8),
                          _buildInfoRow(
                            '当前金额',
                            '¥${formatter.format(_goal?.currentAmount ?? 0)}',
                            theme,
                          ),
                          const SizedBox(height: 8),
                          _buildInfoRow(
                            '完成进度',
                            '${((_goal?.progress ?? 0) * 100).toStringAsFixed(1)}%',
                            theme,
                          ),
                          const SizedBox(height: 8),
                          _buildInfoRow(
                            '创建日期',
                            '无',
                            theme,
                          ),
                          const SizedBox(height: 8),
                          _buildInfoRow(
                            '截止日期',
                            _goal?.deadline != null ? DateFormat('yyyy-MM-dd').format(_goal!.deadline) : '无',
                            theme,
                          ),
                          if ((_goal?.description ?? '').isNotEmpty) ...[
                            const SizedBox(height: 8),
                            _buildInfoRow(
                              '目标描述',
                              _goal?.description ?? '',
                              theme,
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                  
                  const SizedBox(height: 24),
                  
                  // 操作按钮
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () {
                            // 跳转到编辑页面，传递当前储蓄目标ID
                            final goalId = _goal?.id;
                            if (goalId != null) {
                              context.push('/saving-goals/form/$goalId');
                            }
                          },
                          icon: const Icon(Icons.edit),
                          label: const Text('编辑'),
                          style: OutlinedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: 24),
                  

                ],
              ),
            ),
    );
  }

  Widget _buildInfoRow(String label, String value, ThemeData theme) {
    return Row(
      children: [
        Expanded(
          flex: 1,
          child: Text(
            label,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        Expanded(
          flex: 2,
          child: Text(
            value,
            style: theme.textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w500,
            ),
            textAlign: TextAlign.right,
          ),
        ),
      ],
    );
  }


}