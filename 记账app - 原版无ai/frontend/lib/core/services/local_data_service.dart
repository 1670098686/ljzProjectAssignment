import 'dart:developer' as developer;
import '../database/database_service.dart';
import '../services/offline_sync_service.dart';
import '../../data/models/bill_model.dart';
import '../../data/models/category_model.dart';
import '../../data/models/budget_model.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/models/saving_record_model.dart';


/// 本地数据服务 - 负责将数据保存到本地SQLite数据库
/// 集成离线数据同步功能
class LocalDataService {
  final DatabaseService _databaseService = DatabaseService.instance;
  final OfflineSyncService _offlineSyncService = OfflineSyncService();

  /// 保存账单到本地数据库（集成离线同步）
  Future<Bill> saveBillToLocal(
    Bill bill, {
    bool enableSync = true,
  }) async {
    try {
      final db = await _databaseService.database;
      print('💾 LocalDataService: 开始保存账单到数据库');
      print('💾 LocalDataService: 账单数据: ${bill.toJson()}');
      print('   📷 待保存的图片路径: ${bill.imagePath ?? "null"}');
      
      late Bill savedBill;
      
      // 使用事务确保数据一致性
      await db.transaction((txn) async {
        if (bill.id != null) {
          // 如果账单有ID，尝试更新现有记录
          final updatedRows = await txn.update(
            'bills',
            bill.toJson(),
            where: 'id = ?',
            whereArgs: [bill.id],
          );

          if (updatedRows == 0) {
            // 如果没有更新到任何行，说明账单不存在，插入新记录
            final id = await txn.insert('bills', bill.toJson());
            print('💾 LocalDataService: 账单插入成功，新ID: $id');
            developer.log('账单已插入到本地数据库，ID: $id', name: 'LocalDataService');
            // 创建带有生成id的新Bill对象
            savedBill = bill.copyWith(id: id);
          } else {
            print('💾 LocalDataService: 账单更新成功，影响行数: $updatedRows');
            developer.log('账单已更新到本地数据库', name: 'LocalDataService');
            // 使用原始bill对象，因为更新操作不需要修改id
            savedBill = bill;
          }
        } else {
          // 如果账单没有ID，插入新记录
          final id = await txn.insert('bills', bill.toJson());
          print('💾 LocalDataService: 账单插入成功，新ID: $id');
          developer.log('账单已插入到本地数据库，ID: $id', name: 'LocalDataService');
          
          // 重点修复：将生成的id赋值给bill对象，创建带有生成id的新Bill对象
          savedBill = bill.copyWith(id: id);
          
          // 如果启用同步，将创建操作添加到待同步队列
          if (enableSync) {
            await _offlineSyncService.addPendingOperation(
              OperationType.createTransaction,
              savedBill.toJson(),
            );
          }
        }
      });
      
      // 查询刚刚保存的记录，确保数据正确保存
      final verifyResults = await db.query(
        'bills',
        where: 'id = ?',
        whereArgs: [savedBill.id],
        limit: 1,
      );
      
      if (verifyResults.isNotEmpty) {
        print('✅ LocalDataService: 账单保存成功，验证通过');
      } else {
        print('⚠️ LocalDataService: 账单保存成功，但验证失败');
      }
      
      // 返回带有生成id的Bill对象
      return savedBill;
    } catch (e, stackTrace) {
      print('❌ LocalDataService: 保存账单到本地数据库失败: $e');
      developer.log(
        '保存账单到本地数据库失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      // 重新抛出异常，让调用方处理错误
      rethrow;
    }
  }

  /// 保存分类到本地数据库（集成离线同步）
  Future<void> saveCategoryToLocal(
    Category category, {
    bool enableSync = true,
  }) async {
    try {
      final db = await _databaseService.database;

      if (category.id != null) {
        // 如果分类有ID，尝试更新现有记录
        final updatedRows = await db.update(
          'categories',
          {
            'name': category.name,
            'type': category.type,
            'icon': category.icon ?? '',
          },
          where: 'id = ?',
          whereArgs: [category.id],
        );

        if (updatedRows == 0) {
          // 如果没有更新到任何行，说明分类不存在，插入新记录
          await db.insert('categories', {
            'id': category.id,
            'name': category.name,
            'type': category.type,
            'icon': category.icon ?? '',
          });
          developer.log('分类已插入到本地数据库', name: 'LocalDataService');
        } else {
          developer.log('分类已更新到本地数据库', name: 'LocalDataService');
        }

        // 如果启用同步且不在离线模式，尝试同步更新操作
        if (enableSync && !_offlineSyncService.isOfflineMode) {
          try {
            // 这里可以调用实际的同步方法
          } catch (e) {
            await _offlineSyncService.addPendingOperation(
              OperationType.updateCategory,
              category.toJson(),
            );
          }
        }
      } else {
        // 如果分类没有ID，插入新记录
        await db.insert('categories', {
          'name': category.name,
          'type': category.type,
          'icon': category.icon ?? '',
        });
        developer.log('分类已插入到本地数据库', name: 'LocalDataService');

        // 如果启用同步，将创建操作添加到待同步队列
        if (enableSync) {
          await _offlineSyncService.addPendingOperation(
            OperationType.createCategory,
            category.toJson(),
          );
        }
      }
    } catch (e, stackTrace) {
      developer.log(
        '保存分类到本地数据库失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 保存预算到本地数据库
  Future<int> saveBudgetToLocal(Budget budget) async {
    try {
      final db = await _databaseService.database;
      int id = budget.id ?? -1;

      if (budget.id != null) {
        // 如果预算有ID，尝试更新现有记录
        final updatedRows = await db.update(
          'budgets',
          {
            'categoryName': budget.categoryName,
            'amount': budget.amount,
            'year': budget.year,
            'month': budget.month,
            'spent': budget.spent,
            'budgetName': budget.budgetName,
          },
          where: 'id = ?',
          whereArgs: [budget.id],
        );

        if (updatedRows == 0) {
          // 如果没有更新到任何行，说明预算不存在，插入新记录
          id = await db.insert('budgets', {
            'id': budget.id,
            'categoryName': budget.categoryName,
            'amount': budget.amount,
            'year': budget.year,
            'month': budget.month,
            'spent': budget.spent,
            'budgetName': budget.budgetName,
          });
          developer.log('预算已插入到本地数据库，ID: $id', name: 'LocalDataService');
        } else {
          developer.log('预算已更新到本地数据库，ID: $id', name: 'LocalDataService');
        }
      } else {
        // 如果预算没有ID，插入新记录
        id = await db.insert('budgets', {
          'categoryName': budget.categoryName,
          'amount': budget.amount,
          'year': budget.year,
          'month': budget.month,
          'spent': budget.spent,
          'budgetName': budget.budgetName,
        });
        developer.log('预算已插入到本地数据库，ID: $id', name: 'LocalDataService');
      }
      return id;
    } catch (e, stackTrace) {
      developer.log(
        '保存预算到本地数据库失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 更新本地预算
  Future<void> updateBudgetInLocal(Budget budget) async {
    try {
      final db = await _databaseService.database;

      await db.update(
        'budgets',
        {
          'categoryName': budget.categoryName,
          'amount': budget.amount,
          'year': budget.year,
          'month': budget.month,
          'spent': budget.spent,
          'budgetName': budget.budgetName,
        },
        where: 'id = ?',
        whereArgs: [budget.id],
      );

      developer.log('预算已更新到本地数据库', name: 'LocalDataService');
    } catch (e, stackTrace) {
      developer.log(
        '更新预算到本地数据库失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 从本地删除预算（同时检查并删除未使用的关联分类）
  Future<void> deleteBudgetFromLocal(int id) async {
    try {
      final db = await _databaseService.database;
      
      // 先获取要删除的预算信息，用于后续检查分类使用情况
      final budgetToDelete = await db.query(
        'budgets',
        where: 'id = ?',
        whereArgs: [id],
      );
      
      if (budgetToDelete.isNotEmpty) {
        final deletedBudget = budgetToDelete.first;
        final categoryName = deletedBudget['categoryName'] as String?;
        final budgetName = deletedBudget['budgetName'] as String?;
        
        // 如果预算有预算名称，创建复合分类名进行检查
        String? combinedCategoryName;
        if (budgetName != null && categoryName != null) {
          combinedCategoryName = '$budgetName-$categoryName';
        }
        
        // 删除预算记录
        await db.delete('budgets', where: 'id = ?', whereArgs: [id]);
        
        developer.log('预算已从本地数据库删除', name: 'LocalDataService');
        
        // 检查并删除未使用的分类
        await _checkAndDeleteUnusedCategoryForBudget(categoryName, combinedCategoryName);
      } else {
        // 如果找不到预算记录，仍然执行删除操作
        await db.delete('budgets', where: 'id = ?', whereArgs: [id]);
        developer.log('预算已从本地数据库删除（记录不存在）', name: 'LocalDataService');
      }
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库删除预算失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 检查并删除预算相关的未使用分类
  Future<void> _checkAndDeleteUnusedCategoryForBudget(String? categoryName, String? combinedCategoryName) async {
    try {
      final db = await _databaseService.database;
      
      // 1. 检查基础分类名是否还在其他预算中使用
      if (categoryName != null) {
        // 检查是否在交易记录中使用（这是最重要的检查）
        final billCount = await db.rawQuery(
          'SELECT COUNT(*) as count FROM bills WHERE categoryName = ?',
          [categoryName],
        );
        final billUseCount = billCount.first['count'] as int? ?? 0;
        
        if (billUseCount > 0) {
          // 分类在交易记录中被使用，不能删除
          developer.log('基础分类 $categoryName 在交易记录中使用，保留不删除', name: 'LocalDataService');
        } else {
          // 检查分类名是否还在其他预算中使用
          final budgetCount = await db.rawQuery(
            'SELECT COUNT(*) as count FROM budgets WHERE categoryName = ?',
            [categoryName],
          );
          final count = budgetCount.first['count'] as int? ?? 0;
          
          if (count == 0) {
            // 检查是否在储蓄目标中使用
            final savingGoalCount = await db.rawQuery(
              'SELECT COUNT(*) as count FROM saving_goals WHERE category_name = ?',
              [categoryName],
            );
            final savingGoalUseCount = savingGoalCount.first['count'] as int? ?? 0;
            
            if (savingGoalUseCount == 0) {
              // 查找并删除基础分类
              final categoryToDelete = await db.query(
                'categories',
                where: 'name = ? AND type = ?',
                whereArgs: [categoryName, 2], // 支出类型
              );
              
              if (categoryToDelete.isNotEmpty) {
                final categoryId = categoryToDelete.first['id'] as int;
                await db.delete('categories', where: 'id = ?', whereArgs: [categoryId]);
                developer.log('未使用的预算基础分类已删除: $categoryName', name: 'LocalDataService');
              }
            } else {
              developer.log('基础分类 $categoryName 在储蓄目标中使用，保留不删除', name: 'LocalDataService');
            }
          } else {
            developer.log('基础分类 $categoryName 在其他预算中使用，保留不删除', name: 'LocalDataService');
          }
        }
      }
      
      // 2. 检查复合分类名是否还在其他预算中使用
      if (combinedCategoryName != null) {
        // 检查是否在交易记录中使用（这是最重要的检查）
        final billCount = await db.rawQuery(
          'SELECT COUNT(*) as count FROM bills WHERE categoryName = ?',
          [combinedCategoryName],
        );
        final billUseCount = billCount.first['count'] as int? ?? 0;
        
        if (billUseCount > 0) {
          // 分类在交易记录中被使用，不能删除
          developer.log('组合分类 $combinedCategoryName 在交易记录中使用，保留不删除', name: 'LocalDataService');
        } else {
          // 检查分类名是否还在其他预算中使用
          final combinedBudgetCount = await db.rawQuery(
            'SELECT COUNT(*) as count FROM budgets WHERE categoryName = ?',
            [combinedCategoryName],
          );
          final combinedCount = combinedBudgetCount.first['count'] as int? ?? 0;
          
          if (combinedCount == 0) {
            // 检查是否在储蓄目标中使用
            final savingGoalCount = await db.rawQuery(
              'SELECT COUNT(*) as count FROM saving_goals WHERE category_name = ?',
              [combinedCategoryName],
            );
            final savingGoalUseCount = savingGoalCount.first['count'] as int? ?? 0;
            
            if (savingGoalUseCount == 0) {
              // 查找并删除复合分类
              final categoryToDelete = await db.query(
                'categories',
                where: 'name = ? AND type = ?',
                whereArgs: [combinedCategoryName, 2], // 支出类型
              );
              
              if (categoryToDelete.isNotEmpty) {
                final categoryId = categoryToDelete.first['id'] as int;
                await db.delete('categories', where: 'id = ?', whereArgs: [categoryId]);
                developer.log('未使用的预算组合分类已删除: $combinedCategoryName', name: 'LocalDataService');
              }
            } else {
              developer.log('组合分类 $combinedCategoryName 在储蓄目标中使用，保留不删除', name: 'LocalDataService');
            }
          } else {
            developer.log('组合分类 $combinedCategoryName 在其他预算中使用，保留不删除', name: 'LocalDataService');
          }
        }
      }
    } catch (e, stackTrace) {
      developer.log(
        '检查并删除未使用的预算分类失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      // 不抛出异常，避免影响主删除流程
    }
  }

  /// 保存储蓄目标到本地数据库（集成离线同步）
  Future<void> saveSavingGoalToLocal(
    SavingGoal goal, {
    bool enableSync = true,
  }) async {
    try {
      final db = await _databaseService.database;

      if (goal.id != null) {
        // 如果目标有ID，尝试更新现有记录
        final updatedRows = await db.update(
          'saving_goals',
          {
            'name': goal.name,
            'target_amount': goal.targetAmount,
            'current_amount': goal.currentAmount,
            'deadline': goal.deadline.millisecondsSinceEpoch,
            'description': goal.description ?? '',
            'category_name': goal.categoryName,
          },
          where: 'id = ?',
          whereArgs: [goal.id],
        );

        if (updatedRows == 0) {
          // 如果没有更新到任何行，说明目标不存在，插入新记录
          await db.insert('saving_goals', {
            'id': goal.id,
            'name': goal.name,
            'target_amount': goal.targetAmount,
            'current_amount': goal.currentAmount,
            'deadline': goal.deadline.millisecondsSinceEpoch,
            'description': goal.description ?? '',
            'category_name': goal.categoryName,
          });
          developer.log('储蓄目标已插入到本地数据库', name: 'LocalDataService');
        } else {
          developer.log('储蓄目标已更新到本地数据库', name: 'LocalDataService');
        }

        // 如果启用同步且不在离线模式，更新同步操作
        if (enableSync && !_offlineSyncService.isOfflineMode) {
          // 尝试直接同步到服务器
          // 如果失败，将操作添加到待同步队列
          try {
            // 这里可以调用实际的同步方法
          } catch (e) {
            await _offlineSyncService.addPendingOperation(
              OperationType.updateSavingGoal,
              goal.toJson(),
            );
          }
        }
      } else {
        // 如果目标没有ID，插入新记录
        await db.insert('saving_goals', {
          'name': goal.name,
          'target_amount': goal.targetAmount,
          'current_amount': goal.currentAmount,
          'deadline': goal.deadline.millisecondsSinceEpoch,
          'description': goal.description ?? '',
          'category_name': goal.categoryName,
        });
        developer.log('储蓄目标已插入到本地数据库', name: 'LocalDataService');

        // 如果启用同步，将操作添加到待同步队列
        if (enableSync) {
          await _offlineSyncService.addPendingOperation(
            OperationType.createSavingGoal,
            goal.toJson(),
          );
        }
      }
    } catch (e, stackTrace) {
      developer.log(
        '保存储蓄目标到本地数据库失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 批量保存数据到本地数据库
  Future<void> saveAllDataToLocal({
    List<Bill>? bills,
    List<Category>? categories,
    List<Budget>? budgets,
    List<SavingGoal>? savingGoals,
  }) async {
    final db = await _databaseService.database;

    await db.transaction((txn) async {
      if (bills != null && bills.isNotEmpty) {
        for (final bill in bills) {
          await txn.insert('bills', {
            'type': bill.type,
            'categoryName': bill.categoryName,
            'amount': bill.amount,
            'transactionDate': bill.transactionDate,
            'remark': bill.remark ?? '',
            'imagePath': bill.imagePath,
          });
        }
      }

      if (categories != null && categories.isNotEmpty) {
        for (final category in categories) {
          await txn.insert('categories', {
            'name': category.name,
            'type': category.type,
            'icon': category.icon,
          });
        }
      }

      if (budgets != null && budgets.isNotEmpty) {
        for (final budget in budgets) {
          await txn.insert('budgets', {
            'categoryName': budget.categoryName,
            'amount': budget.amount,
            'year': budget.year,
            'month': budget.month,
            'spent': budget.spent,
            'budgetName': budget.budgetName,
          });
        }
      }

      if (savingGoals != null && savingGoals.isNotEmpty) {
        for (final goal in savingGoals) {
          await txn.insert('saving_goals', {
            'name': goal.name,
            'target_amount': goal.targetAmount,
            'current_amount': goal.currentAmount,
            'deadline': goal.deadline.millisecondsSinceEpoch,
            'description': goal.description ?? '',
            'category_name': goal.categoryName,
          });
        }
      }
    });

    developer.log('批量数据已保存到本地数据库', name: 'LocalDataService');
  }

  /// 从本地数据库获取所有数据
  Future<Map<String, dynamic>> getAllDataFromLocal() async {
    final db = await _databaseService.database;

    final bills = await db.query('bills');
    final categories = await db.query('categories');
    final budgets = await db.query('budgets');
    final savingGoals = await db.query('saving_goals');

    return {
      'bills': bills,
      'categories': categories,
      'budgets': budgets,
      'savingGoals': savingGoals,
    };
  }

  /// 从本地数据库获取账单数据
  Future<List<Bill>> getBillsFromLocal({
    String? startDate,
    String? endDate,
    int? type,
    String? categoryName,
  }) async {
    try {
      final db = await _databaseService.database;

      print('💾 LocalDataService开始获取账单数据...');
      print('💾 数据库连接状态: ${db.isOpen ? "已连接" : "未连接"}');

      List<Map<String, dynamic>> results;

      // 构建查询条件
      String whereClause = '';
      List<dynamic> whereArgs = [];

      if (startDate != null) {
        whereClause +=
            '${whereClause.isEmpty ? '' : ' AND '}transactionDate >= ?';
        whereArgs.add(startDate);
      }

      if (endDate != null) {
        whereClause +=
            '${whereClause.isEmpty ? '' : ' AND '}transactionDate <= ?';
        whereArgs.add(endDate);
      }

      if (type != null) {
        whereClause += '${whereClause.isEmpty ? '' : ' AND '}type = ?';
        whereArgs.add(type);
      }

      if (categoryName != null) {
        whereClause +=
            '${whereClause.isEmpty ? '' : ' AND '}categoryName = ?';
        whereArgs.add(categoryName);
      }

      print('💾 查询条件: whereClause="$whereClause", whereArgs=$whereArgs');

      if (whereClause.isEmpty) {
        results = await db.query('bills', orderBy: 'transactionDate DESC');
        print('💾 执行简单查询: SELECT * FROM bills ORDER BY transactionDate DESC');
      } else {
        results = await db.query(
          'bills',
          where: whereClause,
          whereArgs: whereArgs,
          orderBy: 'transactionDate DESC',
        );
        print('💾 执行条件查询: SELECT * FROM bills WHERE $whereClause ORDER BY transactionDate DESC');
      }

      print('💾 查询结果: 共${results.length}条记录');
      
      // 打印原始数据用于调试
      results.forEach((map) {
        print('💾 原始数据: id=${map['id']}, type=${map['type']}, categoryName=${map['categoryName']}, amount=${map['amount']}, transactionDate=${map['transactionDate']}');
        print('   📷 图片路径: ${map['imagePath'] ?? "null"}');
        print('   📝 备注: ${map['remark'] ?? "null"}');
      });

      final bills = results
          .map(
            (map) => Bill(
              id: map['id'] as int?,
              type: map['type'] as int,
              categoryName: map['categoryName'] as String,
              amount: (map['amount'] as num).toDouble(),
              transactionDate: map['transactionDate'] as String,
              remark: map['remark'] as String?,
              imagePath: map['imagePath'] as String?,
            ),
          )
          .toList();

      print('✅ LocalDataService返回${bills.length}条账单数据');
      return bills;
    } catch (e, stackTrace) {
      print('❌ LocalDataService从本地数据库获取账单失败: $e');
      developer.log(
        '从本地数据库获取账单失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 更新账单的分类名称
  Future<void> updateBillsCategoryName(String oldCategoryName, String newCategoryName) async {
    try {
      final db = await _databaseService.database;
      
      // 更新所有使用旧分类名的账单
      final updatedRows = await db.update(
        'bills',
        {'categoryName': newCategoryName},
        where: 'categoryName = ?',
        whereArgs: [oldCategoryName],
      );
      
      print('💾 LocalDataService: 更新了 $updatedRows 条账单的分类名，从 $oldCategoryName 到 $newCategoryName');
      developer.log('更新了 $updatedRows 条账单的分类名', name: 'LocalDataService');
    } catch (e, stackTrace) {
      print('❌ LocalDataService更新账单分类名失败: $e');
      developer.log(
        '更新账单分类名失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }
  
  /// 用远程数据刷新本地账单缓存
  Future<void> replaceBillsFromRemote(List<Bill> bills) async {
    try {
      final db = await _databaseService.database;
      await db.transaction((txn) async {
        await txn.delete('bills');
        for (final bill in bills) {
          await txn.insert('bills', {
            'id': bill.id,
            'type': bill.type,
            'categoryName': bill.categoryName,
            'amount': bill.amount,
            'transactionDate': bill.transactionDate,
            'remark': bill.remark ?? '',
            'imagePath': bill.imagePath,
          });
        }
      });
      developer.log(
        '已使用远程账单刷新本地缓存，共 ${bills.length} 条',
        name: 'LocalDataService',
      );
    } catch (e, stackTrace) {
      developer.log(
        '刷新本地账单缓存失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
    }
  }

  /// 从本地数据库获取分类数据
  Future<List<Category>> getCategoriesFromLocal({int? type}) async {
    try {
      final db = await _databaseService.database;

      List<Map<String, dynamic>> results;

      if (type != null) {
        results = await db.query(
          'categories',
          where: 'type = ?',
          whereArgs: [type],
          orderBy: 'name ASC',
        );
      } else {
        results = await db.query('categories', orderBy: 'name ASC');
      }

      return results
          .map(
            (map) => Category(
              id: map['id'] as int?,
              name: map['name'] as String,
              type: map['type'] as int,
              icon: map['icon'] as String,
            ),
          )
          .toList();
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库获取分类失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 从本地数据库获取预算数据
  Future<List<Budget>> getBudgetsFromLocal({int? year, int? month}) async {
    try {
      final db = await _databaseService.database;

      List<Map<String, dynamic>> results;

      String whereClause = '';
      List<dynamic> whereArgs = [];

      if (year != null) {
        whereClause += '${whereClause.isEmpty ? '' : ' AND '}year = ?';
        whereArgs.add(year);
      }

      if (month != null) {
        whereClause += '${whereClause.isEmpty ? '' : ' AND '}month = ?';
        whereArgs.add(month);
      }

      if (whereClause.isEmpty) {
        results = await db.query('budgets', orderBy: 'year DESC, month DESC');
      } else {
        results = await db.query(
          'budgets',
          where: whereClause,
          whereArgs: whereArgs,
          orderBy: 'year DESC, month DESC',
        );
      }

      return results
          .map(
            (map) => Budget(
              id: map['id'] as int?,
              categoryName: map['categoryName'] as String? ?? '',
              amount: (map['amount'] as num).toDouble(),
              year: map['year'] as int,
              month: map['month'] as int,
              spent: (map['spent'] as num?)?.toDouble() ?? 0.0,
              budgetName: map['budgetName'] as String? ?? '',
            ),
          )
          .toList();
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库获取预算失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 从本地数据库获取储蓄目标数据
  Future<List<SavingGoal>> getSavingGoalsFromLocal() async {
    try {
      final db = await _databaseService.database;

      final results = await db.query('saving_goals', orderBy: 'name ASC');

      return results
          .map(
            (map) {
              // 处理deadline字段，支持字符串和时间戳两种格式
              DateTime deadline;
              final deadlineValue = map['deadline'];
              
              if (deadlineValue is String) {
                // 尝试解析字符串格式的日期
                try {
                  deadline = DateTime.parse(deadlineValue);
                } catch (e) {
                  // 如果解析失败，使用当前日期作为默认值
                  deadline = DateTime.now();
                }
              } else if (deadlineValue is int) {
                // 直接使用时间戳
                deadline = DateTime.fromMillisecondsSinceEpoch(deadlineValue);
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
            },
          )
          .toList();
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库获取储蓄目标失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 根据分类名称获取收支记录（只支持精确匹配）
  Future<List<Bill>> getTransactionsByCategory(String categoryName) async {
    try {
      final db = await _databaseService.database;

      // 只进行精确匹配
      final exactResults = await db.query(
        'bills',
        where: 'categoryName = ?',
        whereArgs: [categoryName],
        orderBy: 'transactionDate DESC',
      );

      List<Bill> results = exactResults
          .map(
            (map) => Bill(
              id: map['id'] as int?,
              type: map['type'] as int,
              categoryName: map['categoryName'] as String,
              amount: (map['amount'] as num).toDouble(),
              transactionDate: map['transactionDate'] as String,
              remark: map['remark'] as String?,
              imagePath: map['imagePath'] as String?,            ),
          )
          .toList();

      developer.log(
        '分类精确匹配结果: 目标分类=$categoryName, 找到${results.length}条记录',
        name: 'LocalDataService',
      );

      return results;
    } catch (e, stackTrace) {
      developer.log(
        '根据分类获取收支记录失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 分类名匹配逻辑（支持"计划名-分类名"格式）
  bool _isCategoryMatch(String billCategoryName, String targetCategoryName) {
    // 完全匹配
    if (billCategoryName == targetCategoryName) {
      return true;
    }
    
    // 包含匹配（目标分类包含账单分类）
    if (targetCategoryName.contains(billCategoryName)) {
      return true;
    }
    
    // 账单包含目标分类
    if (billCategoryName.contains(targetCategoryName)) {
      return true;
    }
    
    // 分隔符匹配
    final targetParts = targetCategoryName.split('-');
    final billParts = billCategoryName.split('-');
    
    for (final targetPart in targetParts) {
      for (final billPart in billParts) {
        if (targetPart.trim() == billPart.trim()) {
          return true;
        }
      }
    }
    
    return false;
  }

  /// 计算预算的实际支出金额
  /// 支持基础分类名（如"餐饮"）与组合分类名（如"伙食费-餐饮"）的匹配
  Future<double> calculateBudgetSpent(String categoryName, int year, int month) async {
    try {
      final db = await _databaseService.database;
      
      print('🔍 计算预算支出: 分类=$categoryName, 年份=$year, 月份=$month');
      
      // 获取指定年月的所有账单
      final startDate = '$year-${month.toString().padLeft(2, '0')}-01';
      final endDate = month == 12 
          ? '${year + 1}-01-01'
          : '$year-${(month + 1).toString().padLeft(2, '0')}-01';
      
      final billsResults = await db.query(
        'bills',
        where: 'transactionDate >= ? AND transactionDate < ?',
        whereArgs: [startDate, endDate],
      );
      
      print('🔍 找到${billsResults.length}条指定年月的账单记录');
      
      double totalSpent = 0.0;
      
      for (final billMap in billsResults) {
        final billCategoryName = billMap['categoryName'] as String;
        final amount = (billMap['amount'] as num).toDouble();
        final type = billMap['type'] as int;
        
        print('🔍 检查账单: 分类=$billCategoryName, 金额=$amount, 类型=$type');
        
        // 只统计支出类型的账单（type = 2）
        if (type == 2) {
          // 使用现有的分类匹配逻辑
          if (_isCategoryMatch(billCategoryName, categoryName)) {
            print('✅ 匹配成功: $billCategoryName -> $categoryName, 金额=$amount');
            totalSpent += amount;
          } else {
            print('❌ 匹配失败: $billCategoryName != $categoryName');
          }
        } else {
          print('⏭️ 跳过收入类型账单: 分类=$billCategoryName, 类型=$type');
        }
      }
      
      print('💰 预算支出计算完成: $categoryName 总支出=$totalSpent');
      return totalSpent;
      
    } catch (e, stackTrace) {
      developer.log(
        '计算预算支出失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      return 0.0; // 出错时返回0，避免影响用户体验
    }
  }

  /// 根据ID从本地数据库获取单个储蓄目标
  Future<SavingGoal?> getSavingGoalFromLocal(int id) async {
    try {
      final db = await _databaseService.database;

      final results = await db.query(
        'saving_goals',
        where: 'id = ?',
        whereArgs: [id],
      );

      if (results.isNotEmpty) {
        // 处理deadline字段，支持字符串和时间戳两种格式
        DateTime deadline;
        final deadlineValue = results[0]['deadline'];
        
        if (deadlineValue is String) {
          // 尝试解析字符串格式的日期
          try {
            deadline = DateTime.parse(deadlineValue);
          } catch (e) {
            // 如果解析失败，使用当前日期作为默认值
            deadline = DateTime.now();
          }
        } else if (deadlineValue is int) {
          // 直接使用时间戳
          deadline = DateTime.fromMillisecondsSinceEpoch(deadlineValue);
        } else {
          // 默认使用当前日期
          deadline = DateTime.now();
        }
        
        return SavingGoal(
          id: results[0]['id'] as int?,
          name: results[0]['name'] as String? ?? '',
          targetAmount: (results[0]['target_amount'] as num).toDouble(),
          currentAmount: (results[0]['current_amount'] as num).toDouble(),
          deadline: deadline,
          description: results[0]['description'] as String?,
          categoryName: results[0]['category_name'] as String? ?? '',
        );
      }
      return null;
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库获取单个储蓄目标失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 从本地数据库获取所有储蓄记录

  Future<void> updateCurrentAmount(int goalId, double newAmount, {bool enableSync = true}) async {
    try {
      final db = await _databaseService.database;
      await db.update(
        'saving_goals',
        {'current_amount': newAmount},
        where: 'id = ?',
        whereArgs: [goalId],
      );
      developer.log('储蓄目标当前金额已更新', name: 'LocalDataService');

      // 如果启用同步且不在离线模式，尝试同步更新操作
      if (enableSync && !_offlineSyncService.isOfflineMode) {
        try {
          // 这里可以调用实际的同步方法
        } catch (e) {
          // 获取更新后的储蓄目标信息用于同步
          final updatedGoal = await db.query(
            'saving_goals',
            where: 'id = ?',
            whereArgs: [goalId],
          );
          if (updatedGoal.isNotEmpty) {
            await _offlineSyncService.addPendingOperation(
              OperationType.updateSavingGoal,
              updatedGoal.first,
            );
          }
        }
      } else if (enableSync) {
        // 离线模式下直接添加到待同步队列
        final updatedGoal = await db.query(
          'saving_goals',
          where: 'id = ?',
          whereArgs: [goalId],
        );
        if (updatedGoal.isNotEmpty) {
          await _offlineSyncService.addPendingOperation(
            OperationType.updateSavingGoal,
            updatedGoal.first,
          );
        }
      }
    } catch (e, stackTrace) {
      developer.log(
        '更新储蓄目标当前金额失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  Future<List<SavingRecord>> getSavingRecordsFromLocal() async {
    try {
      final db = await _databaseService.database;

      final results = await db.query(
        'saving_records',
        orderBy: 'created_at DESC',
      );

      return results
          .map(
            (map) => SavingRecord(
              id: map['id'] as int?,
              goalId: map['goal_id'] as int,
              amount: (map['amount'] as num).toDouble(),
              type: map['type'] as String? ?? 'deposit',
              remark: map['remark'] as String?,
              createdAt: DateTime.fromMillisecondsSinceEpoch(map['created_at'] as int),
              updatedAt: map['updated_at'] != null
                  ? DateTime.fromMillisecondsSinceEpoch(map['updated_at'] as int)
                  : null,
            ),
          )
          .toList();
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库获取储蓄记录失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 从本地数据库获取指定目标的储蓄记录
  Future<List<SavingRecord>> getSavingRecordsByGoalId(int goalId) async {
    try {
      final db = await _databaseService.database;

      final results = await db.query(
        'saving_records',
        where: 'goal_id = ?',
        whereArgs: [goalId],
        orderBy: 'created_at DESC',
      );

      return results
          .map(
            (map) => SavingRecord(
              id: map['id'] as int?,
              goalId: map['goal_id'] as int,
              amount: (map['amount'] as num).toDouble(),
              type: map['type'] as String? ?? 'deposit',
              remark: map['remark'] as String?,
              createdAt: DateTime.fromMillisecondsSinceEpoch(map['created_at'] as int),
              updatedAt: map['updated_at'] != null
                  ? DateTime.fromMillisecondsSinceEpoch(map['updated_at'] as int)
                  : null,
            ),
          )
          .toList();
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库获取指定目标的储蓄记录失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 删除本地账单（集成离线同步）
  Future<void> deleteBillFromLocal(int id, {bool enableSync = true}) async {
    try {
      final db = await _databaseService.database;

      await db.delete('bills', where: 'id = ?', whereArgs: [id]);

      developer.log('账单已从本地数据库删除', name: 'LocalDataService');

      // 如果启用同步，添加删除操作到待同步队列
      if (enableSync) {
        await _offlineSyncService.addPendingOperation(
          OperationType.deleteTransaction,
          {'id': id},
        );
      }
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库删除账单失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 删除本地分类
  Future<void> deleteCategoryFromLocal(int id) async {
    try {
      final db = await _databaseService.database;

      await db.delete('categories', where: 'id = ?', whereArgs: [id]);

      developer.log('分类已从本地数据库删除', name: 'LocalDataService');
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库删除分类失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }









  /// 从本地数据库删除储蓄目标（集成离线同步，同时检查并删除未使用的关联分类）
  Future<void> deleteSavingGoalFromLocal(
    int id, {
    bool enableSync = true,
  }) async {
    try {
      final db = await _databaseService.database;
      
      // 先获取要删除的储蓄目标信息，用于后续检查分类使用情况
      final savingGoalToDelete = await db.query(
        'saving_goals',
        where: 'id = ?',
        whereArgs: [id],
      );
      
      String? categoryName;
      if (savingGoalToDelete.isNotEmpty) {
        categoryName = savingGoalToDelete.first['category_name'] as String?;
      }

      await db.delete('saving_goals', where: 'id = ?', whereArgs: [id]);

      developer.log('储蓄目标已从本地数据库删除', name: 'LocalDataService');

      // 如果启用同步，添加删除操作到待同步队列
      if (enableSync) {
        await _offlineSyncService.addPendingOperation(
          OperationType.deleteSavingGoal,
          {'id': id},
        );
      }
      
      // 检查并删除未使用的分类
      await _checkAndDeleteUnusedCategoryForSavingGoal(categoryName);
    } catch (e, stackTrace) {
      developer.log(
        '从本地数据库删除储蓄目标失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      rethrow;
    }
  }

  /// 检查并删除储蓄目标相关的未使用分类
  Future<void> _checkAndDeleteUnusedCategoryForSavingGoal(String? categoryName) async {
    try {
      if (categoryName == null) return; // 如果没有分类名，直接返回
      
      final db = await _databaseService.database;
      
      // 检查是否在交易记录中使用（这是最重要的检查）
      final billCount = await db.rawQuery(
        'SELECT COUNT(*) as count FROM bills WHERE categoryName = ?',
        [categoryName],
      );
      final billUseCount = billCount.first['count'] as int? ?? 0;
      
      if (billUseCount > 0) {
        // 分类在交易记录中被使用，不能删除
        developer.log('分类 $categoryName 在交易记录中使用，保留不删除', name: 'LocalDataService');
        return;
      }
      
      // 检查分类名是否还在预算中使用
      final budgetCount = await db.rawQuery(
        'SELECT COUNT(*) as count FROM budgets WHERE categoryName = ?',
        [categoryName],
      );
      final budgetUseCount = budgetCount.first['count'] as int? ?? 0;
      
      if (budgetUseCount > 0) {
        // 分类在预算中使用，不能删除
        developer.log('分类 $categoryName 在预算中使用，保留不删除', name: 'LocalDataService');
        return;
      }
      
      // 检查是否在其他储蓄目标中使用
      final savingGoalCount = await db.rawQuery(
        'SELECT COUNT(*) as count FROM saving_goals WHERE category_name = ?',
        [categoryName],
      );
      final otherSavingGoalCount = savingGoalCount.first['count'] as int? ?? 0;
      
      if (otherSavingGoalCount == 0) {
        // 查找并删除未使用的分类
        final categoryToDelete = await db.query(
          'categories',
          where: 'name = ?',
          whereArgs: [categoryName],
        );
        
        if (categoryToDelete.isNotEmpty) {
          final categoryId = categoryToDelete.first['id'] as int;
          final categoryType = categoryToDelete.first['type'] as int;
          
          await db.delete('categories', where: 'id = ?', whereArgs: [categoryId]);
          developer.log('未使用的储蓄目标分类已删除: $categoryName (类型: $categoryType)', name: 'LocalDataService');
        }
      }
    } catch (e, stackTrace) {
      developer.log(
        '检查并删除未使用的储蓄目标分类失败',
        name: 'LocalDataService',
        error: e,
        stackTrace: stackTrace,
      );
      // 不抛出异常，避免影响主删除流程
    }
  }



  /// 检查本地数据库是否有数据
  Future<bool> hasLocalData() async {
    final db = await _databaseService.database;

    final billResult = await db.rawQuery('SELECT COUNT(*) as count FROM bills');
    final billCount = billResult.first['count'] as int? ?? 0;

    final categoryResult = await db.rawQuery(
      'SELECT COUNT(*) as count FROM categories',
    );
    final categoryCount = categoryResult.first['count'] as int? ?? 0;

    final budgetResult = await db.rawQuery(
      'SELECT COUNT(*) as count FROM budgets',
    );
    final budgetCount = budgetResult.first['count'] as int? ?? 0;

    final savingGoalResult = await db.rawQuery(
      'SELECT COUNT(*) as count FROM saving_goals',
    );
    final savingGoalCount = savingGoalResult.first['count'] as int? ?? 0;

    return billCount > 0 ||
        categoryCount > 0 ||
        budgetCount > 0 ||
        savingGoalCount > 0;
  }
}
