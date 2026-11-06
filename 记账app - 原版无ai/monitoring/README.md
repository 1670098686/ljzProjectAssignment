# 个人收支记账APP - 系统级监控和运维支持

## 概述

本监控系统采用 Prometheus + Grafana + Alertmanager 架构，提供完整的应用监控、告警和运维支持功能。

## 系统架构

### 核心组件

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Prometheus    │    │     Grafana     │    │  Alertmanager   │
│   指标收集器    │────│   可视化仪表板   │────│    告警管理     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         └──────────────│   监控仪表板    │──────────────┘
                        │  (业务指标)     │
                        └─────────────────┘
         │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Webhook服务器   │    │     日志收集    │    │   告警处理      │
│   (Python)      │    │    (Promtail)   │    │   (自定义)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 监控范围

1. **系统级监控**
   - CPU、内存、磁盘、网络使用率
   - 容器运行状态
   - 节点健康状态

2. **应用级监控**
   - Spring Boot 应用性能指标
   - JVM 堆内存、GC、线程状态
   - HTTP 请求率、响应时间、错误率
   - 数据库连接池状态

3. **业务指标监控**
   - 交易处理统计
   - 用户活跃度分析
   - 分类使用情况
   - 预算执行情况
   - 储蓄目标进度

## 快速开始

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 可用内存: 4GB+ (推荐)
- 可用磁盘: 10GB+ (用于日志和指标存储)

### 2. 启动监控堆栈

```bash
# 进入监控目录
cd monitoring

# 启动所有监控服务
./start-monitoring.sh

# 或者手动启动
docker-compose -f docker-compose.monitoring.yml up -d
```

### 3. 访问监控界面

- **Grafana Dashboard**: http://localhost:3000
  - 默认用户名: admin
  - 默认密码: admin123
  - 数据源: Prometheus (自动配置)

- **Prometheus**: http://localhost:9090
  - 告警规则: Configuration → Alerting Rules
  - 目标状态: Status → Targets
  - 指标查询: Graph → Console

- **Alertmanager**: http://localhost:9093
  - 告警状态: Alerts
  - 抑制规则: Silences
  - 配置状态: Status

- **Webhook服务器**: http://localhost:8080
  - 健康检查: /health
  - 指标端点: /metrics
  - 告警处理: /webhook/alert

## 详细配置

### Prometheus 配置

#### prometheus.yml
- **全局配置**: 抓取间隔、评估间隔
- **目标发现**: 静态配置和服务发现
- **告警集成**: Alertmanager 地址配置
- **规则文件**: 预计算规则和告警规则

#### alert_rules.yml
包含以下告警类别:
- **系统级**: CPU、内存、磁盘、网络
- **应用级**: 响应时间、错误率、JVM状态
- **业务级**: 交易失败率、预算超支
- **安全级**: 可疑访问模式

#### recording_rules.yml
预计算常用指标:
- **系统性能**: 节点负载率、内存使用率
- **应用性能**: 请求成功率、响应时间P95
- **业务指标**: 交易成功率、预算执行率

### Grafana 仪表板

#### system-overview.json
- **系统状态总览**: 关键指标概览
- **CPU使用率**: 实时和历史趋势
- **内存使用率**: 堆内存和非堆内存
- **网络流量**: 入站和出站流量
- **HTTP请求**: 请求率和错误率

#### business-monitoring.json
- **交易处理统计**: 交易量、成功率
- **用户活跃度**: 日活、月活用户数
- **分类使用情况**: 各分类交易统计
- **预算执行情况**: 预算vs实际支出
- **储蓄目标进度**: 目标达成率

### Alertmanager 配置

#### 路由规则
- **按严重程度分组**: critical、warning、info
- **按服务类型分组**: system、application、business
- **按时间规则**: 工作时间vs非工作时间

#### 通知方式
- **邮件通知**: SMTP配置，支持模板
- **Slack通知**: Webhook集成
- **Webhook处理**: 自定义告警处理逻辑

### 日志收集配置

