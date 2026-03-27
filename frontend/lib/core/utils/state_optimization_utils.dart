import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:provider/provider.dart';

/// 状态管理优化工具类
/// 提供多种优化策略来减少UI重建和提升性能
class StateOptimizationUtils {
  /// 创建一个优化的ValueListenable包装器
  static ValueListenable<T> createOptimizedValueListenable<T>(
    T initialValue,
    Listenable source,
    T Function(T) transform,
  ) {
    return _OptimizedValueListenable<T>(initialValue, source, transform);
  }

  /// 创建防抖函数
  static void Function() createDebouncedFunction(
    VoidCallback function,
    Duration delay,
  ) {
    Timer? _timer;
    return () {
      _timer?.cancel();
      _timer = Timer(delay, function);
    };
  }

  /// 创建节流函数
  static void Function() createThrottledFunction(
    VoidCallback function,
    Duration interval,
  ) {
    bool _canCall = true;
    return () {
      if (_canCall) {
        _canCall = false;
        function();
        Timer(interval, () => _canCall = true);
      }
    };
  }

  /// 检查是否需要重建Widget
  static bool shouldRebuild<T>(
    T oldValue,
    T newValue,
  ) {
    return !identical(oldValue, newValue) && oldValue != newValue;
  }

  /// 创建优化的Provider监听器
  static Widget createOptimizedConsumer<T extends ChangeNotifier>({
    required Widget child,
    required Widget Function(BuildContext context, T value) builder,
    String? debugLabel,
  }) {
    return _OptimizedConsumer<T>(
      child: child,
      builder: builder,
      debugLabel: debugLabel,
    );
  }
}

/// 优化的ValueListenable实现
class _OptimizedValueListenable<T> extends ValueListenable<T> {
  T _value;
  final Listenable _source;
  final T Function(T) _transform;
  final Set<VoidCallback> _listeners = {};

  _OptimizedValueListenable(this._value, this._source, this._transform) {
    _source.addListener(_onSourceChanged);
  }

  void _onSourceChanged() {
    final newValue = _transform(_value);
    if (newValue != _value) {
      _value = newValue;
      _notifyListeners();
    }
  }

  void _notifyListeners() {
    for (final listener in _listeners) {
      listener();
    }
  }

  @override
  T get value => _value;

  @override
  void addListener(VoidCallback listener) {
    _listeners.add(listener);
  }

  @override
  void removeListener(VoidCallback listener) {
    _listeners.remove(listener);
  }

  @override
  void dispose() {
    _source.removeListener(_onSourceChanged);
    _listeners.clear();
  }
}

/// 优化的Consumer实现
class _OptimizedConsumer<T extends ChangeNotifier> extends StatelessWidget {
  final Widget child;
  final Widget Function(BuildContext context, T value) builder;
  final String? debugLabel;

  const _OptimizedConsumer({
    required this.child,
    required this.builder,
    this.debugLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<T>(
      builder: (context, notifier, child) {
        return builder(context, notifier);
      },
    );
  }
}

/// 状态管理优化混入
mixin StateOptimizationMixin on State {
  /// 追踪重建次数
  int _rebuildCount = 0;

  /// 追踪状态变更
  final Map<String, dynamic> _lastState = {};

  /// 获取重建计数
  int get rebuildCount => _rebuildCount;

  /// 检查状态是否变更
  bool hasStateChanged<T>(String key, T newValue) {
    final oldValue = _lastState[key];
    _lastState[key] = newValue;
    return oldValue != newValue;
  }

  /// 重置状态追踪
  void resetStateTracking() {
    _lastState.clear();
  }

  @override
  void setState(VoidCallback fn) {
    _rebuildCount++;
    super.setState(fn);
  }
}

/// 性能分析工具
class PerformanceAnalyzer {
  static final Map<String, Stopwatch> _stopwatches = {};

  /// 开始性能分析
  static void startAnalysis(String key) {
    _stopwatches[key] = Stopwatch()..start();
  }

  /// 结束性能分析并输出结果
  static void endAnalysis(String key) {
    final stopwatch = _stopwatches[key];
    if (stopwatch != null) {
      stopwatch.stop();
      final duration = stopwatch.elapsedMilliseconds;
      debugPrint('$key took ${duration}ms');
      _stopwatches.remove(key);
    }
  }

  /// 批量分析
  static Map<String, int> getAllAnalyses() {
    final results = <String, int>{};
    for (final entry in _stopwatches.entries) {
      results[entry.key] = entry.value.elapsedMilliseconds;
    }
    return results;
  }
}

/// 内存泄漏检测工具
class MemoryLeakDetector {
  static final Set<Object> _trackedObjects = {};

  /// 开始追踪对象
  static void trackObject(Object object, String label) {
    _trackedObjects.add(object);
    debugPrint('Tracking object: $label (Total: ${_trackedObjects.length})');
  }

  /// 停止追踪对象
  static void untrackObject(Object object) {
    _trackedObjects.remove(object);
  }

  /// 获取当前追踪的对象数量
  static int get trackedObjectCount => _trackedObjects.length;

  /// 检查内存泄漏
  static bool checkMemoryLeaks() {
    final count = _trackedObjects.length;
    if (count > 100) {
      debugPrint('WARNING: High number of tracked objects: $count');
      return true;
    }
    return false;
  }
}

/// Widget重建优化工具
class WidgetRebuildOptimizer {
  static const int _maxRebuildsPerMinute = 60;
  static DateTime? _lastResetTime;
  static int _rebuildCount = 0;

  /// 检查是否应该重建
  static bool shouldRebuild() {
    final now = DateTime.now();
    
    // 每分钟重置计数器
    if (_lastResetTime == null || 
        now.difference(_lastResetTime!).inMinutes >= 1) {
      _rebuildCount = 0;
      _lastResetTime = now;
    }

    if (_rebuildCount >= _maxRebuildsPerMinute) {
      debugPrint('WARNING: Too many widget rebuilds in short time');
      return false;
    }

    _rebuildCount++;
    return true;
  }

  /// 获取重建统计
  static Map<String, dynamic> getRebuildStats() {
    return {
      'rebuild_count': _rebuildCount,
      'last_reset': _lastResetTime?.toIso8601String(),
      'max_allowed': _maxRebuildsPerMinute,
    };
  }
}

/// 优化的Widget Key
class OptimizedWidgetKey extends ValueKey<String> {
  const OptimizedWidgetKey(String value) : super(value);

  /// 生成稳定的key
  static OptimizedWidgetKey createStableKey(String identifier) {
    final stableId = identifier.hashCode.toString();
    return OptimizedWidgetKey(stableId);
  }
}

/// 优化的动画控制器管理
class OptimizedAnimationController {
  final AnimationController _controller;
  bool _isDisposed = false;

  OptimizedAnimationController({
    required TickerProvider vsync,
    Duration duration = const Duration(milliseconds: 300),
  }) : _controller = AnimationController(vsync: vsync, duration: duration);

  /// 安全地启动动画
  void safeForward() {
    if (!_isDisposed && _controller.isDismissed) {
      _controller.forward();
    }
  }

  /// 安全地重置动画
  void safeReset() {
    if (!_isDisposed && _controller.isCompleted) {
      _controller.reset();
    }
  }

  /// 安全地反向动画
  void safeReverse() {
    if (!_isDisposed && _controller.isCompleted) {
      _controller.reverse();
    }
  }

  /// 释放资源
  void dispose() {
    if (!_isDisposed) {
      _isDisposed = true;
      _controller.dispose();
    }
  }

  AnimationController get raw => _controller;
}