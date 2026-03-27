import 'dart:math';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// 字符串扩展方法
extension StringExtensions on String {
  /// 检查字符串是否为空或仅包含空格
  bool get isNullOrEmpty => trim().isEmpty;
  
  /// 检查字符串是否为数字
  bool get isNumeric => double.tryParse(this) != null;
  
  /// 首字母大写
  String get capitalize {
    if (isEmpty) return this;
    return '${this[0].toUpperCase()}${substring(1).toLowerCase()}';
  }
  
  /// 每个单词首字母大写
  String get capitalizeWords {
    return split(' ').map((word) => word.capitalize).join(' ');
  }
  
  /// 截断字符串并添加省略号
  String truncateWithEllipsis(int maxLength) {
    if (length <= maxLength) return this;
    return '${substring(0, maxLength)}...';
  }
  
  /// 移除所有空格
  String get removeSpaces => replaceAll(' ', '');
  
  /// 移除特殊字符
  String get removeSpecialCharacters => replaceAll(RegExp(r'[^\w\s]'), '');
  
  /// 检查是否为有效邮箱
  bool get isValidEmail {
    final emailRegex = RegExp(
      r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$'
    );
    return emailRegex.hasMatch(this);
  }
  
  /// 检查是否为有效手机号
  bool get isValidPhone {
    final phoneRegex = RegExp(
      r'^1[3-9]\d{9}$'
    );
    return phoneRegex.hasMatch(this);
  }
  
  /// 检查是否为有效身份证号
  bool get isValidIdCard {
    final idCardRegex = RegExp(
      r'^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$'
    );
    return idCardRegex.hasMatch(this);
  }
  
  /// 转换为金额格式
  String toAmountFormat({String currency = '¥'}) {
    final amount = double.tryParse(this);
    if (amount == null) return this;
    
    final formatter = NumberFormat.currency(
      symbol: currency,
      decimalDigits: 2,
    );
    return formatter.format(amount);
  }
  
  /// 转换为日期对象
  DateTime? toDateTime() {
    try {
      return DateTime.parse(this);
    } catch (e) {
      return null;
    }
  }
  
  /// 隐藏手机号中间四位
  String get hidePhoneNumber {
    if (length < 7) return this;
    return '${substring(0, 3)}****${substring(7)}';
  }
  
  /// 隐藏邮箱中间部分
  String get hideEmail {
    final parts = split('@');
    if (parts.length != 2) return this;
    
    final username = parts[0];
    final domain = parts[1];
    
    if (username.length <= 2) {
      return '${username.substring(0, 1)}***@$domain';
    } else {
      return '${username.substring(0, 2)}***@$domain';
    }
  }
  
  /// 获取拼音首字母（简化实现）
  String get pinyinInitials {
    if (isEmpty) return '';
    return this[0].toUpperCase();
  }
  
  /// 模糊匹配
  bool fuzzyMatch(String target) {
    final sourceLower = toLowerCase();
    final targetLower = target.toLowerCase();
    return sourceLower.contains(targetLower);
  }
}

/// 数字扩展方法
extension NumberExtensions on num {
  /// 转换为金额格式字符串
  String toAmountFormat({String currency = '¥'}) {
    final formatter = NumberFormat.currency(
      symbol: currency,
      decimalDigits: 2,
    );
    return formatter.format(this);
  }
  
  /// 转换为百分比格式字符串
  String toPercentageFormat({int decimalDigits = 1}) {
    return '${toStringAsFixed(decimalDigits)}%';
  }
  
  /// 限制数值范围
  num clampRange(num min, num max) {
    if (this < min) return min;
    if (this > max) return max;
    return this;
  }
  
  /// 转换为文件大小格式
  String toFileSizeFormat() {
    if (this <= 0) return '0 B';
    const suffixes = ['B', 'KB', 'MB', 'GB', 'TB'];
    final i = (log(toDouble()) / log(1024)).floor();
    return '${(this / pow(1024, i)).toStringAsFixed(2)} ${suffixes[i]}';
  }
  
  /// 检查是否为整数
  bool get isInteger => this is int || this == roundToDouble();
  
  /// 检查是否为正数
  bool get isPositive => this > 0;
  
  /// 检查是否为负数
  bool get isNegative => this < 0;
  
