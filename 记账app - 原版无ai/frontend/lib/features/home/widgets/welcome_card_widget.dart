import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:frontend/core/providers/user_provider.dart';
import '../../../core/utils/animation_utils.dart';

/// 欢迎卡片组件
/// 显示用户问候语和当前日期，增强版美观设计
class WelcomeCardWidget extends StatefulWidget {
  const WelcomeCardWidget({super.key});

  @override
  State<WelcomeCardWidget> createState() => _WelcomeCardWidgetState();
}

class _WelcomeCardWidgetState extends State<WelcomeCardWidget> {
  DateTime? _currentTime;
  int _messageIndex = 0; // 用于循环显示不同的欢迎消息

  @override
  void initState() {
    super.initState();
    _updateCurrentTime();
  }

  @override
  void dispose() {
    // 清理定时器，防止内存泄漏
    super.dispose();
  }

  /// 更新当前时间和欢迎消息
  void _updateCurrentTime() {
    if (mounted) {
      setState(() {
        _currentTime = DateTime.now();
        // 每次更新时，随机选择一条消息，确保每次刷新都更换欢迎语句
        _messageIndex = DateTime.now().millisecond % 5;
      });
      
      // 每分钟更新一次时间和消息，让欢迎语句更加动态
      Future.delayed(const Duration(minutes: 1), _updateCurrentTime);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final userProvider = Provider.of<UserProvider>(context);
    
    // 获取当前日期信息
    final now = _currentTime ?? DateTime.now();
    final weekdayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    final weekday = weekdayNames[now.weekday - 1];
    final dateString = '${now.month}月${now.day}日';
    final greeting = _getGreeting(now.hour);
    final userName = userProvider.user?.displayName ?? '用户';
    final welcomeMessage = _getWelcomeMessage(now.hour, userName);
    
    // 获取季节相关颜色
    final seasonalColors = _getSeasonalColors(now.month, colorScheme);
    
    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 600),
      beginOffset: const Offset(-30, 0),
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 16),
        decoration: BoxDecoration(
          // 增强的渐变背景效果
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: seasonalColors,
            stops: const [0.0, 0.3, 0.7, 1.0],
          ),
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(20),
              blurRadius: 12,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Stack(
          children: [
            // 背景装饰元素
            Positioned(
              top: -20,
              right: -20,
              child: AnimationUtils.createFadeIn(
                duration: const Duration(milliseconds: 800),
                delay: const Duration(milliseconds: 200),
                child: Opacity(
                  opacity: 0.05,
                  child: Icon(
                    Icons.waving_hand_outlined,
                    size: 120,
                    color: colorScheme.onSurface,
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 主要问候区域
                  Row(
                    children: [
                      // 图标和问候文字区域
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // 问候语 - 更精致的样式
                            AnimationUtils.createFadeIn(
                              duration: const Duration(milliseconds: 500),
                              delay: const Duration(milliseconds: 300),
                              child: Text(
                                greeting,
                                style: theme.textTheme.titleLarge?.copyWith(
                                  color: colorScheme.onSurface.withAlpha(217),
                                  fontWeight: FontWeight.w500,
                                  letterSpacing: 0.5,
                                  height: 1.3,
                                ),
                              ),
                            ),
                            const SizedBox(height: 8),
                            // 欢迎文字 - 更突出的设计
                            AnimationUtils.createSlideIn(
                              duration: const Duration(milliseconds: 500),
                              delay: const Duration(milliseconds: 400),
                              beginOffset: const Offset(20, 0),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      welcomeMessage,
                                      style: theme.textTheme.headlineSmall?.copyWith(
                                        fontWeight: FontWeight.bold,
                                        color: colorScheme.onSurface,
                                        height: 1.2,
                                        letterSpacing: 0.2,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 10),
                                  AnimationUtils.createScaleIn(
                                    duration: const Duration(milliseconds: 300),
                                    child: Icon(
                                      Icons.waving_hand_rounded,
                                      color: colorScheme.primary,
                                      size: 28,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 20),
                      
                      // 日期信息区域 - 与欢迎卡片深度融合设计
                      AnimationUtils.createSlideIn(
                        duration: const Duration(milliseconds: 500),
                        delay: const Duration(milliseconds: 600),
                        beginOffset: const Offset(30, 0),
                        child: Column(
                          children: [
                            // 日期图标 - 简化设计，仅保留图标
                            AnimationUtils.createScaleIn(
                              duration: const Duration(milliseconds: 400),
                              child: Icon(
                                Icons.calendar_today,
                                size: 32,
                                color: Colors.black, // 图标颜色为黑色
                              ),
                            ),
                            const SizedBox(height: 12),
                            // 星期 - 清晰的样式
                            AnimationUtils.createFadeIn(
                              duration: const Duration(milliseconds: 400),
                              delay: const Duration(milliseconds: 700),
                              child: Text(
                                weekday,
                                style: theme.textTheme.labelLarge?.copyWith(
                                  fontWeight: FontWeight.w600,
                                  color: colorScheme.onSurface.withAlpha(217),
                                  letterSpacing: 0.8,
                                ),
                              ),
                            ),
                            const SizedBox(height: 6),
                            // 具体日期 - 醒目的设计
                            AnimationUtils.createFadeIn(
                              duration: const Duration(milliseconds: 400),
                              delay: const Duration(milliseconds: 750),
                              child: Text(
                                dateString,
                                style: theme.textTheme.titleLarge?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: colorScheme.onSurface,
                                  height: 1.1,
                                  letterSpacing: 0.3,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: 20),
                  
                  const SizedBox(height: 12),
                  
                  // 底部装饰 - 改进设计
                  AnimationUtils.createSlideIn(
                    duration: const Duration(milliseconds: 600),
                    delay: const Duration(milliseconds: 800),
                    beginOffset: const Offset(0, 20),
                    child: Container(
                      height: 3,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [
                            colorScheme.primary.withAlpha(204),
                            colorScheme.primary.withAlpha(77),
                            colorScheme.primary.withAlpha(204),
                          ],
                          stops: const [0.1, 0.5, 0.9],
                        ),
                        borderRadius: BorderRadius.circular(2),
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
  
  /// 根据当前时间获取问候语
  String _getGreeting(int hour) {
    if (hour >= 5 && hour < 12) {
      return '早上好';
    } else if (hour >= 12 && hour < 14) {
      return '中午好';
    } else if (hour >= 14 && hour < 18) {
      return '下午好';
    } else if (hour >= 18 && hour < 22) {
      return '晚上好';
    } else {
      return '夜深了';
    }
  }
  
  /// 根据季节获取渐变颜色
  List<Color> _getSeasonalColors(int month, ColorScheme colorScheme) {
    // 根据月份确定季节，调整渐变颜色
    if (month >= 3 && month <= 5) {
      // 春季 - 绿色调
      return [
        colorScheme.primary.withAlpha(31),
        colorScheme.tertiary.withAlpha(26),
        colorScheme.primary.withAlpha(20),
        colorScheme.tertiary.withAlpha(15),
      ];
    } else if (month >= 6 && month <= 8) {
      // 夏季 - 蓝色调
      return [
        colorScheme.primary.withAlpha(31),
        Colors.blue.withAlpha(26),
        colorScheme.primary.withAlpha(20),
        Colors.blue.withAlpha(15),
      ];
    } else if (month >= 9 && month <= 11) {
      // 秋季 - 橙色调
      return [
        colorScheme.primary.withAlpha(31),
        Colors.orange.withAlpha(26),
        colorScheme.primary.withAlpha(20),
        Colors.orange.withAlpha(15),
      ];
    } else {
      // 冬季 - 紫色调
      return [
        colorScheme.primary.withAlpha(31),
        Colors.purple.withAlpha(26),
        colorScheme.primary.withAlpha(20),
        Colors.purple.withAlpha(15),
      ];
    }
  }

  /// 根据时间段生成个性化的欢迎消息
  String _getWelcomeMessage(int hour, String userName) {
    final messages = <String>[];
    
    // 早上时段 (5:00-11:59)
    if (hour >= 5 && hour < 12) {
      messages.addAll([
        '新的一天开始了，$userName！',
        '今天也要元气满满哦！',
        '清晨的阳光真美好！',
        '早餐吃了吗？',
        '今天有什么计划呢？'
      ]);
    }
    // 中午时段 (12:00-13:59)
    else if (hour >= 12 && hour < 14) {
      messages.addAll([
        '午餐时间到！',
        '午休时间，记得休息一下哦！',
        '阳光正好的中午！',
        '午餐吃什么呢？',
        '今天过得怎么样？'
      ]);
    }
    // 下午时段 (14:00-17:59)
    else if (hour >= 14 && hour < 18) {
      messages.addAll([
        '继续加油！',
        '下午时光，工作学习顺利吗？',
        '来杯下午茶吧！',
        '阳光明媚的下午！',
        '今天有什么收获？'
      ]);
    }
    // 晚上时段 (18:00-21:59)
    else if (hour >= 18 && hour < 22) {
      messages.addAll([
        '晚餐时间到！',
        '今天辛苦了！',
        '傍晚时光，放松一下吧！',
        '有什么安排吗？',
        '美丽的夜晚！'
      ]);
    }
    // 深夜时段 (22:00-4:59)
    else {
      messages.addAll([
        '早点休息哦！',
        '注意休息时间！',
        '深夜时光，还在工作吗？',
        '记得照顾好自己！',
        '安静的夜晚！'
      ]);
    }
    
    // 使用消息索引选择消息，实现轮播效果
    return messages[_messageIndex % messages.length];
  }
}