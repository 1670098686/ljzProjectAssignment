package com.campustrade.android.repository

import com.campustrade.android.model.ApiResponse
import com.campustrade.android.model.Order
import com.campustrade.android.network.ApiService
import com.campustrade.android.service.BatchRequestService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val apiService: ApiService,
    private val batchRequestService: BatchRequestService
) {
    
    suspend fun getOrders(
        page: Int = 0,
        size: Int = 20,
        status: String? = null
    ): ApiResponse<List<Order>> {
        return apiService.getOrders(page, size, status)
    }
    
    suspend fun getOrderById(id: Long): ApiResponse<Order> {
        return apiService.getOrderById(id)
    }
    
    suspend fun createOrder(order: Order): ApiResponse<Order> {
        return apiService.createOrder(order)
    }
    
    suspend fun updateOrder(id: Long, order: Order): ApiResponse<Order> {
        return apiService.updateOrder(id, order)
    }
    
    suspend fun cancelOrder(id: Long): ApiResponse<Unit> {
        return apiService.cancelOrder(id)
    }
    
    suspend fun payOrder(id: Long, paymentMethod: String): ApiResponse<Unit> {
        return apiService.payOrder(id, paymentMethod)
    }
    
    suspend fun confirmReceipt(id: Long): ApiResponse<Unit> {
        return apiService.confirmReceipt(id)
    }
    
    // ========== 批量请求优化方法 ==========
    
    /**
     * 批量获取多个订单详情（优化版本）
     * 使用批量请求合并，减少网络请求次数
     */
    suspend fun getOrdersBatch(orderIds: List<Long>): Map<Long, ApiResponse<Order>> {
        return batchRequestService.getOrdersBatch(orderIds)
    }
    
    /**
     * 批量获取订单详情，支持回退机制
     */
    suspend fun getOrdersBatchWithFallback(orderIds: List<Long>): Map<Long, ApiResponse<Order>> {
        return try {
            batchRequestService.getOrdersBatch(orderIds)
        } catch (e: Exception) {
            getOrdersSequentially(orderIds)
        }
    }
    
    /**
     * 顺序获取多个订单详情（回退方法）
     */
    private suspend fun getOrdersSequentially(orderIds: List<Long>): Map<Long, ApiResponse<Order>> {
        val results = mutableMapOf<Long, ApiResponse<Order>>()
        
        orderIds.forEach { orderId ->
            try {
                val response = apiService.getOrderById(orderId)
                results[orderId] = response
            } catch (e: Exception) {
                results[orderId] = ApiResponse(
                    success = false,
                    message = "获取订单详情失败: ${e.message}"
                )
            }
        }
        
        return results
    }
    
    /**
     * 获取订单列表并预加载详情（性能优化）
     */
    suspend fun getOrdersWithDetails(
        page: Int = 0,
        size: Int = 20,
        status: String? = null
    ): ApiResponse<List<Order>> {
        // 首先获取订单列表
        val ordersResponse = apiService.getOrders(page, size, status)
        
        if (!ordersResponse.success || ordersResponse.data == null) {
            return ordersResponse
        }
        
        // 提取订单ID列表
        val orderIds = ordersResponse.data.map { it.id }
        
        // 批量获取订单详情
        val detailsMap = getOrdersBatchWithFallback(orderIds)
        
        // 合并详情信息
        val enrichedOrders = ordersResponse.data.map { order ->
            val detailResponse = detailsMap[order.id]
            if (detailResponse?.success == true && detailResponse.data != null) {
                // 使用详细的订单信息
                detailResponse.data
            } else {
                // 使用基础的订单信息
                order
            }
        }
        
        return ApiResponse(
            success = true,
            data = enrichedOrders,
            message = ordersResponse.message
        )
    }
    
    /**
     * 清除订单缓存
     */
    suspend fun clearOrderCache(orderId: Long? = null) {
        batchRequestService.clearAllCache() // 暂时使用清除所有缓存
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): com.campustrade.android.service.CacheStats {
        return batchRequestService.getCacheStats()
    }
}