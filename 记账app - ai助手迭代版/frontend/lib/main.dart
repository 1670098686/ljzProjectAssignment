import 'package:flutter/material.dart';
import 'app/app.dart';
import 'core/services/local_api_server.dart';

void main() async {
  // 初始化Flutter绑定
  WidgetsFlutterBinding.ensureInitialized();

  // 启动给 Python AI Agent 访问的本地 HTTP Server
  try {
    await LocalApiServer().start();
  } catch (e) {
    // 启动失败不阻塞 App 正常运行（Python 工具会调失败并报错）
    // ignore: avoid_print
    print('⚠️ LocalApiServer 启动失败: $e');
  }

  // 启动应用
  runApp(const MyApp());
}
