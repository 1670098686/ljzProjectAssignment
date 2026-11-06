/// Flutter 本地 HTTP API Server
///
/// 目的: 让 Python AI Agent 在 Windows 宿主机上能通过 ADB forward
/// 调用这个 Server，从而读写 Flutter 本地 SQLite 数据库。
///
/// 暴露端口: 0.0.0.0:8999
/// ADB forward: adb forward tcp:8999 tcp:8999
/// Python 调用: http://localhost:8999/api/bills
library;

import 'dart:convert';
import 'dart:io';

import '../../core/errors/error_center.dart';
import '../../core/services/local_data_service.dart';
import '../../core/services/local_database_service.dart';
import '../../data/models/bill_model.dart';
import '../../data/models/budget_model.dart';
import '../../data/models/saving_goal_model.dart';
import '../../data/services/bill_service.dart';
import '../../data/services/budget_service.dart';
import '../../data/services/category_service.dart';
import '../../data/services/statistics_service.dart';

/// Flutter 本地 HTTP Server —— 给 Python AI Agent 访问 SQLite 用
class LocalApiServer {
  static LocalApiServer? _instance;
  static const int _port = 8999;

  HttpServer? _server;
  final BillService _billService = BillService();
  final CategoryService _categoryService = CategoryService();
  final BudgetService _budgetService = BudgetService(LocalDataService(), ErrorCenter());
  final StatisticsService _statisticsService = StatisticsService();
  final LocalDatabaseService _db = LocalDatabaseService();

  LocalApiServer._();

  factory LocalApiServer() => _instance ??= LocalApiServer._();

  bool get isRunning => _server != null;

  Future<void> start() async {
    if (_server != null) return;
    try {
      _server = await HttpServer.bind(InternetAddress.anyIPv4, _port);
      print('🚀 LocalApiServer 启动成功: http://0.0.0.0:$_port');
      print('   ADB forward: adb forward tcp:$_port tcp:$_port');
      print('   Python 访问: http://localhost:$_port/api/...');
      _server!.listen(_handleRequest);
    } catch (e) {
      print('❌ LocalApiServer 启动失败: $e');
      rethrow;
    }
  }

  Future<void> stop() async {
    await _server?.close(force: true);
    _server = null;
  }

