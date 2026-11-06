import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/services/data_reset_service.dart';
import '../../core/providers/theme_provider.dart';
import '../../services/notification_service.dart';
import '../../services/privacy_service.dart';
import '../../shared/utils/constants.dart';
import '../../widgets/layout/responsive_container.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  final NotificationService _notificationService = NotificationService();
  final PrivacyService _privacyService = PrivacyService();
  final DataResetService _dataResetService = DataResetService();

  Map<String, dynamic> _notificationSettings = {
    'budget_alerts': true,
    'budget_alert_threshold': 90,
    'saving_reminders': true,
    'daily_reminders': false,
    'theme_notifications': true,
  };

  PrivacySettings _privacySettings = PrivacySettings.defaultSettings();
  String _userName = '未设置昵称';
  String _userEmail = '完善邮箱以接收通知';
  bool _loadingSettings = true;
  final Set<_DataActionType> _runningDataActions = <_DataActionType>{};
  ThemeMode _tempSelectedMode = ThemeMode.system; // 临时存储用户选择的主题模式

  @override
  void initState() {
    super.initState();
    _initializePage();
  }

  Future<void> _initializePage() async {
    await Future.wait([
      _loadUserProfile(),
      _loadNotificationSettings(),
      _loadPrivacySettings(),
    ]);

    if (mounted) {
      setState(() {
        _loadingSettings = false;
      });
    }
  }

  Future<void> _loadUserProfile() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(AppConstants.userSettingsKey);
      if (raw == null) return;

      final data = jsonDecode(raw) as Map<String, dynamic>;
      final name = data['profile_name'] as String?;
      final email = data['profile_email'] as String?;

      if (!mounted) return;
      setState(() {
        if (name != null && name.trim().isNotEmpty) {
          _userName = name.trim();
        }
        if (email != null && email.trim().isNotEmpty) {
          _userEmail = email.trim();
        }
      });
    } catch (e) {
      debugPrint('加载用户信息失败: $e');
    }
  }

  Future<void> _loadNotificationSettings() async {
    try {
      final settings = await _notificationService.getNotificationSettings();
      if (mounted) {
        setState(() {
          _notificationSettings = settings;
        });
      }
    } catch (e) {
      // 记录错误但不影响用户体验
      debugPrint('加载通知设置失败: $e');
    }
  }

  Future<void> _loadPrivacySettings() async {
    try {
      final settings = await _privacyService.getPrivacySettings();
      if (mounted) {
        setState(() {
          _privacySettings = settings;
        });
      }
    } catch (e) {
      debugPrint('加载隐私设置失败: $e');
    }
  }

  bool _isDataActionBusy(_DataActionType type) {
    return _runningDataActions.contains(type);
  }

  Future<void> _triggerDataAction(
    _DataActionType type,
    Future<void> Function() action,
  ) async {
    if (_runningDataActions.contains(type)) return;

    setState(() {
      _runningDataActions.add(type);
    });

    try {
      await action();
    } finally {
      if (mounted) {
        setState(() {
          _runningDataActions.remove(type);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('设置'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
        foregroundColor: theme.colorScheme.onSurface,
      ),
      body: _loadingSettings
          ? const Center(child: CircularProgressIndicator())
          : NarrowContentContainer(
              padding: const EdgeInsets.symmetric(horizontal: 0, vertical: 12),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 32),
                physics: const BouncingScrollPhysics(),
                children: [
                  _buildPageHero(context),
                  const SizedBox(height: 12),
                  _UserProfileCard(
                    name: _userName,
                    email: _userEmail,
                    onEdit: () => context.push('/settings/user-edit'),
                  ),
                  const SizedBox(height: 16),
                  _buildQuickCards(context),
                  const SizedBox(height: 16),
                  _buildThemeSettings(context),
                  const SizedBox(height: 16),
                  _buildNotificationSettings(context),
                  const SizedBox(height: 16),
                  _buildPrivacySettings(context),
                  const SizedBox(height: 16),
                  _buildDataManagement(context),
                  const SizedBox(height: 16),
                  _buildAboutAndHelp(context),
                ],
              ),
            ),
    );
  }

  Widget _buildPageHero(BuildContext context) {
    final theme = Theme.of(context);
    final onSurface = theme.colorScheme.onSurface;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '个性化设置',
          style: theme.textTheme.headlineSmall?.copyWith(
            fontWeight: FontWeight.w700,
            color: onSurface,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          '管理账号资料、主题外观和数据安全，打造属于你的记账体验。',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildQuickCards(BuildContext context) {
    final dailyReminderEnabled =
        _notificationSettings['daily_reminders'] ?? false;

    final cards = [
      _QuickSettingCard(
        icon: Icons.schedule_outlined,
        title: '每日记账提醒',
        description: '每天固定时间提醒记录收支',
        trailing: Switch.adaptive(
          value: dailyReminderEnabled,
          onChanged: (value) =>
              _updateNotificationSetting('daily_reminders', value),
        ),
      ),
      _QuickSettingCard(
        icon: Icons.shield_moon_outlined,
        title: '隐私模式',
        description: _privacySettings.privacyModeDescription,
        trailing: FilledButton.tonal(
          onPressed: () => _showPrivacyModeDialog(context),
          child: Text(_privacySettings.privacyModeDisplayText),
        ),
        onTap: () => _showPrivacyModeDialog(context),
      ),
      _QuickSettingCard(
        icon: Icons.lock_clock_outlined,
        title: '自动锁定',
        description: '闲置${_privacySettings.lockAppAfterMinutes}分钟后锁定应用',
        trailing: Switch.adaptive(
          value: _privacySettings.enableAutoLock,
          onChanged: (value) => _updatePrivacySetting('autoLock', value),
        ),
      ),
    ];

    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth > 700) {
          final children = <Widget>[];
          for (var i = 0; i < cards.length; i++) {
            children.add(Expanded(child: cards[i]));
            if (i != cards.length - 1) {
              children.add(const SizedBox(width: 12));
            }
          }
          return Row(children: children);
        }

        return Column(
          children: [
            for (var i = 0; i < cards.length; i++) ...[
              cards[i],
              if (i != cards.length - 1) const SizedBox(height: 12),
            ],
          ],
        );
      },
    );
  }

  Widget _buildThemeSettings(BuildContext context) {
    return Consumer<ThemeProvider>(
      builder: (context, themeProvider, child) {
        return _SettingsSectionCard(
          title: '主题与外观',
          description: '切换主题模式并快速预览不同外观效果。',
          child: Column(
            children: [
              _buildThemeModeSetting(context, themeProvider),
              const Divider(height: 1),
              _buildQuickThemeSwitch(context, themeProvider),
            ],
          ),
        );
      },
    );
  }

  Widget _buildThemeModeSetting(
    BuildContext context,
    ThemeProvider themeProvider,
  ) {
    return _buildSettingItem(
      context,
      '主题模式',
      themeProvider.getThemeModeIcon(),
      onTap: () => _showThemeModeDialog(context, themeProvider),
      trailing: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.primaryContainer,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Text(
          themeProvider.getThemeModeText(),
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: Theme.of(context).colorScheme.onPrimaryContainer,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  Widget _buildQuickThemeSwitch(
    BuildContext context,
    ThemeProvider themeProvider,
  ) {
    return _buildSettingItem(
      context,
      '快速切换',
      themeProvider.getNextThemeModeIcon(),
      onTap: () => themeProvider.toggleTheme(),
      subtitle: '下一步切换到${themeProvider.getNextThemeModeText()}',
      trailing: Icon(
        Icons.swap_horiz_outlined,
        color: Theme.of(context).colorScheme.onSurface,
      ),
    );
  }

  void _showThemeModeDialog(BuildContext context, ThemeProvider themeProvider) {
    // 使用局部变量存储用户选择，避免与类成员变量冲突
    ThemeMode tempSelectedMode = themeProvider.themeMode; // 初始化为当前主题模式
    
    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('选择主题模式'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildThemeModeOption(
                context,
                '浅色模式',
                '明亮的主题风格',
                tempSelectedMode == ThemeMode.light,
                ThemeMode.light,
                themeProvider,
                (mode) {
                  setState(() {
                    tempSelectedMode = mode;
                  });
                },
              ),
              const SizedBox(height: 8),
              _buildThemeModeOption(
                context,
                '深色模式',
                '暗色主题风格',
                tempSelectedMode == ThemeMode.dark,
                ThemeMode.dark,
                themeProvider,
                (mode) {
                  setState(() {
                    tempSelectedMode = mode;
                  });
                },
              ),
              const SizedBox(height: 8),
              _buildThemeModeOption(
                context,
                '跟随系统',
                '根据系统设置自动切换',
                tempSelectedMode == ThemeMode.system,
                ThemeMode.system,
                themeProvider,
                (mode) {
                  setState(() {
                    tempSelectedMode = mode;
                  });
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context), // 取消不应用任何更改
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () async {
                Navigator.pop(context); // 关闭对话框
                
                // 应用用户选择的主题
                switch (tempSelectedMode) {
                  case ThemeMode.light:
                    await themeProvider.setLightTheme();
                    break;
                  case ThemeMode.dark:
                    await themeProvider.setDarkTheme();
                    break;
                  case ThemeMode.system:
                    await themeProvider.setSystemTheme();
                    break;
                }
              },
              child: const Text('确定'),
            ),
          ],
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
      ),
    );
  }

  Widget _buildThemeModeOption(
    BuildContext context,
    String title,
    String subtitle,
    bool isSelected,
    ThemeMode mode,
    ThemeProvider themeProvider,
    Function(ThemeMode) onSelect, // 新增回调函数
  ) {
    return GestureDetector(
      onTap: () {
        onSelect(mode); // 只更新临时选择，不应用主题
      },
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          border: Border.all(
            color: isSelected
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.outline,
            width: 2,
          ),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      fontWeight: isSelected
                          ? FontWeight.w600
                          : FontWeight.normal,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            if (isSelected)
              Icon(
                Icons.check_circle,
                color: Theme.of(context).colorScheme.primary,
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildNotificationSettings(BuildContext context) {
    return _SettingsSectionCard(
      title: '通知与提醒',
      description: '掌控重要提醒，避免遗漏预算、储蓄等关键事件。',
      child: Column(
        children: [
          _buildSwitchSetting(
            context,
            '预算预警',
            '当预算使用超过90%时发送通知',
            _notificationSettings['budget_alerts'] ?? true,
            Icons.warning_amber_outlined,
            (value) => _updateNotificationSetting('budget_alerts', value),
          ),
          const Divider(height: 1),
          _buildSettingItem(
            context,
            '预算预警阈值',
            Icons.tune_outlined,
            onTap: () => _showBudgetAlertThresholdDialog(context),
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Text(
                '${_notificationSettings['budget_alert_threshold'] ?? 90}%',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onPrimaryContainer,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
          const Divider(height: 1),
          _buildSwitchSetting(
            context,
            '储蓄提醒',
            '储蓄目标进度和截止日期提醒',
            _notificationSettings['saving_reminders'] ?? true,
            Icons.savings_outlined,
            (value) => _updateNotificationSetting('saving_reminders', value),
          ),
          const Divider(height: 1),
          _buildSwitchSetting(
            context,
            '每日记账提醒',
            '每天定时提醒记录收支',
            _notificationSettings['daily_reminders'] ?? false,
            Icons.schedule_outlined,
            (value) => _updateNotificationSetting('daily_reminders', value),
          ),
          const Divider(height: 1),
          _buildSwitchSetting(
            context,
            '主题切换通知',
            '主题切换时显示提示通知',
            _notificationSettings['theme_notifications'] ?? true,
            Icons.palette_outlined,
            (value) => _updateNotificationSetting('theme_notifications', value),
          ),
        ],
      ),
    );
  }

  Widget _buildPrivacySettings(BuildContext context) {
    return _SettingsSectionCard(
      title: '隐私与安全',
      description: '管理数据加密、生物识别等安全策略，守护个人数据。',
      child: Column(
        children: [
          _buildSettingItem(
            context,
            '隐私模式',
            Icons.security_outlined,
            onTap: () => _showPrivacyModeDialog(context),
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Text(
                _privacySettings.privacyModeDisplayText,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onPrimaryContainer,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
          const Divider(height: 1),
          _buildSwitchSetting(
            context,
            '生物识别验证',
            '使用指纹或面部识别解锁应用',
            _privacySettings.biometricEnabled,
            Icons.fingerprint_outlined,
            (value) => _updatePrivacySetting('biometric', value),
          ),
          const Divider(height: 1),
          _buildSwitchSetting(
            context,
            '数据加密',
            '对敏感数据进行加密存储',
            _privacySettings.dataEncryptionEnabled,
            Icons.lock_outline,
            (value) => _updatePrivacySetting('encryption', value),
          ),
          const Divider(height: 1),
          _buildSwitchSetting(
            context,
            '自动锁定',
            '闲置一段时间后自动锁定应用',
            _privacySettings.enableAutoLock,
            Icons.timer_outlined,
            (value) => _updatePrivacySetting('autoLock', value),
          ),
        ],
      ),
    );
  }

  Widget _buildSwitchSetting(
    BuildContext context,
    String title,
    String subtitle,
    bool value,
    IconData icon,
    Function(bool) onChanged,
  ) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Icon(icon, size: 24, color: Theme.of(context).colorScheme.onSurface),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.bodyLarge),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          Switch.adaptive(
            value: value,
            onChanged: onChanged,
            thumbColor: WidgetStateProperty.resolveWith((states) {
              if (states.contains(WidgetState.selected)) {
                return Theme.of(context).colorScheme.primary;
              }
              return null;
            }),
          ),
        ],
      ),
    );
  }

  Future<void> _updateNotificationSetting(String key, dynamic value) async {
    try {
      setState(() {
        _notificationSettings[key] = value;
      });

      await _notificationService.saveNotificationSettings(
        _notificationSettings,
      );

      if (key == 'daily_reminders' && value is bool) {
        await _notificationService.setDailyReminderEnabled(value);
      }

      if (mounted) {
        String message;
        if (value is bool) {
          message = '已${value ? '开启' : '关闭'}${_getNotificationSettingName(key)}';
        } else if (value is int && key == 'budget_alert_threshold') {
          message = '已设置预算预警阈值为 $value%';
        } else {
          message = '设置已更新';
        }
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(message),
            duration: const Duration(seconds: 1),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('设置失败: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }

  Future<void> _updatePrivacySetting(String key, bool value) async {
    try {
      PrivacySettings newSettings = _privacySettings;

      switch (key) {
        case 'biometric':
          if (value && !_privacySettings.biometricEnabled) {
            final enabled = await _privacyService.enableBiometricAuth();
            if (!enabled) {
              if (mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(const SnackBar(content: Text('生物识别验证失败')));
              }
              return;
            }
          } else if (!value && _privacySettings.biometricEnabled) {
            await _privacyService.disableBiometricAuth();
          }
          newSettings = _privacySettings.copyWith(biometricEnabled: value);
          break;
        case 'encryption':
          if (value && !_privacySettings.dataEncryptionEnabled) {
            _showEncryptionPasswordDialog(context, true);
            return;
          } else if (!value && _privacySettings.dataEncryptionEnabled) {
            await _privacyService.disableDataEncryption();
          }
          newSettings = _privacySettings.copyWith(dataEncryptionEnabled: value);
          break;
        case 'autoLock':
          newSettings = _privacySettings.copyWith(enableAutoLock: value);
          break;
        default:
          return;
      }

      await _privacyService.savePrivacySettings(newSettings);

      setState(() {
        _privacySettings = newSettings;
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '已${value ? '开启' : '关闭'}${_getPrivacySettingName(key)}',
            ),
            duration: const Duration(seconds: 1),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('设置失败: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }

  String _getNotificationSettingName(String key) {
    switch (key) {
      case 'budget_alerts':
        return '预算预警';
      case 'saving_reminders':
        return '储蓄提醒';
      case 'daily_reminders':
        return '每日记账提醒';
      case 'theme_notifications':
        return '主题切换通知';
      default:
        return '通知设置';
    }
  }

  String _getPrivacySettingName(String key) {
    switch (key) {
      case 'biometric':
        return '生物识别验证';
      case 'encryption':
        return '数据加密';
      case 'autoLock':
        return '自动锁定';
      default:
        return '隐私设置';
    }
  }

  void _showPrivacyModeDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('选择隐私模式'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildPrivacyModeOption(
              context,
              '严格模式',
              '最高级别隐私保护，隐藏所有敏感信息',
              _privacySettings.privacyMode == PrivacyMode.strict,
              PrivacyMode.strict,
            ),
            const SizedBox(height: 8),
            _buildPrivacyModeOption(
              context,
              '正常模式',
              '平衡隐私保护，隐藏部分敏感信息',
              _privacySettings.privacyMode == PrivacyMode.normal,
              PrivacyMode.normal,
            ),
            const SizedBox(height: 8),
            _buildPrivacyModeOption(
              context,
              '开放模式',
              '最小隐私限制，显示所有信息',
              _privacySettings.privacyMode == PrivacyMode.open,
              PrivacyMode.open,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('确定'),
          ),
        ],
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
    );
  }

  Widget _buildPrivacyModeOption(
    BuildContext context,
    String title,
    String subtitle,
    bool isSelected,
    PrivacyMode mode,
  ) {
    return GestureDetector(
      onTap: () async {
        Navigator.pop(context);
        final newSettings = _privacySettings.copyWith(privacyMode: mode);
        await _privacyService.savePrivacySettings(newSettings);
        if (!context.mounted) return;
        setState(() {
          _privacySettings = newSettings;
        });

        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('已切换到$title'),
            duration: const Duration(seconds: 1),
          ),
        );
      },
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          border: Border.all(
            color: isSelected
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.outline,
            width: 2,
          ),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      fontWeight: isSelected
                          ? FontWeight.w600
                          : FontWeight.normal,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            if (isSelected)
              Icon(
                Icons.check_circle,
                color: Theme.of(context).colorScheme.primary,
              ),
          ],
        ),
      ),
    );
  }

  void _showEncryptionPasswordDialog(BuildContext context, bool enable) {
    final TextEditingController passwordController = TextEditingController();
    final TextEditingController confirmController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(enable ? '设置加密密码' : '验证密码'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: passwordController,
              decoration: const InputDecoration(
                labelText: '密码',
                hintText: '请输入加密密码',
              ),
              obscureText: true,
            ),
            if (enable) ...[
              const SizedBox(height: 16),
              TextField(
                controller: confirmController,
                decoration: const InputDecoration(
                  labelText: '确认密码',
                  hintText: '请再次输入密码',
                ),
                obscureText: true,
              ),
            ],
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () async {
              if (passwordController.text.isEmpty) return;

              if (enable && passwordController.text != confirmController.text) {
                if (mounted) {
                  ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(const SnackBar(content: Text('两次输入的密码不一致')));
                }
                return;
              }

              Navigator.pop(context);
              // 这里实现加密设置逻辑
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  Widget _buildDataManagement(BuildContext context) {
    final actions = [
      _DataActionButton(
        icon: Icons.file_download_outlined,
        label: '数据导出',
        description: '导出为 Excel/CSV',
        busy: _isDataActionBusy(_DataActionType.export),
        onPressed: _isDataActionBusy(_DataActionType.export)
            ? null
            : () => _triggerDataAction(
                _DataActionType.export,
                () => _navigateToExportPage(context),
              ),
      ),
      _DataActionButton(
        icon: Icons.backup_outlined,
        label: '备份恢复',
        description: '备份和恢复数据',
        busy: _isDataActionBusy(_DataActionType.backup),
        onPressed: _isDataActionBusy(_DataActionType.backup)
            ? null
            : () => _triggerDataAction(
                _DataActionType.backup,
                () => _navigateToBackupPage(context),
              ),
      ),
      _DataActionButton(
        icon: Icons.delete_forever_outlined,
        label: '清除数据',
        description: '删除所有本地数据',
        danger: true,
        busy: _isDataActionBusy(_DataActionType.clear),
        onPressed: _isDataActionBusy(_DataActionType.clear)
            ? null
            : () => _triggerDataAction(
                _DataActionType.clear,
                () => _showClearDataDialog(context),
              ),
      ),
    ];

    return _SettingsSectionCard(
      title: '数据与备份',
      description: '导出、备份或清理数据，确保信息安全可靠。',
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isTwoColumns = constraints.maxWidth > 420;
          final itemWidth = isTwoColumns
              ? (constraints.maxWidth - 12) / 2
              : constraints.maxWidth;

          return Wrap(
            spacing: 12,
            runSpacing: 12,
            children: actions
                .map((action) => SizedBox(width: itemWidth, child: action))
                .toList(),
          );
        },
      ),
    );
  }

  Future<void> _navigateToExportPage(BuildContext context) async {
    await context.push('/settings/export');
  }

  Future<void> _navigateToBackupPage(BuildContext context) async {
    await context.push('/settings/backup');
  }



  Future<void> _showClearDataDialog(BuildContext context) {
    final rootContext = context;
    return showDialog(
      context: rootContext,
      builder: (dialogContext) => AlertDialog(
        title: const Text('清除所有数据'),
        content: const Text('确定要清除所有数据吗？这将删除所有收支记录、分类、预算和目标，且无法恢复。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () async {
              Navigator.pop(dialogContext);
              try {
                await _dataResetService.clearAllData();
                if (!mounted) return;
                ScaffoldMessenger.of(rootContext).showSnackBar(
                  const SnackBar(
                    content: Text('所有数据已成功清除'),
                    backgroundColor: Colors.green,
                  ),
                );
              } catch (e) {
                if (!mounted) return;
                ScaffoldMessenger.of(rootContext).showSnackBar(
                  SnackBar(
                    content: Text('清除数据失败: $e'),
                    backgroundColor: Colors.red,
                  ),
                );
              }
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  Widget _buildAboutAndHelp(BuildContext context) {
    return _SettingsSectionCard(
      title: '关于与帮助',
      description: '查看版本信息、提交反馈，或获取更多支持。',
      child: Column(
        children: [
          _buildSettingItem(
            context,
            '关于应用',
            Icons.info_outline,
            onTap: () => _showAboutDialog(context),
          ),
          const Divider(height: 1),
          _buildSettingItem(
            context,
            '用户反馈',
            Icons.feedback_outlined,
            onTap: () => _showFeedbackDialog(context),
          ),
          const Divider(height: 1),
          _buildSettingItem(
            context,
            '版本信息',
            Icons.system_security_update_good_outlined,
            subtitle: 'v1.0.0',
          ),
        ],
      ),
    );
  }

  void _showAboutDialog(BuildContext context) {
    showAboutDialog(
      context: context,
      applicationName: AppConstants.appName,
      applicationVersion: AppConstants.appVersion,
      applicationLegalese: '© 2024 个人记账APP',
      children: const [
        SizedBox(height: 16),
        Text(
          '功能特性：\n'
          '• 简单易用的收支记录\n'
          '• 智能分类和统计分析\n'
          '• 预算管理和预警提醒\n'
          '• 储蓄目标追踪\n'
          '• 数据备份与恢复\n'
          '• 隐私安全保护',
        ),
      ],
    );
  }

  void _showFeedbackDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('用户反馈'),
        content: const Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('我们重视您的意见和建议！', style: TextStyle(fontWeight: FontWeight.bold)),
            SizedBox(height: 12),
            Text(
              '反馈渠道：\n'
              '• 邮箱：feedback@financeapp.com\n'
              '• 微信群：扫码加入用户交流群\n'
              '• 应用商店：评分和评论',
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  void _showBudgetAlertThresholdDialog(BuildContext context) {
    final currentThreshold = _notificationSettings['budget_alert_threshold'] ?? 90;
    int selectedThreshold = currentThreshold;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            title: const Text('预算预警阈值'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '设置预算使用率阈值，当预算使用超过该比例时发送预警通知。',
                  style: TextStyle(fontSize: 14, color: Colors.grey),
                ),
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      '$selectedThreshold%',
                      style: const TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.bold,
                        color: Colors.orange,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Slider(
                  value: selectedThreshold.toDouble(),
                  min: 50,
                  max: 95,
                  divisions: 9,
                  label: '$selectedThreshold%',
                  onChanged: (value) {
                    setState(() {
                      selectedThreshold = value.round();
                    });
                  },
                ),
                const SizedBox(height: 10),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: const [
                    Text('50%', style: TextStyle(fontSize: 12)),
                    Text('70%', style: TextStyle(fontSize: 12)),
                    Text('80%', style: TextStyle(fontSize: 12)),
                    Text('90%', style: TextStyle(fontSize: 12)),
                    Text('95%', style: TextStyle(fontSize: 12)),
                  ],
                ),
                const SizedBox(height: 10),
                Text(
                  _getThresholdDescription(selectedThreshold),
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: () {
                  _updateNotificationSetting('budget_alert_threshold', selectedThreshold);
                  Navigator.pop(context);
                },
                child: const Text('保存'),
              ),
            ],
          );
        },
      ),
    );
  }

  String _getThresholdDescription(int threshold) {
    if (threshold <= 70) {
      return '宽松提醒：适合预算控制较好的用户';
    } else if (threshold <= 80) {
      return '适中提醒：适合大多数用户';
    } else if (threshold <= 90) {
      return '严格提醒：适合需要严格控制预算的用户';
    } else {
      return '紧急提醒：预算即将用完，建议及时调整';
    }
  }

  Widget _buildSettingItem(
    BuildContext context,
    String title,
    IconData icon, {
    VoidCallback? onTap,
    Widget? trailing,
    String? subtitle,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Row(
          children: [
            Icon(
              icon,
              size: 24,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.bodyLarge),
                  if (subtitle != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            if (trailing != null) ...[
              const SizedBox(width: 8),
              trailing,
            ] else ...[
              const SizedBox(width: 8),
              Icon(
                Icons.chevron_right,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _UserProfileCard extends StatelessWidget {
  final String name;
  final String email;
  final VoidCallback onEdit;

  const _UserProfileCard({
    required this.name,
    required this.email,
    required this.onEdit,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final initials = name.trim().isNotEmpty
        ? name.trim()[0].toUpperCase()
        : 'U';

    return Card(
      elevation: 0,
      color: colorScheme.surfaceContainerHighest,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: colorScheme.primary,
                  child: Text(
                    initials,
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        name,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        email,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                FilledButton.icon(
                  onPressed: onEdit,
                  icon: const Icon(Icons.edit_outlined),
                  label: const Text('编辑资料'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: colorScheme.secondaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.verified_user_outlined,
                    color: colorScheme.onSecondaryContainer,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '完善账号信息可更好地同步备份数据并接收提醒。',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSecondaryContainer,
                      ),
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
}

class _QuickSettingCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final Widget trailing;
  final VoidCallback? onTap;

  const _QuickSettingCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.trailing,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 0,
      color: colorScheme.surfaceContainerHighest,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: colorScheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(icon, color: colorScheme.primary),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      description,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              trailing,
            ],
          ),
        ),
      ),
    );
  }
}

class _SettingsSectionCard extends StatelessWidget {
  final String title;
  final String description;
  final Widget child;

  const _SettingsSectionCard({
    required this.title,
    required this.description,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Card(
      elevation: 0,
      color: colorScheme.surfaceContainerHighest,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              description,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            child,
          ],
        ),
      ),
    );
  }
}

class _DataActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final String description;
  final VoidCallback? onPressed;
  final bool busy;
  final bool danger;

  const _DataActionButton({
    required this.icon,
    required this.label,
    required this.description,
    required this.onPressed,
    this.busy = false,
    this.danger = false,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final background = danger
        ? colorScheme.errorContainer
        : colorScheme.secondaryContainer;
    final foreground = danger
        ? colorScheme.onErrorContainer
        : colorScheme.onSecondaryContainer;

    return AnimatedOpacity(
      duration: const Duration(milliseconds: 200),
      opacity: onPressed == null ? 0.6 : 1,
      child: Material(
        color: background,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Icon(icon, color: foreground),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        label,
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          color: foreground,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        description,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: foreground.withValues(alpha: 0.9),
                        ),
                      ),
                    ],
                  ),
                ),
                if (busy)
                  SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(foreground),
                    ),
                  )
                else
                  Icon(Icons.chevron_right, color: foreground),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

enum _DataActionType { export, backup, clear }
