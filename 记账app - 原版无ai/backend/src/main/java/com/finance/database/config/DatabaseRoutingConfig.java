package com.finance.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库配置类
 * 实现主从数据库配置和读写分离路由
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseRoutingConfig {
    
    /**
     * 主数据源配置（写库）
     */
    @Bean
    @Primary
    public DataSource primaryDataSource(DatabaseProperties properties) {
        HikariConfig config = new HikariConfig();
        DatabaseProperties.DataSourceConfig primaryConfig = properties.getPrimary();
        
        config.setJdbcUrl(primaryConfig.getUrl());
        config.setUsername(primaryConfig.getUsername());
        config.setPassword(primaryConfig.getPassword());
        config.setDriverClassName(primaryConfig.getDriverClassName());
        config.setMaximumPoolSize(primaryConfig.getMaximumPoolSize());
        config.setMinimumIdle(primaryConfig.getMinimumIdle());
        config.setConnectionTimeout(primaryConfig.getConnectionTimeout());
        config.setIdleTimeout(primaryConfig.getIdleTimeout());
        config.setMaxLifetime(primaryConfig.getMaxLifetime());
        
        // 连接池优化配置
        config.setLeakDetectionThreshold(60000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        
        return new HikariDataSource(config);
    }
    
    /**
     * 从数据源配置（读库）
     */
    @Bean
    @Profile("read-replica")
    public DataSource replicaDataSource(DatabaseProperties properties) {
        DatabaseProperties.DataSourceConfig replicaConfig = properties.getReplica();
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(replicaConfig.getUrl());
        config.setUsername(replicaConfig.getUsername());
        config.setPassword(replicaConfig.getPassword());
        config.setDriverClassName(replicaConfig.getDriverClassName());
        config.setMaximumPoolSize(replicaConfig.getMaximumPoolSize());
        config.setMinimumIdle(replicaConfig.getMinimumIdle());
        config.setConnectionTimeout(replicaConfig.getConnectionTimeout());
        config.setIdleTimeout(replicaConfig.getIdleTimeout());
        config.setMaxLifetime(replicaConfig.getMaxLifetime());
        
        // 从库连接池配置（相对较小）
        config.setMaximumPoolSize(Math.max(2, replicaConfig.getMaximumPoolSize() / 2));
        config.setMinimumIdle(Math.max(1, replicaConfig.getMinimumIdle() / 2));
        
        return new HikariDataSource(config);
    }
    
    /**
     * 路由数据源（读写分离核心）
     */
    @Bean
    @Profile("read-replica")
    public DataSource routingDataSource(DatabaseProperties properties, 
                                       DataSource primaryDataSource,
                                       DataSource replicaDataSource) {
        
        AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return DatabaseContextHolder.getDatabaseType();
            }
        };
        
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DatabaseType.PRIMARY, primaryDataSource);
        dataSourceMap.put(DatabaseType.REPLICA, replicaDataSource);
        
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        
        return routingDataSource;
    }
    
    /**
     * 单数据源配置（开发环境使用）
     */
    @Bean
    @Profile("!read-replica")
    public DataSource singleDataSource(DatabaseProperties properties) {
        DatabaseProperties.DataSourceConfig config = properties.getPrimary();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        
        return new HikariDataSource(hikariConfig);
    }
}