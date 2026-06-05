import 'package:flutter/material.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/models/category_model.dart';

/// 数据库服务类 - 提供本地SQLite数据库的初始化、操作和管理功能
/// 
/// 该类采用单例模式设计，负责应用程序的所有数据库操作，包括：
/// - 数据库初始化和升级
/// - 表创建和维护
/// - 数据增删改查
/// - 用户认证
/// - 同步状态管理
class DatabaseService {
  /// 单例实例
  static final DatabaseService instance = DatabaseService._internal();
  /// 数据库连接实例
  static Database? _database;

  /// 私有构造函数，防止外部实例化
  DatabaseService._internal();

  /// 工厂构造函数，返回单例实例
  factory DatabaseService() {
    return instance;
  }

  /// 获取数据库连接实例
  /// 
  /// 如果已有连接则直接返回，否则初始化新连接
  /// 
  /// 返回：
  /// - 初始化完成的数据库连接实例
  Future<Database> get database async {
    print('🔌 DatabaseService: 开始获取数据库连接...');
    if (_database != null) {
      print('🔌 DatabaseService: 使用现有数据库连接');
      return _database!;
    }
    print('🔌 DatabaseService: 数据库连接为空，开始初始化...');
    _database = await _initDB('finance_app.db');
    print('🔌 DatabaseService: 数据库初始化完成');
    return _database!;
  }

  /// 初始化数据库
  /// 
  /// 创建或打开数据库文件，并设置数据库版本和回调函数
  /// 
  /// 参数：
  /// - filePath: 数据库文件名
  /// 
  /// 返回：
  /// - 初始化完成的数据库实例
  Future<Database> _initDB(String filePath) async {
    try {
      // 获取数据库存储路径
      final dbPath = await getDatabasesPath();
      // 拼接完整数据库路径
      final path = join(dbPath, filePath);
      print('数据库路径: $path');

      // 打开数据库
      final db = await openDatabase(
        path, 
        version: 4, // 数据库版本号
        onCreate: _createDB,  // 数据库创建时的回调
        onUpgrade: _upgradeDB, // 数据库升级时的回调
        onDowngrade: onDatabaseDowngradeDelete, // 数据库降级时的回调
        onOpen: (db) {
          print('数据库打开成功');
        }
      );
      
      // 在返回数据库实例之前，确保所有必要的表都已经创建完成
      await _checkAndCreateMissingTables(db);
      // 然后检查并添加缺失的列
      await _addColumnsIfNotExists(db);
      
      return db;
    } catch (e) {
      print('数据库初始化失败: $e');
      rethrow;
    }
  }

