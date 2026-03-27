import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../core/router/route_guards.dart';
import '../../../features/saving_goals/saving_goal_form_page.dart';
import '../../../features/saving_goals/saving_goals_page.dart';
import '../../../features/saving_goals/saving_goal_detail_page.dart';


class SavingGoalsModule {
  const SavingGoalsModule._();

  static RouteBase route() => GoRoute(
    path: AppRoutes.savingGoals,
    name: 'savingGoals',
    builder: (context, state) => const SavingGoalsPage(),
    routes: [
      // 储蓄目标表单页面（深层路由）
      GoRoute(
        path: 'form/:id',
        name: 'savingGoalForm',
        builder: (context, state) {
          final goalId = int.tryParse(state.pathParameters['id'] ?? '');
          return SavingGoalFormPage(goalId: goalId);
        },
      ),

      // 储蓄目标详情页面（深层路由）
      GoRoute(
        path: 'detail/:id',
        name: 'savingGoalDetail',
        builder: (context, state) {
          final goalId = int.tryParse(state.pathParameters['id'] ?? '');
          return SavingGoalDetailPage(goalId: goalId);
        },
        routes: [
          // 储蓄目标编辑页面（深层嵌套路由）
          GoRoute(
            path: 'edit',
            name: 'savingGoalEdit',
            builder: (context, state) {
              final goalId = int.tryParse(state.pathParameters['id'] ?? '');
              return SavingGoalFormPage(goalId: goalId);
            },
          ),
        ],
      ),


    ],
  );

  static List<RouteBase> modalRoutes(
    GlobalKey<NavigatorState> parentNavigatorKey,
  ) => [
    // 模态储蓄目标表单页面（用于非深层导航）
    GoRoute(
      path: AppRoutes.savingGoalForm,
      name: 'savingGoalFormModal',
      parentNavigatorKey: parentNavigatorKey,
      builder: (context, state) => SavingGoalFormPage(
        goalId: RouteGuards.parseOptionalInt(state, key: 'id'),
      ),
    ),
  ];
}
