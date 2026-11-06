import 'dart:developer';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/providers/bill_provider.dart';
import '../../../core/providers/category_provider.dart';
import '../../../core/services/image_upload_service.dart';
import '../../../core/utils/animation_utils.dart';
import '../../../data/models/bill_model.dart';
import '../../../data/models/category_model.dart';
import '../../../services/permission_service.dart';
import '../../../shared/widgets/category_selector_field.dart';
import '../../../utils/icon_mapper.dart';

class QuickEntryWidget extends StatefulWidget {
  const QuickEntryWidget({super.key, this.onEntrySaved});

  final VoidCallback? onEntrySaved;
  
  // 🔧 全局图片路径变量，绕过GlobalKey问题
  static String? _globalImagePath;

  @override
  State<QuickEntryWidget> createState() => _QuickEntryWidgetState();
}

class _QuickEntryWidgetState extends State<QuickEntryWidget> {
  bool _showRemarkField = false;
  int _selectedTransactionType = 2; // 默认支出
  final GlobalKey<_AmountInputSectionState> _amountInputKey = GlobalKey();
  final GlobalKey<_CategorySelectorState> _categorySelectorKey = GlobalKey();
  final GlobalKey<_RemarkFieldState> _remarkFieldKey = GlobalKey();

  @override
  Widget build(BuildContext context) {
    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 500),
      beginOffset: const Offset(0, 30),
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(
            color: Theme.of(context).colorScheme.outline.withAlpha(26),
          ),
        ),
        color: Theme.of(context).colorScheme.surface,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AnimationUtils.createFadeIn(
                duration: const Duration(milliseconds: 400),
                child: Text(
                  '快速记一笔',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                ),
              ),
              const SizedBox(height: 16),
              RepaintBoundary(
                child: _AmountInputSection(key: _amountInputKey),
              ),
              const SizedBox(height: 12),
              RepaintBoundary(
                child: _TransactionTypeSection(
                  selectedType: _selectedTransactionType,
                  onTypeChanged: (type) {
                    setState(() {
                      _selectedTransactionType = type;
                    });
                    // 通知金额输入组件更新类型
                    _amountInputKey.currentState?.updateTransactionType(type);
                    // 更新分类选择器的类型
                    _categorySelectorKey.currentState?.updateTransactionType(type);
                  },
                ),
              ),
              const SizedBox(height: 16),
              RepaintBoundary(
                child: _CategorySection(categorySelectorKey: _categorySelectorKey),
              ),
              const SizedBox(height: 12),
              _RemarkSection(
                key: _remarkFieldKey,
                showRemarkField: _showRemarkField,
                onRemarkToggle: (show) => setState(() => _showRemarkField = show),
              ),
              const SizedBox(height: 16),
              RepaintBoundary(
                child: _KeypadSection(
                  onKeyTap: (key) => _amountInputKey.currentState?.handleKeyTap(key),
                ),
              ),
              const SizedBox(height: 12),
              RepaintBoundary(
                child: _ActionSection(
                  onEntrySaved: widget.onEntrySaved,
                  amountInputKey: _amountInputKey,
                  categorySelectorKey: _categorySelectorKey,
                  remarkFieldKey: _remarkFieldKey,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AmountInputSection extends StatefulWidget {
  const _AmountInputSection({super.key});

  @override
  State<_AmountInputSection> createState() => _AmountInputSectionState();
}

class _AmountInputSectionState extends State<_AmountInputSection>
    with AutomaticKeepAliveClientMixin {
  String _amountInput = '0';
  int _transactionType = 2;

  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = _transactionType == 1 ? Colors.green : Colors.red;

    return AnimationUtils.createScaleIn(
      duration: const Duration(milliseconds: 300),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
        decoration: BoxDecoration(
          color: theme.colorScheme.surfaceContainerHighest.withAlpha(30),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            Icon(
              _transactionType == 1 ? Icons.trending_up : Icons.trending_down,
              color: color,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                '¥$_amountInput',
                textAlign: TextAlign.end,
                style: theme.textTheme.displaySmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void handleKeyTap(String key) {
    setState(() {
      if (key == 'DEL') {
        if (_amountInput.length <= 1) {
          _amountInput = '0';
        } else {
          _amountInput = _amountInput.substring(0, _amountInput.length - 1);
        }
        return;
      }

      if (key == 'CLR') {
        _amountInput = '0';
        return;
      }

      if (key == '.') {
        if (_amountInput.contains('.')) {
          return;
        }
        _amountInput = '$_amountInput.';
        return;
      }

      if (_amountInput == '0') {
        _amountInput = key;
      } else {
        _amountInput = '$_amountInput$key';
      }
    });
  }

  void updateTransactionType(int type) {
    if (mounted) {
      setState(() {
        _transactionType = type;
      });
    }
  }

  double get currentAmount => double.tryParse(_amountInput) ?? 0;
  int get currentType => _transactionType;
  void resetAmount() {
    if (mounted) {
      setState(() {
        _amountInput = '0';
      });
    }
  }
}

class _TransactionTypeSection extends StatelessWidget {
  const _TransactionTypeSection({
    required this.selectedType,
    required this.onTypeChanged,
  });

  final int selectedType;
  final Function(int) onTypeChanged;

  @override
  Widget build(BuildContext context) {
    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 300),
      child: Row(
        children: [
          Expanded(
            child: GestureDetector(
              onTap: () => onTypeChanged(1),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: selectedType == 1
                      ? Colors.green.withAlpha(36)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: selectedType == 1
                        ? Colors.green
                        : Theme.of(context).colorScheme.outline.withAlpha(60),
                    width: 1.5,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.trending_up,
                      color: selectedType == 1 ? Colors.green : Colors.green.withAlpha(60),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '收入',
                      style: TextStyle(
                        color: selectedType == 1
                            ? Colors.green
                            : Colors.green.withAlpha(60),
                        fontWeight: selectedType == 1
                            ? FontWeight.bold
                            : FontWeight.normal,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: GestureDetector(
              onTap: () => onTypeChanged(2),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: selectedType == 2
                      ? Colors.red.withAlpha(36)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: selectedType == 2
                        ? Colors.red
                        : Theme.of(context).colorScheme.outline.withAlpha(60),
                    width: 1.5,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.trending_down,
                      color: selectedType == 2 ? Colors.red : Colors.red.withAlpha(60),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '支出',
                      style: TextStyle(
                        color: selectedType == 2
                            ? Colors.red
                            : Colors.red.withAlpha(60),
                        fontWeight: selectedType == 2
                            ? FontWeight.bold
                            : FontWeight.normal,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TypeSection extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: _TypeSelector(),
    );
  }
}

class _TypeSelector extends StatefulWidget {
  const _TypeSelector();

  @override
  State<_TypeSelector> createState() => _TypeSelectorState();
}

class _TypeSelectorState extends State<_TypeSelector>
    with AutomaticKeepAliveClientMixin {
  int _transactionType = 2;

  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    return AnimationUtils.createSlideIn(
      duration: const Duration(milliseconds: 300),
      child: Row(
        children: [
          Expanded(
            child: _TypeChip(
              label: '收入',
              isSelected: _transactionType == 1,
              color: Colors.green,
              onTap: () => setState(() {
                _transactionType = 1;
              }),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _TypeChip(
              label: '支出',
              isSelected: _transactionType == 2,
              color: Colors.red,
              onTap: () => setState(() {
                _transactionType = 2;
              }),
            ),
          ),
        ],
      ),
    );
  }
}

class _CategorySection extends StatelessWidget {
  const _CategorySection({super.key, required this.categorySelectorKey});

  final GlobalKey<_CategorySelectorState> categorySelectorKey;

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: AnimationUtils.createSlideIn(
        duration: const Duration(milliseconds: 300),
        delay: const Duration(milliseconds: 200),
        beginOffset: const Offset(20, 0),
        child: _CategorySelector(key: categorySelectorKey),
      ),
    );
  }
}

class _CategorySelector extends StatefulWidget {
  const _CategorySelector({super.key});

  @override
  State<_CategorySelector> createState() => _CategorySelectorState();
}

class _CategorySelectorState extends State<_CategorySelector> {
  final TextEditingController _categoryController = TextEditingController();
  int _transactionType = 2; // 默认支出
  bool _categoriesLoaded = false;

  @override
  void initState() {
    super.initState();
    _loadCategories();
  }

  @override
  void dispose() {
    _categoryController.dispose();
    super.dispose();
  }

  void updateTransactionType(int type) {
    setState(() {
      _transactionType = type;
    });
  }

  String get selectedCategory {
    final category = _categoryController.text.trim();
    debugPrint('分类选择器状态 - category: "$category", isEmpty: ${category.isEmpty}');
    return category;
  }

  Future<void> _loadCategories() async {
    try {
      final categoryProvider = Provider.of<CategoryProvider>(
        context,
        listen: false,
      );
      await categoryProvider.loadCategories();
      
      if (mounted) {
        setState(() {
          _categoriesLoaded = true;
        });
      }
    } catch (e) {
      debugPrint('首页加载分类失败: $e');
      if (mounted) {
        setState(() {
          _categoriesLoaded = true;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Consumer<CategoryProvider>(
      builder: (context, provider, _) {
        // 首页记账模块显示全部分类，不按类型过滤
        final categories = provider.getAllAvailableCategories()
            .toList()
          ..sort((a, b) => a.name.compareTo(b.name));

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            CategorySelectorField(
              controller: _categoryController,
              categories: categories,
              labelText: '分类',
              hintText: categories.isEmpty
                  ? '输入分类名称并自动记录'
                  : '请输入或选择分类',
              onChanged: (_) {},
              onCategorySelected: (_) {},
              onClear: () {},
              colorResolver: (category) =>
                  category.type == 1 ? Colors.green : Colors.red,
              iconResolver: (category) {
                final iconName = category.icon;
                if (iconName == null || iconName.isEmpty) {
                  return category.type == 1
                      ? Icons.trending_up
                      : Icons.trending_down;
                }
                return IconMapper.getIconData(iconName);
              },
            ),
            if (categories.isNotEmpty) ...[
              const SizedBox(height: 8),
              _buildCategoryShortcuts(theme, categories),
            ],
          ],
        );
      },
    );
  }

  Widget _buildCategoryShortcuts(ThemeData theme, List<Category> categories) {
    final shortcuts = categories.take(6).toList();
    if (shortcuts.isEmpty) {
      return const SizedBox.shrink();
    }
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: shortcuts.map((category) {
        final isSelected = _categoryController.text.trim() == category.name;
        final color = category.type == 1 ? Colors.green : Colors.red;
        return ChoiceChip(
          label: Text(category.name),
          selected: isSelected,
          onSelected: (_) {
            _categoryController.text = category.name;
            _categoryController.selection = TextSelection.fromPosition(
              TextPosition(offset: category.name.length),
            );
          },
          selectedColor: color.withAlpha(36),
          side: BorderSide(color: color.withAlpha(60)),
        );
      }).toList(),
    );
  }
}

class _RemarkSection extends StatelessWidget {
  const _RemarkSection({
    super.key,
    required this.showRemarkField,
    required this.onRemarkToggle,
  });

  final bool showRemarkField;
  final ValueChanged<bool> onRemarkToggle;

  @override
  Widget build(BuildContext context) {
    return AnimationUtils.createFadeIn(
      duration: const Duration(milliseconds: 300),
      delay: const Duration(milliseconds: 250),
      child: _RemarkField(
        showRemarkField: showRemarkField,
        onRemarkToggle: onRemarkToggle,
      ),
    );
  }
}

class _RemarkField extends StatefulWidget {
  const _RemarkField({
    super.key,
    required this.showRemarkField,
    required this.onRemarkToggle,
  });

  final bool showRemarkField;
  final ValueChanged<bool> onRemarkToggle;

  @override
  State<_RemarkField> createState() => _RemarkFieldState();
}

class _RemarkFieldState extends State<_RemarkField> {
  final TextEditingController _remarkController = TextEditingController();
  final ImageUploadService _imageUploadService = ImageUploadService();
  String? _selectedImagePath;
  bool _isUploading = false;

  @override
  void dispose() {
    _remarkController.dispose();
    super.dispose();
  }

  String get remark => _remarkController.text.trim();
  String? get imagePath => _selectedImagePath;
  
  /// 重置所有输入状态
  void reset() {
    setState(() {
      _remarkController.clear();
      _selectedImagePath = null;
    });
  }

  Future<void> _pickImage() async {
    print('🎯 按钮被点击：开始执行 _pickImage 方法');
    

    
    print('🔍 检查上传状态：$_isUploading');
    if (_isUploading) {
      print('⚠️ 正在上传中，退出');
      return;
    }
    
    print('🔄 设置上传状态为 true');
    setState(() => _isUploading = true);
    print('✅ 上传状态设置完成');
    
    try {
      print('🔄 首页: 开始选择图片...');
      final imageFile = await _imageUploadService.pickImageFromGallery();
      print('📁 图片选择服务调用完成');
      
      if (imageFile != null && mounted) {
        print('✅ 首页: 图片选择成功: ${imageFile.path}');
        // 保存图片到应用目录并获取路径
        final imagePath = await _imageUploadService.saveImageToAppDirectory(imageFile);
        print('📁 首页: 图片保存到: $imagePath');
        print('🔄 设置图片路径到表单前的值: $_selectedImagePath');
        setState(() => _selectedImagePath = imagePath);
        print('🔄 设置图片路径到表单后的值: $_selectedImagePath');
        print('💾 首页: 设置图片路径到表单: $_selectedImagePath');
        print('🔍 立即检查imagePath属性: $imagePath');
        
        // 🔧 设置全局图片路径变量，绕过GlobalKey问题
        QuickEntryWidget._globalImagePath = imagePath;
        print('🌐 设置全局图片路径: $imagePath');  // 检查通过getter获取的值
        
        // 显示成功提示
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('图片选择并保存成功！'),
            backgroundColor: Colors.green,
          ),
        );
      } else {
        print('⚠️ 首页: 图片选择被取消或失败');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('图片选择被取消'),
              backgroundColor: Colors.orange,
            ),
          );
        }
      }
    } catch (e) {
      print('❌ 首页: 图片选择出错: $e');
      if (mounted) {
        // 根据错误类型显示不同的提示信息
        String errorMessage;
        if (e.toString().contains('永久拒绝')) {
          errorMessage = '权限被永久拒绝，请在手机设置中为应用授权相册和存储权限';
        } else if (e.toString().contains('权限被拒绝')) {
          errorMessage = '权限被拒绝，请授权应用访问相册和存储';
        } else {
          errorMessage = '图片选择失败: $e';
        }
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(errorMessage),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
            action: e.toString().contains('权限') ? SnackBarAction(
              label: '去设置',
              onPressed: () {
                // 打开应用设置页面
                PermissionService.openAppSettingsPage();
              },
            ) : null,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isUploading = false);
      }
    }
  }

  void _removeImage() {
    setState(() => _selectedImagePath = null);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextButton.icon(
          onPressed: () => widget.onRemarkToggle(!widget.showRemarkField),
          icon: Icon(
            widget.showRemarkField ? Icons.expand_less : Icons.note_alt_outlined,
          ),
          label: Text(widget.showRemarkField ? '收起备注' : '添加备注'),
        ),
        AnimatedCrossFade(
          firstChild: const SizedBox.shrink(),
          secondChild: Column(
            children: [
              TextField(
                controller: _remarkController,
                maxLines: 2,
                decoration: const InputDecoration(
                  labelText: '备注',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.all(Radius.circular(12)),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              // 图片上传区域
              _buildImageUploadSection(),
            ],
          ),
          crossFadeState: widget.showRemarkField
              ? CrossFadeState.showSecond
              : CrossFadeState.showFirst,
          duration: const Duration(milliseconds: 200),
        ),
      ],
    );
  }

  Widget _buildImageUploadSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              '图片附件',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(width: 8),
            if (_selectedImagePath != null)
              Text(
                '(已选择1张图片)',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            // 上传按钮
            ElevatedButton.icon(
              onPressed: _isUploading ? null : _pickImage,
              icon: _isUploading
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.add_photo_alternate_outlined, size: 18),
              label: Text(_isUploading ? '上传中...' : '选择图片'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(120, 40),
              ),
            ),
            const SizedBox(width: 8),
            // 图片预览
            if (_selectedImagePath != null) ...[
              Expanded(
                child: Container(
                  height: 60,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: Theme.of(context).dividerColor,
                    ),
                  ),
                  child: Stack(
                    children: [
                      // 图片预览
                      ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.file(
                          File(_selectedImagePath!),
                          width: double.infinity,
                          height: double.infinity,
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) {
                            return Container(
                              color: Theme.of(context).colorScheme.surfaceContainerLow,
                              child: const Icon(Icons.broken_image, size: 24),
                            );
                          },
                        ),
                      ),
                      // 删除按钮
                      Positioned(
                        top: 4,
                        right: 4,
                        child: GestureDetector(
                          onTap: _removeImage,
                          child: Container(
                            padding: const EdgeInsets.all(4),
                            decoration: BoxDecoration(
                              color: Colors.black.withOpacity(0.6),
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(
                              Icons.close,
                              size: 16,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ],
    );
  }
}

class _KeypadSection extends StatelessWidget {
  const _KeypadSection({
    required this.onKeyTap,
  });

  final Function(String) onKeyTap;

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: AnimationUtils.createScaleIn(
        duration: const Duration(milliseconds: 400),
        child: _Keypad(onKeyTap: onKeyTap),
      ),
    );
  }
}

class _Keypad extends StatefulWidget {
  const _Keypad({
    required this.onKeyTap,
  });

  final Function(String) onKeyTap;

  @override
  State<_Keypad> createState() => _KeypadState();
}

class _KeypadState extends State<_Keypad> {
  static const _keys = [
    _KeypadButtonConfig('1'),
    _KeypadButtonConfig('2'),
    _KeypadButtonConfig('3'),
    _KeypadButtonConfig('4'),
    _KeypadButtonConfig('5'),
    _KeypadButtonConfig('6'),
    _KeypadButtonConfig('7'),
    _KeypadButtonConfig('8'),
    _KeypadButtonConfig('9'),
    _KeypadButtonConfig('.'),
    _KeypadButtonConfig('0'),
    _KeypadButtonConfig('DEL', icon: Icons.backspace_outlined),
  ];

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.1,
      ),
      itemCount: _keys.length,
      itemBuilder: (context, index) {
        final config = _keys[index];
        return _KeypadButton(
          label: config.value,
          icon: config.icon,
          onTap: () => widget.onKeyTap(config.value),
        );
      },
    );
  }
}

class _ActionSection extends StatelessWidget {
  const _ActionSection({
    required this.onEntrySaved,
    required this.amountInputKey,
    required this.categorySelectorKey,
    required this.remarkFieldKey,
  });

  final VoidCallback? onEntrySaved;
  final GlobalKey<_AmountInputSectionState> amountInputKey;
  final GlobalKey<_CategorySelectorState> categorySelectorKey;
  final GlobalKey<_RemarkFieldState> remarkFieldKey;

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: AnimationUtils.createFadeIn(
        duration: const Duration(milliseconds: 300),
        delay: const Duration(milliseconds: 350),
        child: _ActionRow(
          onEntrySaved: onEntrySaved,
          amountInputKey: amountInputKey,
          categorySelectorKey: categorySelectorKey,
          remarkFieldKey: remarkFieldKey,
        ),
      ),
    );
  }
}

