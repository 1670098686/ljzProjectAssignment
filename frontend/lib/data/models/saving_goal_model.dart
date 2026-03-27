class SavingGoal {
  final int? id;
  final String name;
  final double targetAmount;
  final double currentAmount;
  final DateTime deadline;
  final String? description;
  final String categoryName;

  SavingGoal({
    this.id,
    required this.name,
    required this.targetAmount,
    required this.currentAmount,
    required this.deadline,
    this.description,
    required this.categoryName,
  });

  factory SavingGoal.fromJson(Map<String, dynamic> json) {
    return SavingGoal(
      id: json['id'] as int?,
      name: json['name'] as String? ?? '',
      targetAmount: (json['targetAmount'] as num).toDouble(),
      currentAmount: (json['currentAmount'] as num).toDouble(),
      deadline: DateTime.parse(json['deadline'] as String),
      description: json['description'] as String?,
      categoryName: json['categoryName'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'targetAmount': targetAmount,
      'currentAmount': currentAmount,
      'deadline': deadline.toIso8601String().split('T')[0],
      'description': description,
      'categoryName': categoryName,
    };
  }

  /// 获取日期字符串用于显示
  String get deadlineString {
    return deadline.toIso8601String().split('T')[0];
  }

  /// 计算进度百分比
  double get progress {
    if (targetAmount <= 0) return 0.0;
    return currentAmount / targetAmount;
  }

  /// 判断是否完成
  bool get isCompleted {
    return currentAmount >= targetAmount;
  }

  /// 判断是否逾期
  bool get isOverdue {
    return DateTime.now().isAfter(deadline) && !isCompleted;
  }

  /// 获取剩余天数
  int get remainingDays {
    if (isCompleted) return 0;
    final difference = deadline.difference(DateTime.now()).inDays;
    return difference.isNegative ? 0 : difference;
  }

  /// 从数据库Map数据构造对象
  factory SavingGoal.fromMap(Map<String, dynamic> map) {
    // 处理deadline字段，支持多种格式：ISO字符串、数字字符串、整数时间戳
    DateTime deadline;
    final deadlineValue = map['deadline'];
    
    if (deadlineValue is String) {
      // 尝试解析字符串格式的日期
      try {
        // 先尝试将字符串转换为整数（处理字符串类型的时间戳）
        final timestamp = int.tryParse(deadlineValue);
        if (timestamp != null) {
          deadline = DateTime.fromMillisecondsSinceEpoch(timestamp);
        } else {
          // 如果不是数字字符串，尝试解析ISO格式
          deadline = DateTime.parse(deadlineValue);
        }
      } catch (e) {
        // 如果解析失败，使用当前日期作为默认值
        deadline = DateTime.now();
      }
    } else if (deadlineValue is int) {
      // 直接使用整数时间戳
      deadline = DateTime.fromMillisecondsSinceEpoch(deadlineValue);
    } else if (deadlineValue is double) {
      // 处理double类型的时间戳
      deadline = DateTime.fromMillisecondsSinceEpoch(deadlineValue.toInt());
    } else {
      // 默认使用当前日期
      deadline = DateTime.now();
    }
    
    return SavingGoal(
      id: map['id'] as int?,
      name: map['name'] as String? ?? '',
      targetAmount: (map['target_amount'] as num).toDouble(),
      currentAmount: (map['current_amount'] as num).toDouble(),
      deadline: deadline,
      description: map['description'] as String?,
      categoryName: map['category_name'] as String? ?? '',
    );
  }

  /// 转换为数据库存储的Map
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'target_amount': targetAmount,
      'current_amount': currentAmount,
      'deadline': deadline.millisecondsSinceEpoch,
      'description': description,
      'category_name': categoryName,
    };
  }

  /// 复制对象
  SavingGoal copyWith({
    int? id,
    String? name,
    double? targetAmount,
    double? currentAmount,
    DateTime? deadline,
    String? description,
    String? categoryName,
  }) {
    return SavingGoal(
      id: id ?? this.id,
      name: name ?? this.name,
      targetAmount: targetAmount ?? this.targetAmount,
      currentAmount: currentAmount ?? this.currentAmount,
      deadline: deadline ?? this.deadline,
      description: description ?? this.description,
      categoryName: categoryName ?? this.categoryName,
    );
  }
}
