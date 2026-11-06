import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// 高级筛选和搜索对话框组件
class AdvancedFilterDialog extends StatefulWidget {
  final List<String> categories;
  final FilterOptions? initialFilter;

  const AdvancedFilterDialog({
    super.key,
    required this.categories,
    this.initialFilter,
  });

  @override
  State<AdvancedFilterDialog> createState() => _AdvancedFilterDialogState();
}

class FilterOptions {
  /// 交易类型：0=全部，1=收入，2=支出
  int transactionType;
  
  /// 选中的分类列表
  List<String> selectedCategories;
  
  /// 搜索关键词
  String searchKeyword;
  
  /// 开始日期
  DateTime? startDate;
  
  /// 结束日期
  DateTime? endDate;
  
  /// 金额范围 - 最小值
  double? minAmount;
  
  /// 金额范围 - 最大值
  double? maxAmount;
  
  /// 排序方式
  SortOption sortOption;

  FilterOptions({
    this.transactionType = 0,
    this.selectedCategories = const [],
    this.searchKeyword = '',
    this.startDate,
    this.endDate,
    this.minAmount,
    this.maxAmount,
    this.sortOption = SortOption.dateDesc,
  });

  FilterOptions copyWith({
    int? transactionType,
    List<String>? selectedCategories,
    String? searchKeyword,
    DateTime? startDate,
    DateTime? endDate,
    double? minAmount,
    double? maxAmount,
    SortOption? sortOption,
  }) {
    return FilterOptions(
      transactionType: transactionType ?? this.transactionType,
      selectedCategories: selectedCategories ?? this.selectedCategories,
      searchKeyword: searchKeyword ?? this.searchKeyword,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      minAmount: minAmount ?? this.minAmount,
      maxAmount: maxAmount ?? this.maxAmount,
      sortOption: sortOption ?? this.sortOption,
    );
  }
}

enum SortOption {
  dateDesc('日期降序'),
  dateAsc('日期升序'),
  amountDesc('金额降序'),
  amountAsc('金额升序'),
  categoryAsc('分类升序'),
  categoryDesc('分类降序');

  const SortOption(this.label);
  final String label;
}

class _AdvancedFilterDialogState extends State<AdvancedFilterDialog> {
  late FilterOptions _filterOptions;

