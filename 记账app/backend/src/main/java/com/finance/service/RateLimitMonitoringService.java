package com.finance.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API限流监控服务
 * 
 * 提供限流统计、监控和报告功能
 * 
 * @author Finance Application Team
 * @since 2025-01
 */
@Service
public class RateLimitMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitMonitoringService.class);
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    
    // 记录API调用统计
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> rateLimitCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> responseTimers = new ConcurrentHashMap<>();
    
    // 客户端IP统计
    private final Map<String, Counter> clientRequestCounters = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 初始化所有限流器的监控指标
        rateLimiterRegistry.getAllRateLimiters().forEach(this::setupRateLimiterMonitoring);
    }
    
    /**
     * 设置限流器监控
     */
    private void setupRateLimiterMonitoring(RateLimiter rateLimiter) {
        String name = rateLimiter.getName();
        
        // 可用权限计数
        Gauge.builder("resilience4j.ratelimiter.available_permissions", rateLimiter, 
            rl -> rl.getMetrics().getAvailablePermissions())
            .description("Rate limiter available permissions")
            .tag("name", name)
            .register(meterRegistry);
        
        // 等待线程数
        Gauge.builder("resilience4j.ratelimiter.waiting_threads", rateLimiter, 
            rl -> rl.getMetrics().getNumberOfWaitingThreads())
            .description("Rate limiter waiting threads")
            .tag("name", name)
            .register(meterRegistry);
    }

    /**
     * 记录API请求
     */
    public void recordApiRequest(String apiPath, String clientIp, long responseTimeMs, boolean rateLimited) {
        String key = apiPath;
        
        // 记录总体请求计数
        requestCounters.computeIfAbsent(key, k -> 
            Counter.builder("api.requests.total")
                .description("Total API requests")
                .tag("path", apiPath)
                .register(meterRegistry))
            .increment();
            
        // 记录响应时间
        responseTimers.computeIfAbsent(key, k ->
            Timer.builder("api.response.time")
                .description("API response time")
                .tag("path", apiPath)
                .register(meterRegistry))
            .record(responseTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        // 记录客户端请求
        String clientKey = clientIp + ":" + apiPath;
        clientRequestCounters.computeIfAbsent(clientKey, k ->
            Counter.builder("api.client.requests")
                .description("Client API requests")
                .tag("client_ip", clientIp)
                .tag("path", apiPath)
                .register(meterRegistry))
            .increment();
            
        // 记录限流拒绝
        if (rateLimited) {
            rateLimitCounters.computeIfAbsent(key, k ->
                Counter.builder("api.rate_limit.rejected")
                    .description("API rate limit rejections")
                    .tag("path", apiPath)
                    .register(meterRegistry))
                .increment();
        }
        
        logger.debug("Recorded metrics - Path: {}, Client: {}, ResponseTime: {}ms, RateLimited: {}", 
                    apiPath, clientIp, responseTimeMs, rateLimited);
    }

    /**
     * 获取API调用统计
     */
    public ApiCallStatistics getApiStatistics(String apiPath) {
        String key = apiPath;
        
        long totalRequests = requestCounters.containsKey(key) ? 
            (long) requestCounters.get(key).count() : 0;
            
        long rateLimitedCount = rateLimitCounters.containsKey(key) ? 
            (long) rateLimitCounters.get(key).count() : 0;
            
        double avgResponseTime = responseTimers.containsKey(key) ?
            responseTimers.get(key).mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
            
        return new ApiCallStatistics(apiPath, totalRequests, rateLimitedCount, avgResponseTime);
    }

    /**
     * 获取客户端请求统计
     */
    public ClientRequestStatistics getClientStatistics(String clientIp) {
        long totalRequests = clientRequestCounters.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(clientIp + ":"))
            .mapToLong(entry -> (long) entry.getValue().count())
            .sum();
            
        return new ClientRequestStatistics(clientIp, totalRequests);
    }

    /**
     * 获取系统限流统计
     */
    public SystemRateLimitStatistics getSystemStatistics() {
        long totalRateLimiters = rateLimiterRegistry.getAllRateLimiters().size();
        
        long totalAllowedPermissions = rateLimiterRegistry.getAllRateLimiters().stream()
            .mapToLong(rateLimiter -> rateLimiter.getMetrics().getAvailablePermissions())
            .sum();
            
        long totalWaitingThreads = rateLimiterRegistry.getAllRateLimiters().stream()
            .mapToInt(rateLimiter -> rateLimiter.getMetrics().getNumberOfWaitingThreads())
            .sum();
            
        return new SystemRateLimitStatistics(totalRateLimiters, totalAllowedPermissions, totalWaitingThreads);
    }

    /**
     * API调用统计类
     */
    public static class ApiCallStatistics {
        private final String apiPath;
        private final long totalRequests;
        private final long rateLimitedCount;
        private final double avgResponseTimeMs;
        
        public ApiCallStatistics(String apiPath, long totalRequests, long rateLimitedCount, double avgResponseTimeMs) {
            this.apiPath = apiPath;
            this.totalRequests = totalRequests;
            this.rateLimitedCount = rateLimitedCount;
            this.avgResponseTimeMs = avgResponseTimeMs;
        }
        
        // Getters
        public String getApiPath() { return apiPath; }
        public long getTotalRequests() { return totalRequests; }
        public long getRateLimitedCount() { return rateLimitedCount; }
        public double getAvgResponseTimeMs() { return avgResponseTimeMs; }
        
        public double getRateLimitPercentage() {
            return totalRequests > 0 ? (double) rateLimitedCount / totalRequests * 100 : 0.0;
        }
    }

    /**
     * 客户端请求统计类
     */
    public static class ClientRequestStatistics {
        private final String clientIp;
        private final long totalRequests;
        
        public ClientRequestStatistics(String clientIp, long totalRequests) {
            this.clientIp = clientIp;
            this.totalRequests = totalRequests;
        }
        
        // Getters
        public String getClientIp() { return clientIp; }
        public long getTotalRequests() { return totalRequests; }
    }

    /**
     * 系统限流统计类
     */
    public static class SystemRateLimitStatistics {
        private final long totalRateLimiters;
        private final long totalAllowedPermissions;
        private final long totalWaitingThreads;
        
        public SystemRateLimitStatistics(long totalRateLimiters, long totalAllowedPermissions, long totalWaitingThreads) {
            this.totalRateLimiters = totalRateLimiters;
            this.totalAllowedPermissions = totalAllowedPermissions;
            this.totalWaitingThreads = totalWaitingThreads;
        }
        
        // Getters
        public long getTotalRateLimiters() { return totalRateLimiters; }
        public long getTotalAllowedPermissions() { return totalAllowedPermissions; }
        public long getTotalWaitingThreads() { return totalWaitingThreads; }
    }
}