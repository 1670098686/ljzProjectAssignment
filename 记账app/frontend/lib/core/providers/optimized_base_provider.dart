import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import '../errors/error_center.dart';
import '../network/app_exception.dart';
import '../utils/state_optimization_utils.dart';

enum ViewState { idle, busy, error, success }

/// 优化的基础Provider类
/// 提供更好的性能监控和状态管理优化
class OptimizedBaseProvider extends ChangeNotifier {
  OptimizedBaseProvider({ErrorCenter? errorCenter}) : _errorCenter = errorCenter, _performanceKey = '' {
    // 初始化性能分析
    _performanceKey = 'provider_${runtimeType.hashCode}';
    PerformanceAnalyzer.startAnalysis(_performanceKey);
  }

  final ErrorCenter? _errorCenter;
  String _performanceKey;
  
  ViewState _state = ViewState.idle;
  String? _errorMessage;
  int _operationCount = 0;

  ViewState get state => _state;
  String? get errorMessage => _errorMessage;
  int get operationCount => _operationCount;
  bool get isLoading => _state == ViewState.busy;
  bool get hasError => _state == ViewState.error;
  bool get isSuccess => _state == ViewState.success;

  /// 优化的状态设置方法
  void setState(ViewState viewState) {
    if (_state == viewState) return;
    
    _state = viewState;
    _incrementOperationCount();
    notifyListeners();
  }

  /// 优化的错误设置方法
  void setError(dynamic error, {RetryCallback? retry}) {
    if (error is AppException) {
      _errorMessage = error.message;
    } else {
      _errorMessage = error.toString();
    }
    
    _errorCenter?.showError(
      message: _errorMessage ?? '未知错误',
      retry: retry,
    );
    
    setState(ViewState.error);
    _logError(error, null);
  }

  /// 优化的忙碌状态设置
  void setBusy([String? operation]) {
    _errorMessage = null;
    if (operation != null) {
      debugPrint('🔄 Provider $runtimeType: $operation');
    }
    setState(ViewState.busy);
  }

  /// 优化的空闲状态设置
  void setIdle() {
    setState(ViewState.idle);
  }

  /// 优化的成功状态设置
  void setSuccess() {
    setState(ViewState.success);
  }

  /// 批量更新状态方法
  void updateState(ViewState viewState, VoidCallback updater) {
    final oldState = _state;
    updater();
    
    if (oldState != _state) {
      _incrementOperationCount();
      notifyListeners();
    }
  }

  /// 安全的notifyListeners包装
  void safeNotifyListeners([String? operation]) {
    if (operation != null) {
      PerformanceAnalyzer.startAnalysis('${_performanceKey}_$operation');
    }
    
    try {
      notifyListeners();
    } catch (e) {
      debugPrint('⚠️ Provider $runtimeType 通知监听器失败: $e');
    }
    
    if (operation != null) {
      PerformanceAnalyzer.endAnalysis('${_performanceKey}_$operation');
    }
  }

  /// 记录操作次数
  void _incrementOperationCount() {
    _operationCount++;
    
    // 检查操作频率
    if (_operationCount > 1000) {
      debugPrint('⚠️ Provider $runtimeType 操作次数过多: $_operationCount');
    }
  }

  /// 错误日志记录
  void _logError(dynamic error, String? context) {
    final errorInfo = {
      'provider': runtimeType.toString(),
      'error': error.toString(),
      'context': context,
      'operationCount': _operationCount,
      'state': _state.toString(),
      'timestamp': DateTime.now().toIso8601String(),
    };
    
    debugPrint('❌ Provider错误: $errorInfo');
  }

  /// 获取性能统计
  Map<String, dynamic> getPerformanceStats() {
    return {
      'provider': runtimeType.toString(),
      'operationCount': _operationCount,
      'currentState': _state.toString(),
      'hasError': hasError,
      'isLoading': isLoading,
    };
  }

  /// 检查内存泄漏
  bool checkMemoryLeaks() {
    final hasLeaks = MemoryLeakDetector.checkMemoryLeaks();
    if (hasLeaks) {
      debugPrint('⚠️ Provider $runtimeType 可能存在内存泄漏');
    }
    return hasLeaks;
  }

  /// 重置统计信息
  void resetStats() {
    _operationCount = 0;
    debugPrint('🔄 Provider $runtimeType 统计信息已重置');
  }

