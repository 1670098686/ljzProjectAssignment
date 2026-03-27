import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/providers/bill_provider.dart';
import '../../../core/providers/category_provider.dart';
// import '../../../core/providers/budget_provider.dart'; // 预算功能已移除
import '../../../core/services/local_backup_service.dart';
import '../../../shared/utils/helpers.dart';

/// 数据备份恢复页面
class BackupRestorePage extends StatefulWidget {
  const BackupRestorePage({super.key});

  @override
  State<BackupRestorePage> createState() => _BackupRestorePageState();
}

class _BackupRestorePageState extends State<BackupRestorePage> with TickerProviderStateMixin {
  late TabController _tabController;
  bool _isCreatingBackup = false;
  List<BackupFileInfo> _backupFiles = [];
  final Map<String, bool> _selectedDataTypes = {
    'transactions': true,
    'categories': true,
    'budgets': true,
    'saving_goals': true,
  };

  DateTime? _fromDate;
  DateTime? _toDate;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadBackupFiles();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('数据备份与恢复'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.backup), text: '数据备份'),
            Tab(icon: Icon(Icons.restore), text: '数据恢复'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildBackupTab(),
          _buildRestoreTab(),
        ],
      ),
    );
  }

  Widget _buildBackupTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionTitle('备份内容选择'),
          const SizedBox(height: 12),
          _buildDataTypeSelection(),
          const SizedBox(height: 24),
          _buildDateRangeSelection(),
          const SizedBox(height: 24),
          _buildCreateBackupButton(),
        ],
      ),
    );
  }

  Widget _buildRestoreTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionTitle('选择备份文件'),
          const SizedBox(height: 12),
          _buildBackupFileList(),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.bold,
        color: Colors.black87,
      ),
    );
  }

  Widget _buildDataTypeSelection() {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '请选择要备份的数据类型',
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
            const SizedBox(height: 12),
            CheckboxListTile(
              title: const Text('交易记录'),
              subtitle: const Text('收入和支出明细记录'),
              value: _selectedDataTypes['transactions'],
              onChanged: (value) {
                setState(() {
                  _selectedDataTypes['transactions'] = value ?? false;
                });
              },
              controlAffinity: ListTileControlAffinity.leading,
            ),
            CheckboxListTile(
              title: const Text('分类数据'),
              subtitle: const Text('收入支出分类管理'),
              value: _selectedDataTypes['categories'],
              onChanged: (value) {
                setState(() {
                  _selectedDataTypes['categories'] = value ?? false;
                });
              },
              controlAffinity: ListTileControlAffinity.leading,
            ),
            /*
            CheckboxListTile(
              title: const Text('预算数据'),
              subtitle: const Text('月度预算设置'),
              value: _selectedDataTypes['budgets'],
              onChanged: (value) {
                setState(() {
                  _selectedDataTypes['budgets'] = value ?? false;
                });
              },
              controlAffinity: ListTileControlAffinity.leading,
            ),
            */
            CheckboxListTile(
              title: const Text('储蓄目标'),
              subtitle: const Text('理财目标管理'),
              value: _selectedDataTypes['saving_goals'],
              onChanged: (value) {
                setState(() {
                  _selectedDataTypes['saving_goals'] = value ?? false;
                });
              },
              controlAffinity: ListTileControlAffinity.leading,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDateRangeSelection() {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '日期范围（可选）',
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: InkWell(
                    onTap: () async {
                      final date = await showDatePicker(
                        context: context,
                        initialDate: _fromDate ?? DateTime.now(),
                        firstDate: DateTime(2020),
                        lastDate: DateTime.now(),
                      );
                      if (date != null) {
                        setState(() {
                          _fromDate = date;
                        });
                      }
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.grey),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.calendar_today, size: 16),
                          const SizedBox(width: 8),
                          Text(_fromDate != null 
                            ? '${_fromDate!.year}-${_fromDate!.month.toString().padLeft(2, '0')}-${_fromDate!.day.toString().padLeft(2, '0')}'
                            : '开始日期'),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: InkWell(
                    onTap: () async {
                      final date = await showDatePicker(
                        context: context,
                        initialDate: _toDate ?? DateTime.now(),
                        firstDate: _fromDate ?? DateTime(2020),
                        lastDate: DateTime.now(),
                      );
                      if (date != null) {
                        setState(() {
                          _toDate = date;
                        });
                      }
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.grey),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.calendar_today, size: 16),
                          const SizedBox(width: 8),
                          Text(_toDate != null 
                            ? '${_toDate!.year}-${_toDate!.month.toString().padLeft(2, '0')}-${_toDate!.day.toString().padLeft(2, '0')}'
                            : '结束日期'),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () {
                setState(() {
                  _fromDate = null;
                  _toDate = null;
                });
              },
              child: const Text('清除日期范围'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCreateBackupButton() {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: _isCreatingBackup ? null : _createBackup,
        icon: _isCreatingBackup 
          ? const SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : const Icon(Icons.backup),
        label: Text(_isCreatingBackup ? '正在创建备份...' : '创建备份'),
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).primaryColor,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }

  Widget _buildBackupFileList() {
    if (_backupFiles.isEmpty) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            children: [
              Icon(
                Icons.folder_open,
                size: 64,
                color: Colors.grey[400],
              ),
              const SizedBox(height: 16),
              Text(
                '暂无备份文件',
                style: TextStyle(
                  color: Colors.grey[600],
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '请先创建数据备份',
                style: TextStyle(
                  color: Colors.grey[500],
                  fontSize: 14,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: _backupFiles.map((backupFile) {
        return Card(
          elevation: 2,
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: Colors.blue[100],
              child: Icon(
                Icons.backup,
                color: Colors.blue[700],
              ),
            ),
            title: Text(backupFile.fileName),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('创建时间: ${_formatDateTime(backupFile.createdAt)}'),   
                Text('文件大小: ${AppHelpers.formatFileSize(backupFile.fileSize)}'),
                Text('文件路径: ${backupFile.filePath}'),
              ],
            ),
            trailing: PopupMenuButton<String>(
              onSelected: (value) => _handleBackupAction(value, backupFile),
              itemBuilder: (context) => [
                const PopupMenuItem(
                  value: 'restore',
                  child: ListTile(
                    leading: Icon(Icons.restore),
                    title: Text('恢复'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                const PopupMenuItem(
                  value: 'share',
                  child: ListTile(
                    leading: Icon(Icons.share),
                    title: Text('分享'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                const PopupMenuItem(
                  value: 'delete',
                  child: ListTile(
                    leading: Icon(Icons.delete, color: Colors.red),
                    title: Text('删除'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }

  String _formatDateTime(DateTime dateTime) {
    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
           '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
  }

  Future<void> _createBackup() async {
    if (!_selectedDataTypes.values.any((selected) => selected)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请至少选择一种数据类型进行备份')),
      );
      return;
    }

    setState(() {
      _isCreatingBackup = true;
    });

    try {
      // 准备备份数据
      final backupData = <String, dynamic>{};
      
      if (_selectedDataTypes['transactions']!) {
        backupData['transactions'] = [];
      }
      if (_selectedDataTypes['categories']!) {
        backupData['categories'] = [];
      }
      // 预算功能已移除，跳过预算数据备份
      if (_selectedDataTypes['saving_goals']!) {
        backupData['saving_goals'] = [];
      }

      final backupService = LocalBackupService();
      final result = await backupService.createBackup(backupData);

      if (result.success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('备份创建成功: ${result.filePath ?? "成功"}'),
            backgroundColor: Colors.green,
          ),
        );
        _loadBackupFiles();
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result.message),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('备份创建失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isCreatingBackup = false;
        });
      }
    }
  }

  Future<void> _loadBackupFiles() async {
    try {
      final backupService = LocalBackupService();
      final files = await backupService.getBackupFiles();
      setState(() {
        _backupFiles = files;
      });
    } catch (e) {
      // 忽略加载错误
    }
  }

  void _handleBackupAction(String action, BackupFileInfo backupFile) async {       
    switch (action) {
      case 'restore':
        _showRestoreConfirmation(backupFile);
        break;
      case 'share':
        // TODO: 实现分享功能
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('分享功能暂未实现')),
        );
        break;
      case 'delete':
        _showDeleteConfirmation(backupFile);
        break;
    }
  }

  void _showRestoreConfirmation(BackupFileInfo backupFile) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认恢复'),
        content: Text('确定要恢复备份文件 "${backupFile.fileName}" 吗？\n\n此操作将覆盖当前数据，请确保已创建最新备份。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _restoreBackup(backupFile);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
            ),
            child: const Text('确认恢复'),
          ),
        ],
      ),
    );
  }

  void _showDeleteConfirmation(BackupFileInfo backupFile) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除备份文件 "${backupFile.fileName}" 吗？\n\n此操作不可撤销。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _deleteBackupFile(backupFile);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
            ),
            child: const Text('确认删除'),
          ),
        ],
      ),
    );
  }

  Future<void> _restoreBackup(BackupFileInfo backupFile) async {

    try {
      final backupService = LocalBackupService();
      final result = await backupService.restoreBackup(backupFile.filePath);

      if (result.success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('数据恢复成功！${result.message}'),
            backgroundColor: Colors.green,
          ),
        );
        
        // 刷新所有数据
        if (mounted) {
          context.read<BillProvider>().loadBills();
          context.read<CategoryProvider>().loadCategories();
          // 预算功能已移除，跳过预算数据刷新
        }
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result.message),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('恢复失败: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        // 恢复状态已处理
      }
    }
  }

  Future<void> _deleteBackupFile(BackupFileInfo backupFile) async {
    try {
      final backupService = LocalBackupService();
      final result = await backupService.deleteBackupFile(backupFile.filePath);   

      if (result.success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('备份文件删除成功')),
        );
        _loadBackupFiles();
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('删除失败: $e')),
        );
      }
    }
  }
}