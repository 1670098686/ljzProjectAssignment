class Bill {
  final int? id;
  final int type;
  final String categoryName;
  final double amount;
  final String transactionDate;
  final String? remark;
  final String? imagePath; // 图片文件路径

  Bill({
    this.id,
    required this.type,
    required this.categoryName,
    required this.amount,
    required this.transactionDate,
    this.remark,
    this.imagePath,
  });

  factory Bill.fromJson(Map<String, dynamic> json) {
    return Bill(
      id: json['id'],
      type: json['type'],
      categoryName: json['categoryName'],
      amount: (json['amount'] as num).toDouble(),
      transactionDate: json['transactionDate'],
      remark: json['remark'],
      imagePath: json['imagePath'],
    );
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = {
      'type': type,
      'categoryName': categoryName,
      'amount': amount,
      'transactionDate': transactionDate,
      'remark': remark,
      'imagePath': imagePath,
    };
    
    // 只有当id不为null时才包含id字段
    if (id != null) {
      data['id'] = id;
    }
    
    return data;
  }

  Bill copyWith({
    int? id,
    int? type,
    String? categoryName,
    double? amount,
    String? transactionDate,
    String? remark,
    String? imagePath,
  }) {
    return Bill(
      id: id ?? this.id,
      type: type ?? this.type,
      categoryName: categoryName ?? this.categoryName,
      amount: amount ?? this.amount,
      transactionDate: transactionDate ?? this.transactionDate,
      remark: remark ?? this.remark,
      imagePath: imagePath ?? this.imagePath,
    );
  }

  /// 获取日期对象（便于UI使用）
  DateTime get date {
    return DateTime.parse(transactionDate);
  }

  /// 判断是否有图片
  bool get hasImage {
    return imagePath != null && imagePath!.isNotEmpty;
  }

  /// 获取类型名称
  String get typeName {
    return type == 1 ? '收入' : '支出';
  }

  /// 获取金额显示（带符号）
  String get amountWithSign {
    return type == 1 ? '+¥${amount.toStringAsFixed(2)}' : '-¥${amount.toStringAsFixed(2)}';
  }
}
