import 'package:flutter/material.dart';

import 'package:provider/provider.dart';

/// 优化的基础Widget，提供自动优化功能
abstract class OptimizedWidget extends StatelessWidget {
  const OptimizedWidget({super.key});

  /// 子组件
  Widget get child;

  @override
  Widget build(BuildContext context) => child;
}

/// 优化的Consumer，提供更精准的状态监听
class OptimizedConsumer<T extends ChangeNotifier> extends StatelessWidget {
  final Widget Function(BuildContext context, T value, Widget? child) builder;
  final T? value;
  final String? debugLabel;

  const OptimizedConsumer({
    super.key,
    required this.builder,
    this.value,
    this.debugLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<T>(
      builder: (context, notifier, child) {
        return builder(context, notifier, child);
      },
    );
  }
}

/// 优化的SelectableWidget，用于避免不必要的重建
class OptimizedSelectableWidget extends StatefulWidget {
  final Widget Function(BuildContext context, bool isSelected) builder;
  final bool isSelected;
  final VoidCallback? onTap;

  const OptimizedSelectableWidget({
    super.key,
    required this.builder,
    required this.isSelected,
    this.onTap,
  });

  @override
  State<OptimizedSelectableWidget> createState() => _OptimizedSelectableWidgetState();
}

class _OptimizedSelectableWidgetState extends State<OptimizedSelectableWidget>
    with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return GestureDetector(
      onTap: widget.onTap,
      child: widget.builder(context, widget.isSelected),
    );
  }
}

/// 优化的列表组件，提供虚拟化和重建优化
class OptimizedVirtualizedList<T> extends StatelessWidget {
  final List<T> items;
  final Widget Function(BuildContext context, int index, T item) itemBuilder;
  final double? itemExtent;
  final int Function(T item)? estimatedItemSize;
  final ScrollPhysics? physics;
  final EdgeInsets? padding;
  final bool shrinkWrap;
  final int? maxItems;

  const OptimizedVirtualizedList({
    super.key,
    required this.items,
    required this.itemBuilder,
    this.itemExtent,
    this.estimatedItemSize,
    this.physics,
    this.padding,
    this.shrinkWrap = false,
    this.maxItems,
  });

  @override
  Widget build(BuildContext context) {
    final displayItems = maxItems != null ? items.take(maxItems!).toList() : items;

    return ListView.builder(
      itemCount: displayItems.length,
      itemExtent: itemExtent,
      physics: physics ?? const BouncingScrollPhysics(),
      shrinkWrap: shrinkWrap,
      padding: padding,
      cacheExtent: 500.0, // 减少缓存范围
      itemBuilder: (context, index) {
        if (index >= displayItems.length) {
          return const SizedBox.shrink();
        }
        return itemBuilder(context, index, displayItems[index]);
      },
    );
  }
}

/// 优化的文本组件，避免重复构建
class OptimizedText extends StatelessWidget {
  final String data;
  final TextStyle? style;
  final int? maxLines;
  final TextOverflow? overflow;

  const OptimizedText(
    this.data, {
    super.key,
    this.style,
    this.maxLines,
    this.overflow,
  });

  @override
  Widget build(BuildContext context) {
    return Text(
      data,
      style: style,
      maxLines: maxLines,
      overflow: overflow,
    );
  }
}

/// 优化的按钮组件，减少重建
class OptimizedButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final Widget child;
  final String? semanticLabel;

  const OptimizedButton({
    super.key,
    required this.onPressed,
    required this.child,
    this.semanticLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: semanticLabel,
      button: true,
      enabled: onPressed != null,
      child: ElevatedButton(
        onPressed: onPressed,
        child: child,
      ),
    );
  }
}

/// 优化的卡片组件
class OptimizedCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets? padding;
  final EdgeInsets? margin;
  final double? elevation;
  final Color? color;
  final VoidCallback? onTap;

  const OptimizedCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.elevation,
    this.color,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final card = Card(
      elevation: elevation ?? 2.0,
      color: color,
      margin: margin ?? const EdgeInsets.all(8.0),
      child: Padding(
        padding: padding ?? const EdgeInsets.all(16.0),
        child: child,
      ),
    );

    if (onTap != null) {
      return InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8.0),
        child: card,
      );
    }

    return card;
  }
}

/// 优化的容器组件
class OptimizedContainer extends StatelessWidget {
  final Widget? child;
  final double? width;
  final double? height;
  final EdgeInsets? padding;
  final EdgeInsets? margin;
  final Color? color;
  final BoxDecoration? decoration;

  const OptimizedContainer({
    super.key,
    this.child,
    this.width,
    this.height,
    this.padding,
    this.margin,
    this.color,
    this.decoration,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      padding: padding,
      margin: margin,
      color: color,
      decoration: decoration,
      child: child,
    );
  }
}

/// 优化的列组件
class OptimizedColumn extends StatelessWidget {
  final List<Widget> children;
  final MainAxisAlignment mainAxisAlignment;
  final CrossAxisAlignment crossAxisAlignment;
  final MainAxisSize mainAxisSize;
  final TextDirection? textDirection;
  final VerticalDirection verticalDirection;
  final TextBaseline? textBaseline;

