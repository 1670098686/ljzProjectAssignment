import 'dart:async';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../../data/models/user_model.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';

class LocalDatabaseService {
  static Database? _database;
  static const String _databaseName = 'finance_app.db';
  static const int _databaseVersion = 6; // 升到 6 强制重建

  // 获取数据库实例
  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  // 初始化数据库
  Future<Database> _initDatabase() async {
    String path = join(await getDatabasesPath(), _databaseName);

    // 不再删除旧数据库，保留现有数据
    print('正在打开数据库: $path');

    return await openDatabase(
      path,
      version: _databaseVersion,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
      onOpen: _onOpen, // 添加打开数据库后的回调
    );
  }
  
  // 打开数据库后的回调
  Future<void> _onOpen(Database db) async {
    print('进入_onOpen回调...');

    // ===== 确保所有必要的表存在 =====
    await _ensureTable(db, 'CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type INTEGER NOT NULL, icon TEXT)');
    await _ensureTable(db, 'CREATE TABLE IF NOT EXISTS bills (id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER NOT NULL, categoryName TEXT NOT NULL, amount REAL NOT NULL, transactionDate TEXT NOT NULL, remark TEXT, imagePath TEXT)');
    await _ensureTable(db, 'CREATE TABLE IF NOT EXISTS budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, categoryName TEXT NOT NULL, amount REAL NOT NULL, year INTEGER NOT NULL, month INTEGER NOT NULL, spent REAL NOT NULL DEFAULT 0, budgetName TEXT)');
    await _ensureTable(db, 'CREATE TABLE IF NOT EXISTS sync_status (id INTEGER PRIMARY KEY AUTOINCREMENT, lastSyncTime INTEGER NOT NULL, isSyncing INTEGER NOT NULL DEFAULT 0)');

    // ===== 检查 users 表，为空则插入种子数据 =====
    try {
      final count = Sqflite.firstIntValue(await db.rawQuery('SELECT COUNT(*) FROM users')) ?? 0;
      if (count == 0) {
        print('🚀 users 为空，开始注入种子数据...');
        await _insertSeedData(db);
        print('✅ 种子数据注入完成！账号: test@demo.com / 密码: 123456');
      } else {
        print('✅ users 表已有 $count 条，跳过种子数据');
      }
    } catch (e) {
      print('⚠️ 检查 users 表出错: $e');
    }
  }

  Future<void> _ensureTable(Database db, String createSql) async {
    try {
      await db.execute(createSql);
    } catch (e) {
      // 表可能已存在，忽略
    }
  }

