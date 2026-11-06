import 'package:flutter/material.dart';

/// 图标映射工具类 - 解决运行时动态创建IconData导致的tree shaking问题
class IconMapper {
  /// 图标名称到IconData的映射表
  static const Map<String, IconData> _iconMap = {
    'category': Icons.category,
    'restaurant': Icons.restaurant,
    'directions_car': Icons.directions_car,
    'shopping_cart': Icons.shopping_cart,
    'movie': Icons.movie,
    'other_houses': Icons.other_houses,
    'home': Icons.home,
    'work': Icons.work,
    'flight': Icons.flight,
    'local_grocery_store': Icons.local_grocery_store,
    'local_dining': Icons.local_dining,
    'commute': Icons.commute,
    'subscriptions': Icons.subscriptions,
    'gamepad': Icons.gamepad,
    'medical_services': Icons.medical_services,
    'school': Icons.school,
    'fitness_center': Icons.fitness_center,
    'music_note': Icons.music_note,
    'brush': Icons.brush,
    'book': Icons.book,
    'phone': Icons.phone,
    'computer': Icons.computer,
    'car_repair': Icons.car_repair,
    'handyman': Icons.handyman,
  };

  /// 根据图标名称获取IconData
  static IconData getIconData(String iconName) {
    return _iconMap[iconName] ?? Icons.category;
  }

  /// 获取所有支持的图标名称列表
  static List<String> getAllIconNames() {
    return _iconMap.keys.toList();
  }

  /// 检查图标名称是否有效
  static bool isValidIconName(String iconName) {
    return _iconMap.containsKey(iconName);
  }

  /// 根据IconData获取图标名称（反向查找）
  static String getIconName(IconData iconData) {
    for (final entry in _iconMap.entries) {
      if (entry.value.codePoint == iconData.codePoint && 
          entry.value.fontFamily == iconData.fontFamily) {
        return entry.key;
      }
    }
    return 'category';
  }

  /// 根据图标名称获取Icon组件（便捷方法）
  static Icon getIcon(String iconName, {Color? color, double? size}) {
    return Icon(
      getIconData(iconName),
      color: color,
      size: size,
    );
  }
}