package com.campustrade.android.ui.base

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.campustrade.android.util.MemoryConfig
import com.campustrade.android.util.MemoryMonitor
import com.campustrade.android.util.MemoryTestUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 基础Activity类，提供统一的内存监控和生命周期管理
 */
abstract class BaseActivity : AppCompatActivity() {
    
    protected val TAG: String = this::class.java.simpleName
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 注册Activity到内存监控
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            MemoryMonitor.registerActivity(this)
        }
        
        // 检查内存压力并自动清理
        checkMemoryPressure()
        
        // 如果启用测试模式，执行内存测试
        if (MemoryConfig.isTestModeEnabled()) {
            performMemoryTests()
        }
        
        Log.d(TAG, "Activity创建: ${this::class.java.simpleName}")
    }
    
    override fun onResume() {
        super.onResume()
        
        // 更新Activity活跃状态
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            MemoryMonitor.updateActivityState(this, true)
        }
        
        // 检查内存使用情况
        checkMemoryUsage()
    }
    
    override fun onPause() {
        super.onPause()
        
        // 更新Activity非活跃状态
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            MemoryMonitor.updateActivityState(this, false)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 从内存监控中注销Activity
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            MemoryMonitor.unregisterActivity(this)
        }
        
        // 清理资源
        cleanupResources()
        
        Log.d(TAG, "Activity销毁: ${this::class.java.simpleName}")
    }
    
    /**
     * 检查内存压力并自动清理
     */
    private fun checkMemoryPressure() {
        if (MemoryConfig.isMemoryMonitoringEnabled() && MemoryMonitor.isMemoryPressure(this)) {
            Log.w(TAG, "检测到内存压力，执行自动清理")
            MemoryMonitor.cleanupMemory(this)
        }
    }
    
    /**
     * 检查内存使用情况
     */
    private fun checkMemoryUsage() {
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            val memoryInfo = MemoryMonitor.getMemoryInfo(this)
            val usagePercentage = memoryInfo.usedMemory.toFloat() / memoryInfo.totalMemory.toFloat()
            
            if (usagePercentage > 0.7f) {
                Log.w(TAG, "内存使用率超过70%，建议优化")
                
                // 如果使用率过高，自动清理
                if (usagePercentage > 0.8f) {
                    triggerMemoryCleanup()
                }
            }
        }
    }
    
    /**
     * 执行内存测试
     */
    private fun performMemoryTests() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "开始内存优化测试")
                
                // 执行内存压力测试
                val stressTestResult = MemoryTestUtil.performMemoryStressTest(this@BaseActivity)
                Log.d(TAG, "内存压力测试完成: ${stressTestResult.success}")
                
                // 执行图片加载性能测试
                val imageTestResult = MemoryTestUtil.testImageLoadingPerformance(this@BaseActivity)
                Log.d(TAG, "图片加载性能测试完成")
                
                // 生成内存优化报告
                val report = MemoryTestUtil.generateMemoryOptimizationReport(this@BaseActivity)
                Log.d(TAG, "内存优化报告生成完成")
                
                // 打印测试结果摘要
                printTestSummary(stressTestResult, imageTestResult)
                
            } catch (e: Exception) {
                Log.e(TAG, "内存测试失败: ${e.message}")
            }
        }
    }
    
    /**
     * 打印测试结果摘要
     */
    private fun printTestSummary(
        stressResult: MemoryTestUtil.MemoryTestResult,
        imageResult: MemoryTestUtil.ImageLoadingTestResult
    ) {
        Log.d(TAG, """
            ===== 内存优化测试摘要 =====
            内存压力测试:
            - 状态: ${if (stressResult.success) "成功" else "失败"}
            - 持续时间: ${stressResult.duration}ms
            - 内存增加: ${stressResult.memoryIncrease}MB
            
            图片加载测试:
            - 标准加载平均时间: ${imageResult.standardLoadingTimes.average()}ms
            - 优化加载平均时间: ${imageResult.optimizedLoadingTimes.average()}ms
            - 性能提升: ${(imageResult.standardLoadingTimes.average() - imageResult.optimizedLoadingTimes.average()) / imageResult.standardLoadingTimes.average() * 100}%
            ===========================
        """.trimIndent())
    }
    
    /**
     * 清理资源，子类可以重写此方法进行特定清理
     */
    protected open fun cleanupResources() {
        // 默认实现，子类可以重写
        Log.d(TAG, "清理Activity资源")
    }
    
    /**
     * 获取内存信息
     */
    protected fun getMemoryInfo(): MemoryMonitor.MemoryInfo {
        return MemoryMonitor.getMemoryInfo(this)
    }
    
    /**
     * 手动触发内存清理
     */
    protected fun triggerMemoryCleanup() {
        if (MemoryConfig.isMemoryMonitoringEnabled()) {
            MemoryMonitor.cleanupMemory(this)
        }
    }
    
    /**
     * 生成内存优化报告
     */
    protected fun generateMemoryReport(): String {
        return MemoryTestUtil.generateMemoryOptimizationReport(this)
    }
}