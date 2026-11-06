import 'package:event_bus/event_bus.dart';
import 'dart:developer' as developer;

import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';
import '../../data/models/budget_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/bill_model.dart';

/// 应用事件总线服务
/// 用于解决跨页面、跨Provider的数据同步问题
class EventBusService {
  static EventBusService? _instance;
  static EventBus? _eventBus;

  // 私有构造函数
  EventBusService._();

  /// 获取单例实例
  static EventBusService get instance {
    _instance ??= EventBusService._();
    _eventBus ??= EventBus();
    return _instance!;
  }

  /// 获取EventBus实例
  EventBus get eventBus => _eventBus!;

  // ========== 储蓄目标相关事件 ==========

  /// 发布储蓄目标更新事件
  void emitSavingGoalUpdated(SavingGoal goal) {
    try {
      final event = SavingGoalUpdatedEvent(goal);
      _eventBus!.fire(event);
      developer.log('储蓄目标更新事件已发布: ${goal.name}', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布储蓄目标更新事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布储蓄目标创建事件
  void emitSavingGoalCreated(SavingGoal goal) {
    try {
      final event = SavingGoalCreatedEvent(goal);
      _eventBus!.fire(event);
      developer.log('储蓄目标创建事件已发布: ${goal.name}', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布储蓄目标创建事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布储蓄目标删除事件
  void emitSavingGoalDeleted(int goalId) {
    try {
      final event = SavingGoalDeletedEvent(goalId);
      _eventBus!.fire(event);
      developer.log('储蓄目标删除事件已发布: ID=$goalId', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布储蓄目标删除事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  // ========== 储蓄记录相关事件 ==========

  /// 发布储蓄记录更新事件
  void emitSavingRecordUpdated(SavingRecord record) {
    try {
      final event = SavingRecordUpdatedEvent(record);
      _eventBus!.fire(event);
      developer.log(
        '储蓄记录更新事件已发布: 目标ID=${record.goalId}, 金额=${record.amount}',
        name: 'EventBusService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '发布储蓄记录更新事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布储蓄记录创建事件
  void emitSavingRecordCreated(SavingRecord record) {
    try {
      final event = SavingRecordCreatedEvent(record);
      _eventBus!.fire(event);
      developer.log(
        '储蓄记录创建事件已发布: 目标ID=${record.goalId}, 金额=${record.amount}',
        name: 'EventBusService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '发布储蓄记录创建事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布储蓄记录删除事件
  void emitSavingRecordDeleted(int recordId) {
    try {
      final event = SavingRecordDeletedEvent(recordId);
      _eventBus!.fire(event);
      developer.log('储蓄记录删除事件已发布: ID=$recordId', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布储蓄记录删除事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  // ========== 预算相关事件 ==========

  /// 发布预算更新事件
  void emitBudgetUpdated(Budget budget) {
    try {
      final event = BudgetUpdatedEvent(budget);
      _eventBus!.fire(event);
      developer.log(
        '预算更新事件已发布: ${budget.categoryName}, 金额=${budget.amount}',
        name: 'EventBusService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '发布预算更新事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布预算创建事件
  void emitBudgetCreated(Budget budget) {
    try {
      final event = BudgetCreatedEvent(budget);
      _eventBus!.fire(event);
      developer.log(
        '预算创建事件已发布: ${budget.categoryName}, 金额=${budget.amount}',
        name: 'EventBusService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '发布预算创建事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布预算删除事件
  void emitBudgetDeleted(int budgetId) {
    try {
      final event = BudgetDeletedEvent(budgetId);
      _eventBus!.fire(event);
      developer.log('预算删除事件已发布: ID=$budgetId', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布预算删除事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  // ========== 分类相关事件 ==========  

  /// 发布分类更新事件
  void emitCategoryUpdated(Category category) {
    try {
      final event = CategoryUpdatedEvent(category);
      _eventBus!.fire(event);
      developer.log('分类更新事件已发布: ${category.name}', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布分类更新事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  // ========== 交易相关事件 ==========

  /// 发布交易创建事件
  void emitTransactionCreated(Bill bill) {
    try {
      final event = TransactionCreatedEvent(bill);
      _eventBus!.fire(event);
      developer.log(
        '交易创建事件已发布: ${bill.categoryName}, 金额=${bill.amount}',
        name: 'EventBusService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '发布交易创建事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布交易更新事件
  void emitTransactionUpdated(Bill bill) {
    try {
      final event = TransactionUpdatedEvent(bill);
      _eventBus!.fire(event);
      developer.log(
        '交易更新事件已发布: ${bill.categoryName}, 金额=${bill.amount}',
        name: 'EventBusService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '发布交易更新事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 发布交易删除事件
  void emitTransactionDeleted(int billId) {
    try {
      final event = TransactionDeletedEvent(billId);
      _eventBus!.fire(event);
      developer.log('交易删除事件已发布: ID=$billId', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '发布交易删除事件失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  // ========== 通用方法 ==========

  /// 清理所有监听器
  void dispose() {
    try {
      _eventBus?.destroy();
      _instance = null;
      _eventBus = null;
      developer.log('事件总线已清理', name: 'EventBusService');
    } catch (e, stackTrace) {
      developer.log(
        '清理事件总线失败: $e',
        name: 'EventBusService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }
}

// ========== 事件类定义 ==========

/// 储蓄目标更新事件
class SavingGoalUpdatedEvent {
  final SavingGoal goal;
  SavingGoalUpdatedEvent(this.goal);
}

/// 储蓄目标创建事件
class SavingGoalCreatedEvent {
  final SavingGoal goal;
  SavingGoalCreatedEvent(this.goal);
}

/// 储蓄目标删除事件
class SavingGoalDeletedEvent {
  final int goalId;
  SavingGoalDeletedEvent(this.goalId);
}

/// 储蓄记录更新事件
class SavingRecordUpdatedEvent {
  final SavingRecord record;
  SavingRecordUpdatedEvent(this.record);
}

/// 储蓄记录创建事件
class SavingRecordCreatedEvent {
  final SavingRecord record;
  SavingRecordCreatedEvent(this.record);
}

/// 储蓄记录删除事件
class SavingRecordDeletedEvent {
  final int recordId;
  SavingRecordDeletedEvent(this.recordId);
}

/// 预算更新事件
class BudgetUpdatedEvent {
  final Budget budget;
  BudgetUpdatedEvent(this.budget);
}

/// 预算创建事件
class BudgetCreatedEvent {
  final Budget budget;
  BudgetCreatedEvent(this.budget);
}

/// 预算删除事件
class BudgetDeletedEvent {
  final int budgetId;
  BudgetDeletedEvent(this.budgetId);
}

/// 分类更新事件
class CategoryUpdatedEvent {
  final Category category;
  CategoryUpdatedEvent(this.category);
}

/// 数据同步事件（用于批量数据更新）
class DataSyncEvent {
  final String type; // 同步类型：goals, records, budgets, categories
  final DateTime timestamp;
  final dynamic data; // 可选的相关数据

  DataSyncEvent({required this.type, required this.timestamp, this.data});
}

/// 交易创建事件
class TransactionCreatedEvent {
  final Bill bill;
  TransactionCreatedEvent(this.bill);
}

/// 交易更新事件
class TransactionUpdatedEvent {
  final Bill bill;
  TransactionUpdatedEvent(this.bill);
}

/// 交易删除事件
class TransactionDeletedEvent {
  final int billId;
  TransactionDeletedEvent(this.billId);
}
