package com.campustrade.android.network

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络请求合并管理器
 * 用于合并相同类型的请求，减少网络请求次数
 */
@Singleton
class BatchRequestManager {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    
    // 存储正在进行的请求
    private val pendingRequests = ConcurrentHashMap<String, Deferred<*>>()
    
    // 请求缓存，避免短时间内重复请求
    private val requestCache = ConcurrentHashMap<String, CacheEntry<*>>()
    
    /**
     * 执行批量请求，合并相同类型的请求
     */
    suspend fun <T> executeBatch(
        requestKey: String,
        cacheDuration: Long = DEFAULT_CACHE_DURATION,
        request: suspend () -> T
    ): T {
        // 检查缓存
        val cachedResult = getFromCache<T>(requestKey)
        if (cachedResult != null) {
            return cachedResult
        }
        
        return mutex.withLock {
            // 检查是否有正在进行的相同请求
            val existingRequest = pendingRequests[requestKey] as? Deferred<T>
            
            if (existingRequest != null) {
                // 等待现有请求完成并返回结果
                return@withLock existingRequest.await()
            }
            
            // 创建新的请求
            val deferred = scope.async {
                try {
                    val result = request()
                    // 缓存结果
                    cacheResult(requestKey, result, cacheDuration)
                    result
                } finally {
                    // 请求完成后从pending中移除
                    mutex.withLock {
                        pendingRequests.remove(requestKey)
                    }
                }
            }
            
            // 保存到pending请求
            pendingRequests[requestKey] = deferred
            
            deferred.await()
        }
    }
    
    /**
     * 批量执行多个请求，合并为一次网络调用
     */
    suspend fun <T> executeMultiple(
        requests: List<Pair<String, suspend () -> T>>,
        cacheDuration: Long = DEFAULT_CACHE_DURATION
    ): Map<String, T> {
        return coroutineScope {
            val deferredResults = requests.map { (key, request) ->
                async {
                    key to executeBatch(key, cacheDuration, request)
                }
            }
            
            deferredResults.awaitAll().associate { it }
        }
    }
    
    /**
     * 清除指定请求的缓存
     */
    suspend fun clearCache(requestKey: String) {
        mutex.withLock {
            requestCache.remove(requestKey)
        }
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        mutex.withLock {
            requestCache.clear()
        }
    }
    
    /**
     * 获取缓存中的请求数量
     */
    fun getCachedRequestCount(): Int {
        return requestCache.size
    }
    
    /**
     * 获取正在进行的请求数量
     */
    fun getPendingRequestCount(): Int {
        return pendingRequests.size
    }
    
    private fun <T> getFromCache(requestKey: String): T? {
        val entry = requestCache[requestKey] as? CacheEntry<T>
        return if (entry != null && System.currentTimeMillis() < entry.expireTime) {
            entry.data
        } else {
            // 缓存过期，移除
            requestCache.remove(requestKey)
            null
        }
    }
    
    private fun <T> cacheResult(requestKey: String, data: T, duration: Long) {
        val expireTime = System.currentTimeMillis() + duration
        requestCache[requestKey] = CacheEntry(data, expireTime)
    }
    
    companion object {
        const val DEFAULT_CACHE_DURATION = TimeUnit.MINUTES.toMillis(5) // 5分钟缓存
    }
    
    private data class CacheEntry<T>(
        val data: T,
        val expireTime: Long
    )
}