class _ActionRow extends StatefulWidget {
  const _ActionRow({
    required this.onEntrySaved,
    required this.amountInputKey,
    required this.categorySelectorKey,
    required this.remarkFieldKey,
  });

  final VoidCallback? onEntrySaved;
  final GlobalKey<_AmountInputSectionState> amountInputKey;
  final GlobalKey<_CategorySelectorState> categorySelectorKey;
  final GlobalKey<_RemarkFieldState> remarkFieldKey;

  @override
  State<_ActionRow> createState() => _ActionRowState();
}

class _ActionRowState extends State<_ActionRow> {
  bool _isSaving = false;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        TextButton(
          onPressed: _isSaving ? null : () => _handleClear(),
          child: const Text('清零'),
        ),
        const Spacer(),
        FilledButton.icon(
          onPressed: _isSaving ? null : _handleSave,
          icon: _isSaving
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.check_circle_outline),
          label: Text(_isSaving ? '保存中…' : '保存记账'),
        ),
      ],
    );
  }

  void _handleClear() {
    // 清零逻辑
    widget.amountInputKey.currentState?.resetAmount();
  }

  Future<void> _handleSave() async {
    if (_isSaving) return;

    setState(() => _isSaving = true);

    try {
      // 保存逻辑
      final amount = widget.amountInputKey.currentState?.currentAmount ?? 0;
      final type = widget.amountInputKey.currentState?.currentType ?? 2;
      
      // 从分类选择器状态获取分类值
      final categorySelectorState = widget.categorySelectorKey.currentState as _CategorySelectorState?;
      final category = categorySelectorState?.selectedCategory ?? '';
      final remark = widget.remarkFieldKey.currentState?.remark ?? '';
      
      if (amount <= 0) {
        // 如果金额为0，显示提示
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('请输入有效金额'),
              backgroundColor: Colors.orange,
            ),
          );
        }
        return;
      }

      // 调试信息：打印分类值和选择器状态
      debugPrint('分类验证 - category: "$category", isEmpty: ${category.isEmpty}, isNull: ${category == null}');
      debugPrint('分类选择器状态: ${categorySelectorState != null ? "存在" : "null"}');
      
      if (category.isEmpty) {
        // 如果分类为空，显示提示
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('请选择或输入分类'),
              backgroundColor: Colors.orange,
            ),
          );
        }
        return;
      }

      // 🔧 最终修复：直接使用全局图片路径变量，绕过GlobalKey问题
       final imagePath = QuickEntryWidget._globalImagePath;
       print('💾 保存账单: 账单数据');
       print('   - 金额: $amount');
       print('   - 分类: $category');
       print('   - 备注: $remark');
       print('   - 直接使用的imagePath: $imagePath');
       print('   - 全局图片路径变量: ${QuickEntryWidget._globalImagePath}');

      // 创建账单记录（不手动设置id，让SQLite自动生成）
      final bill = Bill(
        transactionDate: DateTime.now().toIso8601String(),
        amount: amount,
        type: type,
        categoryName: category,
        remark: remark,
        imagePath: imagePath,
      );
      
      // 🔴 关键测试：直接打印完整的Bill对象
      print('🔴 完整Bill对象：$bill');
      print('🔴 Bill对象的imagePath：${bill.imagePath}');
      print('🔴 Bill对象的imagePath类型：${bill.imagePath.runtimeType}');
      print('🔴 Bill对象的imagePath是否为null：${bill.imagePath == null}');
      print('🔴 Bill对象的imagePath是否为空：${bill.imagePath?.isEmpty}');
      
      // 🔴 再次确认全局图片路径变量
      print('🔴 再次检查全局图片路径：${QuickEntryWidget._globalImagePath}');

      // 使用Provider保存记账记录
      final billProvider = Provider.of<BillProvider>(context, listen: false);
      await billProvider.addBill(bill);

      // 保存成功后，更新首页数据
      widget.onEntrySaved?.call();

      // 清空输入
      widget.amountInputKey.currentState?.resetAmount();
      widget.categorySelectorKey.currentState?._categoryController.clear();
      widget.remarkFieldKey.currentState?.reset();  // 使用reset方法重置所有状态

      // 显示保存成功提示
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('记账成功！'),
            backgroundColor: Colors.green,
            duration: Duration(seconds: 1),
          ),
        );
      }
    } catch (e) {
      // 错误处理
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('保存失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
  }
}

// 类型选择组件
class _TypeChip extends StatelessWidget {
  const _TypeChip({
    required this.label,
    required this.isSelected,
    required this.color,
    required this.onTap,
  });

  final String label;
  final bool isSelected;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(vertical: 12),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: isSelected ? color.withAlpha(20) : Colors.transparent,
          border: Border.all(
            color: isSelected ? color : Theme.of(context).dividerColor,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? color : Theme.of(context).colorScheme.onSurface,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }
}

// 键盘按钮配置
class _KeypadButtonConfig {
  const _KeypadButtonConfig(this.value, {this.icon});

  final String value;
  final IconData? icon;
}

// 键盘按钮组件
class _KeypadButton extends StatelessWidget {
  const _KeypadButton({
    required this.label,
    required this.onTap,
    this.icon,
  });

  final String label;
  final VoidCallback onTap;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.surfaceContainerHighest.withAlpha(40),
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Center(
          child: icon != null
              ? Icon(
                  icon,
                  size: 26,
                  color: Theme.of(context).colorScheme.onSurface,
                )
              : Text(
                  label,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
        ),
      ),
    );
  }
}