import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../app/routes.dart';
import '../../core/providers/bill_provider.dart';
import '../../core/providers/theme_provider.dart';
import '../../core/utils/animation_utils.dart';
import '../../data/models/bill_model.dart';
import '../../shared/utils/constants.dart';
import '../../features/statistics/statistics_page.dart';
import 'widgets/overview_card_widget.dart';
import 'widgets/quick_actions_widget.dart';
import 'widgets/quick_entry_widget.dart';
import 'widgets/recent_transactions_widget.dart';
import 'widgets/welcome_card_widget.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with AutomaticKeepAliveClientMixin {
  bool _initialized = false;
  bool _isLoading = false;
  double _monthIncome = 0;
  double _monthExpense = 0;
  double _todayIncome = 0;
  double _todayExpense = 0;
  List<Bill> _recentTransactions = [];
  
  // 缓存上次计算的时间，避免重复计算
  DateTime? _lastCalculationTime;
  // 缓存统计结果
  Map<String, dynamic>? _cachedStatistics;

  @override
  bool get wantKeepAlive => true;

  double get _monthBalance => _monthIncome - _monthExpense;

  @override
  void initState() {
    super.initState();
    // 延迟初始化，避免阻塞UI线程
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initializeData();
    });
  }

  /// 获取主题刷新计数器，用于强制重建页面
  int _getThemeRefreshCounter(BuildContext context) {
    try {
      return context.read<ThemeProvider>().refreshCounter;
    } catch (e) {
      return 0;
    }
  }

  Future<void> _initializeData() async {
    // 等待一帧完成，确保Provider完全初始化
    await Future.delayed(const Duration(milliseconds: 50));
    
    if (!mounted) return;
    
    // 检查缓存是否有效（5分钟内有效）
    final now = DateTime.now();
    if (_cachedStatistics != null && 
        _lastCalculationTime != null && 
        now.difference(_lastCalculationTime!).inMinutes < 5) {
      _applyCachedStatistics();
      return;
    }
    
    // 确保BillProvider已经完成初始化
    final billProvider = context.read<BillProvider>();
    
    // 如果BillProvider尚未加载数据，则使用懒加载
    if (billProvider.bills.isEmpty) {
      await _loadDataLazily();
    } else {
      // 否则直接使用已缓存的数据进行计算
      await _calculateStatisticsFromCache();
    }
  }

  Future<void> _loadDataLazily() async {
    if (!mounted) return;
    
    setState(() => _isLoading = true);
    
    try {
      final billProvider = context.read<BillProvider>();
      
      // 只加载最近3个月的数据，而不是全部数据
      final threeMonthsAgo = DateTime.now().subtract(const Duration(days: 90));
      await billProvider.loadBills(
        startDate: threeMonthsAgo.toIso8601String().substring(0, 10)
      );
      
      final bills = List<Bill>.from(billProvider.bills);
      await _processBillsData(bills);
      
    } catch (e) {
      print('❌ 首页: 懒加载数据失败: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('加载数据失败，请下拉重试'))
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _calculateStatisticsFromCache() async {
    if (!mounted) return;
    
    final billProvider = context.read<BillProvider>();
    final bills = List<Bill>.from(billProvider.bills);
    await _processBillsData(bills);
  }
  
  void _applyCachedStatistics() {
    if (_cachedStatistics == null) return;
    
    setState(() {
      _monthIncome = _cachedStatistics!['monthIncome'] ?? 0;
      _monthExpense = _cachedStatistics!['monthExpense'] ?? 0;
      _todayIncome = _cachedStatistics!['todayIncome'] ?? 0;
      _todayExpense = _cachedStatistics!['todayExpense'] ?? 0;
      _recentTransactions = List<Bill>.from(_cachedStatistics!['recentTransactions'] ?? []);
      _initialized = true;
    });
  }

  Future<void> _loadData({bool silent = false}) async {
    if (!mounted) {
      return;
    }
    if (!silent) {
      setState(() => _isLoading = true);
    }
    final billProvider = context.read<BillProvider>();

    try {
      print('🏠 首页: 开始加载数据...');
      await billProvider.loadBills();
      print('🏠 首页: BillProvider数据加载完成，账单数量: ${billProvider.bills.length}');
      
      final bills = List<Bill>.from(billProvider.bills);
      await _processBillsData(bills);
      
    } catch (e, stackTrace) {
      print('❌ 首页: 加载数据失败: $e');
      print('❌ 首页: 错误堆栈: $stackTrace');
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('加载首页数据失败，请下拉重试')));
      }
    } finally {
      if (mounted && !silent) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _processBillsData(List<Bill> bills) async {
    if (!mounted) return;
    
    print('🏠 首页: 开始处理 ${bills.length} 条账单数据...');
    
    final now = DateTime.now();
    final startOfMonth = DateTime(now.year, now.month, 1);
    final endOfMonth = DateTime(now.year, now.month + 1, 0);
    final todayDate = DateTime(now.year, now.month, now.day);

    // 优化计算逻辑：使用单次遍历计算所有统计数据
    double monthIncome = 0;
    double monthExpense = 0;
    double todayIncome = 0;
    double todayExpense = 0;
    List<Bill> recentBills = [];

    // 单次遍历计算所有统计数据
    for (final bill in bills) {
      final date = DateTime.parse(bill.transactionDate);
      
      // 检查是否为当月数据
      final isCurrentMonth = !date.isBefore(startOfMonth) && !date.isAfter(endOfMonth);
      
      // 检查是否为今日数据
      final isToday = date.year == todayDate.year &&
                     date.month == todayDate.month &&
                     date.day == todayDate.day;
      
      if (isCurrentMonth) {
        if (bill.type == 1) {
          monthIncome += bill.amount;
        } else if (bill.type == 2) {
          monthExpense += bill.amount;
        }
      }
      
      if (isToday) {
        if (bill.type == 1) {
          todayIncome += bill.amount;
        } else if (bill.type == 2) {
          todayExpense += bill.amount;
        }
      }
      
      // 收集最近6条交易记录
      if (recentBills.length < 6) {
        recentBills.add(bill);
      }
    }

    // 按日期排序最近交易记录
    recentBills.sort((a, b) => _parseBillDate(b).compareTo(_parseBillDate(a)));

    print('📊 首页: 数据统计完成 - 月收入: $monthIncome, 月支出: $monthExpense, 今日收入: $todayIncome, 今日支出: $todayExpense');
    print('📝 首页: 最近交易数量: ${recentBills.length}');

    // 缓存计算结果
    _cachedStatistics = {
      'monthIncome': monthIncome,
      'monthExpense': monthExpense,
      'todayIncome': todayIncome,
      'todayExpense': todayExpense,
      'recentTransactions': recentBills,
    };
    _lastCalculationTime = DateTime.now();

    setState(() {
      _monthIncome = monthIncome;
      _monthExpense = monthExpense;
      _todayIncome = todayIncome;
      _todayExpense = todayExpense;
      _recentTransactions = recentBills;
      _initialized = true;
    });
    
    print('✅ 首页: 状态更新完成，界面应该刷新显示数据');
  }

  Future<void> _handleEntryCreated() async {
    await _loadData(silent: true);
  }

  @override
  Widget build(BuildContext context) {
    // 监听BillProvider的变化，当账单数据变化时自动更新
    context.watch<BillProvider>();
    
    // 当未初始化时，显示加载指示器
    if (!_initialized) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text(AppConstants.appName),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
        foregroundColor: Theme.of(context).colorScheme.onSurface,
        surfaceTintColor: Colors.transparent,
      ),
      body: RefreshIndicator(
        onRefresh: _loadData,
        child: ListView(
          physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.only(bottom: 24),
          children: [
            const SizedBox(height: 12),
            AnimationUtils.createSlideIn(
              duration: const Duration(milliseconds: 500),
              beginOffset: const Offset(-30, 0),
              child: const WelcomeCardWidget(),
            ),
            const SizedBox(height: 16),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AnimationUtils.createSlideIn(
                    duration: const Duration(milliseconds: 600),
                    delay: const Duration(milliseconds: 100),
                    child: GestureDetector(
                      onTap: () => context.go(AppRoutes.statistics),
                      child: OverviewCardWidget(
                        income: _monthIncome,
                        expense: _monthExpense,
                        balance: _monthBalance,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  AnimationUtils.createSlideIn(
                    duration: const Duration(milliseconds: 500),
                    delay: const Duration(milliseconds: 200),
                    beginOffset: const Offset(30, 0),
                    child: HomeQuickActions(
                      todayIncome: _todayIncome,
                      todayExpense: _todayExpense,
                      onNavigateStatistics: (timeRange) async {
                        // 检查今日是否有收支记录
                        final hasTodayRecords = _todayIncome > 0 || _todayExpense > 0;
                        
                        if (timeRange == 'today') {
                          // 跳转到统计页面并选择"本日"时间范围
                          context.go('${AppRoutes.statistics}?timeRange=本日&hasData=${hasTodayRecords}');
                        } else {
                          context.go(AppRoutes.statistics);
                        }
                      },
                    ),
                  ),
                  const SizedBox(height: 16),
                  AnimationUtils.createSlideIn(
                    duration: const Duration(milliseconds: 300),
                    child: QuickEntryWidget(onEntrySaved: _handleEntryCreated),
                  ),
                  const SizedBox(height: 16),
                  AnimationUtils.createSlideIn(
                    duration: const Duration(milliseconds: 500),
                    delay: const Duration(milliseconds: 400),
                    child: RecentTransactionsWidget(
                      transactions: _recentTransactions,
                      onViewAll: () => context.go(AppRoutes.transactions),
                      onItemTap: (bill) => context.go(AppRoutes.transactions),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  DateTime _parseBillDate(Bill bill) {
    return DateTime.tryParse(bill.transactionDate) ??
        DateTime.fromMillisecondsSinceEpoch(0);
  }
}