  /// 检查是否为零
  bool get isZero => this == 0;
}

/// 日期时间扩展方法
extension DateTimeExtensions on DateTime {
  /// 格式化日期显示
  String formatDate({String format = 'yyyy-MM-dd'}) {
    final formatter = DateFormat(format);
    return formatter.format(this);
  }
  
  /// 格式化时间显示
  String formatTime({String format = 'HH:mm'}) {
    final formatter = DateFormat(format);
    return formatter.format(this);
  }
  
  /// 格式化日期时间显示
  String formatDateTime({String format = 'yyyy-MM-dd HH:mm'}) {
    final formatter = DateFormat(format);
    return formatter.format(this);
  }
  
  /// 检查是否为今天
  bool get isToday {
    final now = DateTime.now();
    return year == now.year && month == now.month && day == now.day;
  }
  
  /// 检查是否为昨天
  bool get isYesterday {
    final yesterday = DateTime.now().subtract(const Duration(days: 1));
    return year == yesterday.year && 
           month == yesterday.month && 
           day == yesterday.day;
  }
  
  /// 检查是否为明天
  bool get isTomorrow {
    final tomorrow = DateTime.now().add(const Duration(days: 1));
    return year == tomorrow.year && 
           month == tomorrow.month && 
           day == tomorrow.day;
  }
  
  /// 检查是否为同一天
  bool isSameDay(DateTime other) {
    return year == other.year && 
           month == other.month && 
           day == other.day;
  }
  
  /// 获取月份的第一天
  DateTime get firstDayOfMonth {
    return DateTime(year, month, 1);
  }
  
  /// 获取月份的最后一天
  DateTime get lastDayOfMonth {
    return DateTime(year, month + 1, 0);
  }
  
  /// 获取年龄
  int get age {
    final now = DateTime.now();
    int age = now.year - year;
    if (now.month < month || 
        (now.month == month && now.day < day)) {
      age--;
    }
    return age;
  }
  
  /// 计算两个日期之间的天数差
  int daysBetween(DateTime other) {
    final from = DateTime(year, month, day);
    final to = DateTime(other.year, other.month, other.day);
    return (to.difference(from).inHours / 24).round();
  }
  
  /// 添加天数
  DateTime addDays(int days) {
    return add(Duration(days: days));
  }
  
  /// 减去天数
  DateTime subtractDays(int days) {
    return subtract(Duration(days: days));
  }
  
  /// 获取星期名称
  String get weekdayName {
    final weekdays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'];
    return weekdays[weekday - 1];
  }
  
  /// 获取月份名称
  String get monthName {
    final months = ['一月', '二月', '三月', '四月', '五月', '六月', 
                   '七月', '八月', '九月', '十月', '十一月', '十二月'];
    return months[month - 1];
  }
}

/// 列表扩展方法
extension ListExtensions<T> on List<T> {
  /// 检查列表是否为空或null
  bool get isNullOrEmpty => isEmpty;
  
  /// 安全获取元素
  T? safeGet(int index) {
    if (index < 0 || index >= length) return null;
    return this[index];
  }
  
  /// 深拷贝列表
  List<T> deepCopy() {
    return List<T>.from(this);
  }
  
  /// 移除重复元素
  List<T> removeDuplicates() {
    return toSet().toList();
  }
  
  /// 按条件分组
  Map<K, List<T>> groupBy<K>(K Function(T) keyFunction) {
    final map = <K, List<T>>{};
    for (final element in this) {
      final key = keyFunction(element);
      map.putIfAbsent(key, () => []).add(element);
    }
    return map;
  }
  
  /// 分页
  List<T> paginate(int page, int pageSize) {
    final start = (page - 1) * pageSize;
    final end = start + pageSize;
    
    if (start >= length) return [];
    if (end > length) return sublist(start);
    
    return sublist(start, end);
  }
  
  /// 随机排序
  List<T> shuffleList() {
    final list = deepCopy();
    list.shuffle();
    return list;
  }
  
  /// 获取随机元素
  T? get randomElement {
    if (isEmpty) return null;
    return this[Random().nextInt(length)];
  }
  
  /// 检查是否包含所有元素
  bool containsAll(List<T> elements) {
    for (final element in elements) {
      if (!contains(element)) return false;
    }
    return true;
  }
  