  /// 获取性能分析结果
  Map<String, int> getPerformanceAnalysis() {
    final allAnalyses = PerformanceAnalyzer.getAllAnalyses();
    final providerAnalyses = <String, int>{};
    
    for (final entry in allAnalyses.entries) {
      if (entry.key.startsWith(_performanceKey)) {
        providerAnalyses[entry.key] = entry.value;
      }
    }
    
    return providerAnalyses;
  }

  @override
  void dispose() {
    // 记录最终性能统计
    final stats = getPerformanceStats();
    final analysis = getPerformanceAnalysis();
    
    debugPrint('📊 Provider $runtimeType 最终统计: $stats');
    if (analysis.isNotEmpty) {
      debugPrint('📈 Provider $runtimeType 性能分析: $analysis');
    }
    
    // 结束性能分析
    PerformanceAnalyzer.endAnalysis(_performanceKey);
    
    super.dispose();
  }
}

/// 异步操作优化装饰器
mixin AsyncOperationMixin on OptimizedBaseProvider {
  /// 优化的异步操作执行器
  Future<T> executeAsync<T>(
    Future<T> Function() operation,
    String operationName, {
    bool showLoading = true,
    bool rethrowError = true,
  }) async {
    if (showLoading) {
      setBusy(operationName);
    }

    try {
      PerformanceAnalyzer.startAnalysis('${operationName}_${runtimeType.hashCode}');
      
      final result = await operation();
      
      PerformanceAnalyzer.endAnalysis('${operationName}_${runtimeType.hashCode}');
      
      if (showLoading) {
        setSuccess();
      }
      
      return result;
    } catch (e) {
      PerformanceAnalyzer.endAnalysis('${operationName}_${runtimeType.hashCode}');
      
      if (showLoading) {
        setError(e);
      }
      
      if (rethrowError) {
        rethrow;
      }
      
      // 返回默认值而不是抛出异常
      return Future<T>.value() as T;
    }
  }

  /// 优化的批量异步操作
  Future<List<T>> executeBatchAsync<T>(
    List<Future<T> Function()> operations,
    String batchName, {
    bool showLoading = true,
    bool stopOnError = false,
  }) async {
    if (showLoading) {
      setBusy(batchName);
    }

    final results = <T>[];
    
    try {
      for (int i = 0; i < operations.length; i++) {
        try {
          final result = await operations[i]();
          results.add(result);
        } catch (e) {
          if (stopOnError) {
            rethrow;
          }
          debugPrint('⚠️ 批量操作 $batchName 第${i + 1}项失败: $e');
        }
      }

      if (showLoading) {
        setSuccess();
      }

      return results;
    } catch (e) {
      if (showLoading) {
        setError(e);
      }
      rethrow;
    }
  }
}

/// 优化的Listenable包装器
class OptimizedValueListenable<T> extends ValueListenable<T> {
  T _value;
  final Set<VoidCallback> _listeners = {};

  OptimizedValueListenable(this._value);

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

  /// 优化的值更新方法
  void updateValue(T newValue, {bool force = false}) {
    if (!force && _value == newValue) return;
    
    // 注意：这里需要重新创建_listeners来避免引用问题
    final oldListeners = Set<VoidCallback>.from(_listeners);
    
    // 更新值
    _value = newValue;
    
    // 通知所有监听器
    for (final listener in oldListeners) {
      listener();
    }
  }

  @override
  void dispose() {
    _listeners.clear();
  }
}

/// Provider工厂类 - 用于创建优化的Provider实例
class OptimizedProviderFactory {
  /// 创建优化的Provider实例
  static T createProvider<T extends OptimizedBaseProvider>(
    T Function() providerFactory, {
    String? debugLabel,
  }) {
    final provider = providerFactory();
    
    if (debugLabel != null) {
      MemoryLeakDetector.trackObject(provider, debugLabel);
    }
    
    return provider;
  }

  /// 创建优化的ValueListenable
  static OptimizedValueListenable<T> createOptimizedListenable<T>(
    T initialValue, {
    String? debugLabel,
  }) {
    final listenable = OptimizedValueListenable(initialValue);
    
    if (debugLabel != null) {
      MemoryLeakDetector.trackObject(listenable, debugLabel);
    }
    
    return listenable;
  }
}