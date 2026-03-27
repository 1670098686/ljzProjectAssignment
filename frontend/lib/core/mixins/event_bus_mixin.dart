import 'package:flutter/material.dart';
import 'dart:async';
import '../services/event_bus_service.dart';

/// 事件监听混入类
/// 为页面组件提供便捷的事件监听功能
mixin EventBusMixin<W extends StatefulWidget> on State<W> {
  final List<StreamSubscription> _subscriptions = [];

  /// 获取事件总线服务
  EventBusService get eventBus => EventBusService.instance;

  /// 注册事件监听器
  /// 
  /// [eventType] 事件类型
  /// [handler] 事件处理器
  /// [onError] 错误处理回调（可选）
  void listen<T>(Type eventType, void Function(T event) handler, {Function? onError}) {
    final subscription = eventBus.eventBus.on<T>().listen(
      (event) {
        if (mounted) {
          handler(event);
        }
      },
      onError: (error) {
        if (mounted) {
          onError?.call(error);
        }
      },
    );
    
    _subscriptions.add(subscription);
  }

  /// 监听储蓄记录更新事件
  void onSavingRecordUpdated(void Function(SavingRecordUpdatedEvent event) handler) {
    listen<SavingRecordUpdatedEvent>(SavingRecordUpdatedEvent, handler);
  }

  /// 监听储蓄目标更新事件
  void onSavingGoalUpdated(void Function(SavingGoalUpdatedEvent event) handler) {
    listen<SavingGoalUpdatedEvent>(SavingGoalUpdatedEvent, handler);
  }

  /// 监听预算更新事件
  void onBudgetUpdated(void Function(BudgetUpdatedEvent event) handler) {
    listen<BudgetUpdatedEvent>(BudgetUpdatedEvent, handler);
  }

  /// 监听分类更新事件
  void onCategoryUpdated(void Function(CategoryUpdatedEvent event) handler) {
    listen<CategoryUpdatedEvent>(CategoryUpdatedEvent, handler);
  }

  /// 监听储蓄记录创建事件
  void onSavingRecordCreated(void Function(SavingRecordCreatedEvent event) handler) {
    listen<SavingRecordCreatedEvent>(SavingRecordCreatedEvent, handler);
  }

  /// 监听储蓄目标创建事件
  void onSavingGoalCreated(void Function(SavingGoalCreatedEvent event) handler) {
    listen<SavingGoalCreatedEvent>(SavingGoalCreatedEvent, handler);
  }

  /// 监听数据同步事件
  void onDataSync(void Function(DataSyncEvent event) handler) {
    listen<DataSyncEvent>(DataSyncEvent, handler);
  }

  /// 手动刷新页面数据的抽象方法
  /// 子类可以重写此方法来处理特定的数据刷新逻辑
  void refreshPageData() {
    // 默认实现为空，子类可以重写
  }

  @override
  void dispose() {
    // 取消所有事件监听
    for (final subscription in _subscriptions) {
      subscription.cancel();
    }
    _subscriptions.clear();
    super.dispose();
  }
}

/// 用于StatelessWidget的事件监听混入
/// 注意：StatelessWidget的生命周期管理相对简单
mixin StatelessEventBusMixin {
  final List<StreamSubscription> _subscriptions = [];

  /// 获取事件总线服务
  EventBusService get eventBus => EventBusService.instance;

  /// 注册事件监听器
  void listen<T>(Type eventType, void Function(T event) handler, {Function? onError}) {
    final subscription = eventBus.eventBus.on<T>().listen(
      (event) {
        handler(event);
      },
      onError: (error) {
        onError?.call(error);
      },
    );
    
    _subscriptions.add(subscription);
  }

  /// 手动清理事件监听器
  void disposeEventListeners() {
    for (final subscription in _subscriptions) {
      subscription.cancel();
    }
    _subscriptions.clear();
  }
}

/// Provider事件监听混入
/// 用于Provider类监听事件并触发状态更新
mixin ProviderEventBusMixin on ChangeNotifier {
  final List<StreamSubscription> _subscriptions = [];

  /// 获取事件总线服务
  EventBusService get eventBus => EventBusService.instance;

  /// 注册事件监听器
  void listen<T>(Type eventType, void Function(T event) handler) {
    final subscription = eventBus.eventBus.on<T>().listen(
      (event) {
        handler(event);
        notifyListeners(); // 自动触发状态更新
      },
    );
    
    _subscriptions.add(subscription);
  }

  /// 监听储蓄记录更新事件
  void onSavingRecordUpdated(void Function(SavingRecordUpdatedEvent event) handler) {
    listen<SavingRecordUpdatedEvent>(SavingRecordUpdatedEvent, handler);
  }

  /// 监听储蓄目标更新事件
  void onSavingGoalUpdated(void Function(SavingGoalUpdatedEvent event) handler) {
    listen<SavingGoalUpdatedEvent>(SavingGoalUpdatedEvent, handler);
  }

  /// 监听预算更新事件
  void onBudgetUpdated(void Function(BudgetUpdatedEvent event) handler) {
    listen<BudgetUpdatedEvent>(BudgetUpdatedEvent, handler);
  }

  /// 监听分类更新事件
  void onCategoryUpdated(void Function(CategoryUpdatedEvent event) handler) {
    listen<CategoryUpdatedEvent>(CategoryUpdatedEvent, handler);
  }

  @override
  void dispose() {
    // 取消所有事件监听
    for (final subscription in _subscriptions) {
      subscription.cancel();
    }
    _subscriptions.clear();
    super.dispose();
  }
}