  /// 创建数据库表结构
  /// 
  /// 在数据库首次创建时调用，创建所有必要的数据表和索引
  /// 
  /// 参数：
  /// - db: 数据库实例
  /// - version: 数据库版本号
  Future<void> _createDB(Database db, int version) async {
    print('开始创建数据库表，版本: $version');
    
    // 创建账单表 - 存储收支记录
    await db.execute('''
      CREATE TABLE IF NOT EXISTS bills (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        type INTEGER NOT NULL,                 -- 类型：1=收入, 2=支出
        categoryName TEXT NOT NULL,            -- 分类名称
        amount REAL NOT NULL,                  -- 金额
        transactionDate TEXT NOT NULL,         -- 交易日期
        remark TEXT,                           -- 备注
        imagePath TEXT                         -- 图片路径
      )
    ''');

    // 创建分类表 - 存储收支分类
    await db.execute('''
      CREATE TABLE IF NOT EXISTS categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        name TEXT NOT NULL,                    -- 分类名称
        type INTEGER NOT NULL,                 -- 类型：1=收入, 2=支出
        icon TEXT NOT NULL                     -- 图标名称
      )
    ''');

    // 创建预算表 - 存储预算设置
    await db.execute('''
      CREATE TABLE IF NOT EXISTS budgets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        categoryName TEXT NOT NULL,            -- 分类名称
        amount REAL NOT NULL,                  -- 预算金额
        year INTEGER NOT NULL,                 -- 年份
        month INTEGER NOT NULL,                -- 月份
        spent REAL DEFAULT 0,                  -- 已消费金额
        budgetName TEXT NOT NULL               -- 预算名称
      )
    ''');

    // 创建储蓄目标表 - 存储储蓄目标
    await db.execute('''
      CREATE TABLE IF NOT EXISTS saving_goals (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        name TEXT NOT NULL,                    -- 目标名称
        target_amount REAL NOT NULL,           -- 目标金额
        current_amount REAL NOT NULL,          -- 当前金额
        deadline INTEGER NOT NULL,             -- 截止日期（时间戳）
        description TEXT,                      -- 描述
        category_name TEXT NOT NULL DEFAULT '' -- 分类名称
      )
    ''');

    // 创建储蓄记录表 - 存储储蓄相关记录
    await db.execute('''
      CREATE TABLE IF NOT EXISTS saving_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        goal_id INTEGER,                       -- 关联的储蓄目标ID
        type TEXT NOT NULL,                    -- 类型：deposit=存入, withdraw=取出
        amount REAL NOT NULL,                  -- 金额
        created_at INTEGER NOT NULL,           -- 创建时间（时间戳）
        remark TEXT,                           -- 备注
        category TEXT                          -- 分类
      )
    ''');
    
    // 创建用户表 - 用于本地用户认证
    await db.execute('''
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        email TEXT NOT NULL UNIQUE,            -- 邮箱（唯一）
        password TEXT NOT NULL,                -- 密码
        created_at INTEGER NOT NULL            -- 创建时间（时间戳）
      )
    ''');

    // 创建同步状态表 - 存储同步相关信息
    await db.execute('''
      CREATE TABLE IF NOT EXISTS sync_status (
        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键，自动递增
        lastSyncTime INTEGER NOT NULL,         -- 最后同步时间（时间戳）
        isSyncing INTEGER NOT NULL DEFAULT 0   -- 是否正在同步：0=否, 1=是
      )
    ''');

    // 分类由用户自定义，不插入预设分类

    // 为账单表创建索引以优化查询性能
    await db.execute('CREATE INDEX IF NOT EXISTS idx_bills_transaction_date ON bills(transactionDate)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_bills_type ON bills(type)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_bills_category ON bills(categoryName)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_bills_type_date ON bills(type, transactionDate)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_bills_category_date ON bills(categoryName, transactionDate)');
    
    // 为分类表创建索引
    await db.execute('CREATE INDEX IF NOT EXISTS idx_categories_type ON categories(type)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name)');
    
    // 为预算表创建索引
    await db.execute('CREATE INDEX IF NOT EXISTS idx_budgets_category ON budgets(categoryName)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_budgets_year_month ON budgets(year, month)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_budgets_category_month ON budgets(categoryName, year, month)');
    
    // 为储蓄目标和记录表创建索引
    await db.execute('CREATE INDEX IF NOT EXISTS idx_saving_goals_deadline ON saving_goals(deadline)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_saving_records_goal_id ON saving_records(goal_id)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_saving_records_created_at ON saving_records(created_at)');
    await db.execute('CREATE INDEX IF NOT EXISTS idx_saving_records_type ON saving_records(type)');

    // 插入初始储蓄记录，保障统计页初始状态
    final now = DateTime.now();
    final sampleSavingRecords = [
      {
        'goal_id': null,
        'type': 'deposit',
        'amount': 200.0,
        'created_at': now.millisecondsSinceEpoch,
        'remark': '初始生活费存入',
        'category': '生活',
      },
      {
        'goal_id': null,
        'type': 'withdraw',
        'amount': 80.0,
        'created_at': now.subtract(const Duration(days: 2)).millisecondsSinceEpoch,
        'remark': '备用金支出',
        'category': '其他',
      },
    ];

    for (final record in sampleSavingRecords) {
      await db.insert('saving_records', record);
    }
  }

  /// 关闭数据库连接
  /// 
  /// 释放数据库资源，确保数据持久化
  Future<void> close() async {
    final db = await instance.database;
    db.close();
  }

  /// 加载所有分类数据
  /// 
  /// 从数据库中查询所有分类，并转换为Category对象列表
  /// 
  /// 返回：
  /// - 分类对象列表，如果没有数据或查询失败则返回空列表
  Future<List<Category>> loadCategories() async {
    try {
      final db = await instance.database;
      final List<Map<String, dynamic>> maps = await db.query('categories');
      
      if (maps.isNotEmpty) {
        return List.generate(maps.length, (i) => Category.fromMap(maps[i]));
      } else {
        // 返回空列表，不自动创建默认分类
        print('未找到分类数据，返回空列表');
        return [];
      }
    } catch (e) {
      print('加载分类数据失败: $e');
      // 发生错误时返回空列表，而不是默认分类
      return [];
    }
  }

