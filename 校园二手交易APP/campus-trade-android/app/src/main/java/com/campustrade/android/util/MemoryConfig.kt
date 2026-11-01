package com.campustrade.android.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 内存优化配置管理类
 */
object MemoryConfig {
    
    private const val PREFS_NAME = "memory_config"
    private const val KEY_IMAGE_QUALITY = "image_quality"
    private const val KEY_CACHE_SIZE = "cache_size"
    private const val KEY_AUTO_CLEANUP = "auto_cleanup"
    private const val KEY_MEMORY_MONITORING = "memory_monitoring"
    
    // 默认配置值
    private const val DEFAULT_IMAGE_QUALITY = 85 // 图片质量百分比
    private const val DEFAULT_CACHE_SIZE = 50 // 缓存大小（MB）
    private const val DEFAULT_AUTO_CLEANUP = true
    private const val DEFAULT_MEMORY_MONITORING = true
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化配置
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 设置默认值
        if (!prefs.contains(KEY_IMAGE_QUALITY)) {
            setImageQuality(DEFAULT_IMAGE_QUALITY)
        }
        if (!prefs.contains(KEY_CACHE_SIZE)) {
            setCacheSize(DEFAULT_CACHE_SIZE)
        }
        if (!prefs.contains(KEY_AUTO_CLEANUP)) {
            setAutoCleanupEnabled(DEFAULT_AUTO_CLEANUP)
        }
        if (!prefs.contains(KEY_MEMORY_MONITORING)) {
            setMemoryMonitoringEnabled(DEFAULT_MEMORY_MONITORING)
        }
    }
    
    /**
     * 获取图片质量配置
     */
    fun getImageQuality(): Int {
        return prefs.getInt(KEY_IMAGE_QUALITY, DEFAULT_IMAGE_QUALITY)
    }
    
    /**
     * 设置图片质量配置
     */
    fun setImageQuality(quality: Int) {
        val validQuality = quality.coerceIn(50, 100)
        prefs.edit().putInt(KEY_IMAGE_QUALITY, validQuality).apply()
    }
    
    /**
     * 获取缓存大小配置（MB）
     */
    fun getCacheSize(): Int {
        return prefs.getInt(KEY_CACHE_SIZE, DEFAULT_CACHE_SIZE)
    }
    
    /**
     * 设置缓存大小配置（MB）
     */
    fun setCacheSize(size: Int) {
        val validSize = size.coerceIn(10, 200) // 限制在10-200MB之间
        prefs.edit().putInt(KEY_CACHE_SIZE, validSize).apply()
    }
    
    /**
     * 是否启用自动清理
     */
    fun isAutoCleanupEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CLEANUP, DEFAULT_AUTO_CLEANUP)
    }
    
    /**
     * 设置自动清理启用状态
     */
    fun setAutoCleanupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLEANUP, enabled).apply()
    }
    
    /**
     * 是否启用内存监控
     */
    fun isMemoryMonitoringEnabled(): Boolean {
        return prefs.getBoolean(KEY_MEMORY_MONITORING, DEFAULT_MEMORY_MONITORING)
    }
    
    /**
     * 设置内存监控启用状态
     */
    fun setMemoryMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEMORY_MONITORING, enabled).apply()
    }
    
    /**
     * 根据设备内存情况自动调整配置
     */
    fun autoAdjustConfig(context: Context) {
        val memoryInfo = MemoryMonitor.getMemoryInfo(context)
        
        when {
            memoryInfo.totalMemory < 1024 -> {
                // 低内存设备（< 1GB）
                setImageQuality(70)
                setCacheSize(20)
            }
            memoryInfo.totalMemory < 2048 -> {
                // 中等内存设备（1-2GB）
                setImageQuality(80)
                setCacheSize(30)
            }
            else -> {
                // 高内存设备（> 2GB）
                setImageQuality(90)
                setCacheSize(50)
            }
        }
    }
    
    /**
     * 获取当前配置摘要
     */
    fun getConfigSummary(): String {
        return """
            内存优化配置：
            - 图片质量：${getImageQuality()}%
            - 缓存大小：${getCacheSize()}MB
            - 自动清理：${if (isAutoCleanupEnabled()) "启用" else "禁用"}
            - 内存监控：${if (isMemoryMonitoringEnabled()) "启用" else "禁用"}
        """.trimIndent()
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefaults() {
        setImageQuality(DEFAULT_IMAGE_QUALITY)
        setCacheSize(DEFAULT_CACHE_SIZE)
        setAutoCleanupEnabled(DEFAULT_AUTO_CLEANUP)
        setMemoryMonitoringEnabled(DEFAULT_MEMORY_MONITORING)
    }
}