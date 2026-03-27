import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

/// Widget构建优化工具类
/// 用于修复常见的Widget构建警告和性能问题
class WidgetBuildOptimizer {
  /// 自动优化Widget的key属性
  static Key? optimizeKey(Key? originalKey, String widgetName) {
    if (originalKey != null) return originalKey;
    
    // 为没有key的Widget分配唯一key
    return ValueKey('$widgetName-${DateTime.now().millisecondsSinceEpoch}');
  }

  /// 检查是否应该使用const构造函数
  static bool shouldUseConst(List<Object?> widgetProperties) {
    // 简单的启发式规则：所有属性都是常量或者可以const
    return widgetProperties.every((property) => 
      property == null || 
      property is String || 
      property is num || 
      property is bool ||
      (property is Widget && property.key != null)
    );
  }

  /// 优化Widget树，避免不必要的重建
  static Widget optimizeWidgetTree(Widget child, {bool addRepaintBoundary = false}) {
    Widget result = child;
    
    if (addRepaintBoundary) {
      result = RepaintBoundary(child: result);
    }
    
    return result;
  }

  /// 创建优化的Consumer组件
  static Widget optimizedConsumer<T extends ChangeNotifier>({
    required Widget Function(BuildContext context, T value, Widget? child) builder,
    Widget? child,
    Key? key,
  }) {
    return Consumer<T>(
      key: key,
      builder: builder,
      child: child,
    );
  }

  /// 优化的监听器组件
  static Widget optimizedListener<T extends ChangeNotifier>({
    required VoidCallback? listener,
    Widget? child,
    Key? key,
    List<Object?>? keys,
  }) {
    return Listener<T>(
      key: key,
      listener: listener,
      child: child,
    );
  }
}

/// 优化构建状态混入
mixin BuildOptimizationMixin<T extends StatefulWidget> on State<T> {
  /// 缓存构建结果，避免重复构建
  Widget? _cachedWidget;
  List<Object?>? _cachedDependencies;
  
  /// 检查依赖是否改变
  bool _hasDependenciesChanged(List<Object?> newDependencies) {
    if (_cachedDependencies == null || _cachedDependencies!.length != newDependencies.length) {
      return true;
    }
    
    for (int i = 0; i < newDependencies.length; i++) {
      if (!equals(_cachedDependencies![i], newDependencies[i])) {
        return true;
      }
    }
    
    return false;
  }
  
  /// 优化的构建方法
  Widget optimizedBuild(BuildContext context, Widget Function() builder, {List<Object?>? dependencies}) {
    if (dependencies != null && _hasDependenciesChanged(dependencies)) {
      _cachedDependencies = List.from(dependencies);
      _cachedWidget = builder();
    }
    
    return _cachedWidget ?? builder();
  }
}

/// 优化的组件基类
abstract class OptimizedStatefulWidget extends StatefulWidget {
  const OptimizedStatefulWidget({super.key});

  @override
  State<OptimizedStatefulWidget> createState();
}

abstract class OptimizedState<T extends OptimizedStatefulWidget> extends State<T> 
    with BuildOptimizationMixin<T>, AutomaticKeepAliveClientMixin<T> {
  
  @override
  bool get wantKeepAlive => true;
  
  /// 优化的setState方法
  void optimizedSetState(VoidCallback fn, {List<Object?>? dependencies}) {
    setState(() {
      fn();
      // 清除缓存以强制重新构建
      _cachedWidget = null;
    });
  }
  
  @override
  Widget build(BuildContext context) {
    super.build(context);
    return buildOptimized(context);
  }
  
  /// 子类必须实现的构建方法
  Widget buildOptimized(BuildContext context);
}

/// 性能监控的Widget
class PerformanceMonitorWidget extends StatefulWidget {
  const PerformanceMonitorWidget({
    super.key,
    required this.child,
    this.enableLogging = false,
  });

  final Widget child;
  final bool enableLogging;

  @override
  State<PerformanceMonitorWidget> createState() => _PerformanceMonitorWidgetState();
}

class _PerformanceMonitorWidgetState extends State<PerformanceMonitorWidget> {
  int _buildCount = 0;
  int _lastBuildTime = 0;
  
  @override
  Widget build(BuildContext context) {
    final buildStartTime = DateTime.now().millisecondsSinceEpoch;
    
    // 如果启用日志记录
    if (widget.enableLogging) {
      print('Widget build #${++_buildCount} at ${DateTime.now()}');
    }
    
    return widget.child;
  }
}

/// 优化的列表项组件
class OptimizedListItem<T> extends StatelessWidget {
  const OptimizedListItem({
    super.key,
    required this.item,
    required this.builder,
    this.onTap,
    this.index,
  });

  final T item;
  final Widget Function(BuildContext context, T item, int? index) builder;
  final VoidCallback? onTap;
  final int? index;

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: Material(
        child: InkWell(
          onTap: onTap,
          child: builder(context, item, index),
        ),
      ),
    );
  }
}

/// 缓存的Widget工厂
class CachedWidgetFactory {
  static final Map<String, Widget> _widgetCache = {};
  static final Map<String, List<Object?>> _dependencyCache = {};
  
  /// 获取缓存的Widget
  static Widget getCachedWidget<T>({
    required String cacheKey,
    required Widget Function() builder,
    required List<Object?> dependencies,
    Key? key,
  }) {
    // 检查依赖是否改变
    final cachedDependencies = _dependencyCache[cacheKey];
    final cachedWidget = _widgetCache[cacheKey];
    
    if (cachedWidget != null && cachedDependencies != null) {
      bool dependenciesChanged = false;
      
      if (cachedDependencies.length != dependencies.length) {
        dependenciesChanged = true;
      } else {
        for (int i = 0; i < dependencies.length; i++) {
          if (!equals(cachedDependencies[i], dependencies[i])) {
            dependenciesChanged = true;
            break;
          }
        }
      }
      
      if (!dependenciesChanged) {
        return cachedWidget;
      }
    }
    
    // 创建新的Widget并缓存
    final newWidget = builder();
    _widgetCache[cacheKey] = newWidget;
    _dependencyCache[cacheKey] = List.from(dependencies);
    
    return newWidget;
  }
  
  /// 清除缓存
  static void clearCache() {
    _widgetCache.clear();
    _dependencyCache.clear();
  }
  
  /// 获取缓存统计信息
  static Map<String, dynamic> getCacheStats() {
    return {
      'widgetCacheSize': _widgetCache.length,
      'dependencyCacheSize': _dependencyCache.length,
    };
  }
}

/// 优化的构建包装器
class OptimizedBuildWrapper extends StatelessWidget {
  const OptimizedBuildWrapper({
    super.key,
    required this.builder,
    this.dependencies,
    this.addRepaintBoundary = false,
    this.cacheKey,
  });

  final Widget Function() builder;
  final List<Object?>? dependencies;
  final bool addRepaintBoundary;
  final String? cacheKey;

  @override
  Widget build(BuildContext context) {
    Widget child;
    
    if (cacheKey != null && dependencies != null) {
      child = CachedWidgetFactory.getCachedWidget(
        cacheKey: cacheKey!,
        builder: builder,
        dependencies: dependencies!,
      );
    } else {
      child = builder();
    }
    
    if (addRepaintBoundary) {
      child = RepaintBoundary(child: child);
    }
    
    return child;
  }
}