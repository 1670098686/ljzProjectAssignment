package com.campustrade.android.model

/**
 * 批量请求数据模型
 */
data class BatchRequest(
    val requests: List<SingleRequest>
)

/**
 * 单个请求定义
 */
data class SingleRequest(
    val id: String,           // 请求唯一标识
    val type: RequestType,    // 请求类型
    val path: String,         // 请求路径
    val method: HttpMethod,   // HTTP方法
    val params: Map<String, Any> = emptyMap(), // 请求参数
    val body: Any? = null     // 请求体
)

/**
 * 批量响应数据模型
 */
data class BatchResponse(
    val responses: List<SingleResponse>
)

/**
 * 单个响应定义
 */
data class SingleResponse(
    val id: String,           // 对应请求的ID
    val success: Boolean,     // 是否成功
    val data: Any?,           // 响应数据
    val message: String? = null, // 响应消息
    val errorCode: String? = null // 错误码
)

/**
 * 请求类型枚举
 */
enum class RequestType {
    USER,           // 用户相关
    PRODUCT,        // 商品相关
    ORDER,          // 订单相关
    MESSAGE,        // 消息相关
    PAYMENT,        // 支付相关
    PROFILE         // 个人中心相关
}

/**
 * HTTP方法枚举
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE
}

/**
 * 批量请求构建器
 */
class BatchRequestBuilder {
    private val requests = mutableListOf<SingleRequest>()
    
    fun addUserRequest(id: String, method: HttpMethod, path: String, params: Map<String, Any> = emptyMap(), body: Any? = null): BatchRequestBuilder {
        requests.add(SingleRequest(id, RequestType.USER, path, method, params, body))
        return this
    }
    
    fun addProductRequest(id: String, method: HttpMethod, path: String, params: Map<String, Any> = emptyMap(), body: Any? = null): BatchRequestBuilder {
        requests.add(SingleRequest(id, RequestType.PRODUCT, path, method, params, body))
        return this
    }
    
    fun addOrderRequest(id: String, method: HttpMethod, path: String, params: Map<String, Any> = emptyMap(), body: Any? = null): BatchRequestBuilder {
        requests.add(SingleRequest(id, RequestType.ORDER, path, method, params, body))
        return this
    }
    
    fun addMessageRequest(id: String, method: HttpMethod, path: String, params: Map<String, Any> = emptyMap(), body: Any? = null): BatchRequestBuilder {
        requests.add(SingleRequest(id, RequestType.MESSAGE, path, method, params, body))
        return this
    }
    
    fun build(): BatchRequest {
        return BatchRequest(requests)
    }
}

/**
 * 批量请求工具类
 */
object BatchRequestUtils {
    
    /**
     * 创建获取多个商品的批量请求
     */
    fun createProductBatchRequest(productIds: List<Long>): BatchRequest {
        val builder = BatchRequestBuilder()
        productIds.forEachIndexed { index, productId ->
            builder.addProductRequest(
                id = "product_$index",
                method = HttpMethod.GET,
                path = "/api/v1/products/$productId"
            )
        }
        return builder.build()
    }
    
    /**
     * 创建获取多个用户的批量请求
     */
    fun createUserBatchRequest(userIds: List<Long>): BatchRequest {
        val builder = BatchRequestBuilder()
        userIds.forEachIndexed { index, userId ->
            builder.addUserRequest(
                id = "user_$index",
                method = HttpMethod.GET,
                path = "/api/v1/users/$userId"
            )
        }
        return builder.build()
    }
    
    /**
     * 创建获取多个订单的批量请求
     */
    fun createOrderBatchRequest(orderIds: List<Long>): BatchRequest {
        val builder = BatchRequestBuilder()
        orderIds.forEachIndexed { index, orderId ->
            builder.addOrderRequest(
                id = "order_$index",
                method = HttpMethod.GET,
                path = "/api/v1/orders/$orderId"
            )
        }
        return builder.build()
    }
    
    /**
     * 从批量响应中提取指定ID的响应
     */
    fun <T> extractResponse(batchResponse: BatchResponse, requestId: String): ApiResponse<T>? {
        val singleResponse = batchResponse.responses.find { it.id == requestId }
        return singleResponse?.let {
            ApiResponse(
                success = it.success,
                data = it.data as? T,
                message = it.message,
                errorCode = it.errorCode
            )
        }
    }
    
    /**
     * 从批量响应中提取所有成功的响应
     */
    fun <T> extractAllSuccessfulResponses(batchResponse: BatchResponse, type: Class<T>): List<T> {
        return batchResponse.responses
            .filter { it.success }
            .mapNotNull { it.data as? T }
    }
}