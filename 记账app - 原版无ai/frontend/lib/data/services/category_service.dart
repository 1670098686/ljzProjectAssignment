import '../../core/services/local_data_service.dart';
import '../models/category_model.dart';

class CategoryService {
  final LocalDataService _localDataService;

  CategoryService() 
      : _localDataService = LocalDataService();

  Future<List<Category>> getCategories({int? type}) async {
    try {
      // 直接从本地获取
      return await _localDataService.getCategoriesFromLocal(type: type);
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('获取分类数据失败: $e');
      // 出错时返回空列表
      return [];
    }
  }

  Future<Category> createCategory(Category category) async {
    try {
      // 直接保存到本地
      await _localDataService.saveCategoryToLocal(category);
      return category;
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('创建分类失败: $e');
      // 出错时返回原始对象
      return category;
    }
  }

  Future<Category> updateCategory(int id, Category category) async {
    try {
      final updatedCategory = category.copyWith(id: id);
      // 直接更新本地
      await _localDataService.saveCategoryToLocal(updatedCategory);
      return updatedCategory;
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('更新分类失败: $e');
      // 出错时返回原始对象
      return category.copyWith(id: id);
    }
  }

  Future<void> deleteCategory(int id) async {
    try {
      // 直接从本地删除
      await _localDataService.deleteCategoryFromLocal(id);
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('删除分类失败: $e');
      // 忽略错误
    }
  }

  /// 确保分类存在，如果不存在则创建
  Future<Category> ensureCategoryExists(String name, int type) async {
    try {
      // 获取所有分类
      final categories = await getCategories();
      
      // 查找是否已存在
      final existingCategory = categories.firstWhere(
        (category) => category.name == name && category.type == type,
        orElse: () => Category(name: name, type: type, icon: type == 1 ? 'income' : 'expense'),
      );
      
      // 如果分类已存在，直接返回
      if (existingCategory.id != null) {
        return existingCategory;
      }
      
      // 否则创建新分类
      return await createCategory(existingCategory);
    } catch (e) {
      // 记录错误但不使用BuildContext
      print('确保分类存在失败: $e');
      // 出错时返回一个默认分类对象
      return Category(name: name, type: type, icon: type == 1 ? 'income' : 'expense');
    }
  }
}
