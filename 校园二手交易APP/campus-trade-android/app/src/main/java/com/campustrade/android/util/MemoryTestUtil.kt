package com.campustrade.android.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 内存优化测试工具类
 * 用于验证内存优化效果和性能测试
 */
object MemoryTestUtil {
    
    private const val TAG = "MemoryTestUtil"
    
    /**
     * 执行内存压力测试
     */
    suspend fun performMemoryStressTest(context: Context): MemoryTestResult {
        return withContext(Dispatchers.IO) {
            val startMemory = MemoryMonitor.getMemoryInfo(context)
            val startTime = System.currentTimeMillis()
            
            try {
                // 模拟大量图片加载
                simulateImageLoading(context)
                
                // 模拟大量对象创建
                simulateObjectCreation()
                
                // 模拟内存泄漏场景
                simulateMemoryLeakScenario()
                
                val endMemory = MemoryMonitor.getMemoryInfo(context)
                val endTime = System.currentTimeMillis()
                
                MemoryTestResult(
                    success = true,
                    duration = endTime - startTime,
                    memoryUsageBefore = startMemory.usedMemory,
                    memoryUsageAfter = endMemory.usedMemory,
                    memoryIncrease = endMemory.usedMemory - startMemory.usedMemory,
                    isLowMemory = endMemory.isLowMemory
                )
            } catch (e: Exception) {
                Log.e(TAG, "内存压力测试失败: ${e.message}")
                MemoryTestResult(
                    success = false,
                    duration = System.currentTimeMillis() - startTime,
                    memoryUsageBefore = startMemory.usedMemory,
                    memoryUsageAfter = startMemory.usedMemory,
                    memoryIncrease = 0,
                    isLowMemory = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * 模拟图片加载操作
     */
    private fun simulateImageLoading(context: Context) {
        // 模拟加载多张图片
        repeat(50) { i ->
            // 这里可以模拟图片加载操作
            // 实际项目中应该使用真实的图片加载测试
            Thread.sleep(10) // 模拟加载时间
        }
    }
    
    /**
     * 模拟对象创建
     */
    private fun simulateObjectCreation() {
        // 创建大量临时对象
        val objects = mutableListOf<Any>()
        repeat(10000) {
            objects.add(TestObject("Object_$it", it))
        }
        
        // 强制垃圾回收
        System.gc()
        Thread.sleep(100)
    }
    
    /**
     * 模拟内存泄漏场景
     */
    private fun simulateMemoryLeakScenario() {
        // 创建可能泄漏的对象引用
        val leakReferences = mutableListOf<java.lang.ref.WeakReference<TestObject>>()
        repeat(100) {
            leakReferences.add(java.lang.ref.WeakReference(TestObject("Leak_$it", it)))
        }
        
        // 模拟Activity泄漏
        simulateActivityLeak()
    }
    
    /**
     * 模拟Activity泄漏
     */
    private fun simulateActivityLeak() {
        // 这里可以模拟Activity泄漏的测试场景
        // 实际项目中应该使用真实的Activity测试
    }
    
    /**
     * 测试图片加载性能
     */
    suspend fun testImageLoadingPerformance(context: Context): ImageLoadingTestResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val startMemory = MemoryMonitor.getMemoryInfo(context)
            
            // 测试标准图片加载
            val standardTimes = mutableListOf<Long>()
            repeat(10) {
                val time = measureImageLoadingTime(context, false)
                standardTimes.add(time)
            }
            
            // 测试优化图片加载
            val optimizedTimes = mutableListOf<Long>()
            repeat(10) {
                val time = measureImageLoadingTime(context, true)
                optimizedTimes.add(time)
            }
            
            val endMemory = MemoryMonitor.getMemoryInfo(context)
            
            ImageLoadingTestResult(
                standardLoadingTimes = standardTimes,
                optimizedLoadingTimes = optimizedTimes,
                memoryUsageBefore = startMemory.usedMemory,
                memoryUsageAfter = endMemory.usedMemory,
                duration = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * 测量图片加载时间
     */
    private fun measureImageLoadingTime(context: Context, optimized: Boolean): Long {
        val startTime = System.nanoTime()
        
        // 模拟图片加载操作
        if (optimized) {
            // 使用优化版本的图片加载
            // 这里可以添加实际的优化图片加载测试
        } else {
            // 使用标准图片加载
            // 这里可以添加实际的标准图片加载测试
        }
        
        return (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
    }
    
    /**
     * 生成内存优化报告
     */
    fun generateMemoryOptimizationReport(context: Context): String {
        val memoryInfo = MemoryMonitor.getMemoryInfo(context)
        val config = MemoryConfig.getConfigSummary()
        
        return """
            ===== 内存优化报告 =====
            
            当前内存状态：
            - 已使用内存：${memoryInfo.usedMemory}MB
            - 总内存：${memoryInfo.totalMemory}MB
            - 可用内存：${memoryInfo.availableMemory}MB
            - 低内存状态：${if (memoryInfo.isLowMemory) "是" else "否"}
            
            内存优化配置：
            $config
            
            内存监控统计：
            - Activity数量：${MemoryMonitor.getMemoryStats(context).activityCount}
            - 内存使用率：${MemoryMonitor.getMemoryStats(context).memoryUsage}%
            - 堆内存使用率：${MemoryMonitor.getMemoryStats(context).heapUsage}%
            
            优化建议：
            ${generateOptimizationSuggestions(memoryInfo)}
            
            ======================
        """.trimIndent()
    }
    
    /**
     * 生成优化建议
     */
    private fun generateOptimizationSuggestions(memoryInfo: MemoryMonitor.MemoryInfo): String {
        val suggestions = mutableListOf<String>()
        
        val usagePercentage = memoryInfo.usedMemory.toFloat() / memoryInfo.totalMemory.toFloat()
        
        when {
            usagePercentage > 0.8f -> {
                suggestions.add("⚠️ 内存使用率较高，建议减少缓存大小")
                suggestions.add("⚠️ 考虑启用更激进的自动清理策略")
            }
            usagePercentage > 0.6f -> {
                suggestions.add("📊 内存使用率正常，保持当前配置")
            }
            else -> {
                suggestions.add("✅ 内存使用率较低，可以考虑增加缓存大小提升性能")
            }
        }
        
        if (memoryInfo.isLowMemory) {
            suggestions.add("🚨 设备处于低内存状态，建议启用低内存模式")
        }
        
        return suggestions.joinToString("\n")
    }
    
    /**
     * 测试对象类
     */
    private data class TestObject(val name: String, val id: Int)
    
    /**
     * 内存测试结果数据类
     */
    data class MemoryTestResult(
        val success: Boolean,
        val duration: Long,
        val memoryUsageBefore: Long,
        val memoryUsageAfter: Long,
        val memoryIncrease: Long,
        val isLowMemory: Boolean,
        val error: String? = null
    )
    
    /**
     * 图片加载测试结果数据类
     */
    data class ImageLoadingTestResult(
        val standardLoadingTimes: List<Long>,
        val optimizedLoadingTimes: List<Long>,
        val memoryUsageBefore: Long,
        val memoryUsageAfter: Long,
        val duration: Long
    )
}