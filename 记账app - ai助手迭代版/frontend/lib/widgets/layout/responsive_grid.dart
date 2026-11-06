import 'package:flutter/material.dart';

import 'responsive_layout.dart';

/// Grid/list wrapper that follows the layout plan's responsive rules.
class ResponsiveGrid extends StatelessWidget {
  final List<Widget> children;
  final bool useCompactLayout;

  const ResponsiveGrid({
    super.key,
    required this.children,
    this.useCompactLayout = false,
  });

  @override
  Widget build(BuildContext context) {
    if (ResponsiveLayout.isMobile(context) && useCompactLayout) {
      return ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: children.length,
        separatorBuilder: (context, _) => const SizedBox(height: 8),
        itemBuilder: (context, index) => children[index],
      );
    }

    final crossAxisCount = ResponsiveLayout.isMobile(context)
        ? 2
        : ResponsiveLayout.isTablet(context)
        ? 3
        : 4;

    return GridView.count(
      crossAxisCount: crossAxisCount,
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      childAspectRatio: ResponsiveLayout.isMobile(context) ? 1.5 : 1.2,
      children: children,
    );
  }
}

/// Compact grid dedicated to narrow devices.
class CompactGrid extends StatelessWidget {
  final List<Widget> children;
  final int columns;

  const CompactGrid({super.key, required this.children, this.columns = 2});

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: columns,
      crossAxisSpacing: 8,
      mainAxisSpacing: 8,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      childAspectRatio: 1.8,
      children: children,
    );
  }
}
