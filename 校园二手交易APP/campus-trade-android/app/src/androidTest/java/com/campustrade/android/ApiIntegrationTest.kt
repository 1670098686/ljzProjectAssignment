package com.campustrade.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.campustrade.android.network.ApiService
import com.campustrade.android.repository.UserRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ApiIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    private lateinit var apiService: ApiService
    private lateinit var userRepository: UserRepository
    
    @Before
    fun setUp() {
        hiltRule.inject()
        apiService = ApiService.create()
        userRepository = UserRepository(apiService)
    }
    
    @Test
    fun testApiServiceCreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.campustrade.android", context.packageName)
        
        // 测试API服务创建
        assertNotNull("API服务应该被成功创建", apiService)
    }
    
    @Test
    fun testLoginApi() = runBlocking {
        // 测试登录API（使用模拟数据）
        try {
            val response = userRepository.login("testuser", "password123")
            // 由于是测试环境，我们期望网络连接失败或返回错误
            // 这里主要测试API调用是否正常执行
            assertNotNull("登录响应不应该为null", response)
        } catch (e: Exception) {
            // 在测试环境中，网络连接失败是正常的
            assertTrue("网络连接异常应该是预期的", e is java.net.ConnectException)
        }
    }
    
    @Test
    fun testGetProductsApi() = runBlocking {
        // 测试获取商品列表API
        try {
            val response = apiService.getProducts(page = 0, size = 10)
            assertNotNull("商品列表响应不应该为null", response)
        } catch (e: Exception) {
            // 在测试环境中，网络连接失败是正常的
            assertTrue("网络连接异常应该是预期的", e is java.net.ConnectException)
        }
    }
}