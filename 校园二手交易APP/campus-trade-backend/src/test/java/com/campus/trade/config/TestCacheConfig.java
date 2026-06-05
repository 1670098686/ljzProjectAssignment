package com.campus.trade.config;

import com.campus.trade.service.HotProductCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // 在测试环境中使用无操作缓存管理器
        return new NoOpCacheManager();
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // 创建一个模拟的RedisConnectionFactory
        return new RedisConnectionFactory() {
            @Override
            public org.springframework.data.redis.connection.RedisConnection getConnection() {
                throw new UnsupportedOperationException("Redis is not available in test environment");
            }

            @Override
            public org.springframework.data.redis.connection.RedisClusterConnection getClusterConnection() {
                throw new UnsupportedOperationException("Redis is not available in test environment");
            }

            @Override
            public boolean getConvertPipelineAndTxResults() {
                return false;
            }

            @Override
            public org.springframework.data.redis.connection.RedisSentinelConnection getSentinelConnection() {
                throw new UnsupportedOperationException("Redis is not available in test environment");
            }

            @Override
            public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
                return null; // 返回null表示不进行异常转换
            }
        };
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper objectMapper) {
        // 创建一个模拟的RedisTemplate，不执行实际Redis操作
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    @Bean
    @Primary
    public HotProductCacheService hotProductCacheService(RedisTemplate<String, Object> redisTemplate) {
        // 创建一个模拟的HotProductCacheService，避免Redis依赖
        return new HotProductCacheService(redisTemplate, null) {
            @Override
            public List<String> getHotSearchTitles(int limit) {
                // 返回空列表，避免Redis操作
                return new ArrayList<>();
            }
            
            @Override
            public void evictHotSearchCaches() {
                // 在测试环境中不执行任何操作，避免Redis调用
                // 不调用父类的evictHotSearchCaches方法
            }
        };
    }
}