  Future<void> _insertSeedData(Database db) async {
    final now = DateTime.now();

    // 用户
    await db.insert('users', {
      'email': 'test@demo.com',
      'password': '123456',
      'created_at': now.millisecondsSinceEpoch,
    });

    // 分类
    final categories = [
      {'name': '工资', 'type': 1, 'icon': 'work'},
      {'name': '奖金', 'type': 1, 'icon': 'bonus'},
      {'name': '投资收益', 'type': 1, 'icon': 'investment'},
      {'name': '兼职', 'type': 1, 'icon': 'parttime'},
      {'name': '红包', 'type': 1, 'icon': 'redpacket'},
      {'name': '其他收入', 'type': 1, 'icon': 'other_income'},
      {'name': '餐饮', 'type': 2, 'icon': 'restaurant'},
      {'name': '交通', 'type': 2, 'icon': 'transport'},
      {'name': '购物', 'type': 2, 'icon': 'shopping'},
      {'name': '娱乐', 'type': 2, 'icon': 'entertainment'},
      {'name': '医疗', 'type': 2, 'icon': 'medical'},
      {'name': '教育', 'type': 2, 'icon': 'education'},
      {'name': '住房', 'type': 2, 'icon': 'housing'},
      {'name': '通讯', 'type': 2, 'icon': 'phone'},
      {'name': '水电', 'type': 2, 'icon': 'utility'},
      {'name': '其他支出', 'type': 2, 'icon': 'other_expense'},
    ];
    for (final c in categories) await db.insert('categories', c);

    // 30 条账单
    final bills = [
      {'type': 1, 'categoryName': '工资', 'amount': 15000.00, 'transactionDate': '2026-11-01', 'remark': '11月工资'},
      {'type': 1, 'categoryName': '工资', 'amount': 15000.00, 'transactionDate': '2026-10-01', 'remark': '10月工资'},
      {'type': 1, 'categoryName': '工资', 'amount': 15000.00, 'transactionDate': '2026-09-01', 'remark': '9月工资'},
      {'type': 1, 'categoryName': '奖金', 'amount': 3000.00, 'transactionDate': '2026-10-15', 'remark': '项目奖金'},
      {'type': 1, 'categoryName': '奖金', 'amount': 2000.00, 'transactionDate': '2026-09-20', 'remark': '季度奖'},
      {'type': 1, 'categoryName': '投资收益', 'amount': 500.00, 'transactionDate': '2026-11-03', 'remark': '股票分红'},
      {'type': 1, 'categoryName': '投资收益', 'amount': 300.00, 'transactionDate': '2026-10-10', 'remark': '基金收益'},
      {'type': 1, 'categoryName': '兼职', 'amount': 2000.00, 'transactionDate': '2026-11-05', 'remark': '周末兼职'},
      {'type': 1, 'categoryName': '红包', 'amount': 200.00, 'transactionDate': '2026-10-15', 'remark': '生日红包'},
      {'type': 1, 'categoryName': '其他收入', 'amount': 100.00, 'transactionDate': '2026-11-01', 'remark': '废品出售'},
      {'type': 2, 'categoryName': '餐饮', 'amount': 35.00, 'transactionDate': '2026-11-06', 'remark': '早餐包子豆浆'},
      {'type': 2, 'categoryName': '餐饮', 'amount': 45.50, 'transactionDate': '2026-11-05', 'remark': '午餐盖浇饭'},
      {'type': 2, 'categoryName': '餐饮', 'amount': 128.00, 'transactionDate': '2026-11-04', 'remark': '晚餐火锅'},
      {'type': 2, 'categoryName': '餐饮', 'amount': 280.00, 'transactionDate': '2026-11-02', 'remark': '朋友聚餐'},
      {'type': 2, 'categoryName': '餐饮', 'amount': 50.00, 'transactionDate': '2026-10-30', 'remark': '外卖'},
      {'type': 2, 'categoryName': '交通', 'amount': 150.00, 'transactionDate': '2026-11-01', 'remark': '地铁月卡'},
      {'type': 2, 'categoryName': '交通', 'amount': 28.00, 'transactionDate': '2026-10-28', 'remark': '打车'},
      {'type': 2, 'categoryName': '交通', 'amount': 60.00, 'transactionDate': '2026-10-20', 'remark': '加油'},
      {'type': 2, 'categoryName': '购物', 'amount': 599.00, 'transactionDate': '2026-11-03', 'remark': '羽绒服'},
      {'type': 2, 'categoryName': '购物', 'amount': 299.00, 'transactionDate': '2026-10-25', 'remark': '耳机'},
      {'type': 2, 'categoryName': '购物', 'amount': 89.00, 'transactionDate': '2026-10-18', 'remark': '日用品'},
      {'type': 2, 'categoryName': '娱乐', 'amount': 120.00, 'transactionDate': '2026-11-04', 'remark': '电影票2张'},
      {'type': 2, 'categoryName': '娱乐', 'amount': 200.00, 'transactionDate': '2026-10-31', 'remark': 'KTV'},
      {'type': 2, 'categoryName': '医疗', 'amount': 88.00, 'transactionDate': '2026-10-22', 'remark': '感冒药'},
      {'type': 2, 'categoryName': '教育', 'amount': 500.00, 'transactionDate': '2026-10-15', 'remark': '在线课程'},
      {'type': 2, 'categoryName': '住房', 'amount': 3500.00, 'transactionDate': '2026-11-01', 'remark': '11月房租'},
      {'type': 2, 'categoryName': '通讯', 'amount': 128.00, 'transactionDate': '2026-10-25', 'remark': '手机话费充值'},
      {'type': 2, 'categoryName': '水电', 'amount': 180.00, 'transactionDate': '2026-10-10', 'remark': '水电费'},
      {'type': 2, 'categoryName': '餐饮', 'amount': 68.00, 'transactionDate': '2026-09-28', 'remark': '聚餐AA'},
      {'type': 2, 'categoryName': '购物', 'amount': 1299.00, 'transactionDate': '2026-09-15', 'remark': '新手机'},
    ];
    for (final b in bills) await db.insert('bills', b);

    // 预算
    final budgets = [
      {'categoryName': '餐饮', 'amount': 2000.00, 'year': 2026, 'month': 11, 'spent': 538.50, 'budgetName': '餐饮预算'},
      {'categoryName': '交通', 'amount': 500.00, 'year': 2026, 'month': 11, 'spent': 238.00, 'budgetName': '交通预算'},
      {'categoryName': '购物', 'amount': 1500.00, 'year': 2026, 'month': 11, 'spent': 599.00, 'budgetName': '购物预算'},
      {'categoryName': '娱乐', 'amount': 800.00, 'year': 2026, 'month': 11, 'spent': 120.00, 'budgetName': '娱乐预算'},
      {'categoryName': '住房', 'amount': 4000.00, 'year': 2026, 'month': 11, 'spent': 3500.00, 'budgetName': '住房预算'},
    ];
    for (final b in budgets) await db.insert('budgets', b);

    // 储蓄目标
    final goals = [
      {'name': '购买 MacBook Pro', 'target_amount': 15000.00, 'current_amount': 8000.00, 'deadline': DateTime(2027, 3, 31).millisecondsSinceEpoch, 'description': '工作学习用笔记本', 'category_name': '数码'},
      {'name': '日本旅游基金', 'target_amount': 10000.00, 'current_amount': 3500.00, 'deadline': DateTime(2027, 6, 30).millisecondsSinceEpoch, 'description': '毕业旅行', 'category_name': '旅游'},
      {'name': '紧急备用金', 'target_amount': 20000.00, 'current_amount': 12000.00, 'deadline': DateTime(2027, 12, 31).millisecondsSinceEpoch, 'description': '6个月生活费', 'category_name': '应急'},
      {'name': '新车首付', 'target_amount': 50000.00, 'current_amount': 15000.00, 'deadline': DateTime(2028, 6, 30).millisecondsSinceEpoch, 'description': '第一辆车', 'category_name': '交通'},
    ];
    for (final g in goals) await db.insert('saving_goals', g);
  }

