package com.finance.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * API限流配置
 * 
 * @author Finance Application Team
 * @since 2025-01
 */
@Configuration
public class RateLimitConfig {

    /**
     * 配置限流器注册表
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    /**
     * 配置默认限流器
     */
    @Bean
    public RateLimiter defaultRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 限制时间段内的请求数量
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 限制刷新周期
                .timeoutDuration(Duration.ofMillis(500)) // 等待时间
                .build();

        // 使用rateLimiterRegistry获取或创建限流器，而不是add方法
        return rateLimiterRegistry.rateLimiter("defaultRateLimiter", config);
    }

    /**
     * 配置严格限流器（用于敏感操作）
     */
    @Bean
    public RateLimiter strictRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10) // 限制时间段内的请求数量
                .limitRefreshPeriod(Duration.ofMinutes(5)) // 限制刷新周期
                .timeoutDuration(Duration.ofMillis(1000)) // 等待时间
                .build();

        // 使用rateLimiterRegistry获取或创建限流器，而不是add方法
        return rateLimiterRegistry.rateLimiter("strictRateLimiter", config);
    }

    /**
     * 配置API限流器映射
     */
    @Bean
    public Map<String, RateLimiter> apiRateLimiters(RateLimiterRegistry registry) {
        Map<String, RateLimiter> limiters = new HashMap<>();
        
        // 认证相关接口 - 严格限流
        RateLimiter authLimiter = registry.rateLimiter("authLimiter", 
            RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(1000))
                .build());
        limiters.put("/api/auth/**", authLimiter);
        limiters.put("/api/user/login", authLimiter);
        limiters.put("/api/user/register", authLimiter);
        
        // 数据查询接口 - 标准限流
        RateLimiter queryLimiter = registry.rateLimiter("queryLimiter",
            RateLimiterConfig.custom()
                .limitForPeriod(60)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build());
        limiters.put("/api/transactions", queryLimiter);
        limiters.put("/api/saving-goals", queryLimiter);
        limiters.put("/api/budgets", queryLimiter);
        limiters.put("/api/categories", queryLimiter);
        limiters.put("/api/statistics", queryLimiter);
        
        // 数据修改接口 - 中等限流
        RateLimiter modifyLimiter = registry.rateLimiter("modifyLimiter",
            RateLimiterConfig.custom()
                .limitForPeriod(20)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(800))
                .build());
        limiters.put("/api/transactions/**", modifyLimiter);
        limiters.put("/api/saving-goals/**", modifyLimiter);
        limiters.put("/api/budgets/**", modifyLimiter);
        
        // 导出接口 - 严格限流
        RateLimiter exportLimiter = registry.rateLimiter("exportLimiter",
            RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofMinutes(10))
                .timeoutDuration(Duration.ofSeconds(2))
                .build());
        limiters.put("/api/export/**", exportLimiter);
        
        return limiters;
    }

    /**
     * 配置IP级限流器
     */
    @Bean
    public RateLimiter ipRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1000) // IP级别限制更大的请求数
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(300))
                .build();

        // 使用rateLimiterRegistry获取或创建限流器，而不是add方法
        return rateLimiterRegistry.rateLimiter("ipRateLimiter", config);
    }
}