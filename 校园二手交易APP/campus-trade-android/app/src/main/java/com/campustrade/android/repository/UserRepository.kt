package com.campustrade.android.repository

import com.campustrade.android.model.ApiResponse
import com.campustrade.android.model.User
import com.campustrade.android.network.AdminLoginRequest
import com.campustrade.android.network.ApiService
import com.campustrade.android.network.LoginRequest
import com.campustrade.android.network.RegisterRequest
import com.campustrade.android.service.BatchRequestService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val batchRequestService: BatchRequestService
) {
    
    suspend fun login(username: String, password: String): ApiResponse<User> {
        return apiService.login(LoginRequest(username, password))
    }
    
    suspend fun adminLogin(username: String, password: String): ApiResponse<User> {
        return apiService.adminLogin(AdminLoginRequest(username, password))
    }
    
    suspend fun register(
        username: String,
        password: String,
        email: String,
        phone: String? = null,
        realName: String? = null,
        school: String? = null
    ): ApiResponse<User> {
        return apiService.register(
            RegisterRequest(username, password, email, phone, realName, school)
        )
    }
    
    suspend fun getUserById(id: Long): ApiResponse<User> {
        return apiService.getUserById(id)
    }
    
    suspend fun updateUser(id: Long, user: User): ApiResponse<User> {
        return apiService.updateUser(id, user)
    }
    
    // ========== 批量请求优化方法 ==========
    
    /**
     * 批量获取多个用户信息（优化版本）
     * 使用批量请求合并，减少网络请求次数
     */
    suspend fun getUsersBatch(userIds: List<Long>): Map<Long, ApiResponse<User>> {
        return batchRequestService.getUsersBatch(userIds)
    }
    
    /**
     * 批量获取用户信息，支持回退机制
     */
    suspend fun getUsersBatchWithFallback(userIds: List<Long>): Map<Long, ApiResponse<User>> {
        return try {
            batchRequestService.getUsersBatch(userIds)
        } catch (e: Exception) {
            getUsersSequentially(userIds)
        }
    }
    
    /**
     * 顺序获取多个用户信息（回退方法）
     */
    private suspend fun getUsersSequentially(userIds: List<Long>): Map<Long, ApiResponse<User>> {
        val results = mutableMapOf<Long, ApiResponse<User>>()
        
        userIds.forEach { userId ->
            try {
                val response = apiService.getUserById(userId)
                results[userId] = response
            } catch (e: Exception) {
                results[userId] = ApiResponse(
                    success = false,
                    message = "获取用户信息失败: ${e.message}"
                )
            }
        }
        
        return results
    }
    
    /**
     * 清除用户缓存
     */
    suspend fun clearUserCache(userId: Long? = null) {
        batchRequestService.clearUserCache(userId)
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): com.campustrade.android.service.CacheStats {
        return batchRequestService.getCacheStats()
    }
}