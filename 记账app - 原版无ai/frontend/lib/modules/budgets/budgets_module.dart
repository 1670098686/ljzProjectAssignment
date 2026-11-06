import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../core/router/route_guards.dart';
import '../../../features/budgets/budget_form_page.dart';
import '../../../features/budgets/budgets_page.dart';

class BudgetsModule {
  const BudgetsModule._();

  static RouteBase route() => GoRoute(
    path: AppRoutes.budgets,
    name: 'budgets',
    builder: (context, state) => const BudgetsPage(),
    routes: [
      GoRoute(
        path: 'form',
        name: 'budgetForm',
        builder: (context, state) => BudgetFormPage(
          budgetId: RouteGuards.parseOptionalInt(state, key: 'id'),
        ),
      ),
    ],
  );

  static List<RouteBase> modalRoutes(
    GlobalKey<NavigatorState> parentNavigatorKey,
  ) => [
    GoRoute(
      path: AppRoutes.budgetForm,
      name: 'budgetFormModal',
      parentNavigatorKey: parentNavigatorKey,
      builder: (context, state) => BudgetFormPage(
        budgetId: RouteGuards.parseOptionalInt(state, key: 'id'),
      ),
    ),
  ];
}