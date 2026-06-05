import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppConfigProvider with ChangeNotifier {
  ThemeMode _themeMode = ThemeMode.system;
  bool _enableReminder = false;
  TimeOfDay _reminderTime = const TimeOfDay(hour: 20, minute: 0);
  bool _enableBudgetAlert = true;

  ThemeMode get themeMode => _themeMode;
  bool get enableReminder => _enableReminder;
  TimeOfDay get reminderTime => _reminderTime;
  bool get enableBudgetAlert => _enableBudgetAlert;

  AppConfigProvider() {
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    final isDark = prefs.getBool('isDark');
    if (isDark != null) {
      _themeMode = isDark ? ThemeMode.dark : ThemeMode.light;
    }

    _enableReminder = prefs.getBool('enableReminder') ?? false;
    final hour = prefs.getInt('reminderHour') ?? 20;
    final minute = prefs.getInt('reminderMinute') ?? 0;
    _reminderTime = TimeOfDay(hour: hour, minute: minute);
    
    _enableBudgetAlert = prefs.getBool('enableBudgetAlert') ?? true;

    notifyListeners();
  }

  Future<void> toggleTheme(bool isDark) async {
    _themeMode = isDark ? ThemeMode.dark : ThemeMode.light;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isDark', isDark);
  }

  Future<void> setReminder(bool enable) async {
    _enableReminder = enable;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('enableReminder', enable);
  }

  Future<void> setReminderTime(TimeOfDay time) async {
    _reminderTime = time;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('reminderHour', time.hour);
    await prefs.setInt('reminderMinute', time.minute);
  }

  Future<void> setBudgetAlert(bool enable) async {
    _enableBudgetAlert = enable;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('enableBudgetAlert', enable);
  }
}
