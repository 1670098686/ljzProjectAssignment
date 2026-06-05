class Category {
  final int? id;
  final String name;
  final String? icon;
  final int type; // 1=Income, 2=Expense

  Category({this.id, required this.name, this.icon, required this.type});

  factory Category.fromJson(Map<String, dynamic> json) {
    return Category(
      id: json['id'] as int?,
      name: json['name'] as String,
      icon: json['icon'] as String?,
      type: json['type'] as int,
    );
  }

  factory Category.fromMap(Map<String, dynamic> map) {
    return Category(
      id: map['id'] as int?,
      name: map['name'] as String,
      icon: map['icon'] as String?,
      type: map['type'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {'id': id, 'name': name, 'icon': icon, 'type': type};
  }

  Category copyWith({int? id, String? name, String? icon, int? type}) {
    return Category(
      id: id ?? this.id,
      name: name ?? this.name,
      icon: icon ?? this.icon,
      type: type ?? this.type,
    );
  }
}
