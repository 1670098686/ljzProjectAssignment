import 'dart:async';
import 'dart:math';
import 'dart:ui' show Color;
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
import 'package:shared_preferences/shared_preferences.dart';
import '../core/database/database_service.dart';
import '../data/models/saving_goal_model.dart';
import 'api_service.dart';

/// 通知管理服务
class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  late FlutterLocalNotificationsPlugin _notifications;
  bool _isInitialized = false;

  /// 初始化通知服务
  Future<void> initialize() async {
    if (_isInitialized) {
      return;
    }

    tz.initializeTimeZones();

    const AndroidInitializationSettings androidInitializationSettings =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initializationSettings =
        InitializationSettings(android: androidInitializationSettings);

    _notifications = FlutterLocalNotificationsPlugin();

    await _notifications.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: onDidReceiveNotificationResponse,
    );

    _isInitialized = true;
  }

  /// 通知响应处理
  void onDidReceiveNotificationResponse(NotificationResponse response) {
    // 这里可以添加通知点击后的处理逻辑
    debugPrint('通知被点击: ${response.payload}');
  }

  /// 请求通知权限
  Future<bool> requestPermission() async {
    final androidImplementation = _notifications
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >();

    final granted = await androidImplementation
        ?.requestNotificationsPermission();
    return granted ?? false;
  }

  /// 发送即时通知
  Future<void> showInstantNotification({
    required String title,
    required String body,
    String? payload,
    int? id,
  }) async {
    if (!_isInitialized) await initialize();

    final AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
          'instant_notifications',
          '即时通知',
          channelDescription: '发送即时通知',
          importance: Importance.high,
          priority: Priority.high,
        );

    final NotificationDetails notificationDetails = NotificationDetails(
      android: androidNotificationDetails,
    );

    await _notifications.show(
      id ?? Random().nextInt(1000),
      title,
      body,
      notificationDetails,
      payload: payload,
    );
  }

  /// 发送预算预警通知
  Future<void> showBudgetAlert({
    required String category,
    String? budgetName,
    required double budgetAmount,
    required double spentAmount,
    required int year,
    required int month,
    String? payload,
    int? id,
  }) async {
    if (!_isInitialized) await initialize();

    final progress = (spentAmount / budgetAmount * 100).round();
    final isOverBudget = spentAmount > budgetAmount;

    final title = isOverBudget ? '预算超支提醒' : '预算预警';
    final displayName = budgetName ?? category;
    final body = isOverBudget
        ? '$displayName 本月已超支 ¥${spentAmount.toStringAsFixed(2)}，超出预算 ¥${(spentAmount - budgetAmount).toStringAsFixed(2)}'
        : '$displayName 本月已花费 ¥${spentAmount.toStringAsFixed(2)}，占预算的 $progress%';

    final AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
          'budget_alerts',
          '预算提醒',
          channelDescription: '预算使用情况提醒',
          importance: Importance.defaultImportance,
          priority: Priority.defaultPriority,
          color: isOverBudget
              ? const Color(0xFFFF5722)
              : const Color(0xFFFF9800),
        );

    final NotificationDetails notificationDetails = NotificationDetails(
      android: androidNotificationDetails,
    );

    await _notifications.show(
      id ?? Random().nextInt(1000),
      title,
      body,
      notificationDetails,
      payload: payload,
    );
  }

  /// 发送储蓄目标提醒
  Future<void> showSavingGoalReminder({
    required String goalName,
    required double targetAmount,
    required double currentAmount,
    required DateTime deadline,
    String? payload,
    int? id,
  }) async {
    if (!_isInitialized) await initialize();

    final progress = (currentAmount / targetAmount * 100).round();
    final daysLeft = deadline.difference(DateTime.now()).inDays;

    String title;
    String body;

    if (daysLeft <= 7) {
      title = '储蓄目标紧急提醒';
      body = '$goalName 目标还剩 $daysLeft 天，当前进度 $progress%，请继续努力！';
    } else {
      title = '储蓄目标进度提醒';
      body = '$goalName 目标当前进度 $progress%，剩余 $daysLeft 天，继续加油！';
    }

    const AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
          'saving_reminders',
          '储蓄提醒',
          channelDescription: '储蓄目标进度提醒',
          importance: Importance.defaultImportance,
          priority: Priority.defaultPriority,
          color: Color(0xFF4CAF50),
        );

    final NotificationDetails notificationDetails = NotificationDetails(
      android: androidNotificationDetails,
    );

    await _notifications.show(
      id ?? Random().nextInt(1000),
      title,
      body,
      notificationDetails,
      payload: payload,
    );
  }

  /// 发送主题切换通知
  Future<void> showThemeChangeNotification({
    required String themeName,
    required bool isDarkMode,
    String? payload,
    int? id,
  }) async {
    if (!_isInitialized) await initialize();

    final title = '主题切换成功';
    final body = '已切换到${isDarkMode ? '深色' : '浅色'}模式';

    final AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
          'theme_notifications',
          '主题通知',
          channelDescription: '主题切换通知',
          importance: Importance.min,
          priority: Priority.min,
        );

    final NotificationDetails notificationDetails = NotificationDetails(
      android: androidNotificationDetails,
    );

    await _notifications.show(
      id ?? 999,
      title,
      body,
      notificationDetails,
      payload: payload,
    );
  }

  /// 设置每日记账提醒
  Future<void> scheduleDailyReminder({
    required int hour,
    required int minute,
  }) async {
    if (!_isInitialized) await initialize();

    const AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
          'daily_reminders',
          '每日记账提醒',
          channelDescription: '每日记账提醒通知',
          importance: Importance.defaultImportance,
          priority: Priority.defaultPriority,
        );

    final NotificationDetails notificationDetails = NotificationDetails(
      android: androidNotificationDetails,
    );

    // 设置每日提醒时间
    await _notifications.zonedSchedule(
      1001, // 固定的ID，避免重复创建
      '记账时间到了',
      '记得记录今天的收支情况哦～',
      _nextInstanceOfTime(hour, minute),
      notificationDetails,
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
      payload: 'daily_reminder',
    );
  }

  /// 获取下一个指定时间
  tz.TZDateTime _nextInstanceOfTime(int hour, int minute) {
    final tz.TZDateTime now = tz.TZDateTime.now(tz.local);
    tz.TZDateTime scheduledDate = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      hour,
      minute,
    );

    if (scheduledDate.isBefore(now)) {
      scheduledDate = scheduledDate.add(const Duration(days: 1));
    }

    return scheduledDate;
  }

  /// 取消通知
  Future<void> cancel(int id) async {
    await _notifications.cancel(id);
  }

  /// 取消所有通知
  Future<void> cancelAll() async {
    await _notifications.cancelAll();
  }

  /// 检查预算预警并发送通知
  Future<void> checkBudgetAlerts() async {
    try {
      if (!_isInitialized) await initialize();
      
      // 获取通知设置
      final settings = await getNotificationSettings();
      if (!settings['budget_alerts']!) {
        debugPrint('预算提醒功能已禁用，跳过检查');
        return;
      }
      
      // 调用后端API获取预算预警信息
      final alerts = await _fetchBudgetAlertsFromBackend();
      
      if (alerts.isNotEmpty) {
        for (final alert in alerts) {
          await showBudgetAlert(
            category: alert['categoryName'],
            budgetName: alert['budgetName'],
            budgetAmount: alert['budgetAmount'],
            spentAmount: alert['spentAmount'],
            year: alert['year'],
            month: alert['month'],
            payload: 'budget_alert',
          );
          
          // 避免通知过于频繁，间隔1秒
          await Future.delayed(const Duration(seconds: 1));
        }
        
        debugPrint('已发送 ${alerts.length} 条预算预警通知');
      } else {
        debugPrint('当前没有预算预警信息');
      }
      
    } catch (e) {
      debugPrint('检查预算预警失败: $e');
    }
  }
  
  /// 从后端API获取预算预警信息
  Future<List<Map<String, dynamic>>> _fetchBudgetAlertsFromBackend() async {
    try {
      // 创建ApiService实例
      final apiService = ApiService();
      
      // 调用后端API获取预算预警信息
      final response = await apiService.getBudgetAlerts();
      
      if (response.success && response.data != null) {
        // 将BudgetAlert对象转换为Map格式
        return response.data!.map((alert) => {
          'categoryName': alert.categoryName,
          'budgetName': alert.budgetName,
          'budgetAmount': alert.budgetAmount,
          'spentAmount': alert.spentAmount,
          'year': alert.year,
          'month': alert.month,
          'alertLevel': alert.alertLevel,
          'message': alert.message,
        }).toList();
      } else {
        debugPrint('获取预算预警失败: ${response.message}');
        return [];
      }
    } catch (e) {
      debugPrint('调用预算预警API失败: $e');
      // 如果API调用失败，返回空列表
      return [];
    }
  }
  
  /// 启动预算预警定时检查
  void startBudgetAlertTimer() {
    // 每30分钟检查一次预算预警
    Timer.periodic(const Duration(minutes: 30), (timer) {
      checkBudgetAlerts();
    });
    
    debugPrint('预算预警定时检查已启动，每30分钟检查一次');
  }

  /// 检查并发送储蓄目标提醒
  Future<void> checkSavingGoalReminders() async {
    try {
      final db = await DatabaseService().database;

      // 获取所有储蓄目标
      final goalsResult = await db.query('saving_goals');

      for (final goalMap in goalsResult) {
        final goal = SavingGoal(
          id: goalMap['id'] as int?,
          name: goalMap['name'] as String,
          targetAmount: (goalMap['target_amount'] as num).toDouble(),
          currentAmount: (goalMap['current_amount'] as num).toDouble(),
          deadline: DateTime.fromMillisecondsSinceEpoch(
            goalMap['deadline'] as int,
          ),
          description: goalMap['description'] as String?,
          categoryName: '未分类', // 添加默认分类名称
        );
        final daysLeft = goal.deadline.difference(DateTime.now()).inDays;

        // 只在特定时间节点发送提醒
        final shouldSendReminder =
            daysLeft == 30 || daysLeft == 14 || daysLeft == 7 || daysLeft == 1;

        if (shouldSendReminder) {
          // 检查是否已经发送过今天的提醒
          final prefs = await SharedPreferences.getInstance();
          final todayKey =
              'goal_reminder_${goal.id}_${DateTime.now().toIso8601String().substring(0, 10)}';

          if (!prefs.containsKey(todayKey)) {
            await showSavingGoalReminder(
              goalName: goal.name,
              targetAmount: goal.targetAmount,
              currentAmount: goal.currentAmount,
              deadline: goal.deadline,
              payload: 'saving_goal_reminder',
            );

            // 标记今天已发送
            await prefs.setString(todayKey, 'sent');
          }
        }
      }
    } catch (e) {
      debugPrint('检查储蓄目标提醒失败: $e');
    }
  }

  /// 获取通知设置（支持布尔值和整数值）
  Future<Map<String, dynamic>> getNotificationSettings() async {
    final prefs = await SharedPreferences.getInstance();
    return {
      'budget_alerts': prefs.getBool('budget_alerts') ?? true,
      'budget_alert_threshold': prefs.getInt('budget_alert_threshold') ?? 90,
      'saving_reminders': prefs.getBool('saving_reminders') ?? true,
      'daily_reminders': prefs.getBool('daily_reminders') ?? false,
      'theme_notifications': prefs.getBool('theme_notifications') ?? true,
    };
  }

  /// 保存通知设置（支持布尔值和整数值）
  Future<void> saveNotificationSettings(Map<String, dynamic> settings) async {
    final prefs = await SharedPreferences.getInstance();
    for (final entry in settings.entries) {
      if (entry.value is bool) {
        await prefs.setBool(entry.key, entry.value as bool);
      } else if (entry.value is int) {
        await prefs.setInt(entry.key, entry.value as int);
      }
    }
  }

  /// 设置每日记账提醒开关
  Future<void> setDailyReminderEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();

    if (enabled) {
      // 默认每天晚上8点提醒
      await scheduleDailyReminder(hour: 20, minute: 0);
      await prefs.setBool('daily_reminders', true);
    } else {
      await cancel(1001);
      await prefs.setBool('daily_reminders', false);
    }
  }
}