  // 创建数据库表
  Future<void> _onCreate(Database db, int version) async {
    // 创建用户表
    await db.execute('''
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL,
        created_at INTEGER NOT NULL
      )
    ''');
    
    // 创建储蓄目标表
    await db.execute('''
      CREATE TABLE IF NOT EXISTS saving_goals (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        target_amount REAL NOT NULL,
        current_amount REAL NOT NULL DEFAULT 0,
        deadline INTEGER NOT NULL,
        description TEXT,
        category_name TEXT NOT NULL DEFAULT ''
      )
    ''');

    // 创建储蓄记录表
    await db.execute('''
      CREATE TABLE IF NOT EXISTS saving_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        goal_id INTEGER NOT NULL,
        amount REAL NOT NULL,
        remark TEXT,
        type TEXT NOT NULL,
        category TEXT,
        created_at INTEGER NOT NULL,
        FOREIGN KEY (goal_id) REFERENCES saving_goals(id) ON DELETE CASCADE
      )
    ''');

    // 创建索引
    await db.execute(
      'CREATE INDEX IF NOT EXISTS idx_saving_records_goal_id ON saving_records(goal_id)',
    );
  }

  // 升级数据库
  Future<void> _onUpgrade(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      await db.execute(
        'ALTER TABLE saving_records ADD COLUMN type TEXT NOT NULL DEFAULT "deposit"',
      );
      await db.execute('ALTER TABLE saving_records ADD COLUMN category TEXT');
    }

    if (oldVersion < 3) {
      await _ensureSavingGoalCategoryColumn(db);
    }
    
