import 'dart:math';
import 'dart:async';
import 'package:intl/intl.dart';
import 'package:flutter/material.dart';

/// 辅助函数工具类
class AppHelpers {
  /// 格式化金额显示
  static String formatAmount(double amount, {String currency = '¥'}) {
    final formatter = NumberFormat.currency(
      symbol: currency,
      decimalDigits: 2,
    );
    return formatter.format(amount);
  }
  
  /// 格式化日期显示
  static String formatDate(DateTime date, {String format = 'yyyy-MM-dd'}) {
    final formatter = DateFormat(format);
    return formatter.format(date);
  }
  
  /// 格式化时间显示
  static String formatTime(DateTime time, {String format = 'HH:mm'}) {
    final formatter = DateFormat(format);
    return formatter.format(time);
  }
  
  /// 格式化日期时间显示
  static String formatDateTime(DateTime dateTime, {String format = 'yyyy-MM-dd HH:mm'}) {
    final formatter = DateFormat(format);
    return formatter.format(dateTime);
  }
  
  /// 获取月份名称
  static String getMonthName(int month) {
    final months = ['一月', '二月', '三月', '四月', '五月', '六月', 
                   '七月', '八月', '九月', '十月', '十一月', '十二月'];
    return months[month - 1];
  }
  
  /// 获取星期名称
  static String getWeekdayName(int weekday) {
    final weekdays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'];
    return weekdays[weekday - 1];
  }
  
  /// 计算两个日期之间的天数差
  static int daysBetween(DateTime from, DateTime to) {
    from = DateTime(from.year, from.month, from.day);
    to = DateTime(to.year, to.month, to.day);
    return (to.difference(from).inHours / 24).round();
  }
  
  /// 获取当前月份的第一天
  static DateTime getFirstDayOfMonth(DateTime date) {
    return DateTime(date.year, date.month, 1);
  }
  
  /// 获取当前月份的最后一天
  static DateTime getLastDayOfMonth(DateTime date) {
    return DateTime(date.year, date.month + 1, 0);
  }
  
  /// 检查是否为今天
  static bool isToday(DateTime date) {
    final now = DateTime.now();
    return date.year == now.year && 
           date.month == now.month && 
           date.day == now.day;
  }
  
  /// 检查是否为昨天
  static bool isYesterday(DateTime date) {
    final yesterday = DateTime.now().subtract(const Duration(days: 1));
    return date.year == yesterday.year && 
           date.month == yesterday.month && 
           date.day == yesterday.day;
  }
  
  /// 检查是否为明天
  static bool isTomorrow(DateTime date) {
    final tomorrow = DateTime.now().add(const Duration(days: 1));
    return date.year == tomorrow.year && 
           date.month == tomorrow.month && 
           date.day == tomorrow.day;
  }
  
  /// 检查是否为同一天
  static bool isSameDay(DateTime date1, DateTime date2) {
    return date1.year == date2.year && 
           date1.month == date2.month && 
           date1.day == date2.day;
  }
  
  /// 获取年龄
  static int getAge(DateTime birthDate) {
    final now = DateTime.now();
    int age = now.year - birthDate.year;
    if (now.month < birthDate.month || 
        (now.month == birthDate.month && now.day < birthDate.day)) {
      age--;
    }
    return age;
  }
  
  /// 验证邮箱格式
  static bool isValidEmail(String email) {
    final emailRegex = RegExp(
      r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$'
    );
    return emailRegex.hasMatch(email);
  }
  
  /// 验证手机号格式
  static bool isValidPhone(String phone) {
    final phoneRegex = RegExp(
      r'^1[3-9]\d{9}$'
    );
    return phoneRegex.hasMatch(phone);
  }
  
  /// 验证身份证号格式
  static bool isValidIdCard(String idCard) {
    final idCardRegex = RegExp(
      r'^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$'
    );
    return idCardRegex.hasMatch(idCard);
  }
  
