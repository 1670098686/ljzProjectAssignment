/// 储蓄记录模型
/// 用于表示储蓄目标的存取记录
class SavingRecord {
  final int? id;
  final int goalId; // 关联的储蓄目标ID
  final double amount; // 金额
  final String type; // 类型：'deposit'（存款）或'withdraw'（取款）
  final String? remark; // 备注
  final DateTime createdAt; // 创建时间
  final DateTime? updatedAt; // 更新时间

  const SavingRecord({
    this.id,
    required this.goalId,
    required this.amount,
    required this.type,
    this.remark,
    required this.createdAt,
    this.updatedAt,
  });

  /// 从JSON创建对象
  factory SavingRecord.fromJson(Map<String, dynamic> json) {
    return SavingRecord(
      id: json['id'] as int?,
      goalId: json['goalId'] as int,
      amount: (json['amount'] as num).toDouble(),
      type: json['type'] as String,
      remark: json['remark'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] != null 
          ? DateTime.parse(json['updatedAt'] as String) 
          : null,
    );
  }

  /// 转换为JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'goalId': goalId,
      'amount': amount,
      'type': type,
      'remark': remark,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  /// 从Map创建对象（用于数据库）
  factory SavingRecord.fromMap(Map<String, dynamic> map) {
    return SavingRecord(
      id: map['id']?.toInt(),
      goalId: map['goalId']?.toInt() ?? 0,
      amount: map['amount']?.toDouble() ?? 0.0,
      type: map['type'] ?? 'deposit',
      remark: map['remark'],
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['createdAt']),
      updatedAt: map['updatedAt'] != null 
          ? DateTime.fromMillisecondsSinceEpoch(map['updatedAt']) 
          : null,
    );
  }

  /// 转换为Map（用于数据库存储）
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'goalId': goalId,
      'amount': amount,
      'type': type,
      'remark': remark,
      'createdAt': createdAt.millisecondsSinceEpoch,
      'updatedAt': updatedAt?.millisecondsSinceEpoch,
    };
  }

  /// 创建副本，可以修改某些字段
  SavingRecord copyWith({
    int? id,
    int? goalId,
    double? amount,
    String? type,
    String? remark,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return SavingRecord(
      id: id ?? this.id,
      goalId: goalId ?? this.goalId,
      amount: amount ?? this.amount,
      type: type ?? this.type,
      remark: remark ?? this.remark,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  /// 兼容性别名，返回创建时间
  DateTime get date => createdAt;

  @override
  String toString() {
    return 'SavingRecord{id: $id, goalId: $goalId, amount: $amount, type: $type, remark: $remark, createdAt: $createdAt, updatedAt: $updatedAt}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SavingRecord &&
        other.id == id &&
        other.goalId == goalId &&
        other.amount == amount &&
        other.type == type &&
        other.remark == remark &&
        other.createdAt == createdAt &&
        other.updatedAt == updatedAt;
  }

  @override
  int get hashCode {
    return id.hashCode ^
        goalId.hashCode ^
        amount.hashCode ^
        type.hashCode ^
        remark.hashCode ^
        createdAt.hashCode ^
        updatedAt.hashCode;
  }
}