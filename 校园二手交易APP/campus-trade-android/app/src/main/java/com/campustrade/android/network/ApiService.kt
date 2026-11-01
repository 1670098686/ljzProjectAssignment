package com.campustrade.android.network

import com.campustrade.android.model.User
import com.campustrade.android.model.Product
import com.campustrade.android.model.ApiResponse
import com.campustrade.android.model.BatchRequest
import com.campustrade.android.model.BatchResponse
import com.campustrade.android.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    
    // 批量请求接口
    @POST("/api/v1/batch")
    suspend fun executeBatch(@Body batchRequest: BatchRequest): ApiResponse<BatchResponse>
    
    // 用户相关接口
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<User>
    
    @POST("/api/v1/admin/login")
    suspend fun adminLogin(@Body request: AdminLoginRequest): ApiResponse<User>
    
    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<User>
    
    @GET("/api/v1/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): ApiResponse<User>
    
    @PUT("/api/v1/users/{id}")
    suspend fun updateUser(@Path("id") id: Long, @Body user: User): ApiResponse<User>
    
    // 商品相关接口
    @GET("/api/v1/products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null
    ): ApiResponse<List<Product>>
    
    @GET("/api/v1/products/{id}")
    suspend fun getProductById(@Path("id") id: Long): ApiResponse<Product>
    
    @POST("/api/v1/products")
    suspend fun createProduct(@Body product: Product): ApiResponse<Product>
    
    @PUT("/api/v1/products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body product: Product): ApiResponse<Product>
    
    @DELETE("/api/v1/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): ApiResponse<Unit>

    @GET("/api/v1/products/my")
    suspend fun getMyProducts(
        @Query("userId") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<List<Product>>

    @GET("/api/v1/products/favorites")
    suspend fun getFavoriteProducts(
        @Query("userId") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<List<Product>>
    
    companion object {
        private const val BASE_URL = Constants.BASE_URL
        
        fun create(): ApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
            
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

data class LoginRequest(val username: String, val password: String)
data class AdminLoginRequest(val username: String, val password: String)
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val phone: String? = null,
    val realName: String? = null,
    val school: String? = null
)