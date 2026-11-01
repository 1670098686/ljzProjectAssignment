package com.campustrade.android.repository

import com.campustrade.android.model.Product
import com.campustrade.android.model.ApiResponse
import com.campustrade.android.model.enums.ProductCategory
import com.campustrade.android.network.ApiService
import com.campustrade.android.service.BatchRequestService
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ApiService,
    private val batchRequestService: BatchRequestService
) {
    
    suspend fun getProducts(
        page: Int = 0,
        size: Int = 20,
        category: String? = null,
        keyword: String? = null
    ): ApiResponse<List<Product>> {
        return apiService.getProducts(page, size, category, keyword)
    }
    
    suspend fun getProductById(id: Long): ApiResponse<Product> {
        return apiService.getProductById(id)
    }
    
    suspend fun createProduct(product: Product): ApiResponse<Product> {
        return apiService.createProduct(product)
    }
    
    suspend fun updateProduct(id: Long, product: Product): ApiResponse<Product> {
        return apiService.updateProduct(id, product)
    }
    
    suspend fun deleteProduct(id: Long): ApiResponse<Unit> {
        return apiService.deleteProduct(id)
    }
    
    suspend fun publishProduct(
        title: String,
        price: BigDecimal,
        description: String,
        category: ProductCategory,
        contactInfo: String,
        location: String,
        images: List<String> = emptyList()
    ): ApiResponse<Product> {
        val product = Product(
            title = title,
            price = price,
            description = description,
            category = category,
            contactInfo = contactInfo,
            location = location,
            images = images
        )
        return apiService.createProduct(product)
    }

    suspend fun getMyProducts(userId: Long, page: Int = 0, size: Int = 20): ApiResponse<List<Product>> {
        return apiService.getMyProducts(userId, page, size)
    }

    suspend fun getFavoriteProducts(userId: Long, page: Int = 0, size: Int = 20): ApiResponse<List<Product>> {
        return apiService.getFavoriteProducts(userId, page, size)
    }
    
    // ========== 批量请求优化方法 ==========
    
    /**
     * 批量获取多个商品详情（优化版本）
     * 使用批量请求合并，减少网络请求次数
     */
    suspend fun getProductsBatch(productIds: List<Long>): Map<Long, ApiResponse<Product>> {
        return batchRequestService.getProductsBatch(productIds)
    }
    
    /**
     * 批量获取商品详情，支持回退机制
     * 如果批量请求失败，自动回退到单个请求
     */
    suspend fun getProductsBatchWithFallback(productIds: List<Long>): Map<Long, ApiResponse<Product>> {
        return try {
            // 首先尝试批量请求
            batchRequestService.getProductsBatch(productIds)
        } catch (e: Exception) {
            // 批量请求失败，回退到单个请求
            getProductsSequentially(productIds)
        }
    }
    
    /**
     * 顺序获取多个商品详情（回退方法）
     */
    private suspend fun getProductsSequentially(productIds: List<Long>): Map<Long, ApiResponse<Product>> {
        val results = mutableMapOf<Long, ApiResponse<Product>>()
        
        productIds.forEach { productId ->
            try {
                val response = apiService.getProductById(productId)
                results[productId] = response
            } catch (e: Exception) {
                results[productId] = ApiResponse(
                    success = false,
                    message = "获取商品详情失败: ${e.message}"
                )
            }
        }
        
        return results
    }
    
    /**
     * 获取商品列表并预加载详情（性能优化）
     */
    suspend fun getProductsWithDetails(
        page: Int = 0,
        size: Int = 20,
        category: String? = null,
        keyword: String? = null
    ): ApiResponse<List<Product>> {
        // 首先获取商品列表
        val productsResponse = apiService.getProducts(page, size, category, keyword)
        
        if (!productsResponse.success || productsResponse.data == null) {
            return productsResponse
        }
        
        // 提取商品ID列表
        val productIds = productsResponse.data.map { it.id }
        
        // 批量获取商品详情
        val detailsMap = getProductsBatchWithFallback(productIds)
        
        // 合并详情信息
        val enrichedProducts = productsResponse.data.map { product ->
            val detailResponse = detailsMap[product.id]
            if (detailResponse?.success == true && detailResponse.data != null) {
                // 使用详细的商品信息
                detailResponse.data
            } else {
                // 使用基础的商品信息
                product
            }
        }
        
        return ApiResponse(
            success = true,
            data = enrichedProducts,
            message = productsResponse.message
        )
    }
    
    /**
     * 清除商品缓存
     */
    suspend fun clearProductCache(productId: Long? = null) {
        batchRequestService.clearProductCache(productId)
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): com.campustrade.android.service.CacheStats {
        return batchRequestService.getCacheStats()
    }
}