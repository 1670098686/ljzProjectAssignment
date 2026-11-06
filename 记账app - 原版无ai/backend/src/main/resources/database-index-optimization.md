# 数据库索引优化功能说明

## 功能概述

本功能提供智能的数据库索引分析和优化建议，用于提升查询性能和数据库整体运行效率。

## 核心特性

### 1. 智能索引分析
- **自动发现**：自动扫描数据库中的所有索引
- **性能评估**：分析索引的使用情况和效果
- **瓶颈识别**：识别影响性能的索引问题
- **建议生成**：基于分析结果生成优化建议

### 2. 查询性能监控
- **慢查询识别**：自动识别慢查询语句
- **执行统计**：收集查询执行次数和平均时间
- **效率分析**：分析查询的数据检查效率
- **趋势监控**：跟踪查询性能变化趋势

### 3. 自动优化建议
- **创建建议**：为缺失的索引提供创建建议
- **删除建议**：标记冗余或未使用的索引
- **复合索引**：建议创建多列复合索引
- **性能评估**：预估优化后的性能改善

### 4. 一键优化
- **批量应用**：支持批量应用多个优化建议
- **安全执行**：提供事务保护和安全检查
- **回滚支持**：支持优化操作回滚
- **进度监控**：实时显示优化执行进度

## 快速开始

### 1. 启用索引优化

在 `application.yml` 中配置：

```yaml
finance:
  database:
    index:
      optimization:
        enabled: true
        auto-analyze: true
        analyze-interval: 24h
        slow-query-threshold: 1s
        performance-schema-enabled: true
```

### 2. 执行索引分析

```java
@Autowired
private IndexOptimizationService optimizationService;

// 执行索引分析
IndexAnalysisResult result = optimizationService.analyzeIndexes();

// 查看分析结果
System.out.println("发现索引数量：" + result.getIndexInfos().size());
System.out.println("优化建议数量：" + result.getSuggestions().size());
```

### 3. 应用优化建议

```java
// 获取优化建议
List<OptimizationSuggestion> suggestions = result.getSuggestions();

for (OptimizationSuggestion suggestion : suggestions) {
    if (suggestion.getPriority().getLevel() <= 2) { // 高优先级建议
        optimizationService.applyOptimization(
            suggestion.getType(),
            suggestion.getTableName(),
            suggestion.getIndexName(),
            suggestion.getIndexColumns()
        );
    }
}
```

## 详细功能说明

### 1. 索引分析

#### 1.1 当前索引扫描
```java
// 获取所有索引信息
List<IndexInfo> indexes = optimizationService.getCurrentIndexes();

for (IndexInfo index : indexes) {
    System.out.println(String.format(
        "表：%s，索引：%s，列：%s，基数：%d，唯一性：%s",
        index.getTableName(),
        index.getIndexName(),
        index.getColumnName(),
        index.getCardinality(),
        index.isUnique() ? "唯一" : "非唯一"
    ));
}
```

#### 1.2 索引使用情况分析
```java
// 分析索引使用情况
Map<String, QueryPerformance> performanceMap = optimizationService.analyzeQueryPerformance();

for (Map.Entry<String, QueryPerformance> entry : performanceMap.entrySet()) {
    QueryPerformance performance = entry.getValue();
    if (performance.isSlowQuery()) {
        System.out.println("发现慢查询：" + entry.getKey());
        System.out.println("平均执行时间：" + performance.getAvgExecutionTime() + "秒");
    }
}
```

### 2. 优化建议类型

#### 2.1 创建新索引
```java
// 为频繁查询的字段创建索引
OptimizationSuggestion suggestion = new OptimizationSuggestion();
suggestion.setType(IndexOptimizationType.CREATE_INDEX);
suggestion.setTableName("transactions");
suggestion.setIndexColumns("user_id, transaction_date");
suggestion.setPriority(Priority.HIGH);
```

#### 2.2 删除冗余索引
```java
// 删除重复或未使用的索引
OptimizationSuggestion suggestion = new OptimizationSuggestion();
suggestion.setType(IndexOptimizationType.DROP_INDEX);
suggestion.setIndexName("idx_duplicate");
suggestion.setDescription("删除重复索引，释放存储空间");
```

#### 2.3 创建复合索引
```java
// 为多条件查询创建复合索引
OptimizationSuggestion suggestion = new OptimizationSuggestion();
suggestion.setType(IndexOptimizationType.CREATE_COMPOSITE_INDEX);
suggestion.setTableName("transactions");
suggestion.setIndexColumns("user_id, category_id, transaction_date");
suggestion.setDescription("为用户分类时间查询创建复合索引");
```

### 3. API接口使用

#### 3.1 索引分析API
```bash
# 执行索引分析
POST /api/index-optimization/analyze
Content-Type: application/json

# 响应示例
{
  "analysisTime": "2024-01-15T10:30:00",
  "indexInfos": [...],
  "queryPerformances": {...},
  "suggestions": [...]
}
```

