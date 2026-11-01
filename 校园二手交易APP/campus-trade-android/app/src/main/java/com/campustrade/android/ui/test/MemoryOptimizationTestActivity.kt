package com.campustrade.android.ui.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.R
import com.campustrade.android.ui.base.BaseActivity
import com.campustrade.android.util.MemoryConfig
import com.campustrade.android.util.MemoryMonitor
import com.campustrade.android.util.MemoryTestUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 内存优化测试Activity
 * 用于验证内存优化效果和性能测试
 */
class MemoryOptimizationTestActivity : BaseActivity() {
    
    private lateinit var btnStressTest: Button
    private lateinit var btnImageTest: Button
    private lateinit var btnGenerateReport: Button
    private lateinit var btnCleanMemory: Button
    private lateinit var tvMemoryInfo: TextView
    private lateinit var tvTestResults: TextView
    private lateinit var rvTestLogs: RecyclerView
    
    private val testLogs = mutableListOf<String>()
    private lateinit var testLogAdapter: TestLogAdapter
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_optimization_test)
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        updateMemoryInfo()
        
        addTestLog("内存优化测试Activity已启动")
        addTestLog("当前内存配置: ${MemoryConfig.getConfigSummary()}")
    }
    
    private fun initViews() {
        btnStressTest = findViewById(R.id.btn_stress_test)
        btnImageTest = findViewById(R.id.btn_image_test)
        btnGenerateReport = findViewById(R.id.btn_generate_report)
        btnCleanMemory = findViewById(R.id.btn_clean_memory)
        tvMemoryInfo = findViewById(R.id.tv_memory_info)
        tvTestResults = findViewById(R.id.tv_test_results)
        rvTestLogs = findViewById(R.id.rv_test_logs)
    }
    
    private fun setupRecyclerView() {
        testLogAdapter = TestLogAdapter(testLogs)
        rvTestLogs.apply {
            layoutManager = LinearLayoutManager(this@MemoryOptimizationTestActivity)
            adapter = testLogAdapter
        }
    }
    
    private fun setupClickListeners() {
        btnStressTest.setOnClickListener {
            performStressTest()
        }
        
        btnImageTest.setOnClickListener {
            performImageLoadingTest()
        }
        
        btnGenerateReport.setOnClickListener {
            generateOptimizationReport()
        }
        
        btnCleanMemory.setOnClickListener {
            triggerMemoryCleanup()
            addTestLog("手动触发内存清理")
            updateMemoryInfo()
        }
    }
    
    private fun performStressTest() {
        addTestLog("开始执行内存压力测试...")
        btnStressTest.isEnabled = false
        
        coroutineScope.launch {
            try {
                val result = MemoryTestUtil.performMemoryStressTest(this@MemoryOptimizationTestActivity)
                
                addTestLog("内存压力测试完成")
                addTestLog("测试状态: ${if (result.success) "成功" else "失败"}")
                addTestLog("持续时间: ${result.duration}ms")
                addTestLog("内存增加: ${result.memoryIncrease}MB")
                addTestLog("低内存状态: ${if (result.isLowMemory) "是" else "否"}")
                
                if (!result.success) {
                    addTestLog("错误信息: ${result.error}")
                }
                
                updateTestResults("内存压力测试结果:\n" +
                    "- 状态: ${if (result.success) "成功" else "失败"}\n" +
                    "- 持续时间: ${result.duration}ms\n" +
                    "- 内存增加: ${result.memoryIncrease}MB\n" +
                    "- 低内存: ${if (result.isLowMemory) "是" else "否"}")
                
            } catch (e: Exception) {
                addTestLog("内存压力测试失败: ${e.message}")
                updateTestResults("内存压力测试失败: ${e.message}")
            } finally {
                btnStressTest.isEnabled = true
                updateMemoryInfo()
            }
        }
    }
    
    private fun performImageLoadingTest() {
        addTestLog("开始执行图片加载性能测试...")
        btnImageTest.isEnabled = false
        
        coroutineScope.launch {
            try {
                val result = MemoryTestUtil.testImageLoadingPerformance(this@MemoryOptimizationTestActivity)
                
                addTestLog("图片加载性能测试完成")
                addTestLog("标准加载平均时间: ${result.standardLoadingTimes.average()}ms")
                addTestLog("优化加载平均时间: ${result.optimizedLoadingTimes.average()}ms")
                
                val performanceImprovement = if (result.standardLoadingTimes.average() > 0) {
                    (result.standardLoadingTimes.average() - result.optimizedLoadingTimes.average()) / 
                    result.standardLoadingTimes.average() * 100
                } else {
                    0.0
                }
                
                addTestLog("性能提升: ${String.format("%.2f", performanceImprovement)}%")
                
                updateTestResults("图片加载性能测试结果:\n" +
                    "- 标准加载: ${String.format("%.2f", result.standardLoadingTimes.average())}ms\n" +
                    "- 优化加载: ${String.format("%.2f", result.optimizedLoadingTimes.average())}ms\n" +
                    "- 性能提升: ${String.format("%.2f", performanceImprovement)}%")
                
            } catch (e: Exception) {
                addTestLog("图片加载性能测试失败: ${e.message}")
                updateTestResults("图片加载性能测试失败: ${e.message}")
            } finally {
                btnImageTest.isEnabled = true
                updateMemoryInfo()
            }
        }
    }
    
    private fun generateOptimizationReport() {
        addTestLog("开始生成内存优化报告...")
        
        coroutineScope.launch {
            try {
                val report = MemoryTestUtil.generateMemoryOptimizationReport(this@MemoryOptimizationTestActivity)
                
                addTestLog("内存优化报告生成完成")
                updateTestResults(report)
                
                // 保存报告到日志文件
                saveReportToLog(report)
                
            } catch (e: Exception) {
                addTestLog("生成内存优化报告失败: ${e.message}")
                updateTestResults("生成内存优化报告失败: ${e.message}")
            }
        }
    }
    
    private fun updateMemoryInfo() {
        val memoryInfo = getMemoryInfo()
        val usagePercentage = (memoryInfo.usedMemory.toFloat() / memoryInfo.totalMemory.toFloat() * 100)
        
        tvMemoryInfo.text = """
            当前内存状态:
            已使用: ${memoryInfo.usedMemory}MB / ${memoryInfo.totalMemory}MB
            使用率: ${String.format("%.1f", usagePercentage)}%
            可用内存: ${memoryInfo.availableMemory}MB
            低内存: ${if (memoryInfo.isLowMemory) "是" else "否"}
            活动Activity: ${MemoryMonitor.getMemoryStats(this).activityCount}
        """.trimIndent()
    }
    
    private fun updateTestResults(results: String) {
        tvTestResults.text = results
    }
    
    private fun addTestLog(log: String) {
        val timestamp = System.currentTimeMillis()
        val formattedLog = "[${android.text.format.DateFormat.format("HH:mm:ss", timestamp)}] $log"
        
        testLogs.add(formattedLog)
        testLogAdapter.notifyItemInserted(testLogs.size - 1)
        rvTestLogs.smoothScrollToPosition(testLogs.size - 1)
        
        Log.d(TAG, formattedLog)
    }
    
    private fun saveReportToLog(report: String) {
        // 这里可以添加保存报告到文件的功能
        Log.i(TAG, "内存优化报告:\n$report")
    }
    
    override fun onResume() {
        super.onResume()
        updateMemoryInfo()
    }
    
    override fun cleanupResources() {
        super.cleanupResources()
        addTestLog("清理测试Activity资源")
    }
    
    /**
     * 测试日志适配器
     */
    private class TestLogAdapter(private val logs: List<String>) : 
        RecyclerView.Adapter<TestLogAdapter.ViewHolder>() {
        
        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val textView = TextView(parent.context).apply {
                textSize = 12f
                setPadding(16, 8, 16, 8)
            }
            return ViewHolder(textView)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = logs[position]
        }
        
        override fun getItemCount() = logs.size
    }
}