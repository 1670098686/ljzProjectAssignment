/// 应用路由配置
class AppRoutes {
  // 认证路由
  static const String login = '/login';
  static const String register = '/register';
  
  // 主路由
  static const String home = '/home';
  static const String transactions = '/transactions';
  static const String plan = '/plan'; // 计划页面路由
  static const String savingGoals = '/saving-goals';
  static const String budgets = '/budgets'; // 预算页面路由
  static const String statistics = '/statistics';
  static const String settings = '/settings';

  // 子路由
  static const String transactionForm = '/transactions/form';   
  static const String transactionDetail = '/transactions/detail';
  static const String savingGoalForm = '/saving-goals/form';
  static const String savingGoalDetail = '/saving-goals/detail'; // 储蓄目标详情页面路由
  static const String savingRecordForm = '/saving-goals/records/form';
  static const String savingRecordDetail = '/saving-goals/records/detail';    
  static const String budgetForm = '/budgets/form'; // 预算表单页面路由
  static const String categoryManagement = '/categories';
  static const String export = '/export'; // 数据导出页面路由
  static const String backup = '/backup'; // 备份恢复页面路由

  /// 获取路由名称的显示文本
  static String getRouteDisplayName(String route) {
    switch (route) {
      case home:
        return '首页';
      case transactions:
        return '交易记录';
      case plan:
        return '计划';
      case savingGoals:
        return '储蓄目标';
      case budgets:
        return '预算管理';
      case statistics:
        return '统计分析';
      case settings:
        return '设置';
      default:
        return '未知页面';
    }
  }

  /// 检查是否为有效路由
  static bool isValidRoute(String route) {
    return [
      home,
      transactions,
      plan,
      savingGoals,
      budgets,
      statistics,
      settings,
    ].contains(route);
  }

  /// 获取底部导航栏的路由列表
  static List<String> getBottomNavRoutes() {
    return [statistics, transactions, home, plan, settings];
  }

  /// 获取包含预算的底部导航栏路由列表
  static List<String> getBottomNavRoutesWithBudget() {
    return [statistics, transactions, home, budgets, plan, settings];
  }
}
