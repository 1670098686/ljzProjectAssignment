# 前后端错误码映射规范

## 概述
本文档定义了前后端错误处理的标准规范，包括错误码映射、错误消息格式和错误处理流程。

## 后端错误码定义

### HTTP状态码映射
| HTTP状态码 | 后端错误码 | 前端错误码 | 描述 |
|-----------|-----------|-----------|------|
| 200 | 200 | - | 成功 |
| 400 | 400 | API_BAD_REQUEST | 请求参数错误 |
| 401 | 401 | API_UNAUTHORIZED | 身份验证失效 |
| 403 | 403 | API_FORBIDDEN | 权限不足 |
| 404 | 404 | API_NOT_FOUND | 资源不存在 |
| 429 | 429 | API_RATE_LIMIT | 请求频率超限 |
| 500 | 500 | SERVER_ERROR | 服务器内部错误 |

### 业务错误码
| 错误类型 | 后端错误码 | 前端错误码 | 描述 |
|---------|-----------|-----------|------|
| 验证错误 | 1001 | VALIDATION_ERROR | 数据验证失败 |
| 业务逻辑错误 | 1002 | BUSINESS_LOGIC | 业务规则不满足 |
| 数据重复 | 1003 | DATABASE_CONSTRAINT | 数据已存在 |
| 数据不存在 | 1004 | DATABASE_NOT_FOUND | 数据不存在 |

## 前端错误码定义

### 网络相关错误
```dart
class ErrorCodes {
  // 网络相关错误
  static const String networkTimeout = 'NETWORK_TIMEOUT';
  static const String networkUnavailable = 'NETWORK_UNAVAILABLE';
  static const String serverError = 'SERVER_ERROR';
  
  // API相关错误
  static const String apiBadRequest = 'API_BAD_REQUEST';
  static const String apiUnauthorized = 'API_UNAUTHORIZED';
  static const String apiForbidden = 'API_FORBIDDEN';
  static const String apiNotFound = 'API_NOT_FOUND';
  static const String apiValidation = 'API_VALIDATION';
  static const String apiRateLimit = 'API_RATE_LIMIT';
  
  // 数据库相关错误
  static const String databaseConstraint = 'DATABASE_CONSTRAINT';
  static const String databaseNotFound = 'DATABASE_NOT_FOUND';
  static const String databaseIO = 'DATABASE_IO';
  
  // 业务逻辑错误
  static const String businessValidation = 'BUSINESS_VALIDATION';
  static const String businessLogic = 'BUSINESS_LOGIC';
  
  // 系统错误
  static const String systemUnknown = 'SYSTEM_UNKNOWN';
}
```

## 错误响应格式

### 后端响应格式
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null
}
```

### 前端错误事件格式
```dart
class ErrorEvent {
  final String message;
  final ErrorSeverity severity;
  final DateTime timestamp;
  final String? errorCode;
  final Map<String, dynamic>? metadata;
  final String? source;
}
```

## 错误处理流程

### 后端错误处理流程
1. **全局异常处理**：`GlobalExceptionHandler` 捕获所有异常
2. **业务异常转换**：将异常转换为统一的 `ApiResponse`
3. **日志记录**：记录错误日志到文件系统
4. **响应返回**：返回标准化的错误响应

### 前端错误处理流程
1. **API调用错误**：捕获网络请求异常
2. **错误码映射**：将后端错误码映射为前端错误码
3. **用户提示**：显示友好的错误消息
4. **错误记录**：记录错误事件到历史记录
5. **状态同步**：触发状态同步机制（如需要）

## 错误消息国际化

### 后端错误消息
- 使用中文错误消息，便于调试
- 关键错误消息可考虑支持多语言

### 前端错误消息
- 根据用户语言设置显示对应语言
- 默认使用中文错误消息
- 支持错误消息自定义

## 错误日志记录

### 后端日志记录
- 使用SLF4J记录错误日志
- 区分不同级别的日志（ERROR、WARN、INFO）
- 记录完整的错误堆栈信息

### 前端日志记录
- 开发环境：打印到控制台
- 生产环境：可上报到错误监控系统
- 记录错误事件历史（最多100条）

## 错误恢复机制

### 自动重试机制
- 网络错误：自动重试3次
- 服务器错误：延迟重试
- 业务错误：不自动重试

### 用户操作恢复
- 提供"重试"按钮
- 显示详细的错误信息
- 提供问题解决方案建议

## 测试规范

### 后端测试
- 单元测试：验证异常处理逻辑
- 集成测试：验证API错误响应
- 压力测试：验证错误处理性能

### 前端测试
- 单元测试：验证错误处理服务
- 集成测试：验证错误UI显示
- E2E测试：验证完整错误流程

## 版本兼容性

### 错误码兼容性
- 新增错误码：向后兼容
- 修改错误码：需要版本升级
- 删除错误码：标记为废弃

### API兼容性
- 错误响应格式保持稳定
- 新增错误字段可扩展
- 废弃字段标记为可选

## 监控和告警

### 后端监控
- 监控错误率指标
- 设置错误阈值告警
- 定期分析错误趋势

### 前端监控
- 监控用户操作错误
- 收集错误统计信息
- 优化用户体验