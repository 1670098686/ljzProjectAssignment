package com.campustrade.android.util

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 应用启动性能优化工具类
 * 提供冷启动、热启动优化功能
 */
object StartupOptimizer {
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * 冷启动优化：延迟非关键初始化
     */
    fun optimizeColdStart(activity: Activity, criticalTask: () -> Unit, nonCriticalTask: () -> Unit) {
        // 立即执行关键任务
        criticalTask()
        
        // 延迟执行非关键任务（等待UI渲染完成）
        activity.window.decorView.post {
            nonCriticalTask()
        }
    }
    
    /**
     * 热启动优化：预加载数据
     */
    fun preloadDataForWarmStart(preloadTasks: List<() -> Unit>) {
        ioScope.launch {
            preloadTasks.forEach { task ->
                try {
                    task()
                } catch (e: Exception) {
                    // 预加载失败不影响应用启动
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * 测量启动时间
     */
    fun measureStartupTime(activity: Activity, onComplete: (Long) -> Unit) {
        val startTime = System.currentTimeMillis()
        
        // 等待UI完全渲染完成
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val endTime = System.currentTimeMillis()
                val startupTime = endTime - startTime
                onComplete(startupTime)
            }
        })
    }
    
    /**
     * 延迟初始化管理器
     */
    class LazyInitializer<T>(
        private val initializer: () -> T
    ) {
        private var instance: T? = null
        
        fun get(): T {
            return instance ?: synchronized(this) {
                instance ?: initializer().also { instance = it }
            }
        }
        
        fun isInitialized(): Boolean = instance != null
        
        fun clear() {
            instance = null
        }
    }
    
    /**
     * 启动性能监控
     */
    class StartupMonitor {
        private var appCreateTime: Long = 0
        private var firstActivityCreateTime: Long = 0
        private var firstActivityVisibleTime: Long = 0
        
        fun onApplicationCreate() {
            appCreateTime = System.currentTimeMillis()
        }
        
        fun onFirstActivityCreate() {
            firstActivityCreateTime = System.currentTimeMillis()
        }
        
        fun onFirstActivityVisible() {
            firstActivityVisibleTime = System.currentTimeMillis()
            
            val totalStartupTime = firstActivityVisibleTime - appCreateTime
            val activityStartupTime = firstActivityVisibleTime - firstActivityCreateTime
            
            // 记录启动性能数据
            println("启动性能统计：")
            println("总启动时间：${totalStartupTime}ms")
            println("Activity启动时间：${activityStartupTime}ms")
        }
    }
    
    // 启动性能监控实例
    val monitor = StartupMonitor()
    
    // 预加载任务列表
    val preloadTasks = mutableListOf<() -> Unit>()
    
    /**
     * 添加预加载任务
     */
    fun addPreloadTask(task: () -> Unit) {
        preloadTasks.add(task)
    }
    
    /**
     * 执行所有预加载任务
     */
    fun executePreloadTasks() {
        preloadDataForWarmStart(preloadTasks)
    }
}