class StatisticsSummary {
  final double totalIncome;
  final double totalExpense;
  final double balance;
  
  // 储蓄相关统计
  final double totalDeposits;    // 总存款
  final double totalWithdraws;   // 总取款
  final double netSaving;        // 净储蓄

  StatisticsSummary({
    required this.totalIncome,
    required this.totalExpense,
    required this.balance,
    required this.totalDeposits,
    required this.totalWithdraws,
    required this.netSaving,
  });

  factory StatisticsSummary.fromJson(Map<String, dynamic> json) {
    return StatisticsSummary(
      totalIncome: (json['totalIncome'] as num).toDouble(),
      totalExpense: (json['totalExpense'] as num).toDouble(),
      balance: (json['balance'] as num).toDouble(),
      totalDeposits: (json['totalDeposits'] as num?)?.toDouble() ?? 0.0,
      totalWithdraws: (json['totalWithdraws'] as num?)?.toDouble() ?? 0.0,
      netSaving: (json['netSaving'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

class CategoryStatistics {
  final String categoryName;
  final double amount;
  final double percentage;

  CategoryStatistics({
    required this.categoryName,
    required this.amount,
    required this.percentage,
  });

  factory CategoryStatistics.fromJson(Map<String, dynamic> json) {
    return CategoryStatistics(
      categoryName: json['categoryName'] as String,
      amount: (json['amount'] as num).toDouble(),
      percentage: (json['percentage'] as num).toDouble(),
    );
  }
}

class TrendStatistics {
  final String date;
  final double income;
  final double expense;
  final double deposits;   // 存款
  final double withdraws;  // 取款
  
  // 添加一个getter来提供兼容savingStats的访问方式
  SavingRecordStats? get savingStats {
    // 如果有存款或取款数据，则创建一个SavingRecordStats对象
    if (deposits > 0 || withdraws > 0) {
      return SavingRecordStats(
        totalDeposits: deposits,
        totalWithdraws: withdraws,
        netAmount: deposits - withdraws,
        recordCount: (deposits > 0 ? 1 : 0) + (withdraws > 0 ? 1 : 0),
      );
    }
    return null;
  }

  TrendStatistics({
    required this.date,
    required this.income,
    required this.expense,
    required this.deposits,
    required this.withdraws,
  });

  factory TrendStatistics.fromJson(Map<String, dynamic> json) {
    return TrendStatistics(
      date: json['date'] as String,
      income: (json['income'] as num).toDouble(),
      expense: (json['expense'] as num).toDouble(),
      deposits: (json['deposits'] as num?)?.toDouble() ?? 0.0,
      withdraws: (json['withdraws'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

/// 储蓄记录统计结果
class SavingRecordStats {
  final double totalDeposits;
  final double totalWithdraws;
  final double netAmount;
  final int recordCount;
  final double averageDeposit;   // 平均存款
  final double averageWithdraw;  // 平均取款

  SavingRecordStats({
    required this.totalDeposits,
    required this.totalWithdraws,
    required this.netAmount,
    required this.recordCount,
    this.averageDeposit = 0.0,
    this.averageWithdraw = 0.0,
  });

  factory SavingRecordStats.fromJson(Map<String, dynamic> json) {
    final totalDeposits = (json['totalDeposits'] as num).toDouble();
    final totalWithdraws = (json['totalWithdraws'] as num).toDouble();
    final recordCount = json['recordCount'] as int;
    
    return SavingRecordStats(
      totalDeposits: totalDeposits,
      totalWithdraws: totalWithdraws,
      netAmount: (json['netAmount'] as num).toDouble(),
      recordCount: recordCount,
      averageDeposit: recordCount > 0 ? totalDeposits / recordCount : 0.0,
      averageWithdraw: recordCount > 0 ? totalWithdraws / recordCount : 0.0,
    );
  }
}
