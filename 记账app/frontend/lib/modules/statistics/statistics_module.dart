import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../features/statistics/statistics_page.dart';

/// Statistics module routes definition.
class StatisticsModule {
  const StatisticsModule._();

  /// Returns the primary route for the statistics module.
  static RouteBase route() {
    return GoRoute(
      path: AppRoutes.statistics,
      name: 'statistics',
      pageBuilder: (context, state) {
        // 从查询参数中获取时间范围
        final timeRange = state.uri.queryParameters['timeRange'];
        final hasData = state.uri.queryParameters['hasData'];
        
        return NoTransitionPage<void>(
          child: StatisticsPage(
            timeRange: timeRange,
            hasTodayData: hasData == 'true',
          ),
        );
      },
    );
  }
}
