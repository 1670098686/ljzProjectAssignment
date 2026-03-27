package com.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/**
 * 应用程序配置类
 * 负责限流、缓存、监控等配置的加载和管理
 */
@Configuration
@EnableScheduling
@EnableAsync
public class ApplicationConfig {

    /**
     * 配置限流规则映射
     */
    @Bean
    @ConfigurationProperties(prefix = "app.rate-limit")
    public RateLimitConfigBean rateLimitConfig() {
        return new RateLimitConfigBean();
    }

    /**
     * 限流配置Bean
     */
    public static class RateLimitConfigBean {
        private Map<String, String> globalLimits;
        private Map<String, String> businessLimits;
        private Map<String, String> whitelist;
        private Map<String, String> blacklist;
        private boolean monitoringEnabled = true;
        private boolean alertingEnabled = true;

        // Getters and Setters
        public Map<String, String> getGlobalLimits() { return globalLimits; }
        public void setGlobalLimits(Map<String, String> globalLimits) { this.globalLimits = globalLimits; }
        
        public Map<String, String> getBusinessLimits() { return businessLimits; }
        public void setBusinessLimits(Map<String, String> businessLimits) { this.businessLimits = businessLimits; }
        
        public Map<String, String> getWhitelist() { return whitelist; }
        public void setWhitelist(Map<String, String> whitelist) { this.whitelist = whitelist; }
        
        public Map<String, String> getBlacklist() { return blacklist; }
        public void setBlacklist(Map<String, String> blacklist) { this.blacklist = blacklist; }
        
        public boolean isMonitoringEnabled() { return monitoringEnabled; }
        public void setMonitoringEnabled(boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }
        
        public boolean isAlertingEnabled() { return alertingEnabled; }
        public void setAlertingEnabled(boolean alertingEnabled) { this.alertingEnabled = alertingEnabled; }
    }
}