package com.finance.cache;

/**
 * 缓存加载器函数式接口
 */
@FunctionalInterface
public interface CacheLoader<T> {
    T load() throws Exception;
}