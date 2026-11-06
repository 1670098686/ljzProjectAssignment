package com.finance.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 熔断器和重试配置
 * 
 * 配置Resilience4j熔断器、重试机制、限流等容错策略
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@Configuration
public class Resilience4jConfig {

    /**
     * 配置全局熔断器注册表
     * 
     * @return 熔断器注册表
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // 创建默认熔断器配置
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                // 滑动窗口大小：统计10次调用的结果
                .slidingWindowSize(10)
                // 失败率阈值：失败率达到50%时熔断器打开
                .failureRateThreshold(50f)
                // 等待时间：在熔断器打开状态下等待10秒后再尝试半开状态
                .waitDurationInOpenState(Duration.ofSeconds(10))
                // 最小调用次数：至少需要5次调用才会计算失败率
                .minimumNumberOfCalls(5)
                // 半开状态允许的最大调用次数
                .permittedNumberOfCallsInHalfOpenState(3)
                // 慢调用阈值：调用时间超过60%时被认为是慢调用
                .slowCallRateThreshold(60f)
                // 慢调用阈值时间：超过2秒被认为是慢调用
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // 自动从打开状态转换到半开状态
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // 事件缓冲区大小
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    /**
     * 配置重试注册表
     * 
     * @return 重试注册表
     */
    @Bean
    public RetryRegistry retryRegistry() {
        // 创建默认重试配置
        RetryConfig defaultConfig = RetryConfig.custom()
                // 最大重试次数
                .maxAttempts(3)
                // 重试间隔时间
                .waitDuration(Duration.ofMillis(200))
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    /**
     * 为budget-service配置专用熔断器
     * 
     * @return budget-service的熔断器配置
     */
    public CircuitBreakerConfig budgetServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(5) // 较小窗口，快速响应
                .failureRateThreshold(60f) // 60%失败率触发熔断
                .waitDurationInOpenState(Duration.ofSeconds(5)) // 5秒后尝试恢复
                .minimumNumberOfCalls(3)
                .permittedNumberOfCallsInHalfOpenState(2)
                .slowCallRateThreshold(70f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .build();
    }

    /**
     * 为statistics-service配置专用熔断器
     * 
     * @return statistics-service的熔断器配置
     */
    public CircuitBreakerConfig statisticsServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(8)
                .failureRateThreshold(40f) // 较低的失败率阈值，统计服务要求更高的可用性
                .waitDurationInOpenState(Duration.ofSeconds(15)) // 较长的等待时间
                .minimumNumberOfCalls(4)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(50f)
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // 统计服务可能有较长的计算时间
                .build();
    }

    /**
     * 为alert-service配置专用熔断器
     * 
     * @return alert-service的熔断器配置
     */
    public CircuitBreakerConfig alertServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(6)
                .failureRateThreshold(70f) // 较高的失败率阈值，预警服务容忍度更高
                .waitDurationInOpenState(Duration.ofSeconds(8))
                .minimumNumberOfCalls(3)
                .permittedNumberOfCallsInHalfOpenState(2)
                .slowCallRateThreshold(80f)
                .slowCallDurationThreshold(Duration.ofSeconds(4))
                .build();
    }
}