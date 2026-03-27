import 'budget_model.dart';

class BudgetRealTimeStats {
  final Budget budget;
  final double actualSpent;
  final double usage;
  final double remaining;
  final bool isOverBudget;
  final String status;

  BudgetRealTimeStats({
    required this.budget,
    required this.actualSpent,
    required this.usage,
    required this.remaining,
    required this.isOverBudget,
    required this.status,
  });

  factory BudgetRealTimeStats.fromJson(Map<String, dynamic> json) {
    return BudgetRealTimeStats(
      budget: Budget.fromJson(json['budget']),
      actualSpent: (json['actualSpent'] as num).toDouble(),
      usage: (json['usage'] as num).toDouble(),
      remaining: (json['remaining'] as num).toDouble(),
      isOverBudget: json['isOverBudget'] as bool,
      status: json['status'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'budget': budget.toJson(),
      'actualSpent': actualSpent,
      'usage': usage,
      'remaining': remaining,
      'isOverBudget': isOverBudget,
      'status': status,
    };
  }
}