import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../shared/utils/constants.dart';

/// 主题提供者 - 管理应用主题状态
class ThemeProvider extends ChangeNotifier {
  ThemeMode _themeMode = ThemeMode.system;
  bool _isDarkMode = false;
  int _refreshCounter = 0;

  /// 获取当前主题模式
  ThemeMode get themeMode => _themeMode;
  
  /// 获取是否深色模式
  bool get isDarkMode => _isDarkMode;
  
  /// 获取刷新计数器，用于强制组件重建
  int get refreshCounter => _refreshCounter;

  /// 初始化主题模式
  Future<void> initTheme() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final themeModeString = prefs.getString(AppConstants.themeModeKey);
      
      if (themeModeString != null) {
        switch (themeModeString) {
          case 'light':
            _themeMode = ThemeMode.light;
            _isDarkMode = false;
            break;
          case 'dark':
            _themeMode = ThemeMode.dark;
            _isDarkMode = true;
            break;
          case 'system':
          default:
            _themeMode = ThemeMode.system;
            _isDarkMode = false; // 默认使用浅色
            break;
        }
      }
      
      notifyListeners();
    } catch (e) {
      debugPrint('加载主题模式失败: $e');
    }
  }

  /// 更新系统UI样式
  void _updateSystemUiOverlayStyle() {
    final brightness = _isDarkMode ? Brightness.dark : Brightness.light;
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: brightness == Brightness.dark ? Brightness.light : Brightness.dark,
        systemNavigationBarColor: brightness == Brightness.dark ? const Color(0xFF121212) : Colors.white,
        systemNavigationBarIconBrightness: brightness == Brightness.dark ? Brightness.light : Brightness.dark,
      ),
    );
  }

  /// 切换到浅色主题
  Future<void> setLightTheme() async {
    _themeMode = ThemeMode.light;
    _isDarkMode = false;
    await _saveThemeMode('light');
    _updateSystemUiOverlayStyle();
    _refreshCounter++;
    notifyListeners();
  }

  /// 切换到深色主题
  Future<void> setDarkTheme() async {
    _themeMode = ThemeMode.dark;
    _isDarkMode = true;
    await _saveThemeMode('dark');
    _updateSystemUiOverlayStyle();
    _refreshCounter++;
    notifyListeners();
  }

  /// 切换到跟随系统主题
  Future<void> setSystemTheme() async {
    _themeMode = ThemeMode.system;
    // 系统主题由MaterialApp自动处理，_isDarkMode用于设置页面显示
    _isDarkMode = _isSystemDarkMode();
    await _saveThemeMode('system');
    _updateSystemUiOverlayStyle();
    _refreshCounter++;
    notifyListeners();
  }

  /// 切换主题模式
  Future<void> toggleTheme() async {
    // 使用同步方式切换主题，避免快速切换时的状态不一致
    if (_themeMode == ThemeMode.light) {
      _themeMode = ThemeMode.dark;
      _isDarkMode = true;
    } else if (_themeMode == ThemeMode.dark) {
      _themeMode = ThemeMode.system;
      _isDarkMode = _isSystemDarkMode();
    } else {
      _themeMode = ThemeMode.light;
      _isDarkMode = false;
    }
    
    // 先保存主题，再更新UI样式和通知监听器
    await _saveThemeMode(
      _themeMode == ThemeMode.light ? 'light' :
      _themeMode == ThemeMode.dark ? 'dark' : 'system'
    );
    _updateSystemUiOverlayStyle();
    _refreshCounter++;
    notifyListeners();
  }

  /// 设置自定义主题模式
  Future<void> setThemeMode(ThemeMode mode) async {
    _themeMode = mode;
    
    switch (mode) {
      case ThemeMode.light:
        _isDarkMode = false;
        await _saveThemeMode('light');
        break;
      case ThemeMode.dark:
        _isDarkMode = true;
        await _saveThemeMode('dark');
        break;
      case ThemeMode.system:
        _isDarkMode = _isSystemDarkMode();
        await _saveThemeMode('system');
        break;
    }
    
    _updateSystemUiOverlayStyle();
    _refreshCounter++;
    notifyListeners();
  }

  /// 保存主题模式到本地
  Future<void> _saveThemeMode(String mode) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(AppConstants.themeModeKey, mode);
    } catch (e) {
      debugPrint('保存主题模式失败: $e');
    }
  }

  /// 检测系统是否为深色模式
  bool _isSystemDarkMode() {
    return WidgetsBinding.instance.platformDispatcher.platformBrightness == Brightness.dark;
  }

  /// 获取主题模式显示文本
  String getThemeModeText() {
    switch (_themeMode) {
      case ThemeMode.light:
        return '浅色模式';
      case ThemeMode.dark:
        return '深色模式';
      case ThemeMode.system:
        return '跟随系统';
    }
  }

  /// 获取主题模式图标
  IconData getThemeModeIcon() {
    switch (_themeMode) {
      case ThemeMode.light:
        return Icons.light_mode;
      case ThemeMode.dark:
        return Icons.dark_mode;
      case ThemeMode.system:
        return Icons.brightness_auto;
    }
  }

  /// 获取下一个主题模式（用于切换按钮）
  ThemeMode getNextThemeMode() {
    switch (_themeMode) {
      case ThemeMode.light:
        return ThemeMode.dark;
      case ThemeMode.dark:
        return ThemeMode.system;
      case ThemeMode.system:
        return ThemeMode.light;
    }
  }

  /// 获取下一个主题模式文本
  String getNextThemeModeText() {
    switch (_themeMode) {
      case ThemeMode.light:
        return '深色模式';
      case ThemeMode.dark:
        return '跟随系统';
      case ThemeMode.system:
        return '浅色模式';
    }
  }

  /// 获取下一个主题模式图标
  IconData getNextThemeModeIcon() {
    switch (_themeMode) {
      case ThemeMode.light:
        return Icons.dark_mode;
      case ThemeMode.dark:
        return Icons.brightness_auto;
      case ThemeMode.system:
        return Icons.light_mode;
    }
  }
}