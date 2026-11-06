import 'package:flutter/material.dart';

import '../../shared/utils/constants.dart';
import '../layout/responsive_layout.dart';

class NarrowNavItem {
  final IconData icon;
  final String label;

  const NarrowNavItem({required this.icon, required this.label});
}

/// Custom bottom navigation bar optimised for very narrow Android phones.
class NarrowBottomNavigationBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;
  final List<NarrowNavItem> items;

  const NarrowBottomNavigationBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
    this.items = const [
      NarrowNavItem(icon: Icons.bar_chart, label: '统计'),
      NarrowNavItem(icon: Icons.list, label: '明细'),
      NarrowNavItem(icon: Icons.home, label: '首页'),
      NarrowNavItem(icon: Icons.schedule, label: '计划'),
      NarrowNavItem(icon: Icons.person, label: '我的'),
    ],
  });

  @override
  Widget build(BuildContext context) {
    final isNarrow = NarrowScreenLayout.isNarrowScreen(context);
    final borderColor =
        Color.lerp(AppColors.border, Colors.transparent, 0.3) ??
        AppColors.border;

    return Container(
      height: isNarrow ? 56 : 60,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(top: BorderSide(color: borderColor)),
      ),
      child: Row(
        children: [
          for (var i = 0; i < items.length; i++)
            _NarrowNavItemTile(
              item: items[i],
              index: i,
              isSelected: currentIndex == i,
              onTap: onTap,
              isNarrow: isNarrow,
            ),
        ],
      ),
    );
  }
}

class _NarrowNavItemTile extends StatelessWidget {
  final NarrowNavItem item;
  final int index;
  final bool isSelected;
  final ValueChanged<int> onTap;
  final bool isNarrow;

  const _NarrowNavItemTile({
    required this.item,
    required this.index,
    required this.isSelected,
    required this.onTap,
    required this.isNarrow,
  });

  @override
  Widget build(BuildContext context) {
    final iconSize = isNarrow ? 20.0 : 24.0;
    final fontSize = isNarrow ? 10.0 : 12.0;

    return Expanded(
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => onTap(index),
          child: Padding(
            padding: EdgeInsets.symmetric(vertical: isNarrow ? 4 : 6),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  item.icon,
                  size: iconSize,
                  color: isSelected
                      ? AppColors.primary
                      : AppColors.onSurfaceVariant,
                ),
                const SizedBox(height: 2),
                Text(
                  item.label,
                  style: TextStyle(
                    fontSize: fontSize,
                    color: isSelected
                        ? AppColors.primary
                        : AppColors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
