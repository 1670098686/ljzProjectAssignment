import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../features/plan/plan_page.dart';

class PlanModule {
  const PlanModule._();

  static RouteBase route() {
    return GoRoute(
      path: AppRoutes.plan,
      name: 'plan',
      pageBuilder: (context, state) =>
          const NoTransitionPage<void>(child: PlanPage()),
    );
  }
}
