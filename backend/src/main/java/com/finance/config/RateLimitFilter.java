package com.finance.config;

import com.finance.exception.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.finance.service.RateLimitMonitoringService;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API限流过滤器
 * 
 * 该过滤器拦截所有API请求，并根据配置的限流规则限制请求频率。
 * 支持：
 * - IP级别限流
 * - 用户级别限流
 * - 接口级别限流
 * 
 * @author Finance Application Team
 * @since 2025-01
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    // 修复SC_TOO_MANY_REQUESTS常量问题
    // 在Jakarta Servlet API中，SC_TOO_MANY_REQUESTS的值是429
    private static final int SC_TOO_MANY_REQUESTS = 429;
    
    // 记录每个IP的请求计数（用于自实现限流，作为Resilience4j的补充）
    private final Map<String, AtomicInteger> ipRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> ipRequestTimes = new ConcurrentHashMap<>();
    
    // 记录API调用统计
    private final Map<String, AtomicInteger> apiCallStats = new ConcurrentHashMap<>();
    
    // 记录客户端请求统计
    private final Map<String, AtomicInteger> clientRequestStats = new ConcurrentHashMap<>();
    
    @Autowired
    private Map<String, RateLimiter> apiRateLimiters;
    
    @Autowired
    private RateLimiter ipRateLimiter;
    
    @Autowired
    private RateLimitMonitoringService monitoringService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String clientIp = getClientIpAddress(request);
        String requestMethod = request.getMethod();
        
        // 只对API请求进行限流
        if (!requestPath.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        boolean rateLimited = false;
        
        try {
            // 应用IP级别限流
            checkIpRateLimit(clientIp);
            
            // 应用接口级别限流
            checkApiRateLimit(requestPath, clientIp);
            
            logger.debug("Request allowed: {} from IP: {}", requestPath, clientIp);
            filterChain.doFilter(request, response);
            
        } catch (RequestNotPermitted e) {
            rateLimited = true;
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, requestPath);
            handleRateLimitExceeded(response, clientIp, requestPath);
        } catch (RateLimitExceededException e) {
            rateLimited = true;
            logger.warn("Custom rate limit exceeded for IP: {} on path: {}", clientIp, requestPath);
            handleRateLimitExceeded(response, clientIp, requestPath);
        } finally {
            // 记录请求统计
            long responseTime = System.currentTimeMillis() - startTime;
            recordRequestStatistics(clientIp, requestPath, requestMethod, responseTime, rateLimited);
        }
    }

    /**
     * 检查IP级别限流
     */
    private void checkIpRateLimit(String clientIp) {
        try {
            ipRateLimiter.acquirePermission();
        } catch (RequestNotPermitted e) {
            throw new RateLimitExceededException("IP请求频率超限，请稍后重试");
        }
    }

    /**
     * 检查API级别限流
     */
    private void checkApiRateLimit(String requestPath, String clientIp) {
        // 查找匹配的限流器
        RateLimiter matchedLimiter = findMatchingRateLimiter(requestPath);
        
        if (matchedLimiter != null) {
            try {
                matchedLimiter.acquirePermission();
            } catch (RequestNotPermitted e) {
                String message = String.format("API请求频率超限: %s，请稍后重试", requestPath);
                throw new RateLimitExceededException(message);
            }
        }
        
        // 额外的自实现限流逻辑（用于应对突发流量）
        checkCustomRateLimit(clientIp, requestPath);
    }

    /**
     * 自实现限流逻辑（补充Resilience4j）
     * 用于防止过于频繁的请求
     */
    private void checkCustomRateLimit(String clientIp, String requestPath) {
        Instant now = Instant.now();
        String key = clientIp + ":" + requestPath;
        
        Instant lastRequestTime = ipRequestTimes.get(key);
        int currentCount = ipRequestCounts.getOrDefault(key, new AtomicInteger(0)).get();
        
        // 如果是同一秒内的请求
        if (lastRequestTime != null && Duration.between(lastRequestTime, now).getSeconds() < 1) {
            // 每秒最多允许10个相同路径的请求
            if (currentCount >= 10) {
                throw new RateLimitExceededException("请求过于频繁，请稍后重试");
            }
            ipRequestCounts.get(key).incrementAndGet();
        } else {
            // 重置计数
            ipRequestTimes.put(key, now);
            ipRequestCounts.put(key, new AtomicInteger(1));
        }
    }

    /**
     * 查找匹配的限流器
     */
    private RateLimiter findMatchingRateLimiter(String requestPath) {
        // 精确匹配
        if (apiRateLimiters.containsKey(requestPath)) {
            return apiRateLimiters.get(requestPath);
        }
        
        // 前缀匹配（用于动态路径如 /api/transactions/123）
        for (Map.Entry<String, RateLimiter> entry : apiRateLimiters.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("/**")) {
                String basePath = pattern.substring(0, pattern.length() - 3);
                if (requestPath.startsWith(basePath)) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
    }

    /**
     * 记录请求统计信息
     */
    private void recordRequestStatistics(String clientIp, String requestPath, String requestMethod, 
                                       long responseTime, boolean rateLimited) {
        try {
            // 更新API调用统计
            apiCallStats.computeIfAbsent(requestPath, k -> new AtomicInteger(0)).incrementAndGet();
            
            // 更新客户端请求统计
            clientRequestStats.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
            
            // 发送监控数据到监控服务
            if (monitoringService != null) {
                monitoringService.recordApiRequest(requestPath, clientIp, responseTime, rateLimited);
            }
        } catch (Exception e) {
            // 监控服务异常不影响主要业务流程
            logger.warn("Failed to record monitoring statistics", e);
        }
    }

    /**
     * 处理限流超限
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, String requestPath) throws IOException {
        response.setStatus(SC_TOO_MANY_REQUESTS);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", "60"); // 建议客户端60秒后重试
        
        String responseBody = String.format(
            "{\"code\": 429, \"message\": \"请求频率超限，请稍后重试\", \"data\": null, \"timestamp\": %d}",
            System.currentTimeMillis()
        );
        
        response.getWriter().write(responseBody);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return remoteAddr;
    }
}