package com.finance.database.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 路由数据源实现类
 * 动态路由到主库或从库
 */
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    private static final Logger logger = LoggerFactory.getLogger(RoutingDataSource.class);
    
    // 统计信息
    private final AtomicLong primaryCount = new AtomicLong(0);
    private final AtomicLong replicaCount = new AtomicLong(0);
    private final Map<String, AtomicLong> methodStats = new ConcurrentHashMap<>();
    
    @Override
    protected DataSource determineTargetDataSource() {
        DataSource selectedDataSource = super.determineTargetDataSource();
        
        // 记录路由统计
        DatabaseType databaseType = DatabaseContextHolder.getDatabaseType();
        if (databaseType == DatabaseType.PRIMARY) {
            primaryCount.incrementAndGet();
        } else {
            replicaCount.incrementAndGet();
        }
        
        logger.debug("路由到数据库: {}, 连接池信息: {}", 
            databaseType, getConnectionPoolInfo(selectedDataSource));
        
        return selectedDataSource;
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        DatabaseType databaseType = DatabaseContextHolder.getDatabaseType();
        
        // 统计方法级路由
        String methodName = getCurrentMethodName();
        if (methodName != null) {
            methodStats.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        logger.debug("当前数据库类型: {}, 方法: {}", databaseType, methodName);
        return databaseType;
    }
    
    /**
     * 获取连接池信息
     */
    private String getConnectionPoolInfo(DataSource dataSource) {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikari = (com.zaxxer.hikari.HikariDataSource) dataSource;
                return String.format("活跃连接: %d/%d, 空闲连接: %d, 最大连接数: %d",
                    hikari.getHikariPoolMXBean().getActiveConnections(),
                    hikari.getHikariPoolMXBean().getTotalConnections(),
                    hikari.getHikariPoolMXBean().getIdleConnections(),
                    hikari.getMaximumPoolSize());
            }
        } catch (Exception e) {
            logger.warn("获取连接池信息失败: {}", e.getMessage());
        }
        return "未知";
    }
    
    /**
     * 获取当前方法名
     */
    private String getCurrentMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (className.contains("com.finance.repository") || 
                className.contains("com.finance.service")) {
                return stackTrace[i].getMethodName();
            }
        }
        return null;
    }
    
    /**
     * 获取路由统计信息
     */
    public String getRoutingStats() {
        return String.format("主库路由次数: %d, 从库路由次数: %d, 比例: %.2f",
            primaryCount.get(), 
            replicaCount.get(),
            replicaCount.get() > 0 ? (double) primaryCount.get() / replicaCount.get() : 0);
    }
    
    /**
     * 获取方法级统计信息
     */
    public Map<String, Long> getMethodStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        methodStats.forEach((method, count) -> stats.put(method, count.get()));
        return stats;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        primaryCount.set(0);
        replicaCount.set(0);
        methodStats.clear();
        logger.info("路由统计信息已重置");
    }
    
    /**
     * 检查数据源健康状态
     */
    public Map<String, Boolean> getDataSourceHealthStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        
        try {
            DataSource primary = resolveSpecifiedDataSource(DatabaseType.PRIMARY);
            DataSource replica = resolveSpecifiedDataSource(DatabaseType.REPLICA);
            
            status.put("primary", testConnection(primary));
            status.put("replica", testConnection(replica));
            
        } catch (Exception e) {
            logger.error("检查数据源健康状态失败: {}", e.getMessage());
            status.put("primary", false);
            status.put("replica", false);
        }
        
        return status;
    }
    
    /**
     * 测试数据库连接
     */
    private boolean testConnection(DataSource dataSource) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT 1")) {
            return resultSet.next() && resultSet.getInt(1) == 1;
        } catch (Exception e) {
            logger.warn("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}