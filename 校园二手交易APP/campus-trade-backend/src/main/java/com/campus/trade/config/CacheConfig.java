package com.campus.trade.config;

import com.campus.trade.config.cache.LayeredCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Set<String> LAYERED_CACHE_NAMES = Set.of(
        "product:detail",
        "product:list",
        "search:suggestion"
    );

    @Bean
    public SimpleCacheManager localCacheManager() {
    CaffeineCache productDetailCache = new CaffeineCache(
        "product:detail",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000)
            .recordStats()
            .build()
    );

    CaffeineCache productListCache = new CaffeineCache(
        "product:list",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(500)
            .recordStats()
            .build()
    );

    CaffeineCache searchSuggestionCache = new CaffeineCache(
        "search:suggestion",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(15))
            .maximumSize(200)
            .recordStats()
            .build()
    );

    // 添加admin相关缓存
    CaffeineCache adminOrderReportCache = new CaffeineCache(
        "admin:order-report",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(100)
            .recordStats()
            .build()
    );

    CaffeineCache adminDashboardOverviewCache = new CaffeineCache(
        "admin:dashboard:overview",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(100)
            .recordStats()
            .build()
    );

    CaffeineCache adminDashboardTrendCache = new CaffeineCache(
        "admin:dashboard:trend",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(100)
            .recordStats()
            .build()
    );

    CaffeineCache adminDashboardRankingCache = new CaffeineCache(
        "admin:dashboard:ranking",
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(100)
            .recordStats()
            .build()
    );

    SimpleCacheManager manager = new SimpleCacheManager();
    manager.setCaches(List.of(
        productDetailCache, 
        productListCache, 
        searchSuggestionCache,
        adminOrderReportCache,
        adminDashboardOverviewCache,
        adminDashboardTrendCache,
        adminDashboardRankingCache
    ));
    return manager;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(SimpleCacheManager localCacheManager) {
    // 只使用本地缓存，避免依赖Redis
    return localCacheManager;
    }
}
