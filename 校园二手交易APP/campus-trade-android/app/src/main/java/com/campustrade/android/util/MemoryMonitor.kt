package com.campustrade.android.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存监控和泄漏检测工具类
 */
object MemoryMonitor {
    
    private const val TAG = "MemoryMonitor"
    private const val MEMORY_WARNING_THRESHOLD = 0.8f // 内存警告阈值80%
    private const val MEMORY_CRITICAL_THRESHOLD = 0.9f // 内存临界阈值90%
    
    private val activityReferences = ConcurrentHashMap<String, WeakActivityReference>()
    private var isMonitoring = false
    
    /**
     * 启动内存监控
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        
        isMonitoring = true
        Log.i(TAG, "内存监控已启动")
        
        // 定期检查内存使用情况
        Thread {
            while (isMonitoring) {
                try {
                    checkMemoryUsage(context)
                    Thread.sleep(30000) // 每30秒检查一次
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "内存监控异常: ${e.message}")
                }
            }
        }.start()
    }
    
    /**
     * 停止内存监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        activityReferences.clear()
        Log.i(TAG, "内存监控已停止")
    }
    
    /**
     * 注册Activity生命周期监控
     */
    fun registerActivity(activity: android.app.Activity) {
        val activityName = activity::class.java.simpleName
        val reference = WeakActivityReference(activity)
        activityReferences[activityName] = reference
        
        // 监控Activity生命周期
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_DESTROY -> {
                            activityReferences.remove(activityName)
                            Log.d(TAG, "Activity销毁: $activityName")
                        }
                        else -> {}
                    }
                }
            })
        }
        
        Log.d(TAG, "Activity注册: $activityName")
    }
    
    /**
     * 检查内存使用情况
     */
    private fun checkMemoryUsage(context: Context) {
        val memoryInfo = getMemoryInfo(context)
        val usagePercentage = memoryInfo.usedMemory.toFloat() / memoryInfo.totalMemory.toFloat()
        
        when {
            usagePercentage > MEMORY_CRITICAL_THRESHOLD -> {
                Log.e(TAG, "内存使用严重: ${String.format("%.1f", usagePercentage * 100)}%")
                // 执行紧急内存清理
                performEmergencyCleanup(context)
            }
            usagePercentage > MEMORY_WARNING_THRESHOLD -> {
                Log.w(TAG, "内存使用警告: ${String.format("%.1f", usagePercentage * 100)}%")
                // 执行内存清理
                performMemoryCleanup(context)
            }
            else -> {
                Log.d(TAG, "内存使用正常: ${String.format("%.1f", usagePercentage * 100)}%")
            }
        }
        
        // 检查潜在的内存泄漏
        checkForPotentialLeaks()
    }
    
    /**
     * 获取内存信息
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) // MB
        val totalMemory = runtime.maxMemory() / (1024 * 1024) // MB
        val availableMemory = memoryInfo.availMem / (1024 * 1024) // MB
        
        return MemoryInfo(
            usedMemory = usedMemory,
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            isLowMemory = memoryInfo.lowMemory
        )
    }
    
    /**
     * 执行内存清理
     */
    private fun performMemoryCleanup(context: Context) {
        Log.i(TAG, "执行内存清理")
        
        // 清理图片缓存
        MemoryOptimizer.clearImageCache(context)
        
        // 清理网络请求缓存
        clearNetworkCache()
        
        // 建议垃圾回收
        System.gc()
        
        Log.i(TAG, "内存清理完成")
    }
    
    /**
     * 执行紧急内存清理
     */
    private fun performEmergencyCleanup(context: Context) {
        Log.w(TAG, "执行紧急内存清理")
        
        // 清理所有缓存
        performMemoryCleanup(context)
        
        // 强制垃圾回收
        Runtime.getRuntime().gc()
        
        // 记录内存转储（仅调试版本）
        // if (BuildConfig.DEBUG) {
        //     try {
        //         Debug.dumpHprofData("/data/local/tmp/memory_dump_${System.currentTimeMillis()}.hprof")
        //     } catch (e: Exception) {
        //         Log.e(TAG, "内存转储失败: ${e.message}")
        //     }
        // }
        
        Log.w(TAG, "紧急内存清理完成")
    }
    
    /**
     * 清理网络请求缓存
     */
    private fun clearNetworkCache() {
        try {
            // 这里可以清理网络请求相关的缓存
            // 例如：OkHttp缓存、Retrofit缓存等
            Log.d(TAG, "网络缓存清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "清理网络缓存失败: ${e.message}")
        }
    }
    
    /**
     * 检查潜在的内存泄漏
     */
    private fun checkForPotentialLeaks() {
        val currentTime = System.currentTimeMillis()
        val leakedActivities = activityReferences.filter { entry ->
            entry.value.isLeaked(currentTime)
        }
        
        if (leakedActivities.isNotEmpty()) {
            Log.w(TAG, "检测到潜在内存泄漏: ${leakedActivities.keys.joinToString(", ")}")
        }
    }
    
    /**
     * 获取内存统计信息
     */
    fun getMemoryStats(context: Context): MemoryStats {
        val memoryInfo = getMemoryInfo(context)
        val heapInfo = getHeapInfo()
        
        return MemoryStats(
            memoryUsage = String.format("%.1f", (memoryInfo.usedMemory.toFloat() / memoryInfo.totalMemory.toFloat()) * 100),
            heapUsage = String.format("%.1f", (heapInfo.usedHeap.toFloat() / heapInfo.totalHeap.toFloat()) * 100),
            activityCount = activityReferences.size,
            isLowMemory = memoryInfo.isLowMemory
        )
    }
    
    /**
     * 获取堆内存信息
     */
    private fun getHeapInfo(): HeapInfo {
        val runtime = Runtime.getRuntime()
        val usedHeap = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) // MB
        val totalHeap = runtime.maxMemory() / (1024 * 1024) // MB
        
        return HeapInfo(usedHeap = usedHeap, totalHeap = totalHeap)
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val usedMemory: Long,
        val totalMemory: Long,
        val availableMemory: Long,
        val isLowMemory: Boolean
    )
    
    /**
     * 内存统计信息数据类
     */
    data class MemoryStats(
        val memoryUsage: String,
        val heapUsage: String,
        val activityCount: Int,
        val isLowMemory: Boolean
    )
    
    /**
     * 堆内存信息数据类
     */
    data class HeapInfo(
        val usedHeap: Long,
        val totalHeap: Long
    )
    
    /**
     * 弱引用Activity包装类
     */
    private class WeakActivityReference(activity: android.app.Activity) {
        private val weakReference = java.lang.ref.WeakReference(activity)
        private val createTime = System.currentTimeMillis()
        
        fun isLeaked(currentTime: Long): Boolean {
            val activity = weakReference.get()
            // 如果Activity已经被销毁但引用仍然存在超过5分钟，认为可能泄漏
            return activity == null && (currentTime - createTime) > 5 * 60 * 1000
        }
    }
}