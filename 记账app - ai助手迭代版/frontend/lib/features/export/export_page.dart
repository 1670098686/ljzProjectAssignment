import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/services/local_data_export_service.dart';
import '../../core/providers/bill_provider.dart';
// import '../../core/providers/budget_provider.dart'; // 预算功能已移除
import '../../core/providers/saving_goal_provider.dart';

/// 数据导出页面
class ExportPage extends StatefulWidget {
  const ExportPage({super.key});

  @override
  State<ExportPage> createState() => _ExportPageState();
}

class _ExportPageState extends State<ExportPage> {
  late LocalDataExportService _exportService;

  String _selectedFormat = 'CSV';
  String _selectedExportType = 'transactions';
  DateTime? _startDate;
  DateTime? _endDate;
  int? _selectedType;
  String? _selectedCategory;
  int? _selectedYear;
  int? _selectedMonth;
  String _selectedGranularity = 'daily';

  bool _isExporting = false;

  @override
  void initState() {
    super.initState();
    
    // 通过 Provider 获取数据
    final billProvider = Provider.of<BillProvider>(context, listen: false);
    final savingGoalProvider = Provider.of<SavingGoalProvider>(context, listen: false);
    
    _exportService = LocalDataExportService(
      billProvider,
      savingGoalProvider,
    );

    // 设置默认日期范围（当前月份）
    final now = DateTime.now();
    _startDate = DateTime(now.year, now.month, 1);
    _endDate = DateTime(now.year, now.month + 1, 0);
    _selectedYear = now.year;
    _selectedMonth = now.month;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('数据导出'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildExportTypeSection(),
            const SizedBox(height: 24),
            _buildFormatSection(),
            const SizedBox(height: 24),
            _buildFilterSection(),
            const SizedBox(height: 32),
            _buildExportButton(),
          ],
        ),
      ),
    );
  }

  /// 构建导出类型选择区域
  Widget _buildExportTypeSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '导出类型',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildExportTypeChip('transactions', '交易记录', Icons.receipt),
                /*
                _buildExportTypeChip(
                  'budgets',
                  '预算数据',
                  Icons.account_balance_wallet,
                ),
                */
                _buildExportTypeChip('saving_goals', '储蓄目标', Icons.savings),
                _buildExportTypeChip('statistics', '统计报表', Icons.analytics),
                _buildExportTypeChip('all', '全部数据', Icons.all_inclusive),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 构建导出类型芯片
  Widget _buildExportTypeChip(String value, String label, IconData icon) {
    final isSelected = _selectedExportType == value;
    return FilterChip(
      selected: isSelected,
      onSelected: (selected) {
        setState(() {
          _selectedExportType = value;
        });
      },
      label: Row(
        mainAxisSize: MainAxisSize.min,
        children: [Icon(icon, size: 16), const SizedBox(width: 4), Text(label)],
      ),
      selectedColor: Theme.of(context).primaryColor.withValues(alpha: 0.1),
      checkmarkColor: Theme.of(context).primaryColor,
    );
  }

  /// 构建格式选择区域
  Widget _buildFormatSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '导出格式',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [
                _buildFormatChip('CSV', 'CSV'),
                _buildFormatChip('JSON', 'JSON'),
                if (_selectedExportType == 'all')
                  _buildFormatChip('ZIP', 'ZIP'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 构建格式芯片
  Widget _buildFormatChip(String value, String label) {
    final isSelected = _selectedFormat == value;
    return FilterChip(
      selected: isSelected,
      onSelected: (selected) {
        setState(() {
          _selectedFormat = value;
        });
      },
      label: Text(label),
      selectedColor: Theme.of(context).primaryColor.withValues(alpha: 0.1),
      checkmarkColor: Theme.of(context).primaryColor,
    );
  }

  /// 构建筛选条件区域
  Widget _buildFilterSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '筛选条件',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),

            // 日期范围筛选
            if (_selectedExportType == 'transactions' ||
                _selectedExportType == 'statistics')
              _buildDateRangeFilter(),

            // 交易类型筛选
            if (_selectedExportType == 'transactions')
              _buildTransactionTypeFilter(),

            // 年份月份筛选
            if (_selectedExportType == 'budgets') _buildYearMonthFilter(),

            // 统计粒度筛选
            if (_selectedExportType == 'statistics') _buildGranularityFilter(),
          ],
        ),
      ),
    );
  }

  /// 构建日期范围筛选
  Widget _buildDateRangeFilter() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('日期范围', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: _buildDatePicker(
                '开始日期',
                _startDate,
                (date) => setState(() => _startDate = date),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _buildDatePicker(
                '结束日期',
                _endDate,
                (date) => setState(() => _endDate = date),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  /// 构建交易类型筛选
  Widget _buildTransactionTypeFilter() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('交易类型', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: [
            _buildTypeChip(null, '全部'),
            _buildTypeChip(1, '收入'),
            _buildTypeChip(2, '支出'),
          ],
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  /// 构建年份月份筛选
  Widget _buildYearMonthFilter() {
    final now = DateTime.now();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('预算期间', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: DropdownButtonFormField<int>(
                initialValue: _selectedYear,
                decoration: const InputDecoration(labelText: '年份'),
                items: List.generate(5, (index) {
                  final year = now.year - 2 + index;
                  return DropdownMenuItem(value: year, child: Text('$year年'));
                }),
                onChanged: (value) => setState(() => _selectedYear = value),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: DropdownButtonFormField<int>(
                initialValue: _selectedMonth,
                decoration: const InputDecoration(labelText: '月份'),
                items: List.generate(12, (index) {
                  final month = index + 1;
                  return DropdownMenuItem(value: month, child: Text('$month月'));
                }),
                onChanged: (value) => setState(() => _selectedMonth = value),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  /// 构建统计粒度筛选
  Widget _buildGranularityFilter() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('统计粒度', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: [
            _buildGranularityChip('daily', '日'),
            _buildGranularityChip('weekly', '周'),
            _buildGranularityChip('monthly', '月'),
          ],
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  /// 构建日期选择器
  Widget _buildDatePicker(
    String label,
    DateTime? value,
    Function(DateTime?) onChanged,
  ) {
    return TextFormField(
      decoration: InputDecoration(labelText: label),
      controller: TextEditingController(
        text: value != null
            ? '${value.year}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}'
            : '',
      ),
      readOnly: true,
      onTap: () async {
        final date = await showDatePicker(
          context: context,
          initialDate: value ?? DateTime.now(),
          firstDate: DateTime(2020),
          lastDate: DateTime(2030),
        );
        if (date != null) {
          onChanged(date);
        }
      },
    );
  }

  /// 构建类型芯片
  Widget _buildTypeChip(int? value, String label) {
    final isSelected = _selectedType == value;
    return FilterChip(
      selected: isSelected,
      onSelected: (selected) {
        setState(() {
          _selectedType = value;
        });
      },
      label: Text(label),
      selectedColor: Theme.of(context).primaryColor.withValues(alpha: 0.1),
      checkmarkColor: Theme.of(context).primaryColor,
    );
  }

  /// 构建粒度芯片
  Widget _buildGranularityChip(String value, String label) {
    final isSelected = _selectedGranularity == value;
    return FilterChip(
      selected: isSelected,
      onSelected: (selected) {
        setState(() {
          _selectedGranularity = value;
        });
      },
      label: Text(label),
      selectedColor: Theme.of(context).primaryColor.withValues(alpha: 0.1),
      checkmarkColor: Theme.of(context).primaryColor,
    );
  }

  /// 构建导出按钮
  Widget _buildExportButton() {
    return Center(
      child: ElevatedButton(
        onPressed: _isExporting ? null : _performExport,
        child: _isExporting
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
              )
            : const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.download, size: 20),
                  SizedBox(width: 8),
                  Text('开始导出'),
                ],
              ),
      ),
    );
  }

  /// 显示成功对话框
  Future<void> _performExport() async {
    if (_isExporting) return;

    setState(() {
      _isExporting = true;
    });

    try {
      ExportResult result;

      switch (_selectedExportType) {
        case 'transactions':
          if (_selectedFormat == 'CSV') {
            result = await _exportService.exportTransactionsToCsv(
              startDate: _startDate,
              endDate: _endDate,
              type: _selectedType,
              category: _selectedCategory,
            );
          } else {
            result = await _exportService.exportTransactionsToJson(
              startDate: _startDate,
              endDate: _endDate,
              type: _selectedType,
              category: _selectedCategory,
            );
          }
          break;
        case 'budgets':
          if (_selectedFormat == 'CSV') {
            result = await _exportService.exportBudgetsToCsv(
              year: _selectedYear,
              month: _selectedMonth,
            );
          } else {
            result = await _exportService.exportBudgetsToCsv(
              year: _selectedYear,
              month: _selectedMonth,
            );
          }
          break;
        case 'saving_goals':
          result = await _exportService.exportSavingGoalsToCsv();
          break;
        case 'statistics':
          // 统计报表暂未实现
          result = ExportResult(
            success: false,
            message: '统计报表导出功能暂未实现',
          );
          break;
        case 'all':
          result = await _exportService.exportAllDataToJson();
          break;
        default:
          throw Exception('不支持的导出类型: $_selectedExportType');
      }

      if (result.success && result.data != null && result.fileName != null) {
        // 保存并分享文件
        await _exportService.saveAndShareFile(result);
        _showSuccessDialog(result.message);
      } else {
        _showErrorDialog(result.message);
      }
    } catch (e) {
      _showErrorDialog('导出失败: $e');
    } finally {
      setState(() {
        _isExporting = false;
      });
    }
  }

  /// 显示成功对话框
  void _showSuccessDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('导出成功'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  /// 显示错误对话框
  void _showErrorDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('导出失败'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }
}