#### Promtail 配置
- **应用日志**: Spring Boot应用日志解析
- **数据库日志**: MySQL慢查询监控
- **系统日志**: Linux系统日志收集
- **容器日志**: Docker容器标准输出

### Webhook 服务器

#### 功能特性
- **告警处理**: 接收Alertmanager告警
- **多渠道通知**: 邮件、Slack、自定义
- **告警恢复**: 状态变更通知
- **指标导出**: Prometheus格式指标
- **健康检查**: 服务状态监控

#### 配置选项
```python
NOTIFICATION_CONFIG = {
    'email': {
        'smtp_server': 'smtp.gmail.com',
        'smtp_port': 587,
        'username': 'your-email@gmail.com',
        'password': 'your-app-password',
        'to_emails': ['admin@finance-app.com']
    },
    'slack': {
        'webhook_url': 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
    }
}
```

## Spring Boot 集成

### 依赖添加

```xml
<!-- 在 backend/pom.xml 中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>com.github.joschi.micrometer</groupId>
    <artifactId>micrometer-jvm-extras</artifactId>
</dependency>
```

### 配置更新

```yaml
# 在 application.yml 中配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
```

### 自定义指标

#### 业务指标示例
```java
@RestController
public class TransactionController {
    
    private final MeterRegistry meterRegistry;
    private final Counter transactionCounter;
    private final Timer transactionTimer;
    
    public TransactionController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transactionCounter = Counter.builder("finance.transactions.total")
            .description("Total number of transactions")
            .register(meterRegistry);
        this.transactionTimer = Timer.builder("finance.transactions.duration")
            .description("Transaction processing time")
            .register(meterRegistry);
    }
    
    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request) {
        return transactionTimer.record(() -> {
            // 交易处理逻辑
            transactionCounter.increment();
            return ResponseEntity.ok().build();
        });
    }
}
```

#### 预算预警示例
```java
@Service
public class BudgetAlertService {
    
    private final Gauge budgetGauge;
    private final MeterRegistry meterRegistry;
    
    public BudgetAlertService(MeterRegistry meterRegistry, BudgetRepository budgetRepository) {
        this.meterRegistry = meterRegistry;
        this.budgetGauge = Gauge.builder("finance.budget.usage.ratio")
            .description("Budget usage ratio")
            .register(meterRegistry, budgetRepository, this::calculateBudgetUsage);
    }
    
    private double calculateBudgetUsage(BudgetRepository repository) {
        // 计算预算使用率
        return repository.calculateCurrentUsage();
    }
}
```

## 告警使用指南

### 告警级别

1. **Critical (严重)**: 需要立即处理
   - 应用服务不可用
   - 数据库连接失败
   - 磁盘空间不足

2. **Warning (警告)**: 需要关注但不紧急
   - 内存使用率较高
   - 响应时间过长
   - 预算即将超支

3. **Info (信息)**: 仅需记录
   - 新用户注册
   - 定期任务执行
   - 系统状态变更

### 告警抑制

- **维护窗口**: 计划维护期间抑制告警
- **依赖关系**: 下游服务故障时抑制相关告警
- **重复告警**: 避免短时间内重复通知

### 告警处理流程

1. **接收告警**: 邮件、Slack、Webhook
2. **评估严重程度**: 根据告警级别确定优先级
3. **查看详细信息**: Grafana仪表板、日志分析
4. **执行修复操作**: 重启服务、清理资源等
5. **确认告警恢复**: 监控系统状态变化
6. **记录事件**: 更新告警日志和事后报告

## 性能优化

### 存储优化

- **指标保留期**: 15天（可配置）
- **日志保留期**: 7天（可配置）
- **压缩策略**: 使用时序数据库压缩
- **归档策略**: 冷数据迁移到对象存储

### 查询优化

- **预计算规则**: 减少实时计算开销
- **索引优化**: 使用适当的索引策略
- **缓存策略**: Grafana面板数据缓存
- **批处理**: 大数据量查询分批处理

### 网络优化

- **压缩传输**: 启用gzip压缩
- **连接池**: 复用数据库连接
- **CDN**: 静态资源CDN分发
- **API限流**: 防止监控请求过载

