package com.campus.trade.config.cache;

import java.util.Map;
import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;

public class LayeredCache implements Cache {

    private final String name;
    private final Cache localCache;
    private final Cache remoteCache;

    public LayeredCache(String name, Cache localCache, Cache remoteCache) {
        this.name = name;
        this.localCache = localCache;
        this.remoteCache = remoteCache;
    }

    public Cache getLocalCache() {
        return localCache;
    }

    public Cache getRemoteCache() {
        return remoteCache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return Map.of(
                "local", localCache.getNativeCache(),
                "remote", remoteCache.getNativeCache()
        );
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper local = localCache.get(key);
        if (local != null) {
            return local;
        }
        try {
            ValueWrapper remote = remoteCache.get(key);
            if (remote != null) {
                localCache.put(key, remote.get());
            }
            return remote;
        } catch (Exception e) {
            // Redis连接失败，只使用本地缓存
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (type != null && value != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type: " + type);
        }
        return (T) value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, Object value) {
        localCache.put(key, value);
        try {
            remoteCache.put(key, value);
        } catch (Exception e) {
            // Redis连接失败，只更新本地缓存
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper localResult = localCache.putIfAbsent(key, value);
        try {
            ValueWrapper remoteResult = remoteCache.putIfAbsent(key, value);
            if (localResult == null && remoteResult == null) {
                return null;
            }
            return localResult != null ? localResult : remoteResult;
        } catch (Exception e) {
            // Redis连接失败，只使用本地缓存的结果
            return localResult;
        }
    }

    @Override
    public void evict(Object key) {
        localCache.evict(key);
        try {
            remoteCache.evict(key);
        } catch (Exception e) {
            // Redis连接失败，只清除本地缓存
        }
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean localEvicted = localCache.evictIfPresent(key);
        try {
            boolean remoteEvicted = remoteCache.evictIfPresent(key);
            return localEvicted || remoteEvicted;
        } catch (Exception e) {
            // Redis连接失败，只检查本地缓存
            return localEvicted;
        }
    }

    @Override
    public void clear() {
        localCache.clear();
        try {
            remoteCache.clear();
        } catch (Exception e) {
            // Redis连接失败，只清除本地缓存
        }
    }

    @Override
    public boolean invalidate() {
        boolean localCleared = localCache.invalidate();
        try {
            boolean remoteCleared = remoteCache.invalidate();
            return localCleared || remoteCleared;
        } catch (Exception e) {
            // Redis连接失败，只检查本地缓存
            return localCleared;
        }
    }
}