  /// 隐藏手机号中间四位
  static String hidePhoneNumber(String phone) {
    if (phone.length < 7) return phone;
    return '${phone.substring(0, 3)}****${phone.substring(7)}';
  }
  
  /// 隐藏邮箱中间部分
  static String hideEmail(String email) {
    final parts = email.split('@');
    if (parts.length != 2) return email;
    
    final username = parts[0];
    final domain = parts[1];
    
    if (username.length <= 2) {
      return '${username.substring(0, 1)}***@$domain';
    } else {
      return '${username.substring(0, 2)}***@$domain';
    }
  }
  
  /// 获取文件大小格式化显示
  static String formatFileSize(int bytes) {
    if (bytes <= 0) return '0 B';
    const suffixes = ['B', 'KB', 'MB', 'GB', 'TB'];
    final i = (log(bytes) / log(1024)).floor();
    return '${(bytes / pow(1024, i)).toStringAsFixed(2)} ${suffixes[i]}';
  }
  
  /// 计算颜色的亮度（0-1之间）
  static double getColorBrightness(Color color) {
    // 使用新的颜色属性访问方式
    final red = (color.r * 255.0).round() & 0xff;
    final green = (color.g * 255.0).round() & 0xff;
    final blue = (color.b * 255.0).round() & 0xff;
    return (red * 0.299 + green * 0.587 + blue * 0.114) / 255;
  }
  
  /// 根据背景色确定文字颜色
  static Color getTextColorForBackground(Color backgroundColor) {
    final brightness = getColorBrightness(backgroundColor);
    return brightness > 0.5 ? Colors.black : Colors.white;
  }
  
  /// 生成随机颜色
  static Color getRandomColor() {
    final random = Random();
    return Color.fromARGB(
      255,
      random.nextInt(256),
      random.nextInt(256),
      random.nextInt(256),
    );
  }
  
  /// 生成渐变色
  static LinearGradient getRandomGradient() {
    final colors = [
      [Colors.blue, Colors.purple],
      [Colors.green, Colors.blue],
      [Colors.orange, Colors.red],
      [Colors.pink, Colors.purple],
      [Colors.teal, Colors.blue],
    ];
    final random = Random();
    final gradient = colors[random.nextInt(colors.length)];
    
    return LinearGradient(
      colors: gradient,
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
    );
  }
  
  /// 计算百分比
  static double calculatePercentage(double value, double total) {
    if (total == 0) return 0;
    return (value / total) * 100;
  }
  
  /// 格式化百分比显示
  static String formatPercentage(double percentage, {int decimalDigits = 1}) {
    return '${percentage.toStringAsFixed(decimalDigits)}%';
  }
  
  /// 获取进度条颜色
  static Color getProgressColor(double percentage) {
    if (percentage >= 0.8) {
      return Colors.green;
    } else if (percentage >= 0.6) {
      return Colors.blue;
    } else if (percentage >= 0.4) {
      return Colors.orange;
    } else {
      return Colors.red;
    }
  }
  
  /// 深拷贝列表
  static List<T> deepCopyList<T>(List<T> original) {
    return List<T>.from(original);
  }
  
  /// 深拷贝Map
  static Map<K, V> deepCopyMap<K, V>(Map<K, V> original) {
    return Map<K, V>.from(original);
  }
  
  /// 检查列表是否为空或null
  static bool isListEmptyOrNull(List? list) {
    return list == null || list.isEmpty;
  }
  
  /// 检查Map是否为空或null
  static bool isMapEmptyOrNull(Map? map) {
    return map == null || map.isEmpty;
  }
  
  /// 安全获取列表元素
  static T? safeGetListElement<T>(List<T> list, int index) {
    if (index < 0 || index >= list.length) return null;
    return list[index];
  }
  
  /// 安全获取Map值
  static V? safeGetMapValue<K, V>(Map<K, V> map, K key) {
    return map[key];
  }
  
