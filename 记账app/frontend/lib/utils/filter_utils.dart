import '../../data/models/bill_model.dart';
import '../../shared/widgets/advanced_filter_dialog.dart';

/// 高级筛选工具类
class FilterUtils {
  /// 应用高级筛选条件到交易记录列表
  static List<Bill> applyAdvancedFilter(
    List<Bill> bills, 
    FilterOptions filterOptions,
  ) {
    // 1. 按交易类型筛选
    if (filterOptions.transactionType != 0) {
      bills = bills.where((bill) => bill.type == filterOptions.transactionType).toList();
    }

    // 2. 按分类筛选
    if (filterOptions.selectedCategories.isNotEmpty) {
      bills = bills.where((bill) => 
        filterOptions.selectedCategories.contains(bill.categoryName)
      ).toList();
    }

    // 3. 按搜索关键词筛选（搜索分类名称和备注）
    if (filterOptions.searchKeyword.isNotEmpty) {
      final keyword = filterOptions.searchKeyword.toLowerCase();
      bills = bills.where((bill) => 
        bill.categoryName.toLowerCase().contains(keyword) ||
        (bill.remark?.toLowerCase().contains(keyword) ?? false)
      ).toList();
    }

    // 4. 按日期范围筛选
    if (filterOptions.startDate != null) {
      bills = bills.where((bill) => 
        DateTime.parse(bill.transactionDate).isAfter(filterOptions.startDate!.subtract(const Duration(days: 1)))
      ).toList();
    }

    if (filterOptions.endDate != null) {
      bills = bills.where((bill) => 
        DateTime.parse(bill.transactionDate).isBefore(filterOptions.endDate!.add(const Duration(days: 1)))
      ).toList();
    }

    // 5. 按金额范围筛选
    if (filterOptions.minAmount != null) {
      bills = bills.where((bill) => bill.amount >= filterOptions.minAmount!).toList();
    }

    if (filterOptions.maxAmount != null) {
      bills = bills.where((bill) => bill.amount <= filterOptions.maxAmount!).toList();
    }

    // 6. 排序
    bills = applySort(bills, filterOptions.sortOption);

    return bills;
  }

  /// 应用排序
  static List<Bill> applySort(List<Bill> bills, SortOption sortOption) {
    switch (sortOption) {
      case SortOption.dateDesc:
        bills.sort((a, b) => DateTime.parse(b.transactionDate).compareTo(DateTime.parse(a.transactionDate)));
        break;
      case SortOption.dateAsc:
        bills.sort((a, b) => DateTime.parse(a.transactionDate).compareTo(DateTime.parse(b.transactionDate)));
        break;
      case SortOption.amountDesc:
        bills.sort((a, b) => b.amount.compareTo(a.amount));
        break;
      case SortOption.amountAsc:
        bills.sort((a, b) => a.amount.compareTo(b.amount));
        break;
      case SortOption.categoryAsc:
        bills.sort((a, b) => a.categoryName.compareTo(b.categoryName));
        break;
      case SortOption.categoryDesc:
        bills.sort((a, b) => b.categoryName.compareTo(a.categoryName));
        break;
    }
    return bills;
  }

  /// 从所有交易记录中提取所有分类
  static List<String> extractCategories(List<Bill> bills) {
    final categories = <String>{};
    for (final bill in bills) {
      categories.add(bill.categoryName);
    }
    return categories.toList()..sort();
  }

  /// 获取筛选统计信息
  static FilterStatistics getFilterStatistics(List<Bill> originalBills, List<Bill> filteredBills) {
    final totalCount = originalBills.length;
    final filteredCount = filteredBills.length;
    final totalIncome = originalBills.where((bill) => bill.type == 1).fold(0.0, (sum, bill) => sum + bill.amount);
    final totalExpense = originalBills.where((bill) => bill.type == 2).fold(0.0, (sum, bill) => sum + bill.amount);
    final filteredIncome = filteredBills.where((bill) => bill.type == 1).fold(0.0, (sum, bill) => sum + bill.amount);
    final filteredExpense = filteredBills.where((bill) => bill.type == 2).fold(0.0, (sum, bill) => sum + bill.amount);

    return FilterStatistics(
      totalCount: totalCount,
      filteredCount: filteredCount,
      totalIncome: totalIncome,
      totalExpense: totalExpense,
      filteredIncome: filteredIncome,
      filteredExpense: filteredExpense,
    );
  }
}

/// 筛选统计信息
class FilterStatistics {
  final int totalCount;
  final int filteredCount;
  final double totalIncome;
  final double totalExpense;
  final double filteredIncome;
  final double filteredExpense;

  FilterStatistics({
    required this.totalCount,
    required this.filteredCount,
    required this.totalIncome,
    required this.totalExpense,
    required this.filteredIncome,
    required this.filteredExpense,
  });

  /// 筛选结果覆盖率
  double get filterCoverage {
    return totalCount > 0 ? filteredCount / totalCount : 0.0;
  }

  /// 筛选后的净收入
  double get filteredNetIncome {
    return filteredIncome - filteredExpense;
  }
}