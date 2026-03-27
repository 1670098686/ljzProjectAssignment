import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import '../core/router/app_router.dart';
import '../core/theme/app_theme.dart';
import '../data/services/category_service.dart';
import '../data/services/bill_service.dart';

import '../data/services/local_saving_goal_service.dart';

import '../core/services/local_database_service.dart';
import '../core/services/local_data_service.dart';
import '../data/services/statistics_service.dart';
import '../core/errors/error_center.dart';
import '../core/providers/theme_provider.dart';
import '../core/providers/category_provider.dart';
import '../core/providers/bill_provider.dart';

import '../core/providers/saving_goal_provider.dart';

import '../core/providers/budget_provider.dart';
import '../data/services/budget_service.dart';

import '../core/providers/statistics_provider.dart';
import '../core/providers/data_persistence_provider.dart';
import '../core/providers/user_provider.dart';
import '../core/services/auth_service.dart';
import '../core/network/api_client.dart';
import '../core/services/unified_error_handling_service.dart';

import '../shared/utils/constants.dart';
import '../shared/widgets/global_error_listener.dart';

/// 应用主组件 - 热重载测试完成！
class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late ThemeProvider _themeProvider;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    // 设置系统UI样式
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        systemNavigationBarColor: Colors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
    );

    // 初始化主题提供者
    _themeProvider = ThemeProvider();
    await _themeProvider.initTheme();

    setState(() {
      _isInitialized = true;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // 服务提供者
        ChangeNotifierProvider<ErrorCenter>(create: (_) => ErrorCenter()),
        Provider<CategoryService>(create: (_) => CategoryService()),
        Provider<BillService>(create: (_) => BillService()),


        
        // 本地数据库服务
        Provider<LocalDatabaseService>(
          create: (_) => LocalDatabaseService(),
          dispose: (_, db) => db.close(),
        ),
        
        // 本地数据服务
        Provider<LocalDataService>(
          create: (_) => LocalDataService(),
        ),
        
        // 储蓄目标和记录服务（使用本地数据库）
        Provider<LocalSavingGoalService>(
          create: (context) => LocalSavingGoalService(
            Provider.of<LocalDatabaseService>(context, listen: false),
            Provider.of<LocalDataService>(context, listen: false),
          ),
        ),

        // 预算服务（使用本地数据库）
        Provider<BudgetService>(
          create: (context) => BudgetService(
            Provider.of<LocalDataService>(context, listen: false),
            Provider.of<ErrorCenter>(context, listen: false),
          ),
        ),

        
        Provider<StatisticsService>(
          create: (_) => StatisticsService(),
        ),
        
        // API和认证相关服务
        Provider<ApiClient>(create: (_) => ApiClient()),
        Provider<UnifiedErrorHandlingService>(create: (_) => UnifiedErrorHandlingService()),
        Provider<AuthService>(
          create: (context) => AuthService(
            apiClient: Provider.of<ApiClient>(context, listen: false),
            errorHandlingService: Provider.of<UnifiedErrorHandlingService>(context, listen: false),
          ),
        ),

        // 状态提供者
        ChangeNotifierProvider.value(value: _themeProvider),
        ChangeNotifierProvider<BillProvider>(
          create: (context) => BillProvider(
            Provider.of<BillService>(context, listen: false),
            errorCenter: Provider.of<ErrorCenter>(context, listen: false),
          ),
        ),

        ChangeNotifierProvider<SavingGoalProvider>(
          create: (context) => SavingGoalProvider(
            Provider.of<LocalSavingGoalService>(context, listen: false),
            errorCenter: Provider.of<ErrorCenter>(context, listen: false),
            billProvider: Provider.of<BillProvider>(context, listen: false),
          ),
        ),

        // 预算提供者
        ChangeNotifierProvider<BudgetProvider>(
          create: (context) => BudgetProvider(
            Provider.of<BudgetService>(context, listen: false),
          ),
        ),

        // 分类提供者
        ChangeNotifierProvider<CategoryProvider>(
          create: (context) => CategoryProvider(
            Provider.of<CategoryService>(context, listen: false),
            errorCenter: Provider.of<ErrorCenter>(context, listen: false),
          ),
        ),

        ChangeNotifierProvider<StatisticsProvider>(
          create: (context) => StatisticsProvider(
            Provider.of<StatisticsService>(context, listen: false),
            errorCenter: Provider.of<ErrorCenter>(context, listen: false),
          ),
        ),
        
        // 数据持久化提供者
        ChangeNotifierProvider<DataPersistenceProvider>(
          create: (_) => DataPersistenceProvider(),
        ),
        
        // 用户状态提供者
        ChangeNotifierProvider<UserProvider>(
          create: (context) => UserProvider(
            databaseService: Provider.of<LocalDatabaseService>(context, listen: false),
          ),
        ),
      ],
      child: Consumer<ThemeProvider>(
        builder: (context, themeProvider, child) {
          return GlobalErrorListener(
            child: MaterialApp.router(
              key: ValueKey(themeProvider.refreshCounter),
              title: AppConstants.appName,
              debugShowCheckedModeBanner: false,

              // 主题配置
              theme: AppTheme.lightTheme,
              darkTheme: AppTheme.darkTheme,
              themeMode: themeProvider.themeMode,

              // 路由配置
              routerConfig: AppRouter.routerConfig,

              // 本地化配置
              localizationsDelegates: const [
                GlobalMaterialLocalizations.delegate,
                GlobalWidgetsLocalizations.delegate,
                GlobalCupertinoLocalizations.delegate,
              ],
              supportedLocales: const [Locale('zh', 'CN'), Locale('en', 'US')],

              // 字体配置
              builder: (context, child) {
                if (!_isInitialized) {
                  return const Scaffold(body: Center(child: CircularProgressIndicator()));
                }
                return MediaQuery(
                  data: MediaQuery.of(
                    context,
                  ).copyWith(textScaler: const TextScaler.linear(1.0)),
                  child: child!,
                );
              },
            ),
          );
        },
      ),
    );
  }
}