    if (oldVersion < 4) {
      // 创建用户表
      await db.execute('''
        CREATE TABLE IF NOT EXISTS users (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          email TEXT NOT NULL UNIQUE,
          password TEXT NOT NULL,
          created_at INTEGER NOT NULL
        )
      ''');
    }
  }

  // 保存储蓄目标
  Future<int> saveSavingGoal(SavingGoal goal) async {
    final db = await database;
    return await db.insert('saving_goals', {
      'name': goal.name,
      'target_amount': goal.targetAmount,
      'current_amount': goal.currentAmount,
      'deadline': goal.deadline.millisecondsSinceEpoch,
      'description': goal.description,
      'category_name': goal.categoryName,
    }, conflictAlgorithm: ConflictAlgorithm.replace);
  }

  // 获取所有储蓄目标
  Future<List<SavingGoal>> getAllSavingGoals() async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query('saving_goals');

    return List.generate(maps.length, (i) {
      return SavingGoal.fromMap(maps[i]);
    });
  }

  // 获取指定ID的储蓄目标
  Future<SavingGoal?> getSavingGoalById(int id) async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      'saving_goals',
      where: 'id = ?',
      whereArgs: [id],
    );

    if (maps.isNotEmpty) {
      return SavingGoal.fromMap(maps.first);
    }
    return null;
  }

  // 更新储蓄目标
  Future<int> updateSavingGoal(SavingGoal goal) async {
    final db = await database;
    return await db.update(
      'saving_goals',
      {
        'name': goal.name,
        'target_amount': goal.targetAmount,
        'current_amount': goal.currentAmount,
        'deadline': goal.deadline.millisecondsSinceEpoch,
        'description': goal.description,
        'category_name': goal.categoryName,
      },
      where: 'id = ?',
      whereArgs: [goal.id],
    );
  }

  // 删除储蓄目标
  Future<int> deleteSavingGoal(int id) async {
    final db = await database;
    // 先删除相关的储蓄记录
    await db.delete('saving_records', where: 'goal_id = ?', whereArgs: [id]);

    // 再删除储蓄目标
    return await db.delete('saving_goals', where: 'id = ?', whereArgs: [id]);
  }

  // 获取指定ID的储蓄记录
  Future<SavingRecord?> getSavingRecordById(int id) async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      'saving_records',
      where: 'id = ?',
      whereArgs: [id],
    );

    if (maps.isNotEmpty) {
      return SavingRecord.fromMap(maps.first);
    }
    return null;
  }

  // 更新储蓄记录
  Future<int> updateSavingRecord(SavingRecord record) async {
    final db = await database;
    return await db.update(
      'saving_records',
      {
        'goal_id': record.goalId,
        'amount': record.amount,
        'remark': record.remark,
        'created_at': record.createdAt.millisecondsSinceEpoch,
        'updated_at': record.updatedAt?.millisecondsSinceEpoch,
        'type': record.type,
      },
      where: 'id = ?',
      whereArgs: [record.id],
    );
  }

  // 删除储蓄记录
  Future<int> deleteSavingRecord(int id) async {
    final db = await database;
    return await db.delete('saving_records', where: 'id = ?', whereArgs: [id]);
  }

  // 保存储蓄记录
  Future<int> saveSavingRecord(SavingRecord record) async {
    final db = await database;
    return await db.insert('saving_records', {
      'goal_id': record.goalId,
      'amount': record.amount,
      'remark': record.remark,
      'created_at': record.createdAt.millisecondsSinceEpoch,
      'updated_at': record.updatedAt?.millisecondsSinceEpoch,
      'type': record.type,
    }, conflictAlgorithm: ConflictAlgorithm.replace);
  }

  // 获取指定目标的储蓄记录
  Future<List<SavingRecord>> getSavingRecordsByGoalId(int goalId) async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      'saving_records',
      where: 'goal_id = ?',
      whereArgs: [goalId],
      orderBy: 'created_at DESC',
    );

    return List.generate(maps.length, (i) {
      return SavingRecord.fromMap(maps[i]);
    });
  }

  // 关闭数据库
  Future<void> close() async {
    final db = await database;
    await db.close();
    _database = null;
  }

  Future<void> _ensureSavingGoalCategoryColumn(Database db) async {
    final columns = await db.rawQuery('PRAGMA table_info(saving_goals)');
    final hasCategoryColumn = columns.any(
      (column) => column['name'] == 'category_name',
    );

    if (!hasCategoryColumn) {
      await db.execute(
        "ALTER TABLE saving_goals ADD COLUMN category_name TEXT NOT NULL DEFAULT ''",
      );
    }
  }
  
  // 注册新用户
  Future<User> registerUser(String email, String password) async {
    try {
      final db = await database;
      final now = DateTime.now().millisecondsSinceEpoch;
      
      // 插入新用户
      final id = await db.insert('users', {
        'email': email,
        'password': password, // 注意：实际应用中应该加密存储密码
        'created_at': now,
      });
      
      // 创建用户对象（使用email作为username，设置必要的默认值）
      return User(
        id: id,
        username: email.split('@')[0], // 使用邮箱前缀作为用户名
        email: email,
        createdAt: DateTime.now(),
        enabled: true,
      );
    } catch (e) {
      print('注册失败: $e');
      throw Exception('注册失败: $e');
    }
  }
  
  // 用户登录
  Future<User> loginUser(String email, String password) async {
    try {
      final db = await database;
      
      // 查询用户
      final List<Map<String, dynamic>> results = await db.query(
        'users',
        where: 'email = ? AND password = ?',
        whereArgs: [email, password],
      );
      
      if (results.isEmpty) {
        throw Exception('邮箱或密码错误');
      }
      
      // 创建用户对象
      final userData = results.first;
      return User(
        id: userData['id'],
        username: email.split('@')[0], // 使用邮箱前缀作为用户名
        email: userData['email'],
        createdAt: DateTime.fromMillisecondsSinceEpoch(userData['created_at']),
        enabled: true,
      );
    } catch (e) {
      print('登录失败: $e');
      throw Exception('登录失败: $e');
    }
  }
  
  // 检查邮箱是否已注册
  Future<bool> userExists(String email) async {
    try {
      final db = await database;
      final List<Map<String, dynamic>> results = await db.query(
        'users',
        where: 'email = ?',
        whereArgs: [email],
      );
      return results.isNotEmpty;
    } catch (e) {
      print('检查用户存在性失败: $e');
      return false;
    }
  }
}
