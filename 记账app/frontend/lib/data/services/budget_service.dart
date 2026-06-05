import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../models/budget_model.dart';
import '../../core/services/local_data_service.dart';
import '../../core/errors/error_center.dart';

/// 预算服务类
/// 负责预算数据的CRUD操作
class BudgetService {
  final LocalDataService _localDataService;
  final ErrorCenter _errorCenter;

  BudgetService(this._localDataService, this._errorCenter);

  /// 获取预算列表
  Future<List<Budget>> getBudgets({
    int? year,
    int? month,
  }) async {
    try {
      print('📊 BudgetService: 开始获取预算列表，年份=$year, 月份=$month');
      
      final budgets = await _localDataService.getBudgetsFromLocal(
        year: year,
        month: month,
      );
      
      print('📊 BudgetService: 获取到${budgets.length}个预算');
      
      // 为每个预算计算实际支出
      final budgetsWithSpent = <Budget>[];
      
      for (final budget in budgets) {
        print('📊 BudgetService: 计算预算支出: ${budget.categoryName}');
        
        // 使用新的计算方法动态计算实际支出
        final spentAmount = await _localDataService.calculateBudgetSpent(
          budget.categoryName, 
          budget.year, 
          budget.month,
        );
        
        // 创建更新了支出金额的预算对象
        final updatedBudget = budget.copyWith(spent: spentAmount);
        budgetsWithSpent.add(updatedBudget);
        
        print('📊 BudgetService: 预算 ${budget.categoryName} - 预算: ${budget.amount}, 实际支出: $spentAmount, 使用率: ${budget.amount > 0 ? (spentAmount / budget.amount * 100).toStringAsFixed(1) : '0.0'}%');
      }
      
      print('✅ BudgetService: 预算列表获取完成，共${budgetsWithSpent.length}个预算');
      return budgetsWithSpent;
      
    } catch (e, stackTrace) {
      if (kDebugMode) {
        print('获取预算列表失败: $e');
      }
      _errorCenter.showError(
        message: '获取预算列表失败',
        description: e.toString(),
      );
      throw Exception('获取预算列表失败: $e');
    }
  }

  /// 创建预算
  Future<Budget> createBudget(Budget budget, {BuildContext? context}) async {
    try {
      // 保存到本地数据库，获取自动生成的ID
      final id = await _localDataService.saveBudgetToLocal(budget);
      
      // 返回带有数据库生成ID的预算对象
      return budget.copyWith(id: id);
    } catch (e) {
      // 使用统一错误处理
      if (context != null) {
        _errorCenter.showError(
          message: '创建预算失败',
          description: e.toString(),
        );
      }
      throw Exception('创建预算失败: $e');
    }
  }

  Future<Budget> updateBudget(Budget budget, {BuildContext? context}) async { 
    if (budget.id == null) {
      throw Exception('更新失败: 预算ID不能为空');
    }

    try {
      // 直接更新到本地数据库
      await _localDataService.saveBudgetToLocal(budget);
      return budget;
    } catch (e) {
      // 使用统一错误处理
      if (context != null) {
        _errorCenter.showError(
          message: '更新预算失败',
          description: e.toString(),
        );
      }
      throw Exception('更新预算失败: $e');
    }
  }

  Future<void> deleteBudget(int id, {BuildContext? context}) async {
    try {
      // 直接从本地数据库删除
      await _localDataService.deleteBudgetFromLocal(id);
    } catch (e) {
      // 使用统一错误处理
      if (context != null) {
        _errorCenter.showError(
          message: '删除预算失败',
          description: e.toString(),
        );
      }
      throw Exception('删除预算失败: $e');
    }
  }

  /// 获取指定月份的预算概览
  Future<Map<String, dynamic>> getBudgetOverview(int year, int month) async {
    try {
      final budgets = await getBudgets(year: year, month: month);
      final totalBudget = budgets.fold<double>(
        0.0,
        (sum, budget) => sum + budget.amount,
      );
      final totalSpent = budgets.fold<double>(
        0.0,
        (sum, budget) => sum + (budget.spent ?? 0.0),
      );

      return {
        'totalBudget': totalBudget,
        'totalSpent': totalSpent,
        'remainingBudget': totalBudget - totalSpent,
        'budgetCount': budgets.length,
        'budgets': budgets,
      };
    } catch (e, stackTrace) {
      if (kDebugMode) {
        print('获取预算概览失败: $e');
      }
      _errorCenter.showError(
        message: '获取预算概览失败',
        description: e.toString(),
      );
      throw Exception('获取预算概览失败: $e');
    }
  }
  
  /// 更新预算分类名称
  Future<void> updateBudgetCategoryName(String oldCategoryName, String newCategoryName) async {
    try {
      await _localDataService.updateBillsCategoryName(oldCategoryName, newCategoryName);
      if (kDebugMode) {
        print('预算分类名称已更新: $oldCategoryName -> $newCategoryName');
      }
      developer.log('预算分类名称已更新', name: 'BudgetService');
    } catch (e) {
      if (kDebugMode) {
        print('更新预算分类名称失败: $e');
      }
      developer.log('更新预算分类名称失败: $e', name: 'BudgetService');
      rethrow;
    }
  }
}