package com.finance.cache;

import com.finance.service.CachePenetrationProtection;
import com.finance.service.CacheMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存支持类（已集成优化功能）
 * 提供缓存操作，支持穿透防护和监控统计
 */
@Component
public class CacheSupport {

    @Autowired
    private CachePenetrationProtection penetrationProtection;

    @Autowired
    private CacheMonitoringService monitoringService;

    // 本地缓存实现
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    /**
     * 缓存条目，包含值和过期时间
     */
    private static class CacheEntry {
        final Object value;
        final long expireTime; // 过期时间戳（毫秒）

        CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 从缓存获取数据，如果不存在则加载并缓存
     *
     * @param key 缓存键
     * @param loader 加载函数
     * @param expireSeconds 过期时间（秒）
     * @param cacheName 缓存名称（用于监控统计）
     * @return 缓存数据
     */
    public <T> T getOrLoad(String key, CacheLoader<T> loader, long expireSeconds, String cacheName) {
        try {
            // 1. 尝试从缓存获取数据
            T value = (T) getFromLocalCache(key);
            if (value != null) {
                monitoringService.recordHit(key, cacheName);
                return value;
            } else if (localCache.containsKey(key)) {
                // 存在但已过期或为null值缓存
                monitoringService.recordNullValue(key, cacheName);
                return null;
            }

            // 2. 记录miss
            monitoringService.recordMiss(key, cacheName);

            // 3. 直接加载数据（移除了Redis相关的穿透防护，因为它也依赖Redis）
            T loaded = loader.load();
            
            if (loaded != null) {
                monitoringService.recordWrite(key, cacheName);
                putToLocalCache(key, loaded, expireSeconds);
            } else {
                // 缓存null值，防止穿透
                monitoringService.recordNullValue(key, cacheName);
                putToLocalCache(key, CacheNames.NULL_CACHE, expireSeconds / 4);
            }
            
            return loaded;

        } catch (Exception e) {
            monitoringService.recordError(key, cacheName);
            // 降级到直接加载数据
            try {
                return loader.load();
            } catch (Exception ex) {
                throw new RuntimeException("缓存和直接加载都失败", ex);
            }
        }
    }

    /**
     * 兼容旧版本的getOrLoad方法
     * @deprecated 请使用 getOrLoad(String, CacheLoader, long, String) 方法
     */
    @Deprecated
    public <T> T getOrLoad(String key, CacheLoader<T> loader, long expireSeconds) {
        return getOrLoad(key, loader, expireSeconds, "DEFAULT");
    }

    /**
     * 从本地缓存获取数据
     */
    private Object getFromLocalCache(String key) {
        CacheEntry entry = localCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            localCache.remove(key);
            return null;
        }
        if (CacheNames.NULL_CACHE.equals(entry.value)) {
            return null;
        }
        return entry.value;
    }

    /**
     * 将数据放入本地缓存
     */
    private void putToLocalCache(String key, Object value, long expireSeconds) {
        long expireTime = expireSeconds > 0 ? System.currentTimeMillis() + expireSeconds * 1000 : 0;
        localCache.put(key, new CacheEntry(value, expireTime));
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            localCache.remove(key);
            monitoringService.recordDelete(key, "DEFAULT");
        } catch (Exception e) {
            monitoringService.recordError(key, "DEFAULT");
            throw e;
        }
    }

    /**
     * 删除缓存（带监控）
     */
    public void delete(String key, String cacheName) {
        try {
            localCache.remove(key);
            monitoringService.recordDelete(key, cacheName);
        } catch (Exception e) {
            monitoringService.recordError(key, cacheName);
            throw e;
        }
    }

    /**
     * 设置缓存（包含过期时间）
     */
    public void set(String key, Object value, long expireSeconds) {
        try {
            if (value != null) {
                putToLocalCache(key, value, expireSeconds);
                monitoringService.recordWrite(key, "MANUAL_SET");
            }
        } catch (Exception e) {
            monitoringService.recordError(key, "MANUAL_SET");
            throw e;
        }
    }

    /**
     * 设置缓存（包含过期时间和监控）
     */
    public void set(String key, Object value, long expireSeconds, String cacheName) {
        try {
            if (value != null) {
                putToLocalCache(key, value, expireSeconds);
                monitoringService.recordWrite(key, cacheName);
            }
        } catch (Exception e) {
            monitoringService.recordError(key, cacheName);
            throw e;
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            CacheEntry entry = localCache.get(key);
            if (entry != null && !entry.isExpired()) {
                return true;
            } else if (entry != null) {
                // 已过期，移除
                localCache.remove(key);
            }
            return false;
        } catch (Exception e) {
            monitoringService.recordError(key, "EXISTS_CHECK");
            return false;
        }
    }

    /**
     * 获取缓存值（不加载）
     */
    public Object get(String key) {
        try {
            return getFromLocalCache(key);
        } catch (Exception e) {
            monitoringService.recordError(key, "GET");
            return null;
        }
    }

    /**
     * 获取缓存值（不加载，带监控）
     */
    public Object get(String key, String cacheName) {
        try {
            Object value = getFromLocalCache(key);
            if (value != null) {
                monitoringService.recordHit(key, cacheName);
            } else if (localCache.containsKey(key)) {
                monitoringService.recordNullValue(key, cacheName);
            }
            return value;
        } catch (Exception e) {
            monitoringService.recordError(key, cacheName);
            return null;
        }
    }

    /**
     * 批量删除缓存
     */
    public void deleteBatch(String pattern) {
        try {
            // 简单实现，不支持复杂pattern匹配
            localCache.clear();
            monitoringService.recordDelete(pattern, "BATCH_DELETE");
        } catch (Exception e) {
            monitoringService.recordError(pattern, "BATCH_DELETE");
            throw e;
        }
    }

    /**
     * 设置缓存过期时间
     */
    public boolean expire(String key, long expireSeconds) {
        try {
            CacheEntry entry = localCache.get(key);
            if (entry != null) {
                // 重新设置过期时间
                long expireTime = System.currentTimeMillis() + expireSeconds * 1000;
                localCache.put(key, new CacheEntry(entry.value, expireTime));
                return true;
            }
            return false;
        } catch (Exception e) {
            monitoringService.recordError(key, "EXPIRE_SET");
            return false;
        }
    }

    /**
     * 获取缓存剩余过期时间
     */
    public Long getExpire(String key) {
        try {
            CacheEntry entry = localCache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.expireTime <= 0) {
                return Long.MAX_VALUE;
            }
            long remaining = entry.expireTime - System.currentTimeMillis();
            return remaining > 0 ? remaining / 1000 : 0;
        } catch (Exception e) {
            monitoringService.recordError(key, "EXPIRE_GET");
            return null;
        }
    }
}
