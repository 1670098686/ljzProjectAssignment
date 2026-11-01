package com.campustrade.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.campustrade.android.manager.PushNotificationManager
import com.campustrade.android.util.MemoryConfig
import com.campustrade.android.util.MemoryMonitor
import com.campustrade.android.util.StartupOptimizer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CampusTradeApplication : Application() {
    
    @Inject
    lateinit var pushNotificationManager: PushNotificationManager
    
    private val appScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // 记录应用启动时间
        StartupOptimizer.monitor.onApplicationCreate()
        
        // 初始化内存配置
        MemoryConfig.initialize(this)
        
        // 根据设备内存自动调整配置
        MemoryConfig.autoAdjustConfig(this)
        
        // 启动内存监控
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            MemoryMonitor.startMonitoring(this)
        }
        
        // 主线程初始化：关键配置
        initCriticalConfig()
        
        // 后台线程初始化：非关键配置
        initBackgroundConfig()
    }
    
    /**
     * 关键配置初始化（主线程）
     * 只初始化必须立即使用的组件
     */
    private fun initCriticalConfig() {
        // 初始化必要的配置，如Hilt依赖注入等
        // 这些操作已经在@HiltAndroidApp注解中自动处理
    }
    
    /**
     * 后台配置初始化（IO线程）
     * 延迟初始化非关键组件
     */
    private fun initBackgroundConfig() {
        appScope.launch {
            // 延迟初始化推送通知管理器
            pushNotificationManager.initialize()
            
            // 预加载常用数据
            preloadCommonData()
        }
    }
    
    /**
     * 预加载常用数据
     */
    private fun preloadCommonData() {
        // 预加载常用配置数据，如分类列表、学校列表等
        // 这些数据可以在应用启动后立即使用，提升用户体验
    }
}