package com.finance.database.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 * 数据库健康检查指示器
 * 监控主从数据库的连接状态和读写分离健康状况
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final DataSource routingDataSource;
    private final RoutingDataSource customRoutingDataSource;
    
    public DatabaseHealthIndicator(DataSource routingDataSource, 
                                   RoutingDataSource customRoutingDataSource) {
        this.routingDataSource = routingDataSource;
        this.customRoutingDataSource = customRoutingDataSource;
    }
    
    @Override
    public Health health() {
        try {
            Map<String, Boolean> healthStatus = customRoutingDataSource.getDataSourceHealthStatus();
            String routingStats = customRoutingDataSource.getRoutingStats();
            
            // 检查主从数据库状态
            boolean primaryHealthy = healthStatus.getOrDefault("primary", false);
            boolean replicaHealthy = healthStatus.getOrDefault("replica", false);
            
            Health.Builder healthBuilder = new Health.Builder();
            
            if (primaryHealthy) {
                healthBuilder.up();
                healthBuilder.withDetail("primary", "healthy");
            } else {
                healthBuilder.down();
                healthBuilder.withDetail("primary", "unhealthy");
            }
            
            if (replicaHealthy) {
                healthBuilder.withDetail("replica", "healthy");
            } else {
                healthBuilder.down();
                healthBuilder.withDetail("replica", "unhealthy");
            }
            
            // 添加详细信息
            healthBuilder.withDetail("routing_stats", routingStats);
            healthBuilder.withDetail("data_source_type", routingDataSource.getClass().getSimpleName());
            
            // 整体健康状态评估
            if (!primaryHealthy) {
                healthBuilder.down().withDetail("overall_status", "主库不可用，系统不可用");
            } else if (!replicaHealthy) {
                healthBuilder.down().withDetail("overall_status", "从库不可用，只读功能受影响");
            } else {
                healthBuilder.up().withDetail("overall_status", "读写分离功能正常");
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            logger.error("数据库健康检查失败: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("overall_status", "健康检查失败")
                    .build();
        }
    }
    
    /**
     * 测试数据库连接
     */
    private boolean testConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT 1")) {
            return resultSet.next() && resultSet.getInt(1) == 1;
        } catch (Exception e) {
            logger.warn("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}