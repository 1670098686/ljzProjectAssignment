import 'package:flutter/material.dart';

import 'responsive_layout.dart';

/// Generic container that automatically adapts padding & margin based on screen size.
class ResponsiveContainer extends StatelessWidget {
  final Widget child;
  final Color? color;
  final double? borderRadius;

  const ResponsiveContainer({
    super.key,
    required this.child,
    this.color,
    this.borderRadius,
  });

  @override
  Widget build(BuildContext context) {
    final padding = ResponsiveLayout.isMobile(context)
        ? const EdgeInsets.all(16)
        : const EdgeInsets.all(24);
    final margin = ResponsiveLayout.isMobile(context)
        ? const EdgeInsets.all(8)
        : const EdgeInsets.all(16);

    return Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: color ?? Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(borderRadius ?? 16),
      ),
      child: child,
    );
  }
}

/// Narrow screen specific container mentioned in the layout plan.
class NarrowContentContainer extends StatelessWidget {
  final Widget child;
  final EdgeInsets? padding;

  const NarrowContentContainer({super.key, required this.child, this.padding});

  @override
  Widget build(BuildContext context) {
    final isNarrow = NarrowScreenLayout.isNarrowScreen(context);
    return Container(
      padding:
          padding ??
          EdgeInsets.symmetric(
            horizontal: isNarrow ? 12 : 16,
            vertical: isNarrow ? 8 : 12,
          ),
      child: child,
    );
  }
}