  /// 更新最后同步时间
  /// 
  /// 将当前时间作为最后同步时间保存到数据库
  Future<void> updateLastSyncTime() async {
    final db = await instance.database;
    final now = DateTime.now().millisecondsSinceEpoch;
    // 查询同步状态表记录数
    final count = Sqflite.firstIntValue(
      await db.rawQuery('SELECT COUNT(*) FROM sync_status'),
    );

    if (count == 0) {
      // 无记录则插入新记录
      await db.insert('sync_status', {'lastSyncTime': now, 'isSyncing': 0});
    } else {
      // 有记录则更新第一条记录
      await db.update(
        'sync_status',
        {'lastSyncTime': now},
        where: 'id = ?',
        whereArgs: [1],
      );
    }
  }

  /// 获取最后同步时间
  /// 
  /// 从数据库中查询最后一次同步的时间
  /// 
  /// 返回：
  /// - 最后同步时间的时间戳，如果没有记录则返回null
  Future<int?> getLastSyncTime() async {
    final db = await instance.database;
    // 查询第一条同步状态记录
    final result = await db.query('sync_status', limit: 1);
    if (result.isEmpty) return null;
    return result.first['lastSyncTime'] as int;
  }

  /// 设置同步状态
  /// 
  /// 更新数据库中的同步状态
  /// 
  /// 参数：
  /// - isSyncing: 是否正在同步
  Future<void> setSyncing(bool isSyncing) async {
    final db = await instance.database;
    // 查询同步状态表记录数
    final count = Sqflite.firstIntValue(
      await db.rawQuery('SELECT COUNT(*) FROM sync_status'),
    );

    if (count == 0) {
      // 无记录则插入新记录
      await db.insert('sync_status', {
        'lastSyncTime': DateTime.now().millisecondsSinceEpoch,
        'isSyncing': isSyncing ? 1 : 0, // 将布尔值转换为整数存储
      });
    } else {
      // 有记录则更新第一条记录
      await db.update(
        'sync_status',
        {'isSyncing': isSyncing ? 1 : 0},
        where: 'id = ?',
        whereArgs: [1],
      );
    }
  }

  /// 检查是否正在同步
  /// 
  /// 从数据库中查询当前同步状态
  /// 
  /// 返回：
  /// - 是否正在同步的布尔值
  Future<bool> isSyncing() async {
    final db = await instance.database;
    // 查询第一条同步状态记录
    final result = await db.query('sync_status', limit: 1);
    if (result.isEmpty) return false;
    // 将整数转换为布尔值返回
    return (result.first['isSyncing'] as int) == 1;
  }

