package com.campus.trade.config.cache;

import java.util.Collection;
import java.util.Set;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;

public class LayeredCacheManager extends AbstractCacheManager {

    private final CacheManager localCacheManager;
    private final CacheManager remoteCacheManager;
    private final Set<String> layeredCacheNames;

    public LayeredCacheManager(CacheManager localCacheManager,
                               CacheManager remoteCacheManager,
                               Collection<String> layeredCacheNames) {
        Assert.notNull(localCacheManager, "localCacheManager must not be null");
        Assert.notNull(remoteCacheManager, "remoteCacheManager must not be null");
        Assert.notEmpty(layeredCacheNames, "layeredCacheNames must not be empty");
        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = remoteCacheManager;
        this.layeredCacheNames = Set.copyOf(layeredCacheNames);
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return layeredCacheNames.stream()
                .map(this::createLayeredCache)
                .toList();
    }

    @Override
    protected Cache getMissingCache(String name) {
        if (layeredCacheNames.contains(name)) {
            return createLayeredCache(name);
        }
        Cache remote = remoteCacheManager.getCache(name);
        if (remote != null) {
            return remote;
        }
        return localCacheManager.getCache(name);
    }

    private Cache createLayeredCache(String name) {
        Cache local = localCacheManager.getCache(name);
        if (local == null) {
            throw new IllegalStateException("Local cache not found: " + name);
        }
        
        try {
            Cache remote = remoteCacheManager.getCache(name);
            if (remote != null) {
                return new LayeredCache(name, local, remote);
            }
        } catch (Exception e) {
            // Redis连接失败，仅使用本地缓存
        }
        
        // 如果远程缓存不可用或连接失败，只返回本地缓存
        return local;
    }
}
