class Transaction {
  final int? id;
  final int type; // 1=Income, 2=Expense
  final int? categoryId;
  final String categoryName;
  final double amount;
  final DateTime transactionDate;
  final String? remark;

  const Transaction({
    this.id,
    required this.type,
    this.categoryId,
    required this.categoryName,
    required this.amount,
    required this.transactionDate,
    this.remark,
  });

  factory Transaction.fromJson(Map<String, dynamic> json) {
    return Transaction(
      id: json['id'] as int?,
      type: json['type'] as int,
      categoryId: json['categoryId'] as int?,
      categoryName: json['categoryName'] as String? ?? '',
      amount: (json['amount'] as num).toDouble(),
      transactionDate: DateTime.parse(json['transactionDate'] as String),
      remark: json['remark'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'type': type,
      'categoryId': categoryId,
      'categoryName': categoryName,
      'amount': amount,
      'transactionDate': transactionDate.toIso8601String().split('T')[0],
      'remark': remark,
    };
  }

  Transaction copyWith({
    int? id,
    int? type,
    int? categoryId,
    String? categoryName,
    double? amount,
    DateTime? transactionDate,
    String? remark,
  }) {
    return Transaction(
      id: id ?? this.id,
      type: type ?? this.type,
      categoryId: categoryId ?? this.categoryId,
      categoryName: categoryName ?? this.categoryName,
      amount: amount ?? this.amount,
      transactionDate: transactionDate ?? this.transactionDate,
      remark: remark ?? this.remark,
    );
  }

  bool get isIncome => type == 1;
  bool get isExpense => type == 2;
}