  /// 检查是否包含任一元素
  bool containsAny(List<T> elements) {
    for (final element in elements) {
      if (contains(element)) return true;
    }
    return false;
  }
}

/// Map扩展方法
extension MapExtensions<K, V> on Map<K, V> {
  /// 检查Map是否为空或null
  bool get isNullOrEmpty => isEmpty;
  
  /// 深拷贝Map
  Map<K, V> deepCopy() {
    return Map<K, V>.from(this);
  }
  
  /// 安全获取值
  V? safeGet(K key) {
    return this[key];
  }
  
  /// 按值过滤
  Map<K, V> filterByValue(bool Function(V) predicate) {
    return Map<K, V>.fromEntries(
      entries.where((entry) => predicate(entry.value))
    );
  }
  
  /// 按键过滤
  Map<K, V> filterByKey(bool Function(K) predicate) {
    return Map<K, V>.fromEntries(
      entries.where((entry) => predicate(entry.key))
    );
  }
  
  /// 转换值类型
  Map<K, U> mapValues<U>(U Function(V) transform) {
    return Map<K, U>.fromEntries(
      entries.map((entry) => MapEntry(entry.key, transform(entry.value)))
    );
  }
  
  /// 转换键类型
  Map<U, V> mapKeys<U>(U Function(K) transform) {
    return Map<U, V>.fromEntries(
      entries.map((entry) => MapEntry(transform(entry.key), entry.value))
    );
  }
}

/// Widget扩展方法
extension WidgetExtensions on Widget {
  /// 添加内边距
  Widget paddingAll(double value) {
    return Padding(
      padding: EdgeInsets.all(value),
      child: this,
    );
  }
  
  /// 添加对称内边距
  Widget paddingSymmetric({double horizontal = 0, double vertical = 0}) {
    return Padding(
      padding: EdgeInsets.symmetric(
        horizontal: horizontal,
        vertical: vertical,
      ),
      child: this,
    );
  }
  
  /// 添加指定方向内边距
  Widget paddingOnly({
    double left = 0,
    double top = 0,
    double right = 0,
    double bottom = 0,
  }) {
    return Padding(
      padding: EdgeInsets.only(
        left: left,
        top: top,
        right: right,
        bottom: bottom,
      ),
      child: this,
    );
  }
  
  /// 添加外边距
  Widget marginAll(double value) {
    return Container(
      margin: EdgeInsets.all(value),
      child: this,
    );
  }
  
  /// 添加对称外边距
  Widget marginSymmetric({double horizontal = 0, double vertical = 0}) {
    return Container(
      margin: EdgeInsets.symmetric(
        horizontal: horizontal,
        vertical: vertical,
      ),
      child: this,
    );
  }
  
  /// 添加指定方向外边距
  Widget marginOnly({
    double left = 0,
    double top = 0,
    double right = 0,
    double bottom = 0,
  }) {
    return Container(
      margin: EdgeInsets.only(
        left: left,
        top: top,
        right: right,
        bottom: bottom,
      ),
      child: this,
    );
  }
  
  /// 添加圆角
  Widget roundedCorners(double radius) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: this,
    );
  }
  
  /// 添加边框
  Widget withBorder({
    Color color = Colors.black,
    double width = 1,
    BorderRadius? borderRadius,
  }) {
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: color, width: width),
        borderRadius: borderRadius,
      ),
      child: this,
    );
  }
  
  /// 添加背景色
  Widget withBackgroundColor(Color color) {
    return Container(
      color: color,
      child: this,
    );
  }
  
  /// 添加阴影
  Widget withShadow({
    Color color = Colors.black,
    double blurRadius = 4,
    double spreadRadius = 0,
    Offset offset = Offset.zero,
  }) {
    return Container(
      decoration: BoxDecoration(
        boxShadow: [
          BoxShadow(
            color: color,
            blurRadius: blurRadius,
            spreadRadius: spreadRadius,
            offset: offset,
          ),
        ],
      ),
      child: this,
    );
  }
  
  /// 添加点击事件
  Widget onTap(GestureTapCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: this,
    );
  }
  
  /// 添加长按事件
  Widget onLongPress(GestureLongPressCallback onLongPress) {
    return GestureDetector(
      onLongPress: onLongPress,
      child: this,
    );
  }
  
  /// 设置宽度
  Widget withWidth(double width) {
    return SizedBox(
      width: width,
      child: this,
    );
  }
  
  /// 设置高度
  Widget withHeight(double height) {
    return SizedBox(
      height: height,
      child: this,
    );
  }
  
  /// 设置尺寸
  Widget withSize(double width, double height) {
    return SizedBox(
      width: width,
      height: height,
      child: this,
    );
  }
  
  /// 设置透明度
  Widget withOpacity(double opacity) {
    return Opacity(
      opacity: opacity,
      child: this,
    );
  }
  
  /// 设置旋转
  Widget withRotation(double angle) {
    return Transform.rotate(
      angle: angle,
      child: this,
    );
  }
  
  /// 设置缩放
  Widget withScale(double scale) {
    return Transform.scale(
      scale: scale,
      child: this,
    );
  }
  
  /// 设置平移
  Widget withTranslate(double dx, double dy) {
    return Transform.translate(
      offset: Offset(dx, dy),
      child: this,
    );
  }
}