  @override
  void initState() {
    super.initState();
    _filterOptions = widget.initialFilter ?? FilterOptions();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Container(
        width: MediaQuery.of(context).size.width * 0.95,
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.8,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 标题栏
            _buildHeader(),
            
            // 内容区域
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 搜索框
                    _buildSearchSection(),
                    
                    const SizedBox(height: 20),
                    
                    // 交易类型
                    _buildTypeSection(),
                    
                    const SizedBox(height: 20),
                    
                    // 分类筛选
                    _buildCategorySection(),
                    
                    const SizedBox(height: 20),
                    
                    // 日期范围
                    _buildDateSection(),
                    
                    const SizedBox(height: 20),
                    
                    // 金额范围
                    _buildAmountSection(),
                    
                    const SizedBox(height: 20),
                    
                    // 排序选项
                    _buildSortSection(),
                  ],
                ),
              ),
            ),
            
            // 底部按钮
            _buildBottomButtons(),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).dividerColor,
          ),
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.filter_list, size: 24),
          const SizedBox(width: 12),
          const Text(
            '高级筛选',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
            ),
          ),
          const Spacer(),
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.close),
          ),
        ],
      ),
    );
  }

  Widget _buildSearchSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '搜索',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          decoration: InputDecoration(
            hintText: '输入分类名称或备注搜索',
            prefixIcon: const Icon(Icons.search),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 8,
            ),
          ),
          onChanged: (value) {
            setState(() {
              _filterOptions = _filterOptions.copyWith(searchKeyword: value);
            });
          },
        ),
      ],
    );
  }

  Widget _buildTypeSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '交易类型',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: _buildTypeChip(0, '全部'),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _buildTypeChip(1, '收入'),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _buildTypeChip(2, '支出'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildTypeChip(int value, String label) {
    final isSelected = _filterOptions.transactionType == value;
    
    return FilterChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        setState(() {
          _filterOptions = _filterOptions.copyWith(transactionType: value);
        });
      },
      backgroundColor: Theme.of(context).colorScheme.surface,
      selectedColor: Theme.of(context).colorScheme.primaryContainer,
    );
  }

  Widget _buildCategorySection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '分类筛选',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        if (widget.categories.isEmpty)
          Text(
            '暂无分类数据',
            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          )
        else
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: widget.categories.map((category) {
              final isSelected = _filterOptions.selectedCategories.contains(category);
              
              return FilterChip(
                label: Text(category),
                selected: isSelected,
                onSelected: (selected) {
                  setState(() {
                    final newCategories = List<String>.from(_filterOptions.selectedCategories);
                    if (selected) {
                      newCategories.add(category);
                    } else {
                      newCategories.remove(category);
                    }
                    _filterOptions = _filterOptions.copyWith(selectedCategories: newCategories);
                  });
                },
                backgroundColor: Theme.of(context).colorScheme.surface,
                selectedColor: Theme.of(context).colorScheme.primaryContainer,
              );
            }).toList(),
          ),
      ],
    );
  }

  Widget _buildDateSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '日期范围',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: _buildDateButton(
                _filterOptions.startDate,
                '开始日期',
                (date) => setState(() {
                  _filterOptions = _filterOptions.copyWith(startDate: date);
                }),
              ),
            ),
            const SizedBox(width: 12),
            const Text('至'),
            const SizedBox(width: 12),
            Expanded(
              child: _buildDateButton(
                _filterOptions.endDate,
                '结束日期',
                (date) => setState(() {
                  _filterOptions = _filterOptions.copyWith(endDate: date);
                }),
              ),
            ),
          ],
        ),
        if (_filterOptions.startDate != null || _filterOptions.endDate != null) ...[
          const SizedBox(height: 8),
          Row(
            children: [
              const Spacer(),
              TextButton.icon(
                onPressed: () {
                  setState(() {
                    _filterOptions = _filterOptions.copyWith(
                      startDate: null,
                      endDate: null,
                    );
                  });
                },
                icon: const Icon(Icons.clear, size: 16),
                label: const Text('清除日期'),
                style: TextButton.styleFrom(
                  foregroundColor: Theme.of(context).colorScheme.error,
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }

  Widget _buildDateButton(DateTime? date, String label, Function(DateTime?) onPressed) {
    return OutlinedButton.icon(
      onPressed: () async {
        final result = await showDatePicker(
          context: context,
          initialDate: date ?? DateTime.now(),
          firstDate: DateTime(2020),
          lastDate: DateTime.now(),
        );
        onPressed(result);
      },
      icon: const Icon(Icons.calendar_today, size: 18),
      label: Text(
        date != null 
          ? DateFormat('yyyy-MM-dd').format(date)
          : label,
        style: TextStyle(
          color: date != null 
            ? Theme.of(context).colorScheme.onSurface
            : Theme.of(context).colorScheme.onSurfaceVariant,
        ),
      ),
      style: OutlinedButton.styleFrom(
        alignment: Alignment.centerLeft,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      ),
    );
  }

  Widget _buildAmountSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '金额范围',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: TextField(
                decoration: const InputDecoration(
                  hintText: '最小金额',
                  prefixIcon: Icon(Icons.attach_money),
                  border: OutlineInputBorder(),
                  contentPadding: EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 8,
                  ),
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                onChanged: (value) {
                  final amount = double.tryParse(value);
                  setState(() {
                    _filterOptions = _filterOptions.copyWith(minAmount: amount);
                  });
                },
              ),
            ),
            const SizedBox(width: 12),
            const Text('至'),
            const SizedBox(width: 12),
            Expanded(
              child: TextField(
                decoration: const InputDecoration(
                  hintText: '最大金额',
                  prefixIcon: Icon(Icons.attach_money),
                  border: OutlineInputBorder(),
                  contentPadding: EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 8,
                  ),
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                onChanged: (value) {
                  final amount = double.tryParse(value);
                  setState(() {
                    _filterOptions = _filterOptions.copyWith(maxAmount: amount);
                  });
                },
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildSortSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '排序方式',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        DropdownButtonFormField<SortOption>(
          initialValue: _filterOptions.sortOption,
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 8,
            ),
          ),
          items: SortOption.values.map((option) {
            return DropdownMenuItem(
              value: option,
              child: Text(option.label),
            );
          }).toList(),
          onChanged: (option) {
            if (option != null) {
              setState(() {
                _filterOptions = _filterOptions.copyWith(sortOption: option);
              });
            }
          },
        ),
      ],
    );
  }

  Widget _buildBottomButtons() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: Theme.of(context).dividerColor,
          ),
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton(
              onPressed: () {
                _filterOptions = FilterOptions();
                setState(() {});
              },
              child: const Text('重置'),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: ElevatedButton(
              onPressed: () => Navigator.pop(context, _filterOptions),
              child: const Text('确定'),
            ),
          ),
        ],
      ),
    );
  }
}