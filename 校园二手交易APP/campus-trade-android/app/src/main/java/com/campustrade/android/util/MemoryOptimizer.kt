package com.campustrade.android.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey

/**
 * 内存优化工具类
 * 统一管理图片加载、内存缓存和资源释放
 */
object MemoryOptimizer {
    
    private const val TAG = "MemoryOptimizer"
    
    // 内存优化配置
    private val glideOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .encodeQuality(85) // 压缩质量
        .skipMemoryCache(false) // 启用内存缓存
        .onlyRetrieveFromCache(false)
    
    /**
     * 加载商品图片（优化版本）
     */
    fun loadProductImage(context: Context, imageUrl: String, imageView: ImageView) {
        try {
            Glide.with(context)
                .load(imageUrl)
                .apply(glideOptions)
                .apply(RequestOptions()
                    .override(400, 300) // 限制图片尺寸
                    .centerCrop()
                )
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "加载商品图片失败: ${e.message}")
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    
    /**
     * 加载用户头像（优化版本）
     */
    fun loadUserAvatar(context: Context, avatarUrl: String?, imageView: ImageView) {
        try {
            if (avatarUrl.isNullOrEmpty()) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                return
            }
            
            Glide.with(context)
                .load(avatarUrl)
                .apply(glideOptions)
                .apply(RequestOptions()
                    .override(120, 120) // 头像尺寸限制
                    .circleCrop()
                )
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "加载用户头像失败: ${e.message}")
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    
    /**
     * 加载大图（详情页使用）
     */
    fun loadLargeImage(context: Context, imageUrl: String, imageView: ImageView) {
        try {
            Glide.with(context)
                .load(imageUrl)
                .apply(glideOptions)
                .apply(RequestOptions()
                    .override(800, 600) // 大图尺寸限制
                    .fitCenter()
                )
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "加载大图失败: ${e.message}")
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    
    /**
     * 清除Glide缓存
     */
    fun clearImageCache(context: Context) {
        try {
            Glide.get(context).clearMemory() // 清除内存缓存
            Thread {
                Glide.get(context).clearDiskCache() // 清除磁盘缓存
            }.start()
            Log.i(TAG, "图片缓存已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除图片缓存失败: ${e.message}")
        }
    }
    
    /**
     * 获取内存使用情况
     */
    fun getMemoryInfo(context: Context): String {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
        val totalMemory = runtime.totalMemory() / (1024 * 1024) // MB
        val freeMemory = runtime.freeMemory() / (1024 * 1024) // MB
        val usedMemory = totalMemory - freeMemory // MB
        
        return "内存使用: ${usedMemory}MB / ${maxMemory}MB (${String.format("%.1f", usedMemory.toFloat() / maxMemory.toFloat() * 100)}%)"
    }
    
    /**
     * 检查内存压力
     */
    fun isMemoryPressure(context: Context): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsage = usedMemory.toFloat() / maxMemory.toFloat()
        
        return memoryUsage > 0.8f // 内存使用超过80%认为有压力
    }
    
    /**
     * 内存压力时自动清理
     */
    fun autoCleanupIfNeeded(context: Context) {
        if (isMemoryPressure(context)) {
            Log.w(TAG, "内存压力较大，执行自动清理")
            clearImageCache(context)
            System.gc() // 建议垃圾回收
        }
    }
    
    /**
     * 优化Bitmap配置
     */
    fun getOptimizedBitmapConfig(): Bitmap.Config {
        return Bitmap.Config.RGB_565 // 使用更节省内存的配置
    }
    
    /**
     * 计算合适的图片采样率
     */
    fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}