  /// 数据库升级方法 - 保护用户数据安全
  /// 
  /// 根据版本号差异执行相应的升级逻辑，确保数据安全
  /// 
  /// 参数：
  /// - db: 数据库实例
  /// - oldVersion: 旧版本号
  /// - newVersion: 新版本号
  Future<void> _upgradeDB(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < newVersion) {
      print('开始数据库升级从版本 $oldVersion 到 $newVersion');
      
      try {
        // 检查是否需要升级数据表结构
        if (oldVersion < 2) {
          // 对于版本1到版本2的升级，只添加新字段，不删除数据
          await _upgradeToVersion2(db);
        }
        
        if (oldVersion < 3) {
          // 版本2到版本3：添加索引以优化查询性能
          await _upgradeToVersion3(db);
        }
        
        if (oldVersion < 4) {
          // 版本3到版本4：确保budgets表有budgetName字段
          await _addColumnsIfNotExists(db);
        }
        
        print('数据库升级完成');
      } catch (e) {
        print('数据库升级失败: $e');
        // 升级失败时不应该删除数据，让应用继续使用旧版本
        rethrow;
      }
    }
  }

  /// 升级到版本2的逻辑 - 安全升级，保护现有数据
  /// 
  /// 检查并创建缺失的表，添加必要的字段
  /// 
  /// 参数：
  /// - db: 数据库实例
  Future<void> _upgradeToVersion2(Database db) async {
    try {
      // 检查表是否存在，如果不存在则创建
      final tablesToCheck = [
        'bills',
        'categories', 
        'budgets',
        'saving_goals',
        'saving_records',
        'sync_status'
      ];

      for (final tableName in tablesToCheck) {
        final tableExists = await _checkTableExists(db, tableName);
        if (!tableExists) {
          print('表 $tableName 不存在，正在创建...');
          // 重新创建缺失的表
          await _createMissingTable(db, tableName);
        }
      }
      
      // 对于已有表，只添加新字段，不删除现有数据
      await _addColumnsIfNotExists(db);
      
    } catch (e) {
      print('升级到版本2失败: $e');
      rethrow;
    }
  }

  /// 检查表是否存在
  /// 
  /// 查询SQLite系统表，检查指定表是否存在
  /// 
  /// 参数：
  /// - db: 数据库实例
  /// - tableName: 表名
  /// 
  /// 返回：
  /// - 表是否存在的布尔值
  Future<bool> _checkTableExists(Database db, String tableName) async {
    try {
      final result = await db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        [tableName]
      );
      return result.isNotEmpty;
    } catch (e) {
      print('检查表 $tableName 是否存在时出错: $e');
      return false;
    }
  }

  /// 创建缺失的表
  /// 
  /// 根据表名创建相应的数据表
  /// 
  /// 参数：
  /// - db: 数据库实例
  /// - tableName: 表名
  Future<void> _createMissingTable(Database db, String tableName) async {
    switch (tableName) {
      case 'bills':
        await db.execute('''
          CREATE TABLE bills (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            type INTEGER NOT NULL,
            categoryName TEXT NOT NULL,
            amount REAL NOT NULL,
            transactionDate TEXT NOT NULL,
            remark TEXT,
            imagePath TEXT
          )
        ''');
        break;
      case 'categories':
        await db.execute('''
          CREATE TABLE categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            type INTEGER NOT NULL,
            icon TEXT NOT NULL
          )
        ''');
        break;
      case 'budgets':
        await db.execute('''
          CREATE TABLE budgets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            categoryName TEXT NOT NULL,
            amount REAL NOT NULL,
            year INTEGER NOT NULL,
            month INTEGER NOT NULL,
            spent REAL DEFAULT 0,
            budgetName TEXT NOT NULL
          )
        ''');
        break;
      case 'saving_goals':
        await db.execute('''
          CREATE TABLE saving_goals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            target_amount REAL NOT NULL,
            current_amount REAL NOT NULL,
            deadline INTEGER NOT NULL,
            description TEXT,
            category_name TEXT NOT NULL DEFAULT ''
          )
        ''');
        break;
      case 'saving_records':
        await db.execute('''
          CREATE TABLE saving_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            goal_id INTEGER,
            type TEXT NOT NULL,
            amount REAL NOT NULL,
            created_at INTEGER NOT NULL,
            remark TEXT,
            category TEXT
          )
        ''');
        break;
      case 'sync_status':
        await db.execute('''
          CREATE TABLE sync_status (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            lastSyncTime INTEGER NOT NULL,
            isSyncing INTEGER NOT NULL DEFAULT 0
          )
        ''');
        break;
      case 'users':
        await db.execute('''
          CREATE TABLE users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            created_at INTEGER NOT NULL
          )
        ''');
        break;
    }
  }

  /// 安全地添加新字段（如果不存在）
  /// 
  /// 检查并添加表中缺失的字段
  /// 
  /// 参数：
  /// - db: 数据库实例
  Future<void> _addColumnsIfNotExists(Database db) async {
    try {
      print('开始检查并添加缺失的列...');
      
      // 检查并添加budgets表中缺失的budgetName字段
      print('检查budgets表结构...');
      final budgetsColumns = await db.rawQuery("PRAGMA table_info(budgets)");
      print('budgets表现有列: ${budgetsColumns.map((column) => column['name']).toList()}');
      final hasBudgetNameColumn = budgetsColumns.any((column) => column['name'] == 'budgetName');
      
      if (!hasBudgetNameColumn) {
        await db.execute('ALTER TABLE budgets ADD COLUMN budgetName TEXT NOT NULL DEFAULT ""');
        print('已为budgets表添加budgetName字段');
      } else {
        print('budgets表已有budgetName字段，跳过添加');
      }
      
      // 检查并添加bills表中缺失的imagePath字段
      print('检查bills表结构...');
      final billsColumns = await db.rawQuery("PRAGMA table_info(bills)");
      print('bills表现有列: ${billsColumns.map((column) => column['name']).toList()}');
      final hasImagePathColumn = billsColumns.any((column) => column['name'] == 'imagePath');
      
      if (!hasImagePathColumn) {
        await db.execute('ALTER TABLE bills ADD COLUMN imagePath TEXT');
        print('已为bills表添加imagePath字段');
      } else {
        print('bills表已有imagePath字段，跳过添加');
      }
      
      print('数据表结构检查完成');
    } catch (e) {
      print('添加新字段时出错: $e');
    }
  }
  
  /// 检查并创建所有缺失的数据表
  Future<void> _checkAndCreateMissingTables(Database db) async {
    try {
      print('开始检查并创建缺失的数据表...');
      
      // 所有必需的数据表
      final allTables = [
        'bills',
        'categories',
        'budgets',
        'saving_goals',
        'saving_records',
        'sync_status',
        'users'
      ];
      
      for (final tableName in allTables) {
        final tableExists = await _checkTableExists(db, tableName);
        if (!tableExists) {
          print('表 $tableName 不存在，正在创建...');
          await _createMissingTable(db, tableName);
        } else {
          print('表 $tableName 已存在，跳过创建');
        }
      }
      
      print('缺失数据表检查和创建完成');
    } catch (e) {
      print('检查并创建缺失数据表时出错: $e');
      rethrow;
    }
  }
  
  /// 升级到版本3：添加数据库索引以优化查询性能
  /// 
  /// 为各个表添加必要的索引，提高查询效率
  /// 
  /// 参数：
  /// - db: 数据库实例
  Future<void> _upgradeToVersion3(Database db) async {
    try {
      print('开始添加数据库索引以优化查询性能...');
      
      // 为账单表添加索引
      await _addIndexIfNotExists(db, 'idx_bills_transaction_date', 'CREATE INDEX idx_bills_transaction_date ON bills(transactionDate)');
      await _addIndexIfNotExists(db, 'idx_bills_type', 'CREATE INDEX idx_bills_type ON bills(type)');
      await _addIndexIfNotExists(db, 'idx_bills_category', 'CREATE INDEX idx_bills_category ON bills(categoryName)');
      await _addIndexIfNotExists(db, 'idx_bills_type_date', 'CREATE INDEX idx_bills_type_date ON bills(type, transactionDate)');
      await _addIndexIfNotExists(db, 'idx_bills_category_date', 'CREATE INDEX idx_bills_category_date ON bills(categoryName, transactionDate)');
      
      // 为分类表添加索引
      await _addIndexIfNotExists(db, 'idx_categories_type', 'CREATE INDEX idx_categories_type ON categories(type)');
      await _addIndexIfNotExists(db, 'idx_categories_name', 'CREATE INDEX idx_categories_name ON categories(name)');
      
      // 为预算表添加索引
      await _addIndexIfNotExists(db, 'idx_budgets_category', 'CREATE INDEX idx_budgets_category ON budgets(categoryName)');
      await _addIndexIfNotExists(db, 'idx_budgets_year_month', 'CREATE INDEX idx_budgets_year_month ON budgets(year, month)');
      await _addIndexIfNotExists(db, 'idx_budgets_category_month', 'CREATE INDEX idx_budgets_category_month ON budgets(categoryName, year, month)');
      
      // 为储蓄目标和记录表添加索引
      await _addIndexIfNotExists(db, 'idx_saving_goals_deadline', 'CREATE INDEX idx_saving_goals_deadline ON saving_goals(deadline)');
      await _addIndexIfNotExists(db, 'idx_saving_records_goal_id', 'CREATE INDEX idx_saving_records_goal_id ON saving_records(goal_id)');
      await _addIndexIfNotExists(db, 'idx_saving_records_created_at', 'CREATE INDEX idx_saving_records_created_at ON saving_records(created_at)');
      await _addIndexIfNotExists(db, 'idx_saving_records_type', 'CREATE INDEX idx_saving_records_type ON saving_records(type)');
      
      print('数据库索引添加完成');
    } catch (e) {
      print('升级到版本3失败: $e');
      rethrow;
    }
  }
  
  /// 安全地添加索引（如果不存在）
  /// 
  /// 检查指定索引是否存在，不存在则创建
  /// 
  /// 参数：
  /// - db: 数据库实例
  /// - indexName: 索引名称
  /// - indexSQL: 创建索引的SQL语句
  Future<void> _addIndexIfNotExists(Database db, String indexName, String indexSQL) async {
    try {
      final result = await db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
        [indexName]
      );
      
      if (result.isEmpty) {
        await db.execute(indexSQL);
        print('已创建索引: $indexName');
      } else {
        print('索引 $indexName 已存在，跳过创建');
      }
    } catch (e) {
      print('创建索引 $indexName 时出错: $e');
      // 不抛出异常，继续执行其他索引的创建
    }
  }
  
  /// 直接删除数据库文件的方法，用于测试
  /// 
  /// 删除应用的数据库文件，用于测试场景
  Future<void> deleteDatabaseFile() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'finance_app.db');
    await deleteDatabase(path);
  }

  /// 在本地数据库中注册新用户
  /// 
  /// 将新用户信息保存到数据库
  /// 
  /// 参数：
  /// - email: 用户邮箱
  /// - password: 用户密码（注意：生产环境应加密存储）
  /// 
  /// 返回：
  /// - 新注册用户的ID
  Future<int> registerUser(String email, String password) async {
    try {
      final db = await instance.database;
      final now = DateTime.now().millisecondsSinceEpoch;
      
      // 插入新用户
      final id = await db.insert('users', {
        'email': email,
        'password': password, // In production, you should hash the password
        'created_at': now,
      });
      
      print('用户注册成功，ID: $id');
      return id;
    } catch (e) {
      print('用户注册失败: $e');
      rethrow;
    }
  }

  /// 通过本地数据库验证用户登录
  /// 
  /// 检查用户邮箱和密码是否匹配
  /// 
  /// 参数：
  /// - email: 用户邮箱
  /// - password: 用户密码
  /// 
  /// 返回：
  /// - 匹配的用户信息Map，如果验证失败则返回null
  Future<Map<String, dynamic>?> loginUser(String email, String password) async {
    try {
      final db = await instance.database;
      
      // 根据邮箱和密码查询用户
      final result = await db.query(
        'users',
        where: 'email = ? AND password = ?',
        whereArgs: [email, password],
      );
      
      if (result.isNotEmpty) {
        print('用户登录成功');
        return result.first;
      } else {
        print('用户登录失败：邮箱或密码错误');
        return null;
      }
    } catch (e) {
      print('用户登录查询失败: $e');
      rethrow;
    }
  }

  /// 检查指定邮箱的用户是否已存在
  /// 
  /// 查询数据库中是否已存在该邮箱的用户
  /// 
  /// 参数：
  /// - email: 要检查的邮箱
  /// 
  /// 返回：
  /// - 用户是否存在的布尔值
  Future<bool> userExists(String email) async {
    try {
      final db = await instance.database;
      
      final result = await db.query(
        'users',
        where: 'email = ?',
        whereArgs: [email],
      );
      
      return result.isNotEmpty;
    } catch (e) {
      print('检查用户是否存在失败: $e');
      return false;
    }
  }

  /// 清除数据库和SharedPreferences中的所有数据
  /// 
  /// 删除数据库中所有表的数据，并清除SharedPreferences中的持久化数据
  Future<void> clearAllData() async {
    try {
      // 1. 清除SQLite数据库中的所有数据
      final db = await instance.database;
      await db.transaction((txn) async {
        await txn.delete('bills');
        await txn.delete('categories');
        await txn.delete('budgets');
        await txn.delete('saving_goals');
        await txn.delete('saving_records');
        await txn.delete('sync_status');
      });
      
      // 2. 清除SharedPreferences中的持久化数据
      final prefs = await SharedPreferences.getInstance();
      
      // 清除所有应用相关的数据键
      final removalFutures = <Future<void>>[
        prefs.remove('saving_goals'),
        prefs.remove('saving_records'),
        prefs.remove('transactions'),
        prefs.remove('budgets'),
        prefs.remove('categories'),
        prefs.remove('user_settings'),
        prefs.remove('app_theme'),
        prefs.remove('language_setting'),
        prefs.remove('notification_settings'),
        prefs.remove('backup_settings'),
      ];
      
      // 使用Future.wait的eagerError:false参数确保一个SharedPreferences键的移除失败不会影响其他键
      await Future.wait(removalFutures, eagerError: false);
      
      print('已完全清除SQLite数据库和SharedPreferences中的所有数据');
    } catch (e) {
      print('清除数据时发生错误: $e');
      rethrow;
    }
  }
}
