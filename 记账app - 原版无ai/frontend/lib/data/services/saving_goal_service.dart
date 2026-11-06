import '../models/saving_goal_model.dart';
import '../../core/services/local_data_service.dart';

class SavingGoalService {
  final LocalDataService _localDataService;

  SavingGoalService() 
      : _localDataService = LocalDataService();

  /// 获取所有储蓄目标
  Future<List<SavingGoal>> getSavingGoals() async {
    try {
      // 直接从本地数据库获取储蓄目标
      return await _localDataService.getSavingGoalsFromLocal();
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('获取储蓄目标失败: $e');
      throw Exception('获取储蓄目标失败: $e');
    }
  }

  /// 创建储蓄目标
  Future<SavingGoal> createSavingGoal(SavingGoal goal) async {
    try {
      // 直接保存到本地数据库
      final savedGoal = goal.copyWith(id: DateTime.now().millisecondsSinceEpoch);
      await _localDataService.saveSavingGoalToLocal(savedGoal);
      return savedGoal;
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('创建储蓄目标失败: $e');
      throw Exception('创建储蓄目标失败: $e');
    }
  }

  /// 更新当前金额
  Future<SavingGoal> updateCurrentAmount(int id, double amount) async {
    try {
      // 直接更新到本地数据库
      final goal = await _localDataService.getSavingGoalFromLocal(id);
      if (goal != null) {
        final updatedGoal = goal.copyWith(currentAmount: amount);
        await _localDataService.saveSavingGoalToLocal(updatedGoal);
        return updatedGoal;
      }
      throw Exception('储蓄目标不存在');
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('更新当前金额失败: $e');
      throw Exception('更新当前金额失败: $e');
    }
  }

  /// 更新储蓄目标
  Future<SavingGoal> updateSavingGoal(SavingGoal goal) async {
    if (goal.id == null) {
      throw Exception('更新失败: 目标ID不能为空');
    }

    try {
      // 直接更新到本地数据库
      await _localDataService.saveSavingGoalToLocal(goal);
      return goal;
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('更新储蓄目标失败: $e');
      throw Exception('更新储蓄目标失败: $e');
    }
  }

  /// 删除储蓄目标
  Future<void> deleteSavingGoal(int id) async {
    try {
      // 直接从本地数据库删除
      await _localDataService.deleteSavingGoalFromLocal(id);
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('删除储蓄目标失败: $e');
      throw Exception('删除储蓄目标失败: $e');
    }
  }
}
