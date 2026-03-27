import '../../core/services/local_database_service.dart';
import '../../core/services/local_data_service.dart';
import '../../data/models/saving_goal_model.dart';

class LocalSavingGoalService {
  final LocalDatabaseService _databaseService;
  final LocalDataService _localDataService;

  LocalSavingGoalService(this._databaseService, this._localDataService);

  /// 获取所有储蓄目标
  Future<List<SavingGoal>> getSavingGoals() async {
    try {
      final goals = await _databaseService.getAllSavingGoals();
      return goals;
    } catch (e) {
      throw Exception('获取储蓄目标失败: $e');
    }
  }

  /// 创建储蓄目标
  Future<SavingGoal> createSavingGoal(SavingGoal goal) async {
    try {
      final id = await _databaseService.saveSavingGoal(goal);
      final newGoal = goal.copyWith(id: id);
      return newGoal;
    } catch (e) {
      throw Exception('创建储蓄目标失败: $e');
    }
  }

  /// 根据ID获取储蓄目标
  Future<SavingGoal?> getSavingGoalById(int id) async {
    try {
      return await _databaseService.getSavingGoalById(id);
    } catch (e) {
      throw Exception('获取储蓄目标详情失败: $e');
    }
  }

  /// 更新储蓄目标
  Future<SavingGoal> updateSavingGoal(SavingGoal goal) async {
    try {
      await _databaseService.updateSavingGoal(goal);
      return goal;
    } catch (e) {
      throw Exception('更新储蓄目标失败: $e');
    }
  }

  /// 更新当前金额
  Future<SavingGoal> updateCurrentAmount(int goalId, double amount) async {
    try {
      // 获取现有目标
      final goal = await _databaseService.getSavingGoalById(goalId);
      if (goal == null) {
        throw Exception('未找到指定的储蓄目标');
      }

      // 更新当前金额
      final updatedGoal = goal.copyWith(currentAmount: amount);
      await _databaseService.updateSavingGoal(updatedGoal);
      return updatedGoal;
    } catch (e) {
      throw Exception('更新金额失败: $e');
    }
  }

  /// 删除储蓄目标
  Future<void> deleteSavingGoal(int id) async {
    try {
      await _databaseService.deleteSavingGoal(id);
    } catch (e) {
      throw Exception('删除储蓄目标失败: $e');
    }
  }
  
  /// 更新储蓄目标分类名称
  Future<void> updateSavingGoalCategoryName(String oldCategoryName, String newCategoryName) async {
    try {
      await _localDataService.updateBillsCategoryName(oldCategoryName, newCategoryName);
    } catch (e) {
      throw Exception('更新储蓄目标分类名称失败: $e');
    }
  }
}
