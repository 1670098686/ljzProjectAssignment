import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../core/router/route_guards.dart';
import '../../../features/transactions/category_management_page.dart';
import '../../../features/transactions/transaction_form_page.dart';
import '../../../features/transactions/transaction_detail_page.dart';
import '../../../features/transactions/transactions_page.dart';

class TransactionsModule {
  const TransactionsModule._();

  static RouteBase route() => GoRoute(
    path: AppRoutes.transactions,
    name: 'transactions',
    builder: (context, state) => const TransactionsPage(),
  );

  static List<RouteBase> modalRoutes(
    GlobalKey<NavigatorState> parentNavigatorKey,
  ) => [
    GoRoute(
      path: AppRoutes.transactionForm,
      name: 'transactionForm',
      parentNavigatorKey: parentNavigatorKey,
      builder: (context, state) => TransactionFormPage(
        billId: RouteGuards.parseOptionalInt(state, key: 'id'),
      ),
    ),
    GoRoute(
      path: AppRoutes.transactionDetail,
      name: 'transactionDetail',
      parentNavigatorKey: parentNavigatorKey,
      builder: (context, state) {
        final billId = RouteGuards.parseOptionalInt(state, key: 'id');
        return TransactionDetailPage(billId: billId);
      },
    ),
    GoRoute(
      path: AppRoutes.categoryManagement,
      name: 'categoryManagement',
      parentNavigatorKey: parentNavigatorKey,
      builder: (context, state) => const CategoryManagementPage(),
    ),
  ];
}