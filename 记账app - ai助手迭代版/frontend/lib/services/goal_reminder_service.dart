import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../data/models/saving_goal_model.dart';

/// 储蓄目标提醒服务
class GoalReminderService {
  static final GoalReminderService _instance = GoalReminderService._internal();
  factory GoalReminderService() => _instance;
  GoalReminderService._internal();

  final FlutterLocalNotificationsPlugin _notifications = 
      FlutterLocalNotificationsPlugin();
  Timer? _reminderTimer;
  List<SavingGoal> _goals = [];

  /// 初始化提醒服务
  Future<void> initialize() async {
    // 初始化通知插件
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');
    const InitializationSettings initializationSettings = 
        InitializationSettings(android: initializationSettingsAndroid);
    
    await _notifications.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );

    // 请求通知权限
    await _requestNotificationPermission();
    
    // 启动定时检查
    _startReminderCheck();
  }

  /// 请求通知权限
  Future<void> _requestNotificationPermission() async {
    await _notifications
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
  }

  /// 设置提醒目标
  Future<void> setReminderGoals(List<SavingGoal> goals) async {
    _goals = goals;
    await _checkReminders();
  }

  /// 启动定时检查
  void _startReminderCheck() {
    // 每小时检查一次
    _reminderTimer = Timer.periodic(const Duration(hours: 1), (_) {
      _checkReminders();
    });
    
    // 延迟5秒后立即检查一次
    Timer(const Duration(seconds: 5), () {
      _checkReminders();
    });
  }

  /// 检查提醒条件
  Future<void> _checkReminders() async {
    final prefs = await SharedPreferences.getInstance();

    for (final goal in _goals) {
      await _checkGoalDeadlineReminder(goal, prefs);
      await _checkGoalProgressReminder(goal, prefs);
    }
  }

  /// 检查目标截止日期提醒
  Future<void> _checkGoalDeadlineReminder(SavingGoal goal, SharedPreferences prefs) async {
    if (goal.isCompleted) { return; }

    final remainingDays = goal.remainingDays;
    
    // 检查是否需要发送提醒（7天、3天、1天前）
    final reminderDays = [7, 3, 1];
    
    for (final day in reminderDays) {
      if (remainingDays == day) {
        final key = 'deadline_reminder_${goal.id}_$day';
        if (!(prefs.getBool(key) ?? false)) {
          await _sendDeadlineReminder(goal, day);
          await prefs.setBool(key, true);
        }
      }
    }
  }

  /// 检查目标进度提醒
  Future<void> _checkGoalProgressReminder(SavingGoal goal, SharedPreferences prefs) async {
    if (goal.isCompleted) { return; }

    final progress = goal.progress;
    final remainingDays = goal.remainingDays;
    if (remainingDays <= 0) { return; }
    
    // 计算期望进度（假设均匀储蓄）
    final totalDays = goal.deadline.difference(DateTime.now().subtract(Duration(days: remainingDays))).inDays;
    final expectedProgress = totalDays > 0 ? (totalDays - remainingDays) / totalDays : 0;
    
    // 进度落后超过20%时提醒
    if (progress < expectedProgress - 0.2) {
      final key = 'progress_reminder_${goal.id}';
      final lastReminder = prefs.getString(key);
      final now = DateTime.now();
      
      // 避免重复提醒（24小时内只提醒一次）
      if (lastReminder == null || 
          now.difference(DateTime.parse(lastReminder)).inHours >= 24) {
        await _sendProgressReminder(goal, expectedProgress.toDouble());
        await prefs.setString(key, now.toIso8601String());
      }
    }
  }

  /// 发送截止日期提醒
  Future<void> _sendDeadlineReminder(SavingGoal goal, int daysLeft) async {
    const AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
      'saving_goal_reminders',
      '储蓄目标提醒',
      channelDescription: '储蓄目标截止日期提醒',
      importance: Importance.high,
      priority: Priority.high,
    );
    const NotificationDetails notificationDetails =
        NotificationDetails(android: androidNotificationDetails);

    await _notifications.show(
      DateTime.now().millisecondsSinceEpoch ~/ 1000 + (goal.id ?? 0),
      '储蓄目标提醒',
      '目标 "${goal.name}" 还剩 $daysLeft 天到期，当前进度: ${(goal.progress * 100).toStringAsFixed(1)}%',
      notificationDetails,
    );
  }

  /// 发送进度落后提醒
  Future<void> _sendProgressReminder(SavingGoal goal, double expectedProgress) async {
    const AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
      'saving_goal_reminders',
      '储蓄目标提醒',
      channelDescription: '储蓄目标进度提醒',
      importance: Importance.high,
      priority: Priority.high,
    );
    const NotificationDetails notificationDetails =
        NotificationDetails(android: androidNotificationDetails);

    final currentProgress = goal.progress * 100;
    final expectedProgressPercent = expectedProgress * 100;
    final shortfall = expectedProgressPercent - currentProgress;

    await _notifications.show(
      DateTime.now().millisecondsSinceEpoch ~/ 1000 + (goal.id ?? 0) + 1000,
      '储蓄进度提醒',
      '目标 "${goal.name}" 进度落后了 ${shortfall.toStringAsFixed(1)}%，需要加快储蓄节奏！',
      notificationDetails,
    );
  }

  /// 处理通知点击
  void _onNotificationTapped(NotificationResponse response) {
    // 这里可以根据通知的payload跳转到相应页面
    debugPrint('通知点击: ${response.payload}');
  }

  /// 发送立即提醒
  Future<void> sendImmediateReminder(SavingGoal goal, String title, String message) async {
    const AndroidNotificationDetails androidNotificationDetails =
        AndroidNotificationDetails(
      'saving_goal_reminders',
      '储蓄目标提醒',
      channelDescription: '储蓄目标提醒',
      importance: Importance.high,
      priority: Priority.high,
    );
    const NotificationDetails notificationDetails =
        NotificationDetails(android: androidNotificationDetails);

    await _notifications.show(
      DateTime.now().millisecondsSinceEpoch,
      title,
      message,
      notificationDetails,
    );
  }

  /// 取消所有提醒
  Future<void> cancelAllReminders() async {
    await _notifications.cancelAll();
  }

  /// 清理资源
  void dispose() {
    _reminderTimer?.cancel();
  }
}

/// 储蓄目标提醒管理器
class GoalReminderManager extends ChangeNotifier {
  final GoalReminderService _reminderService = GoalReminderService();
  bool _isInitialized = false;

  bool get isInitialized => _isInitialized;

  /// 初始化提醒管理器
  Future<void> initialize() async {
    if (_isInitialized) { return; }

    await _reminderService.initialize();
    _isInitialized = true;
    notifyListeners();
  }

  /// 设置提醒目标
  Future<void> setReminderGoals(List<SavingGoal> goals) async {
    await _reminderService.setReminderGoals(goals);
  }

  /// 发送自定义提醒
  Future<void> sendReminder(
    SavingGoal goal, 
    String title, 
    String message,
  ) async {
    await _reminderService.sendImmediateReminder(goal, title, message);
  }

  /// 取消所有提醒
  Future<void> cancelAllReminders() async {
    await _reminderService.cancelAllReminders();
  }

  /// 清理资源
  @override
  void dispose() {
    _reminderService.dispose();
    super.dispose();
  }
}