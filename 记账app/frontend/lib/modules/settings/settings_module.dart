import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../features/settings/settings_page.dart';
import '../../../features/export/export_page.dart';
import '../../../features/backup/backup_restore_page.dart';
import '../../../features/user/user_profile_page.dart';

class SettingsModule {
  const SettingsModule._();

  static RouteBase route() => GoRoute(
    path: AppRoutes.settings,
    name: 'settings',
    builder: (context, state) => const SettingsPage(),
    routes: [
      // 数据导出页面路由
      GoRoute(
        path: 'export',
        name: 'export',
        builder: (context, state) => const ExportPage(),
      ),
      // 备份恢复页面路由
      GoRoute(
        path: 'backup',
        name: 'backup',
        builder: (context, state) => const BackupRestorePage(),
      ),
      // 用户资料编辑页面路由
      GoRoute(
        path: 'user-edit',
        name: 'user-edit',
        builder: (context, state) => const UserProfilePage(),
      ),
    ],
  );
}
