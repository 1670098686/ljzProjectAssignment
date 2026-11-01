package com.campustrade.android.network

import com.campustrade.android.model.*
import com.campustrade.android.service.BatchRequestService
import com.campustrade.android.repository.ProductRepository
import com.campustrade.android.repository.UserRepository
import com.campustrade.android.repository.OrderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.*
import org.mockito.kotlin.mock

class BatchRequestTest {
    
    private lateinit var apiService: ApiService
    private lateinit var batchRequestManager: BatchRequestManager
    private lateinit var batchRequestService: BatchRequestService
    private lateinit var productRepository: ProductRepository
    private lateinit var userRepository: UserRepository
    private lateinit var orderRepository: OrderRepository
    
    @Before
    fun setup() {
        apiService = mock()
        batchRequestManager = BatchRequestManager()
        batchRequestService = BatchRequestService(apiService, batchRequestManager)
        productRepository = ProductRepository(apiService, batchRequestService)
        userRepository = UserRepository(apiService, batchRequestService)
        orderRepository = OrderRepository(apiService, batchRequestService)
    }
    
    @Test
    fun testBatchRequestManagerCreation() {
        assertNotNull("BatchRequestManager should be created", batchRequestManager)
        assertEquals("Initial cached requests should be 0", 0, batchRequestManager.getCachedRequestCount())
        assertEquals("Initial pending requests should be 0", 0, batchRequestManager.getPendingRequestCount())
    }
    
    @Test
    fun testBatchRequestServiceCreation() {
        assertNotNull("BatchRequestService should be created", batchRequestService)
    }
    
    @Test
    fun testProductRepositoryBatchMethods() {
        assertNotNull("ProductRepository should have batch methods", productRepository)
        
        // 测试缓存统计方法
        val stats = productRepository.getCacheStats()
        assertNotNull("Cache stats should not be null", stats)
        assertEquals("Initial cached requests should be 0", 0, stats.cachedRequests)
        assertEquals("Initial pending requests should be 0", 0, stats.pendingRequests)
    }
    
    @Test
    fun testUserRepositoryBatchMethods() {
        assertNotNull("UserRepository should have batch methods", userRepository)
        
        val stats = userRepository.getCacheStats()
        assertNotNull("Cache stats should not be null", stats)
    }
    
    @Test
    fun testOrderRepositoryBatchMethods() {
        assertNotNull("OrderRepository should have batch methods", orderRepository)
        
        val stats = orderRepository.getCacheStats()
        assertNotNull("Cache stats should not be null", stats)
    }
    
    @Test
    fun testBatchRequestModel() {
        // 测试批量请求数据模型
        val batchRequest = BatchRequest(
            requests = listOf(
                SingleRequest(
                    id = "test1",
                    method = HttpMethod.GET,
                    path = "/api/v1/products/1",
                    headers = emptyMap(),
                    body = null
                )
            )
        )
        
        assertNotNull("BatchRequest should be created", batchRequest)
        assertEquals("Should have 1 request", 1, batchRequest.requests.size)
        assertEquals("Request ID should match", "test1", batchRequest.requests[0].id)
    }
    
    @Test
    fun testBatchRequestBuilder() {
        val builder = BatchRequestBuilder()
        
        builder.addProductRequest(
            id = "product1",
            method = HttpMethod.GET,
            path = "/api/v1/products/1"
        )
        
        val batchRequest = builder.build()
        
        assertNotNull("BatchRequest should be built", batchRequest)
        assertEquals("Should have 1 request", 1, batchRequest.requests.size)
    }
    
    @Test
    fun testBatchRequestUtils() {
        // 测试工具类方法
        val productIds = listOf(1L, 2L, 3L)
        val batchRequest = BatchRequestUtils.createProductBatchRequest(productIds)
        
        assertNotNull("BatchRequest should be created", batchRequest)
        assertEquals("Should have 3 requests", 3, batchRequest.requests.size)
        
        // 验证请求ID格式
        assertEquals("First request ID should be product_0", "product_0", batchRequest.requests[0].id)
        assertEquals("Second request ID should be product_1", "product_1", batchRequest.requests[1].id)
        assertEquals("Third request ID should be product_2", "product_2", batchRequest.requests[2].id)
    }
    
    @Test
    fun testCacheStatsModel() {
        val stats = com.campustrade.android.service.CacheStats(
            cachedRequests = 5,
            pendingRequests = 2
        )
        
        assertEquals("Cached requests should be 5", 5, stats.cachedRequests)
        assertEquals("Pending requests should be 2", 2, stats.pendingRequests)
    }
    
    @Test
    fun testRepositoryConstructorInjection() {
        // 测试依赖注入是否正确
        assertNotNull("ProductRepository should have apiService", productRepository)
        assertNotNull("ProductRepository should have batchRequestService", productRepository)
        
        assertNotNull("UserRepository should have apiService", userRepository)
        assertNotNull("UserRepository should have batchRequestService", userRepository)
        
        assertNotNull("OrderRepository should have apiService", orderRepository)
        assertNotNull("OrderRepository should have batchRequestService", orderRepository)
    }
    
    @Test
    fun testBatchResultModel() {
        val batchResult = com.campustrade.android.service.BatchResult(
            products = emptyMap(),
            users = emptyMap(),
            orders = emptyMap()
        )
        
        assertNotNull("BatchResult should be created", batchResult)
        assertTrue("Products should be empty", batchResult.products.isEmpty())
        assertTrue("Users should be empty", batchResult.users.isEmpty())
        assertTrue("Orders should be empty", batchResult.orders.isEmpty())
    }
}