  // ====================================================================
  Future<void> _handleRequest(HttpRequest req) async {
    req.response.headers.contentType = ContentType.json;
    try {
      final path = req.requestedUri.path;
      final method = req.method;
      final q = req.requestedUri.queryParameters;

      if (path == '/api/health') {
        return _ok(req, {'status': 'ok', 'port': _port});
      }

      // ============ categories ============
      if (path == '/api/categories' && method == 'GET') {
        final type = int.tryParse(q['type'] ?? '');
        final cats = await _categoryService.getCategories(type: type);
        return _ok(req, cats.map((c) => c.toJson()).toList());
      }
      if (path == '/api/categories' && method == 'POST') {
        final body = await _readBody(req);
        // 支持按 (name, type) 查找已有分类，有则返回，没有则创建新的
        final name = body['name'] as String? ?? '';
        final type = body['type'] as int? ?? 2;
        if (name.isEmpty) return _fail(req, 400, '分类名称不能为空');
        try {
          final cat = await _categoryService.ensureCategoryExists(name, type);
          return _ok(req, cat.toJson());
        } catch (e) {
          return _fail(req, 500, '创建分类失败: $e');
        }
      }
      final catMatch = RegExp(r'^/api/categories/(\d+)$').firstMatch(path);
      if (catMatch != null && method == 'DELETE') {
        final id = int.parse(catMatch.group(1)!);
        await _categoryService.deleteCategory(id);
        return _ok(req, {'deleted': true});
      }

      // ============ bills ============
      if (path == '/api/bills' && method == 'GET') {
        final bills = await _billService.getBills(
          startDate: q['startDate'],
          endDate: q['endDate'],
          type: int.tryParse(q['type'] ?? ''),
          categoryName: q['categoryName'],
        );
        return _ok(req, bills.map((b) => b.toJson()).toList());
      }
      if (path == '/api/bills' && method == 'POST') {
        final body = await _readBody(req);
        final bill = Bill(
          id: null,
          type: body['type'] as int? ?? 2,
          categoryName: body['categoryName'] as String? ?? '',
          amount: (body['amount'] as num?)?.toDouble() ?? 0,
          transactionDate: body['transactionDate'] as String? ?? '',
          remark: body['remark'] as String?,
          imagePath: body['imagePath'] as String?,
        );
        final saved = await _billService.createBill(bill);
        return _ok(req, saved.toJson());
      }

      // /api/bills/{id}
      final billMatch = RegExp(r'^/api/bills/(\d+)$').firstMatch(path);
      if (billMatch != null) {
        final id = int.parse(billMatch.group(1)!);
        if (method == 'DELETE') {
          await _billService.deleteBill(id);
          return _ok(req, {'deleted': true});
        }
        if (method == 'PUT') {
          final body = await _readBody(req);
          final existing = await _billService.getBill(id);
          if (existing == null) return _fail(req, 404, '账单不存在');
          final updated = existing.copyWith(
            type: body.containsKey('type') ? body['type'] as int : existing.type,
            categoryName: body.containsKey('categoryName') ? body['categoryName'] as String : existing.categoryName,
            amount: body.containsKey('amount') ? (body['amount'] as num).toDouble() : existing.amount,
            transactionDate: body.containsKey('transactionDate') ? body['transactionDate'] as String : existing.transactionDate,
            remark: body.containsKey('remark') ? body['remark'] as String? : existing.remark,
          );
          final saved = await _billService.updateBill(id, updated);
          return _ok(req, saved.toJson());
        }
      }

      // ============ budgets ============
      if (path == '/api/budgets' && method == 'GET') {
        final budgets = await _budgetService.getBudgets(
          year: int.tryParse(q['year'] ?? ''),
          month: int.tryParse(q['month'] ?? ''),
        );
        return _ok(req, budgets.map((b) => b.toJson()).toList());
      }
      if (path == '/api/budgets' && method == 'POST') {
        final body = await _readBody(req);
        final budget = Budget(
          id: null,
          categoryName: body['categoryName'] as String? ?? '',
          amount: (body['amount'] as num?)?.toDouble() ?? 0,
          year: body['year'] as int? ?? DateTime.now().year,
          month: body['month'] as int? ?? DateTime.now().month,
          spent: (body['spent'] as num?)?.toDouble() ?? 0,
          budgetName: body['budgetName'] as String?,
        );
        final saved = await _budgetService.createBudget(budget);
        return _ok(req, saved.toJson());
      }
      // /api/budgets/{id} → DELETE
      final budgetMatch = RegExp(r'^/api/budgets/(\d+)$').firstMatch(path);
      if (budgetMatch != null && method == 'DELETE') {
        final id = int.parse(budgetMatch.group(1)!);
        await _budgetService.deleteBudget(id);
        return _ok(req, {'deleted': true});
      }

      if (path == '/api/budgets/alert' && method == 'GET') {
        final year = int.tryParse(q['year'] ?? '') ?? DateTime.now().year;
        final month = int.tryParse(q['month'] ?? '') ?? DateTime.now().month;
        final budgets = await _budgetService.getBudgets(year: year, month: month);
        final alerts = <Map<String, dynamic>>[];
        for (final b in budgets) {
          final usageRate = b.amount > 0 ? b.spent / b.amount : 0.0;
          final alertLevel = usageRate >= 1.0
              ? 'OVERSPENT'
              : usageRate >= 0.8
                  ? 'WARNING'
                  : 'SAFE';
          alerts.add({
            'id': b.id,
            'categoryName': b.categoryName,
            'amount': b.amount,
            'spent': b.spent,
            'remaining': b.amount - b.spent,
            'usageRate': usageRate,
            'alertLevel': alertLevel,
          });
        }
        return _ok(req, alerts);
      }

      // ============ saving-goals ============
      if (path == '/api/saving-goals' && method == 'GET') {
        final goals = await _db.getAllSavingGoals();
        final items = goals.map((g) {
          final progress = g.targetAmount > 0
              ? (g.currentAmount / g.targetAmount) * 100
              : 0.0;
          return {
            'id': g.id,
            'name': g.name,
            'target_amount': g.targetAmount,
            'current_amount': g.currentAmount,
            'deadline_ms': g.deadline.millisecondsSinceEpoch,
            'deadline_iso': g.deadline.toIso8601String(),
            'description': g.description,
            'category_name': g.categoryName,
            'progress_percent': double.parse(progress.toStringAsFixed(2)),
            'achieved': g.currentAmount >= g.targetAmount,
          };
        }).toList();
        return _ok(req, items);
      }
      if (path == '/api/saving-goals' && method == 'POST') {
        final body = await _readBody(req);
        final deadlineMs = body['deadline_ms'] as int? ??
            DateTime.now().add(const Duration(days: 180)).millisecondsSinceEpoch;
        final goal = SavingGoal(
          id: null,
          name: body['name'] as String? ?? '',
          targetAmount: (body['target_amount'] as num?)?.toDouble() ?? 0,
          currentAmount: (body['current_amount'] as num?)?.toDouble() ?? 0,
          deadline: DateTime.fromMillisecondsSinceEpoch(deadlineMs),
          description: body['description'] as String?,
          categoryName: body['category_name'] as String? ?? '',
        );
        final id = await _db.saveSavingGoal(goal);
        return _ok(req, {...body, 'id': id});
      }
      // /api/saving-goals/{id}/amount → PUT 更新 currentAmount（存钱/取钱）
      final goalAmountMatch = RegExp(r'^/api/saving-goals/(\d+)/amount$').firstMatch(path);
      if (goalAmountMatch != null && method == 'PUT') {
        final id = int.parse(goalAmountMatch.group(1)!);
        final body = await _readBody(req);
        final newAmount = (body['current_amount'] as num?)?.toDouble();
        if (newAmount == null) return _fail(req, 400, '需要提供 current_amount');
        try {
          final existing = await _db.getSavingGoalById(id);
          if (existing == null) return _fail(req, 404, '储蓄目标不存在');
          final updated = SavingGoal(
            id: existing.id,
            name: existing.name,
            targetAmount: existing.targetAmount,
            currentAmount: newAmount,
            deadline: existing.deadline,
            description: existing.description,
            categoryName: existing.categoryName,
          );
          await _db.updateSavingGoal(updated);
          return _ok(req, {'id': id, 'current_amount': newAmount});
        } catch (e) {
          return _fail(req, 500, '更新失败: $e');
        }
      }
      // /api/saving-goals/{id} → DELETE 删除储蓄目标
      final goalDeleteMatch = RegExp(r'^/api/saving-goals/(\d+)$').firstMatch(path);
      if (goalDeleteMatch != null && method == 'DELETE') {
        final id = int.parse(goalDeleteMatch.group(1)!);
        await _db.deleteSavingGoal(id);
        return _ok(req, {'deleted': true});
      }

      // ============ statistics ============
      if (path == '/api/statistics/summary' && method == 'GET') {
        final year = int.tryParse(q['year'] ?? '') ?? DateTime.now().year;
        final month = int.tryParse(q['month'] ?? '') ?? DateTime.now().month;
        final summary = await _statisticsService.getSummary(year, month);
        return _ok(req, {
          'totalIncome': summary.totalIncome,
          'totalExpense': summary.totalExpense,
          'balance': summary.balance,
        });
      }
      if (path == '/api/statistics/by-category' && method == 'GET') {
        final stats = await _statisticsService.getByCategory(
          startDate: q['startDate'] ?? '',
          endDate: q['endDate'] ?? '',
          type: int.tryParse(q['type'] ?? '') ?? 2,
        );
        final total = stats.fold<double>(0, (s, e) => s + e.amount);
        return _ok(req, stats.map((s) => ({
          'categoryName': s.categoryName,
          'amount': s.amount,
          'percentage': total > 0 ? (s.amount / total * 100) : 0,
        })).toList());
      }
      if (path == '/api/statistics/trend' && method == 'GET') {
        final trend = await _statisticsService.getTrend(
          startDate: q['startDate'] ?? '',
          endDate: q['endDate'] ?? '',
        );
        return _ok(req, trend.map((t) => ({
          'date': t.date,       // 已是 '2026-11-01' String
          'income': t.income,
          'expense': t.expense,
        })).toList());
      }

      return _fail(req, 404, '未找到路由: $method $path');
    } catch (e, st) {
      print('❌ LocalApiServer 请求异常: $e\n$st');
      return _fail(req, 500, '内部错误: $e');
    }
  }

