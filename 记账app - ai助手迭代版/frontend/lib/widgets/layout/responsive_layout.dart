import 'package:flutter/material.dart';

/// Breakpoint-aware helpers used across layouts to keep spacing consistent.
class ResponsiveLayout {
  static const double _mobileMaxWidth = 600;
  static const double _tabletMaxWidth = 1024;

  const ResponsiveLayout._();

  static bool isMobile(BuildContext context) =>
      MediaQuery.of(context).size.width <= _mobileMaxWidth;

  static bool isTablet(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    return width > _mobileMaxWidth && width <= _tabletMaxWidth;
  }

  static bool isDesktop(BuildContext context) =>
      MediaQuery.of(context).size.width > _tabletMaxWidth;

  /// Returns a responsive horizontal padding.
  static EdgeInsets horizontalPadding(
    BuildContext context, {
    double mobile = 16,
    double tablet = 24,
    double desktop = 32,
  }) {
    if (isDesktop(context)) {
      return EdgeInsets.symmetric(horizontal: desktop);
    }
    if (isTablet(context)) {
      return EdgeInsets.symmetric(horizontal: tablet);
    }
    return EdgeInsets.symmetric(horizontal: mobile);
  }

  /// Returns a responsive vertical padding.
  static EdgeInsets verticalPadding(
    BuildContext context, {
    double mobile = 12,
    double tablet = 16,
    double desktop = 20,
  }) {
    if (isDesktop(context)) {
      return EdgeInsets.symmetric(vertical: desktop);
    }
    if (isTablet(context)) {
      return EdgeInsets.symmetric(vertical: tablet);
    }
    return EdgeInsets.symmetric(vertical: mobile);
  }

  /// Helper to clamp the max width for large screens.
  static double constrainedWidth(
    BuildContext context, {
    double maxWidth = 1200,
  }) {
    return MediaQuery.of(context).size.width.clamp(0, maxWidth).toDouble();
  }
}

/// Helpers specifically tuned for very narrow Android phones documented in the layout plan.
class NarrowScreenLayout {
  static const double narrowScreenThreshold = 360;

  const NarrowScreenLayout._();

  static bool isNarrowScreen(BuildContext context) =>
      MediaQuery.of(context).size.width <= narrowScreenThreshold;

  static double fontSize(double baseSize, BuildContext context) {
    return isNarrowScreen(context) ? baseSize * 0.9 : baseSize;
  }

  static double spacing(double baseSpacing, BuildContext context) {
    return isNarrowScreen(context) ? baseSpacing * 0.8 : baseSpacing;
  }
}
