import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../core/providers/saving_goal_provider.dart';
import '../../../core/router/navigation_result.dart';
import '../../../data/models/saving_goal_model.dart';
import '../../../services/goal_reminder_service.dart';
import '../../../services/smart_suggestion_service.dart';
import '../../../widgets/saving_goal_visualization.dart';
import '../../../shared/widgets/performance_optimization.dart';

class SavingGoalsPage extends StatefulWidget {
  const SavingGoalsPage({super.key});

  @override
  State<SavingGoalsPage> createState() => _SavingGoalsPageState();
}

class _SavingGoalsPageState extends State<SavingGoalsPage>
    with RouteResultMixin<SavingGoalsPage> {
  late GoalReminderManager _reminderManager;
  late SmartSuggestionService _suggestionService;
  bool _isInitialized = false;
  int _selectedTabIndex = 0; // 0: 目标列表, 1: 建议中心

  @override
  void initState() {
    super.initState();
    _reminderManager = GoalReminderManager();
    _suggestionService = SmartSuggestionService();
    _initializeServices();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _reloadGoals();
    });
  }

  Future<void> _initializeServices() async {
    await _reminderManager.initialize();
    setState(() {
      _isInitialized = true;
    });
  }

  Widget _buildTabButton(
    int index,
    String title,
    IconData icon,
    ThemeData theme,
  ) {
    final isSelected = _selectedTabIndex == index;
    return GestureDetector(
      onTap: () {
        setState(() {
          _selectedTabIndex = index;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
        decoration: BoxDecoration(
          color: isSelected ? theme.colorScheme.primary : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              color: isSelected
                  ? theme.colorScheme.onPrimary
                  : theme.colorScheme.onSurface.withAlpha(
                      153,
                    ), // 0.6 * 255 = 153
              size: 20,
            ),
            const SizedBox(width: 8),
            Text(
              title,
              style: theme.textTheme.labelMedium?.copyWith(
                color: isSelected
                    ? theme.colorScheme.onPrimary
                    : theme.colorScheme.onSurface.withAlpha(
                        153,
                      ), // 0.6 * 255 = 153
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGoalsTab(SavingGoalProvider provider, ThemeData theme) {
    if (provider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (provider.hasError) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '加载失败',
              style: theme.textTheme.titleMedium?.copyWith(
                color: Colors.grey[600],
              ),
            ),
            const SizedBox(height: 8),
            Text(
              provider.errorMessage ?? '请稍后再试',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: Colors.grey[500],
              ),
            ),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _reloadGoals, child: const Text('重新加载')),
          ],
        ),
      );
    }

    if (provider.isSuccess) {
      if (provider.goals.isEmpty) {
        return _buildEmptyState(context);
      }

      return RefreshIndicator(
        onRefresh: _reloadGoals,
        child: VirtualizedSavingGoalList(
          goals: provider.goals,
          onTap: (goal) => _openGoalDetail(goal),
          onEdit: (goal) => _openGoalForm(goal),
          onDelete: (goal) => _confirmDeleteGoal(goal),
          physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.all(20),
        ),
      );
    }

    return _buildEmptyState(context);
  }

  Widget _buildEmptyState(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.savings_outlined, size: 64, color: Colors.grey[400]),
          const SizedBox(height: 16),
          Text(
            '暂无储蓄目标',
            style: theme.textTheme.titleMedium?.copyWith(
              color: Colors.grey[600],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '制定你的第一个储蓄目标',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: Colors.grey[500],
            ),
          ),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: () => _openGoalForm(),
            icon: const Icon(Icons.add),
            label: const Text('创建目标'),
          ),
        ],
      ),
    );
  }

  Widget _buildSuggestionsTab(SavingGoalProvider provider, ThemeData theme) {
    if (provider.goals.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.lightbulb_outline, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '暂无建议',
              style: theme.textTheme.titleMedium?.copyWith(
                color: Colors.grey[600],
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '创建储蓄目标后获取智能建议',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: Colors.grey[500],
              ),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => _openGoalForm(),
              child: const Text('创建目标'),
            ),
          ],
        ),
      );
    }

    return _buildSmartSuggestions(provider, theme);
  }

  Widget _buildSmartSuggestions(SavingGoalProvider provider, ThemeData theme) {
    return ListView.builder(
      padding: const EdgeInsets.all(20),
      itemCount: provider.goals.length,
      itemBuilder: (context, index) {
        final goal = provider.goals[index];
        final suggestions = _suggestionService.generateSuggestions(goal);

        return Card(
          margin: const EdgeInsets.only(bottom: 16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 目标概览
                Row(
                  children: [
                    Icon(Icons.flag, color: theme.colorScheme.primary),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        goal.name,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    Text(
                      '${(goal.progress * 100).toStringAsFixed(1)}%',
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: goal.isCompleted
                            ? Colors.green
                            : theme.colorScheme.primary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 16),

                // 进度可视化（紧凑版）
                SavingGoalVisualization(goal: goal, compactMode: true),

                const SizedBox(height: 16),

                // 智能建议列表
                if (suggestions.isNotEmpty) ...[
                  Text(
                    '智能建议',
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  ...suggestions.map(
                    (suggestion) => Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Icon(
                            _getSuggestionIcon(suggestion.type),
                            size: 16,
                            color: theme.colorScheme.primary,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              suggestion.message,
                              style: theme.textTheme.bodySmall,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }

  IconData _getSuggestionIcon(SuggestionType type) {
    switch (type) {
      case SuggestionType.urgent:
        return Icons.warning_amber;
      case SuggestionType.warning:
        return Icons.report_problem;
      case SuggestionType.suggestion:
        return Icons.lightbulb_outline;
      case SuggestionType.encouragement:
        return Icons.emoji_events;
    }
  }

  Future<void> _reloadGoals() async {
    final provider = context.read<SavingGoalProvider>();
    await provider.loadGoals();

    // 更新提醒服务
    if (_isInitialized && provider.goals.isNotEmpty) {
      await _reminderManager.setReminderGoals(provider.goals);
    }
  }

  Future<void> _openGoalForm([SavingGoal? goal]) async {
    final path = goal?.id == null
        ? AppRoutes.savingGoalForm
        : '${AppRoutes.savingGoalForm}?id=${goal!.id}';
    await pushForResult<bool>(location: path, onRefresh: _reloadGoals);
  }

  void _openGoalDetail(SavingGoal goal) {
    final path = '${AppRoutes.savingGoals}/detail?id=${goal.id}';
    context.go(path);
  }

  void _confirmDeleteGoal(SavingGoal goal) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('删除确认'),
          content: Text('确定要删除目标「${goal.name}」吗？此操作不可恢复。'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                _deleteGoal(goal);
              },
              style: TextButton.styleFrom(foregroundColor: Theme.of(context).colorScheme.error),
              child: const Text('删除'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _deleteGoal(SavingGoal goal) async {
    try {
      final provider = context.read<SavingGoalProvider>();
      await provider.deleteGoal(goal.id!);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('目标已删除')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('删除失败: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('储蓄目标'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: theme.colorScheme.surface,
        foregroundColor: theme.colorScheme.onSurface,
      ),
      body: Column(
        children: [
          // 标签页导航
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              children: [
                Expanded(
                  child: _buildTabButton(0, '目标列表', Icons.list_alt, theme),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: _buildTabButton(
                    1,
                    '建议中心',
                    Icons.lightbulb_outline,
                    theme,
                  ),
                ),
              ],
            ),
          ),

          // 标签页内容
          Expanded(
            child: Consumer<SavingGoalProvider>(
              builder: (context, provider, _) {
                if (_selectedTabIndex == 0) {
                  return _buildGoalsTab(provider, theme);
                }
                return _buildSuggestionsTab(provider, theme);
              },
            ),
          ),
        ],
      ),
    );
  }
}