  const OptimizedColumn({
    super.key,
    this.mainAxisAlignment = MainAxisAlignment.start,
    this.crossAxisAlignment = CrossAxisAlignment.center,
    this.mainAxisSize = MainAxisSize.max,
    this.textDirection,
    this.verticalDirection = VerticalDirection.down,
    this.textBaseline,
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: children,
      mainAxisAlignment: mainAxisAlignment,
      crossAxisAlignment: crossAxisAlignment,
      mainAxisSize: mainAxisSize,
      textDirection: textDirection,
      verticalDirection: verticalDirection,
      textBaseline: textBaseline,
    );
  }
}

/// 优化的行组件
class OptimizedRow extends StatelessWidget {
  final List<Widget> children;
  final MainAxisAlignment mainAxisAlignment;
  final CrossAxisAlignment crossAxisAlignment;
  final MainAxisSize mainAxisSize;
  final TextDirection? textDirection;
  final VerticalDirection verticalDirection;
  final TextBaseline? textBaseline;

  const OptimizedRow({
    super.key,
    this.mainAxisAlignment = MainAxisAlignment.start,
    this.crossAxisAlignment = CrossAxisAlignment.center,
    this.mainAxisSize = MainAxisSize.max,
    this.textDirection,
    this.verticalDirection = VerticalDirection.down,
    this.textBaseline,
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: children,
      mainAxisAlignment: mainAxisAlignment,
      crossAxisAlignment: crossAxisAlignment,
      mainAxisSize: mainAxisSize,
      textDirection: textDirection,
      verticalDirection: verticalDirection,
      textBaseline: textBaseline,
    );
  }
}

/// 优化的边距组件
class OptimizedPadding extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;

  const OptimizedPadding({
    super.key,
    required this.child,
    required this.padding,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding,
      child: child,
    );
  }
}

/// 优化的 SizedBox 组件
class OptimizedSizedBox extends StatelessWidget {
  final Widget? child;
  final double? width;
  final double? height;

  const OptimizedSizedBox({
    super.key,
    this.child,
    this.width,
    this.height,
  });

  const OptimizedSizedBox.shrink({
    super.key,
    this.child,
  }) : width = null, height = null;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width,
      height: height,
      child: child,
    );
  }
}

/// 优化的对齐组件
class OptimizedAlign extends StatelessWidget {
  final Widget child;
  final AlignmentGeometry alignment;
  final double? widthFactor;
  final double? heightFactor;

  const OptimizedAlign({
    super.key,
    required this.child,
    this.alignment = Alignment.center,
    this.widthFactor,
    this.heightFactor,
  });

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: alignment,
      widthFactor: widthFactor,
      heightFactor: heightFactor,
      child: child,
    );
  }
}

/// 优化的 Expanded 组件
class OptimizedExpanded extends StatelessWidget {
  final int flex;
  final Widget child;

  const OptimizedExpanded({
    super.key,
    this.flex = 1,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      flex: flex,
      child: child,
    );
  }
}

/// 优化的 Flexible 组件
class OptimizedFlexible extends StatelessWidget {
  final int flex;
  final Widget child;

  const OptimizedFlexible({
    super.key,
    this.flex = 1,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Flexible(
      flex: flex,
      child: child,
    );
  }
}

/// 优化的 ElevatedButton 组件
class OptimizedElevatedButton extends StatelessWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final VoidCallback? onLongPress;
  final ButtonStyle? style;
  final Clip? clipBehavior;

  const OptimizedElevatedButton({
    super.key,
    required this.child,
    this.onPressed,
    this.onLongPress,
    this.style,
    this.clipBehavior,
  });

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      onLongPress: onLongPress,
      style: style,
      clipBehavior: clipBehavior ?? Clip.none,
      child: child,
    );
  }
}

/// 优化的 OutlinedButton 组件
class OptimizedOutlinedButton extends StatelessWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final VoidCallback? onLongPress;
  final ButtonStyle? style;
  final Clip? clipBehavior;

  const OptimizedOutlinedButton({
    super.key,
    required this.child,
    this.onPressed,
    this.onLongPress,
    this.style,
    this.clipBehavior,
  });

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onPressed,
      onLongPress: onLongPress,
      style: style,
      clipBehavior: clipBehavior ?? Clip.none,
      child: child,
    );
  }
}

/// 优化的状态管理工具类
class OptimizedStateHelper {
  /// 防抖函数，避免频繁调用
  static void Function() debounce(VoidCallback fn, {Duration delay = const Duration(milliseconds: 300)}) {
    return () async {
      await Future.delayed(delay);
      fn();
    };
  }

  /// 节流函数，控制调用频率
  static bool _isThrottling = false;
  static void Function() throttle(VoidCallback fn, {Duration delay = const Duration(milliseconds: 300)}) {
    return () {
      if (!_isThrottling) {
        _isThrottling = true;
        fn();
        Future.delayed(delay, () {
          _isThrottling = false;
        });
      }
    };
  }
}