## 运维管理

### 日常检查

- **每日任务**
  - 检查告警状态
  - 查看关键指标趋势
  - 验证备份完整性
  - 更新维护文档

- **每周任务**
  - 性能趋势分析
  - 容量规划评估
  - 告警规则优化
  - 安全配置检查

- **每月任务**
  - 全面性能评估
  - 监控覆盖度检查
  - 告警响应时间统计
  - 运维流程优化

### 故障处理

1. **快速诊断**
   - 查看服务状态
   - 检查错误日志
   - 分析性能指标
   - 确认影响范围

2. **临时措施**
   - 服务降级处理
   - 资源扩容
   - 告警屏蔽
   - 通知相关方

3. **根因分析**
   - 日志深度分析
   - 代码审计
   - 配置检查
   - 外部依赖排查

4. **预防措施**
   - 完善告警规则
   - 增加监控指标
   - 优化代码逻辑
   - 加强文档维护

### 容量管理

- **阈值设置**: 基于历史数据分析
- **趋势预测**: 使用机器学习算法
- **扩展策略**: 自动扩容配置
- **成本控制**: 监控资源使用成本

## 扩展功能

### 高级分析

- **异常检测**: 机器学习异常检测
- **趋势分析**: 时间序列预测
- **根因分析**: 自动故障定位
- **性能调优**: 智能建议系统

### 集成能力

- **APM集成**: New Relic、AppDynamics
- **日志平台**: ELK、Sumo Logic
- **告警平台**: PagerDuty、Opsgenie
- **CI/CD集成**: Jenkins、GitLab CI

### 移动支持

- **移动端仪表板**: 响应式设计
- **推送通知**: 移动端告警通知
- **离线查看**: 缓存关键指标
- **实时更新**: WebSocket实时推送

## 故障排除

### 常见问题

1. **Prometheus目标离线**
   - 检查服务网络连接
   - 验证防火墙配置
   - 确认端口可达性
   - 重启相关服务

2. **Grafana无法连接Prometheus**
   - 验证数据源配置
   - 检查网络连通性
   - 确认认证信息
   - 查看错误日志

3. **Alertmanager告警不发送**
   - 检查SMTP配置
   - 验证Webhook URL
   - 确认告警路由规则
   - 测试通知渠道

4. **Webhook服务器无法访问**
   - 检查服务运行状态
   - 验证端口配置
   - 确认防火墙规则
   - 查看应用日志

### 日志位置

- **Prometheus**: `/var/log/prometheus/`
- **Grafana**: `/var/log/grafana/`
- **Alertmanager**: `/var/log/alertmanager/`
- **Webhook**: `/var/log/webhook/`

### 调试命令

```bash
# 检查Prometheus目标状态
curl http://localhost:9090/api/v1/targets

# 测试Grafana数据源
curl -u admin:admin http://localhost:3000/api/datasources

# 查看Alertmanager告警状态
curl http://localhost:9093/api/v1/alerts

# 检查Webhook健康状态
curl http://localhost:8080/health
```

## 最佳实践

### 配置管理

- **版本控制**: 所有配置文件纳入版本控制
- **环境隔离**: 开发、测试、生产环境独立配置
- **参数化**: 使用环境变量管理敏感配置
- **文档更新**: 配置变更及时更新文档

### 安全考虑

- **访问控制**: 使用强密码和访问控制列表
- **网络安全**: 限制监控端口访问范围
- **数据加密**: 传输和存储数据加密
- **审计日志**: 记录所有配置变更

### 性能监控

- **延迟监控**: 监控自身服务延迟
- **资源使用**: 监控监控系统资源使用
- **错误率**: 监控系统错误率
- **可用性**: 监控系统可用性

## 结论

本监控系统为个人收支记账APP提供了全面的监控、告警和运维支持能力。通过合理配置和使用，可以有效保障应用的稳定运行，快速定位和解决故障，为用户提供优质的服务体验。

建议根据实际使用情况调整监控指标和告警阈值，持续优化监控策略，确保监控系统的有效性和实用性。