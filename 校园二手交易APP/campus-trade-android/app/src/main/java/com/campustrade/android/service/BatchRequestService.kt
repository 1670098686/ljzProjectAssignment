package com.campustrade.android.service

import com.campustrade.android.model.*
import com.campustrade.android.network.ApiService
import com.campustrade.android.network.BatchRequestManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量请求服务
 * 提供批量请求的封装和简化调用
 */
@Singleton
class BatchRequestService @Inject constructor(
    private val apiService: ApiService,
    private val batchRequestManager: BatchRequestManager
) {
    
    /**
     * 批量获取多个商品详情
     */
    suspend fun getProductsBatch(productIds: List<Long>): Map<Long, ApiResponse<Product>> {
        val batchRequest = BatchRequestUtils.createProductBatchRequest(productIds)
        
        val batchResponse = batchRequestManager.executeBatch(
            requestKey = "products_batch_${productIds.joinToString("_")}",
            cacheDuration = BatchRequestManager.DEFAULT_CACHE_DURATION
        ) {
            apiService.executeBatch(batchRequest)
        }
        
        return processBatchResponse(batchResponse, productIds)
    }
    
    /**
     * 批量获取多个用户信息
     */
    suspend fun getUsersBatch(userIds: List<Long>): Map<Long, ApiResponse<User>> {
        val batchRequest = BatchRequestUtils.createUserBatchRequest(userIds)
        
        val batchResponse = batchRequestManager.executeBatch(
            requestKey = "users_batch_${userIds.joinToString("_")}",
            cacheDuration = BatchRequestManager.DEFAULT_CACHE_DURATION
        ) {
            apiService.executeBatch(batchRequest)
        }
        
        return processBatchResponse(batchResponse, userIds)
    }
    
    /**
     * 批量获取多个订单详情
     */
    suspend fun getOrdersBatch(orderIds: List<Long>): Map<Long, ApiResponse<Order>> {
        val batchRequest = BatchRequestUtils.createOrderBatchRequest(orderIds)
        
        val batchResponse = batchRequestManager.executeBatch(
            requestKey = "orders_batch_${orderIds.joinToString("_")}",
            cacheDuration = BatchRequestManager.DEFAULT_CACHE_DURATION
        ) {
            apiService.executeBatch(batchRequest)
        }
        
        return processBatchResponse(batchResponse, orderIds)
    }
    
    /**
     * 合并多个不同类型的请求
     */
    suspend fun executeMixedBatch(
        productIds: List<Long> = emptyList(),
        userIds: List<Long> = emptyList(),
        orderIds: List<Long> = emptyList()
    ): BatchResult {
        val builder = BatchRequestBuilder()
        
        // 添加商品请求
        productIds.forEachIndexed { index, productId ->
            builder.addProductRequest(
                id = "product_$index",
                method = HttpMethod.GET,
                path = "/api/v1/products/$productId"
            )
        }
        
        // 添加用户请求
        userIds.forEachIndexed { index, userId ->
            builder.addUserRequest(
                id = "user_$index",
                method = HttpMethod.GET,
                path = "/api/v1/users/$userId"
            )
        }
        
        // 添加订单请求
        orderIds.forEachIndexed { index, orderId ->
            builder.addOrderRequest(
                id = "order_$index",
                method = HttpMethod.GET,
                path = "/api/v1/orders/$orderId"
            )
        }
        
        val batchRequest = builder.build()
        val requestKey = "mixed_batch_${System.currentTimeMillis()}"
        
        val batchResponse = batchRequestManager.executeBatch(requestKey) {
            apiService.executeBatch(batchRequest)
        }
        
        return BatchResult(
            products = extractResponses(batchResponse, productIds, "product_"),
            users = extractResponses(batchResponse, userIds, "user_"),
            orders = extractResponses(batchResponse, orderIds, "order_")
        )
    }
    
    /**
     * 清除商品相关的缓存
     */
    suspend fun clearProductCache(productId: Long? = null) {
        if (productId != null) {
            batchRequestManager.clearCache("products_batch_$productId")
        } else {
            // 清除所有商品相关的缓存
            batchRequestManager.clearCache("products_batch")
        }
    }
    
    /**
     * 清除用户相关的缓存
     */
    suspend fun clearUserCache(userId: Long? = null) {
        if (userId != null) {
            batchRequestManager.clearCache("users_batch_$userId")
        } else {
            batchRequestManager.clearCache("users_batch")
        }
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        batchRequestManager.clearAllCache()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            cachedRequests = batchRequestManager.getCachedRequestCount(),
            pendingRequests = batchRequestManager.getPendingRequestCount()
        )
    }
    
    private fun <T> processBatchResponse(
        apiResponse: ApiResponse<BatchResponse>,
        ids: List<Long>
    ): Map<Long, ApiResponse<T>> {
        if (!apiResponse.success || apiResponse.data == null) {
            // 批量请求失败，返回空结果
            return ids.associateWith { 
                ApiResponse(success = false, message = "批量请求失败")
            }
        }
        
        val result = mutableMapOf<Long, ApiResponse<T>>()
        ids.forEachIndexed { index, id ->
            val requestId = when (T::class.java) {
                Product::class.java -> "product_$index"
                User::class.java -> "user_$index"
                Order::class.java -> "order_$index"
                else -> "unknown_$index"
            }
            
            val response = BatchRequestUtils.extractResponse<T>(apiResponse.data, requestId)
            result[id] = response ?: ApiResponse(success = false, message = "响应解析失败")
        }
        
        return result
    }
    
    private fun <T> extractResponses(
        apiResponse: ApiResponse<BatchResponse>,
        ids: List<Long>,
        prefix: String
    ): Map<Long, ApiResponse<T>> {
        if (!apiResponse.success || apiResponse.data == null) {
            return emptyMap()
        }
        
        val result = mutableMapOf<Long, ApiResponse<T>>()
        ids.forEachIndexed { index, id ->
            val requestId = "${prefix}$index"
            val response = BatchRequestUtils.extractResponse<T>(apiResponse.data, requestId)
            if (response != null) {
                result[id] = response
            }
        }
        
        return result
    }
}

/**
 * 批量请求结果
 */
data class BatchResult(
    val products: Map<Long, ApiResponse<Product>>,
    val users: Map<Long, ApiResponse<User>>,
    val orders: Map<Long, ApiResponse<Order>>
)

/**
 * 缓存统计信息
 */
data class CacheStats(
    val cachedRequests: Int,
    val pendingRequests: Int
)