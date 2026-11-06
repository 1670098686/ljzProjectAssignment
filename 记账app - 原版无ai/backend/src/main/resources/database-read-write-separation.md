# 数据库读写分离配置说明

## 功能概述
实现了基于Spring AOP的主从数据库读写分离功能，支持：
- 自动根据方法名路由到主库或从库
- 支持强制指定数据库类型
- 支持多从库负载均衡
- 开发环境兼容（单数据源）

## 配置方式

### 1. application.yml 配置
```yaml
finance:
  database:
    primary:
      url: jdbc:mysql://localhost:3306/flutter
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: 20
      minimum-idle: 5
    replica:
      url: jdbc:mysql://localhost:3306/flutter_replica
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: 10
      minimum-idle: 2
    routing:
      enabled: true
      strategy: round-robin
      replica-count: 1
```

### 2. 环境激活
- 开发环境：不激活read-replica profile，使用单数据源
- 生产环境：激活read-replica profile，启用读写分离

### 3. 强制数据库路由
```java
@Repository
public interface TransactionRepository {
    
    // 自动路由到从库（读操作）
    List<TransactionRecord> findByUserId(Long userId);
    
    // 强制路由到主库
    @ForcePrimaryDataSource
    void save(TransactionRecord record);
    
    // 强制路由到从库
    @ForceReplicaDataSource
    List<TransactionRecord> findByDateRange(LocalDateTime start, LocalDateTime end);
}
```

### 4. 分表集成
- 读写分离与分表功能完全兼容
- 支持跨分表查询自动路由
- 分表策略自动适配主从环境

## 性能优化特性
- 连接池优化配置
- 读写负载均衡
- 查询结果缓存
- 慢查询监控