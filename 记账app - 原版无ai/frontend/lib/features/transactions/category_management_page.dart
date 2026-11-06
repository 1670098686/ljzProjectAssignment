import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/providers/category_provider.dart';
import '../../data/models/category_model.dart';

class CategoryManagementPage extends StatefulWidget {
  const CategoryManagementPage({super.key});

  @override
  State<CategoryManagementPage> createState() => _CategoryManagementPageState();
}

class _CategoryManagementPageState extends State<CategoryManagementPage> {
  int _selectedType = 2; // 默认支出
  final TextEditingController _categoryNameController = TextEditingController();
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadCategories();
  }

  @override
  void dispose() {
    _categoryNameController.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    setState(() {
      _isLoading = true;
    });
    try {
      final provider = context.read<CategoryProvider>();
      await provider.loadCategories(type: _selectedType);
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _addCategory() async {
    if (_categoryNameController.text.trim().isEmpty) {
      _showSnackBar('请输入分类名称');
      return;
    }

    final provider = context.read<CategoryProvider>();
    final existingCategories = provider.getCategoriesByType(_selectedType);
    final categoryExists = existingCategories.any(
        (cat) => cat.name == _categoryNameController.text.trim());

    if (categoryExists) {
      _showSnackBar('分类已存在');
      return;
    }

    final newCategory = Category(
      name: _categoryNameController.text.trim(),
      icon: _selectedType == 1 ? 'income' : 'expense',
      type: _selectedType,
    );

    await provider.addCategory(newCategory);
    _categoryNameController.clear();
    _showSnackBar('分类添加成功');
  }

  Future<void> _deleteCategory(int id) async {
    final provider = context.read<CategoryProvider>();
    await provider.deleteCategory(id);
    _showSnackBar('分类删除成功');
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final provider = context.watch<CategoryProvider>();
    final categories = provider.getCategoriesByType(_selectedType);

    return Scaffold(
      appBar: AppBar(
        title: const Text('分类管理'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 分类类型切换
            Row(
              children: [
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedType = 1;
                      });
                      _loadCategories();
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(8),
                        color: _selectedType == 1
                            ? theme.colorScheme.primary
                            : Colors.transparent,
                      ),
                      child: Text(
                        '收入分类',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: _selectedType == 1 ? FontWeight.bold : FontWeight.normal,
                          color: _selectedType == 1
                              ? theme.colorScheme.onPrimary
                              : theme.colorScheme.onSurface,
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedType = 2;
                      });
                      _loadCategories();
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(8),
                        color: _selectedType == 2
                            ? theme.colorScheme.primary
                            : Colors.transparent,
                      ),
                      child: Text(
                        '支出分类',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: _selectedType == 2 ? FontWeight.bold : FontWeight.normal,
                          color: _selectedType == 2
                              ? theme.colorScheme.onPrimary
                              : theme.colorScheme.onSurface,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // 添加分类输入
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _categoryNameController,
                    decoration: InputDecoration(
                      labelText: '添加新分类',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _addCategory,
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // 分类列表
            Text(
              _selectedType == 1 ? '收入分类' : '支出分类',
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            _isLoading
                ? const Center(child: CircularProgressIndicator())
                : categories.isEmpty
                    ? const Center(child: Text('暂无分类'))
                    : ListView.separated(
                        shrinkWrap: true,
                        itemCount: categories.length,
                        separatorBuilder: (context, index) =>
                            const Divider(),
                        itemBuilder: (context, index) {
                          final category = categories[index];
                          return ListTile(
                            title: Text(category.name),
                            trailing: IconButton(
                              icon: const Icon(Icons.delete,
                                  color: Colors.red),
                              onPressed: () => _deleteCategory(category.id!),
                            ),
                          );
                        },
                      ),
          ],
        ),
      ),
    );
  }
}