  // ====================================================================
  Future<Map<String, dynamic>> _readBody(HttpRequest req) async {
    // 正确方式：expand 把 Stream<List<int>> 展平成 Stream<int>，toList() 收集所有 bytes
    final bytes = await req.expand((e) => e).toList();
    if (bytes.isEmpty) return {};
    try {
      final content = utf8.decode(bytes);
      final raw = jsonDecode(content);
      if (raw is! Map<String, dynamic>) return {};
      // jsonDecode 会把整数解析成 double，需要转回 int 方便后端判断
      return _normalizeInts(raw);
    } catch (e) {
      print('❌ _readBody JSON 解析失败: $e, raw bytes len=${bytes.length}');
      return {};
    }
  }

  /// 递归把 JSON 里的 int-like double（如 2.0 → 2）转回 int
  Map<String, dynamic> _normalizeInts(Map<String, dynamic> map) {
    final result = <String, dynamic>{};
    for (final entry in map.entries) {
      final v = entry.value;
      if (v is double && v == v.truncateToDouble()) {
        result[entry.key] = v.toInt();
      } else if (v is Map<String, dynamic>) {
        result[entry.key] = _normalizeInts(v);
      } else if (v is List) {
        result[entry.key] = v.map((e) {
          if (e is double && e == e.truncateToDouble()) return e.toInt();
          if (e is Map<String, dynamic>) return _normalizeInts(e);
          return e;
        }).toList();
      } else {
        result[entry.key] = v;
      }
    }
    return result;
  }

  Future<void> _ok(HttpRequest req, dynamic data) async {
    req.response.statusCode = HttpStatus.ok;
    req.response.write(jsonEncode({'code': 200, 'message': 'OK', 'data': data}));
    await req.response.close();
  }

  Future<void> _fail(HttpRequest req, int code, String message) async {
    req.response.statusCode = code;
    req.response.write(jsonEncode({'code': code, 'message': message, 'data': null}));
    await req.response.close();
  }
}
