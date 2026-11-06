class Budget {
  final int? id;
  final String categoryName;
  final double amount;
  final int year;
  final int month;
  final double spent;
  final String? budgetName;

  Budget({
    this.id,
    required this.categoryName,
    required this.amount,
    required this.year,
    required this.month,
    this.spent = 0.0,
    this.budgetName,
  });

  factory Budget.fromJson(Map<String, dynamic> json) {
    return Budget(
      id: json['id'] as int?,
      categoryName: json['categoryName'] as String,
      amount: (json['amount'] as num).toDouble(),
      year: json['year'] as int,
      month: json['month'] as int,
      spent: (json['spent'] as num?)?.toDouble() ?? 0.0,
      budgetName: json['budgetName'] as String?,
    );
  }

  factory Budget.fromMap(Map<String, dynamic> map) {
    return Budget(
      id: map['id'] as int?,
      categoryName: map['categoryName'] as String,
      amount: (map['amount'] as num).toDouble(),
      year: map['year'] as int,
      month: map['month'] as int,
      spent: (map['spent'] as num?)?.toDouble() ?? 0.0,
      budgetName: map['budgetName'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'categoryName': categoryName,
      'amount': amount,
      'year': year,
      'month': month,
      'spent': spent,
      'budgetName': budgetName,
    };
  }

  Budget copyWith({
    int? id,
    String? categoryName,
    double? amount,
    int? year,
    int? month,
    double? spent,
    String? budgetName,
  }) {
    return Budget(
      id: id ?? this.id,
      categoryName: categoryName ?? this.categoryName,
      amount: amount ?? this.amount,
      year: year ?? this.year,
      month: month ?? this.month,
      spent: spent ?? this.spent,
      budgetName: budgetName ?? this.budgetName,
    );
  }
}

/// 预算预警模型
class BudgetAlert {
  final int? budgetId;
  final int? categoryId;
  final String categoryName;
  final String? budgetName;
  final int year;
  final int month;
  final double budgetAmount;
  final double spentAmount;
  final double remainingAmount;
  final double usageRate;
  final String alertLevel;
  final double triggeredThreshold;
  final String message;
  final bool notificationSent;
  final DateTime alertTime;

  BudgetAlert({
    this.budgetId,
    this.categoryId,
    required this.categoryName,
    this.budgetName,
    required this.year,
    required this.month,
    required this.budgetAmount,
    required this.spentAmount,
    required this.remainingAmount,
    required this.usageRate,
    required this.alertLevel,
    required this.triggeredThreshold,
    required this.message,
    required this.notificationSent,
    required this.alertTime,
  });

  factory BudgetAlert.fromJson(Map<String, dynamic> json) {
    return BudgetAlert(
      budgetId: json['budgetId'] as int?,
      categoryId: json['categoryId'] as int?,
      categoryName: json['categoryName'] as String,
      budgetName: json['budgetName'] as String?,
      year: json['year'] as int,
      month: json['month'] as int,
      budgetAmount: (json['budgetAmount'] as num).toDouble(),
      spentAmount: (json['spentAmount'] as num).toDouble(),
      remainingAmount: (json['remainingAmount'] as num).toDouble(),
      usageRate: (json['usageRate'] as num).toDouble(),
      alertLevel: json['alertLevel'] as String,
      triggeredThreshold: (json['triggeredThreshold'] as num).toDouble(),
      message: json['message'] as String,
      notificationSent: json['notificationSent'] as bool,
      alertTime: DateTime.parse(json['alertTime'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'budgetId': budgetId,
      'categoryId': categoryId,
      'categoryName': categoryName,
      'budgetName': budgetName,
      'year': year,
      'month': month,
      'budgetAmount': budgetAmount,
      'spentAmount': spentAmount,
      'remainingAmount': remainingAmount,
      'usageRate': usageRate,
      'alertLevel': alertLevel,
      'triggeredThreshold': triggeredThreshold,
      'message': message,
      'notificationSent': notificationSent,
      'alertTime': alertTime.toIso8601String(),
    };
  }

  /// 获取使用率百分比（0-100）
  double get usagePercentage => usageRate * 100;

  /// 判断是否超支
  bool get isOverBudget => spentAmount > budgetAmount;

  /// 获取进度百分比（0-1）
  double get progress => spentAmount / budgetAmount;
}