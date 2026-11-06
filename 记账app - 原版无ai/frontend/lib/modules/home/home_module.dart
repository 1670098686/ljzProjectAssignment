import 'package:go_router/go_router.dart';

import '../../../app/routes.dart';
import '../../../features/home/home_page.dart';

class HomeModule {
  const HomeModule._();

  static RouteBase route() => GoRoute(
    path: AppRoutes.home,
    name: 'home',
    builder: (context, state) => const HomePage(),
  );
}
