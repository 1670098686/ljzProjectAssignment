package com.campus.trade.service;

import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.projection.ProductHeatView;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HotProductCacheService {

    private static final Logger logger = LoggerFactory.getLogger(HotProductCacheService.class);
    private static final int MAX_LIMIT = 10;
    private static final Duration HOT_TTL = Duration.ofSeconds(45);
    private static final String HOT_TITLE_KEY_PREFIX = "hot:product:titles:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final boolean redisAvailable;

    public HotProductCacheService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                                  ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.redisAvailable = redisTemplate != null;
    }

    public List<String> getHotSearchTitles(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        String key = buildKey(safeLimit);
        List<String> cached = null;
        
        if (redisAvailable) {
            try {
                cached = readCachedTitles(key);
            } catch (RedisConnectionFailureException e) {
                logger.warn("Failed to read from Redis cache: {}", e.getMessage());
            }
        }
        
        if (cached != null) {
            return cached;
        }
        
        List<String> titles = loadTrendingTitles(safeLimit);
        
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, titles, HOT_TTL);
            } catch (RedisConnectionFailureException e) {
                logger.warn("Failed to write to Redis cache: {}", e.getMessage());
            }
        }
        
        return titles;
    }

    public void evictHotSearchCaches() {
        if (redisAvailable) {
            try {
                for (int i = 1; i <= MAX_LIMIT; i++) {
                    redisTemplate.delete(buildKey(i));
                }
            } catch (RedisConnectionFailureException e) {
                logger.warn("Failed to evict Redis cache: {}", e.getMessage());
            }
        }
    }

    private List<String> loadTrendingTitles(int limit) {
        List<ProductHeatView> hotViews = productRepository.topViewedProducts(PageRequest.of(0, Math.max(limit, 10)));
        if (CollectionUtils.isEmpty(hotViews)) {
            return List.of();
        }
        List<String> titles = new ArrayList<>();
        for (ProductHeatView view : hotViews) {
            if (view == null) {
                continue;
            }
            String title = view.getProductTitle();
            if (!StringUtils.hasText(title)) {
                continue;
            }
            if (!titles.contains(title)) {
                titles.add(title);
            }
            if (titles.size() >= limit) {
                break;
            }
        }
        return titles;
    }

    @SuppressWarnings("unchecked")
    private List<String> readCachedTitles(String key) {
        if (!redisAvailable) {
            return null;
        }
        
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (!(value instanceof List<?> list)) {
                return null;
            }
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (RedisConnectionFailureException e) {
            logger.warn("Failed to read cached titles from Redis: {}", e.getMessage());
            return null;
        }
    }

    private String buildKey(int limit) {
        return HOT_TITLE_KEY_PREFIX + limit;
    }
}
