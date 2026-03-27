import 'dart:async';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../../data/models/user_model.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';

class LocalDatabaseService {
  static Database? _database;
  static const String _databaseName = 'finance_app.db';
  static const int _databaseVersion = 4;

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
    print('进入_onOpen回调，正在检查表结构...');
    // 确保users表存在
    try {
      await db.execute('''
        CREATE TABLE IF NOT EXISTS users (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          email TEXT NOT NULL UNIQUE,
          password TEXT NOT NULL,
          created_at INTEGER NOT NULL
        )
      ''');
      print('users表创建或已存在');
      
      // 检查表是否真正存在
      final tables = await db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'");
      final tableNames = tables.map((e) => e['name']).toList();
      print('当前数据库中的表：$tableNames');
      
      if (tableNames.contains('users')) {
        print('✓ users表确认存在');
      } else {
        print('✗ users表不存在，尝试重新创建...');
        // 再次尝试创建
        await db.execute('''
          CREATE TABLE users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            created_at INTEGER NOT NULL
          )
        ''');
        print('✓ 重新创建users表成功');
      }
    } catch (e) {
      print('创建users表时出错: $e');
    }
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