#### 3.2 获取统计信息API
```bash
# 获取索引统计信息
GET /api/index-optimization/statistics

# 响应示例
{
  "total_indexes": 25,
  "indexed_tables": 8,
  "unique_indexes": 20,
  "table_sizes": [...]
}
```

#### 3.3 应用优化建议API
```bash
# 应用单个优化建议
POST /api/index-optimization/apply
Content-Type: application/json

{
  "type": "CREATE_INDEX",
  "tableName": "transactions",
  "indexName": "idx_user_date",
  "columnNames": "user_id, transaction_date"
}

# 批量应用优化建议
POST /api/index-optimization/apply-batch
Content-Type: application/json

[
  {
    "type": "CREATE_INDEX",
    "tableName": "transactions",
    "indexName": "idx_user_date",
    "columnNames": "user_id, transaction_date"
  },
  {
    "type": "DROP_INDEX",
    "tableName": "transactions",
    "indexName": "idx_old_duplicate"
  }
]
```

## 高级配置

### 1. 性能分析配置

```java
@Configuration
public class IndexOptimizationConfig {
    
    @Value("${finance.database.index.slow-query-threshold:1.0}")
    private double slowQueryThreshold;
    
    @Bean
    public IndexOptimizationService indexOptimizationService() {
        IndexOptimizationService service = new IndexOptimizationService();
        service.setSlowQueryThreshold(slowQueryThreshold);
        return service;
    }
}
```

### 2. 自定义分析规则

```java
@Service
public class CustomIndexAnalyzer {
    
    @Autowired
    private IndexOptimizationService optimizationService;
    
    @PostConstruct
    public void init() {
        // 注册自定义分析规则
        optimizationService.addCustomRule(this::analyzeUserBehavior);
    }
    
    private void analyzeUserBehavior(IndexAnalysisResult result) {
        // 自定义分析逻辑
        // 例如：分析用户的查询模式
    }
}
```

### 3. 监控告警配置

```java
@Component
public class IndexOptimizationMonitor {
    
    @EventListener
    public void handleOptimizationComplete(OptimizationCompleteEvent event) {
        OptimizationSuggestion suggestion = event.getSuggestion();
        
        if (suggestion.getPriority() == Priority.CRITICAL) {
            // 发送紧急告警
            alertService.sendAlert("索引优化", 
                String.format("发现紧急优化建议：%s", suggestion.getDescription()));
        }
    }
}
```

## 性能优化建议

### 1. 索引设计原则
- **选择性强**：索引列的唯一值越多，索引效果越好
- **查询频率高**：为频繁查询的字段创建索引
- **组合合理**：复合索引的列顺序要考虑查询条件
- **避免冗余**：删除重复或覆盖的索引

### 2. 查询优化技巧
- **使用覆盖索引**：查询的字段都在索引中
- **避免函数包装**：不要在索引列上使用函数
- **利用最左前缀**：复合索引要按顺序使用
- **范围查询优化**：合理使用BETWEEN和IN

### 3. 维护策略
- **定期分析**：定期分析表和索引的统计信息
- **重建索引**：定期重建碎片化的索引
- **监控慢查询**：持续监控慢查询日志
- **性能基准**：建立性能基准并持续监控

## 监控指标

### 1. 关键性能指标
- **查询响应时间**：平均查询时间 < 100ms
- **索引命中率**：索引命中率 > 95%
- **慢查询数量**：慢查询数量 < 总查询数的1%
- **索引碎片率**：索引碎片率 < 30%

### 2. 监控查询
```sql
-- 查看索引使用情况
SHOW INDEX FROM transactions;

-- 分析表统计信息
ANALYZE TABLE transactions;

-- 检查索引碎片
SHOW TABLE STATUS LIKE 'transactions';

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';
```

## 常见问题

### Q: 索引优化会影响数据库性能吗？
A: 短期内可能会对写入性能有轻微影响，但长期看会显著提升查询性能。建议在低峰期执行优化。

### Q: 如何判断索引是否有效？
A: 通过分析查询执行计划和性能统计，查看是否使用了索引，以及索引带来的性能提升。

### Q: 复合索引的列顺序如何选择？
A: 按查询频率和选择性排序，频率高的列放在前面，选择性高的列也放在前面。

### Q: 索引越多越好吗？
A: 不是，过多的索引会影响写入性能和存储空间，需要根据实际查询需求合理设计。

## 更新日志

- **v1.0.0**: 初始版本，提供基础索引分析和优化建议功能
- **v1.1.0**: 增加慢查询监控和批量优化功能
- **v1.2.0**: 优化分析算法，增加更多优化建议类型

## 相关文档

- [数据库分表功能](./database-table-sharding.md)
- [数据库读写分离配置](./database-read-write-separation.md)
- [数据库性能优化指南](./database-performance-guide.md)