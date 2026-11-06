package com.finance.database.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 数据库连接配置属性类
 * 支持多数据源配置（主库从库分离）
 */
@Component
@ConfigurationProperties(prefix = "finance.database")
public class DatabaseProperties {
    
    private DataSourceConfig primary;
    private DataSourceConfig replica;
    private RoutingConfig routing;
    
    public DataSourceConfig getPrimary() {
        return primary;
    }
    
    public void setPrimary(DataSourceConfig primary) {
        this.primary = primary;
    }
    
    public DataSourceConfig getReplica() {
        return replica;
    }
    
    public void setReplica(DataSourceConfig replica) {
        this.replica = replica;
    }
    
    public RoutingConfig getRouting() {
        return routing;
    }
    
    public void setRouting(RoutingConfig routing) {
        this.routing = routing;
    }
    
    /**
     * 主数据源配置
     */
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private int maximumPoolSize = 10;
        private int minimumIdle = 5;
        private long connectionTimeout = 30000L;
        private long idleTimeout = 600000L;
        private long maxLifetime = 1800000L;
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        public int getMaximumPoolSize() { return maximumPoolSize; }
        public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
        public int getMinimumIdle() { return minimumIdle; }
        public void setMinimumIdle(int minimumIdle) { this.minimumIdle = minimumIdle; }
        public long getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        public long getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; }
        public long getMaxLifetime() { return maxLifetime; }
        public void setMaxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; }
    }
    
    /**
     * 路由配置
     */
    public static class RoutingConfig {
        private boolean enabled = true;
        private String strategy = "round-robin";
        private int replicaCount = 1;
        private Map<String, String> readOnlyQueries;
        private Map<String, String> writeQueries;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public int getReplicaCount() { return replicaCount; }
        public void setReplicaCount(int replicaCount) { this.replicaCount = replicaCount; }
        public Map<String, String> getReadOnlyQueries() { return readOnlyQueries; }
        public void setReadOnlyQueries(Map<String, String> readOnlyQueries) { this.readOnlyQueries = readOnlyQueries; }
        public Map<String, String> getWriteQueries() { return writeQueries; }
        public void setWriteQueries(Map<String, String> writeQueries) { this.writeQueries = writeQueries; }
    }
}