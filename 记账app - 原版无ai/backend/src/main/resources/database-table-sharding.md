# 交易记录表分表功能说明

## 功能概述

本功能实现了交易记录表的按月分表策略，用于解决大数据量场景下的数据库性能问题。

## 核心特性

### 1. 自动分表管理
- **按月分表**：根据交易日期自动计算分表名称（如 `transactions_2024_01`）
- **自动创建**：每日凌晨自动创建下月分表
- **智能路由**：根据查询条件自动路由到正确的分表
- **无缝切换**：应用层无需感知分表存在

### 2. 数据归档策略
- **自动归档**：定期归档历史数据到归档表
- **保留策略**：可配置保留最近N个月的数据在活跃表中
- **压缩存储**：归档数据支持压缩存储以节省空间

### 3. 查询优化
- **跨分表查询**：支持多个月份的联合查询
- **分页优化**：分页查询时自动优化跨分表性能
- **索引策略**：每个分表独立维护索引

## 快速开始

### 1. 启用分表功能

在 `application.yml` 中配置：

```yaml
finance:
  database:
    sharding:
      enabled: true
      strategy: monthly
      retention-months: 12
      auto-create: true
      archive-enabled: true
```

### 2. 分表命名规则

分表命名格式：`transactions_YYYY_MM`

示例：
- `transactions_2024_01` - 2024年1月数据
- `transactions_2024_02` - 2024年2月数据
- `transactions_2024_12` - 2024年12月数据

### 3. 使用示例

#### 查询当月交易记录
```java
@Autowired
private ShardingTableManager shardingManager;

// 查询2024年1月的交易记录
LocalDate date = LocalDate.of(2024, 1, 15);
String tableName = shardingManager.getTableNameByDate(date);
List<TransactionRecord> records = transactionRecordRepository.findByDateBetween(
    date.withDayOfMonth(1), 
    date.withDayOfMonth(date.lengthOfMonth())
);
```

#### 跨分表查询
```java
// 查询最近3个月的数据
LocalDate startDate = LocalDate.of(2023, 12, 1);
LocalDate endDate = LocalDate.of(2024, 2, 28);
List<String> tables = shardingManager.getTablesByDateRange(startDate, endDate);

List<TransactionRecord> allRecords = new ArrayList<>();
for (String table : tables) {
    List<TransactionRecord> records = transactionRecordRepository
        .findByDateRangeAndTable(table, startDate, endDate);
    allRecords.addAll(records);
}
```

## 高级配置

### 1. 分表策略配置

```java
@Configuration
public class ShardingConfig {
    
    @Bean
    public ShardingStrategy shardingStrategy() {
        return new ShardingStrategy();
    }
    
    @Bean
    public ShardingTableManager shardingTableManager(ShardingStrategy strategy) {
        return new ShardingTableManager(strategy);
    }
}
```

### 2. 自定义分表规则

```java
@Component
public class CustomShardingStrategy implements ShardingStrategy {
    
    @Override
    public String getTableName(Date transactionDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(transactionDate);
        
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        
        return String.format("transactions_%d_%02d", year, month);
    }
    
    @Override
    public List<String> getTableNames(Date startDate, Date endDate) {
        // 实现自定义分表范围查询
        // ...
        return tableNames;
    }
}
```

### 3. 归档策略配置

```java
@Configuration
@EnableScheduling
public class ArchiveConfig {
    
    @Scheduled(cron = "0 0 2 1 * ?") // 每月1号凌晨2点执行归档
    public void archiveOldData() {
        shardingManager.archiveOldData();
    }
}
```

## 监控和运维

### 1. 分表状态监控

```java
// 获取分表统计信息
Map<String, Object> stats = shardingManager.getTableStatistics();
System.out.println("总表数：" + stats.get("totalTables"));
System.out.println("活跃表数：" + stats.get("activeTables"));
System.out.println("归档表数：" + stats.get("archivedTables"));
```

### 2. 手动分表操作

```java
// 手动创建分表
shardingManager.createShardingTable("2024_03");

// 手动归档数据
shardingManager.archiveData("2023_01");

// 清理归档表
shardingManager.cleanupArchivedTables();
```

### 3. 数据迁移

```java
// 历史数据迁移到分表
shardingManager.migrateHistoricalData(
    LocalDate.of(2020, 1, 1), 
    LocalDate.of(2023, 12, 31)
);
```

## 性能优化建议

### 1. 索引优化
- 每个分表维护独立的索引
- 建议在 `date`、`user_id`、`category` 字段上创建索引
- 定期重建索引以保持查询性能

### 2. 查询优化
- 尽量使用日期范围查询，减少跨分表查询
- 对于大范围查询，考虑分批处理
- 使用分页查询，避免一次查询过多数据

### 3. 存储优化
- 定期清理历史数据
- 对归档表进行压缩
- 考虑将历史数据迁移到冷存储

## 常见问题

### Q: 如何处理跨分表的统计查询？
A: 使用 `ShardingTableManager.getTablesByDateRange()` 获取涉及的表列表，然后分别查询各表并合并结果。

### Q: 分表策略可以修改吗？
A: 修改分表策略需要数据迁移，建议在项目初期确定好分表策略。如果确实需要修改，可以编写迁移脚本。

### Q: 如何备份分表数据？
A: 每个分表都是独立的表，可以使用标准的数据库备份工具对单个分表进行备份。

### Q: 分表会影响事务一致性吗？
A: 不会，分表仅影响查询和存储，数据的事务一致性由数据库本身保证。

## 更新日志

- **v1.0.0**: 初始版本，支持按月分表和基础查询功能
- **v1.1.0**: 增加数据归档和自动创建分表功能
- **v1.2.0**: 优化跨分表查询性能，增加监控功能

## 相关文档

- [数据库读写分离配置](./database-read-write-separation.md)
- [索引优化策略](./database-index-optimization.md)
- [数据库性能优化指南](./database-performance-guide.md)