/// Color扩展方法
extension ColorExtensions on Color {
  /// 设置颜色的透明度值（alpha值范围0-1）
  /// 对应 Flutter Material 3 的 withValues 方法
  Color withValues({
    double? alpha,
    double? red,
    double? green,
    double? blue,
  }) {
    final currentRed = (red ?? (r / 255.0)).clamp(0.0, 1.0);
    final currentGreen = (green ?? (g / 255.0)).clamp(0.0, 1.0);
    final currentBlue = (blue ?? (b / 255.0)).clamp(0.0, 1.0);
    final currentAlpha = (alpha ?? (a / 255.0)).clamp(0.0, 1.0);
    
    return Color.fromRGBO(
      (currentRed * 255).round(),
      (currentGreen * 255).round(),
      (currentBlue * 255).round(),
      currentAlpha,
    );
  }
}

/// BuildContext扩展方法
extension BuildContextExtensions on BuildContext {
  /// 获取屏幕尺寸
  Size get screenSize => MediaQuery.of(this).size;
  
  /// 获取屏幕宽度
  double get screenWidth => screenSize.width;
  
  /// 获取屏幕高度
  double get screenHeight => screenSize.height;
  
  /// 检查是否为手机屏幕
  bool get isMobile => screenWidth < 600;
  
  /// 检查是否为平板屏幕
  bool get isTablet => screenWidth >= 600 && screenWidth < 1200;
  
  /// 检查是否为桌面屏幕
  bool get isDesktop => screenWidth >= 1200;
  
  /// 获取主题数据
  ThemeData get theme => Theme.of(this);
  
  /// 获取文本主题
  TextTheme get textTheme => theme.textTheme;
  
  /// 获取颜色方案
  ColorScheme get colorScheme => theme.colorScheme;
  
  /// 导航到新页面
  void push(Widget page) {
    Navigator.of(this).push(
      MaterialPageRoute(builder: (_) => page),
    );
  }
  
  /// 替换当前页面
  void pushReplacement(Widget page) {
    Navigator.of(this).pushReplacement(
      MaterialPageRoute(builder: (_) => page),
    );
  }
  
  /// 返回上一页
  void pop([dynamic result]) {
    Navigator.of(this).pop(result);
  }
  
  /// 显示SnackBar
  void showSnackBar(String message, {
    Color? backgroundColor,
    Duration duration = const Duration(seconds: 4),
  }) {
    ScaffoldMessenger.of(this).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: backgroundColor,
        duration: duration,
      ),
    );
  }
  
  /// 显示对话框
  Future<T?> showCustomDialog<T>({
    required WidgetBuilder builder,
    bool barrierDismissible = true,
  }) {
    return showDialog<T>(
      context: this,
      builder: builder,
      barrierDismissible: barrierDismissible,
    );
  }
  
  /// 显示底部弹窗
  Future<T?> showCustomModalBottomSheet<T>({
    required WidgetBuilder builder,
    bool isScrollControlled = false,
    Color? backgroundColor,
    double? elevation,
    ShapeBorder? shape,
  }) {
    return showModalBottomSheet<T>(
      context: this,
      builder: builder,
      isScrollControlled: isScrollControlled,
      backgroundColor: backgroundColor,
      elevation: elevation,
      shape: shape,
    );
  }
}