  /// 延迟执行
  static Future<void> delay(int milliseconds) {
    return Future.delayed(Duration(milliseconds: milliseconds));
  }
  
  /// 防抖函数
  static Function debounce(Function func, int milliseconds) {
    Timer? timer; // ignore: unused_local_variable
    return () {
      timer?.cancel();
      timer = Timer(Duration(milliseconds: milliseconds), () {
        func();
      });
    };
  }
  
  /// 节流函数
  static Function throttle(Function func, int milliseconds) {
    Timer? timer; // ignore: unused_local_variable
    bool isThrottled = false;
    
    return () {
      if (!isThrottled) {
        func();
        isThrottled = true;
        timer = Timer(Duration(milliseconds: milliseconds), () {
          isThrottled = false;
        });
      }
    };
  }
}

/// 数学计算辅助类
class MathHelpers {
  /// 计算平均值
  static double calculateAverage(List<double> numbers) {
    if (numbers.isEmpty) return 0;
    final sum = numbers.reduce((a, b) => a + b);
    return sum / numbers.length;
  }
  
  /// 计算中位数
  static double calculateMedian(List<double> numbers) {
    if (numbers.isEmpty) return 0;
    
    final sorted = List<double>.from(numbers)..sort();
    final middle = sorted.length ~/ 2;
    
    if (sorted.length.isOdd) {
      return sorted[middle];
    } else {
      return (sorted[middle - 1] + sorted[middle]) / 2;
    }
  }
  
  /// 计算标准差
  static double calculateStandardDeviation(List<double> numbers) {
    if (numbers.isEmpty) return 0;
    
    final mean = calculateAverage(numbers);
    final squaredDifferences = numbers.map((x) => pow(x - mean, 2).toDouble()).toList();
    final variance = calculateAverage(squaredDifferences);
    
    return sqrt(variance);
  }
  
  /// 计算增长率
  static double calculateGrowthRate(double current, double previous) {
    if (previous == 0) return 0;
    return ((current - previous) / previous) * 100;
  }
  
  /// 线性插值
  static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }
  
  /// 限制数值范围
  static double clamp(double value, double min, double max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }
  
  /// 映射数值范围
  static double mapValue(double value, double fromMin, double fromMax, double toMin, double toMax) {
    return (value - fromMin) * (toMax - toMin) / (fromMax - fromMin) + toMin;
  }
}

/// 字符串处理辅助类
class StringHelpers {
  /// 检查字符串是否为空或null
  static bool isNullOrEmpty(String? str) {
    return str == null || str.trim().isEmpty;
  }
  
  /// 检查字符串是否为数字
  static bool isNumeric(String str) {
    return double.tryParse(str) != null;
  }
  
  /// 首字母大写
  static String capitalize(String str) {
    if (str.isEmpty) return str;
    return str[0].toUpperCase() + str.substring(1).toLowerCase();
  }
  
  /// 每个单词首字母大写
  static String capitalizeWords(String str) {
    return str.split(' ').map(capitalize).join(' ');
  }
  
  /// 截断字符串并添加省略号
  static String truncateWithEllipsis(String str, int maxLength) {
    if (str.length <= maxLength) return str;
    return '${str.substring(0, maxLength)}...';
  }
  
  /// 移除字符串中的空格
  static String removeSpaces(String str) {
    return str.replaceAll(' ', '');
  }
  
  /// 移除字符串中的特殊字符
  static String removeSpecialCharacters(String str) {
    return str.replaceAll(RegExp(r'[^\w\s]'), '');
  }
  
  /// 获取字符串的拼音首字母
  static String getPinyinInitials(String str) {
    // 简化实现，实际项目中可以使用拼音库
    if (str.isEmpty) return '';
    return str[0].toUpperCase();
  }
  
  /// 模糊匹配字符串
  static bool fuzzyMatch(String source, String target) {
    final sourceLower = source.toLowerCase();
    final targetLower = target.toLowerCase();
    return sourceLower.contains(targetLower);
  }
}