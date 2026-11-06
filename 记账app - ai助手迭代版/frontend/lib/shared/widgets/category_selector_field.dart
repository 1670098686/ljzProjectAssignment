import 'package:flutter/material.dart';

import '../../data/models/category_model.dart';

typedef CategoryColorResolver = Color Function(Category category);
typedef CategoryIconResolver = IconData Function(Category category);

class CategorySelectorField extends StatefulWidget {
  const CategorySelectorField({
    super.key,
    required this.controller,
    required this.categories,
    this.labelText = '分类',
    this.hintText = '请输入或选择分类',
    this.onChanged,
    this.onCategorySelected,
    this.onClear,
    this.colorResolver,
    this.iconResolver,
    this.showClearButton = true,
  });

  final TextEditingController controller;
  final List<Category> categories;
  final String labelText;
  final String hintText;
  final ValueChanged<String>? onChanged;
  final ValueChanged<Category>? onCategorySelected;
  final VoidCallback? onClear;
  final CategoryColorResolver? colorResolver;
  final CategoryIconResolver? iconResolver;
  final bool showClearButton;

  @override
  State<CategorySelectorField> createState() => _CategorySelectorFieldState();
}

class _CategorySelectorFieldState extends State<CategorySelectorField> {
  late String _currentValue;
  late final FocusNode _focusNode;

  @override
  void initState() {
    super.initState();
    _currentValue = widget.controller.text;
    _focusNode = FocusNode();
    _focusNode.addListener(_handleFocusChange);
    widget.controller.addListener(_handleControllerChanged);
  }

  @override
  void didUpdateWidget(CategorySelectorField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller.removeListener(_handleControllerChanged);
      _currentValue = widget.controller.text;
      widget.controller.addListener(_handleControllerChanged);
    }
  }

  @override
  void dispose() {
    widget.controller.removeListener(_handleControllerChanged);
    _focusNode.removeListener(_handleFocusChange);
    _focusNode.dispose();
    super.dispose();
  }

  void _handleControllerChanged() {
    if (_currentValue != widget.controller.text) {
      setState(() {
        _currentValue = widget.controller.text;
      });
    }
  }

  void _handleFocusChange() {
    if (!mounted) {
      return;
    }
    setState(() {});
  }

  List<Category> _filteredCategories(String keyword) {
    if (keyword.isEmpty) {
      return widget.categories;
    }

    return widget.categories
        .where(
          (category) =>
              category.name.toLowerCase().contains(keyword.toLowerCase()),
        )
        .toList();
  }

  void _handleSuggestionTap(Category category) {
    if (widget.controller.text != category.name) {
      widget.controller.text = category.name;
      widget.controller.selection = TextSelection.fromPosition(
        TextPosition(offset: widget.controller.text.length),
      );
    }
    widget.onCategorySelected?.call(category);

    if (_focusNode.hasFocus) {
      _focusNode.unfocus();
    }
  }

  Widget _buildSuggestionList(ThemeData theme, List<Category> suggestions) {
    if (suggestions.isEmpty || !_focusNode.hasFocus) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Container(
        constraints: const BoxConstraints(maxHeight: 220),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: theme.colorScheme.outline),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha((0.05 * 255).round()),
              blurRadius: 12,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: ListView.separated(
          shrinkWrap: true,
          primary: false,
          padding: EdgeInsets.zero,
          itemCount: suggestions.length,
          separatorBuilder: (_, index) => Divider(
            height: 1,
            color: theme.colorScheme.outline.withAlpha((0.3 * 255).round()),
          ),
          itemBuilder: (context, index) {
            final category = suggestions[index];
            final color =
                widget.colorResolver?.call(category) ??
                theme.colorScheme.primary;
            final icon = widget.iconResolver?.call(category) ?? Icons.category;

            return ListTile(
              dense: true,
              onTap: () => _handleSuggestionTap(category),
              leading: CircleAvatar(
                radius: 16,
                backgroundColor: color.withAlpha(30),
                child: Icon(icon, color: color, size: 18),
              ),
              title: Text(category.name, style: theme.textTheme.bodyMedium),
            );
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final trimmedValue = _currentValue.trim();
    final suggestions = _filteredCategories(trimmedValue);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          focusNode: _focusNode,
          controller: widget.controller,
          decoration: InputDecoration(
            labelText: widget.labelText,
            hintText: widget.hintText,
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            suffixIcon: widget.showClearButton && trimmedValue.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () {
                      widget.controller.clear();
                      widget.onClear?.call();
                    },
                  )
                : null,
          ),
          onChanged: widget.onChanged,
        ),
        _buildSuggestionList(theme, suggestions),
      ],
    );
  }
}
