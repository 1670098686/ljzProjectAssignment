package com.finance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控服务
 * 提供缓存操作的统计、监控和分析功能
 */
@Service
public class CacheMonitoringService {
    
    private static final Logger log = LoggerFactory.getLogger(CacheMonitoringService.class);
    
    // 统计计数器（原子操作保证线程安全）
    private final ConcurrentHashMap<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    private final AtomicLong totalWrites = new AtomicLong(0);
    private final AtomicLong totalDeletes = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalNullValues = new AtomicLong(0);
    
    // 全局统计信息
    private volatile long startTime = System.currentTimeMillis();
    
    /**
     * 缓存统计信息内部类
     */
    public static class CacheStats {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong writes = new AtomicLong(0);
        private final AtomicLong deletes = new AtomicLong(0);
        private final AtomicLong errors = new AtomicLong(0);
        private final AtomicLong nullValues = new AtomicLong(0);
        
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        
        // 移除静态内部类中对外部非静态变量的访问
        public void recordOperation(OperationType type, long responseTime) {
            switch (type) {
                case HIT:
                    hits.incrementAndGet();
                    break;
                case MISS:
                    misses.incrementAndGet();
                    break;
                case WRITE:
                    writes.incrementAndGet();
                    break;
                case DELETE:
                    deletes.incrementAndGet();
                    break;
                case ERROR:
                    errors.incrementAndGet();
                    break;
                case NULL_VALUE:
                    nullValues.incrementAndGet();
                    break;
            }
            
            // 统计响应时间
            if (responseTime > 0) {
                totalResponseTime.addAndGet(responseTime);
                
                // 更新最小响应时间
                long currentMin;
                do {
                    currentMin = minResponseTime.get();
                    if (responseTime >= currentMin) break;
                } while (!minResponseTime.compareAndSet(currentMin, responseTime));
                
                // 更新最大响应时间
                long currentMax;
                do {
                    currentMax = maxResponseTime.get();
                    if (responseTime <= currentMax) break;
                } while (!maxResponseTime.compareAndSet(currentMax, responseTime));
            }
        }
        
        // Getters
        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getWrites() { return writes.get(); }
        public long getDeletes() { return deletes.get(); }
        public long getErrors() { return errors.get(); }
        public long getNullValues() { return nullValues.get(); }
        public long getTotalOperations() { 
            return hits.get() + misses.get() + writes.get() + deletes.get(); 
        }
        
        public double getHitRate() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total * 100 : 0;
        }
        
        public double getAverageResponseTime() {
            long totalOps = getTotalOperations();
            return totalOps > 0 ? (double) totalResponseTime.get() / totalOps : 0;
        }
        
        public long getMinResponseTime() {
            long min = minResponseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public long getMaxResponseTime() {
            return maxResponseTime.get();
        }
        
        public long getTotalResponseTime() {
            return totalResponseTime.get();
        }
    }
    
    /**
     * 缓存操作类型枚举
     */
    public enum OperationType {
        HIT,      // 缓存命中
        MISS,     // 缓存未命中
        WRITE,    // 缓存写入
        DELETE,   // 缓存删除
        ERROR,    // 操作错误
        NULL_VALUE // 空值缓存
    }
    
    /**
     * 记录缓存命中
     */
    public void recordHit(String key, String cacheName) {
        recordOperation(cacheName, OperationType.HIT, 0);
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordMiss(String key, String cacheName) {
        recordOperation(cacheName, OperationType.MISS, 0);
    }
    
    /**
     * 记录缓存写入
     */
    public void recordWrite(String key, String cacheName) {
        recordOperation(cacheName, OperationType.WRITE, 0);
    }
    
    /**
     * 记录缓存删除
     */
    public void recordDelete(String key, String cacheName) {
        recordOperation(cacheName, OperationType.DELETE, 0);
    }
    
    /**
     * 记录操作错误
     */
    public void recordError(String key, String cacheName) {
        recordOperation(cacheName, OperationType.ERROR, 0);
    }
    
    /**
     * 记录空值缓存
     */
    public void recordNullValue(String key, String cacheName) {
        recordOperation(cacheName, OperationType.NULL_VALUE, 0);
    }
    
    /**
     * 记录操作（带响应时间）
     */
    public void recordOperation(String cacheName, OperationType type, long responseTime) {
        cacheStatsMap.computeIfAbsent(cacheName, name -> new CacheStats())
                     .recordOperation(type, responseTime);
        
        // 更新全局统计变量
        switch (type) {
            case HIT:
                totalHits.incrementAndGet();
                break;
            case MISS:
                totalMisses.incrementAndGet();
                break;
            case WRITE:
                totalWrites.incrementAndGet();
                break;
            case DELETE:
                totalDeletes.incrementAndGet();
                break;
            case ERROR:
                totalErrors.incrementAndGet();
                break;
            case NULL_VALUE:
                totalNullValues.incrementAndGet();
                break;
        }
    }
    
    /**
     * 获取特定缓存的统计信息
     */
    public CacheStats getCacheStats(String cacheName) {
        return cacheStatsMap.getOrDefault(cacheName, new CacheStats());
    }
    
    /**
     * 获取所有缓存的统计信息
     */
    public ConcurrentHashMap<String, CacheStats> getAllCacheStats() {
        return new ConcurrentHashMap<>(cacheStatsMap);
    }
    
    /**
     * 获取全局统计信息
     */
    public GlobalCacheStats getGlobalStats() {
        return new GlobalCacheStats(
            totalHits.get(),
            totalMisses.get(),
            totalWrites.get(),
            totalDeletes.get(),
            totalErrors.get(),
            totalNullValues.get(),
            getGlobalHitRate(),
            getUptimeInSeconds(),
            cacheStatsMap.size()
        );
    }
    
    /**
     * 计算全局缓存命中率
     */
    public double getGlobalHitRate() {
        long total = totalHits.get() + totalMisses.get();
        return total > 0 ? (double) totalHits.get() / total * 100 : 0;
    }
    
    /**
     * 获取缓存服务运行时间（秒）
     */
    public long getUptimeInSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    /**
     * 获取缓存性能报告
     */
    public String generatePerformanceReport() {
        GlobalCacheStats global = getGlobalStats();
        StringBuilder report = new StringBuilder();
        
        report.append("\n=== 缓存性能报告 ===\n");
        report.append("生成时间: ").append(LocalDateTime.now()).append("\n");
        report.append("运行时间: ").append(global.uptimeSeconds).append(" 秒\n");
        report.append("监控缓存数量: ").append(global.totalCacheCount).append("\n");
        report.append("全局缓存命中: ").append(global.totalHits).append("\n");
        report.append("全局缓存未命中: ").append(global.totalMisses).append("\n");
        report.append("全局缓存写入: ").append(global.totalWrites).append("\n");
        report.append("全局缓存删除: ").append(global.totalDeletes).append("\n");
        report.append("全局操作错误: ").append(global.totalErrors).append("\n");
        report.append("全局空值缓存: ").append(global.totalNullValues).append("\n");
        report.append("全局命中率: ").append(String.format("%.2f%%", global.globalHitRate)).append("\n");
        
        if (!cacheStatsMap.isEmpty()) {
            report.append("\n=== 各缓存详情 ===\n");
            cacheStatsMap.forEach((name, stats) -> {
                report.append(String.format("缓存 %s:\n", name));
                report.append(String.format("  命中率: %.2f%%\n", stats.getHitRate()));
                report.append(String.format("  操作次数: %d\n", stats.getTotalOperations()));
                report.append(String.format("  平均响应时间: %.2f ms\n", stats.getAverageResponseTime()));
                report.append(String.format("  最小响应时间: %d ms\n", stats.getMinResponseTime()));
                report.append(String.format("  最大响应时间: %d ms\n", stats.getMaxResponseTime()));
                report.append("\n");
            });
        }
        
        return report.toString();
    }
    
    /**
     * 定时打印性能报告（每小时）
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void printPeriodicReport() {
        if (!cacheStatsMap.isEmpty()) {
            log.info("缓存性能报告:\n{}", generatePerformanceReport());
        }
    }
    
    /**
     * 重置所有统计数据
     */
    public void resetStats() {
        cacheStatsMap.clear();
        totalHits.set(0);
        totalMisses.set(0);
        totalWrites.set(0);
        totalDeletes.set(0);
        totalErrors.set(0);
        totalNullValues.set(0);
        startTime = System.currentTimeMillis();
        log.info("缓存统计数据已重置");
    }
    
    /**
     * 获取性能指标（用于监控）
     */
    public CachePerformanceMetrics getPerformanceMetrics() {
        GlobalCacheStats global = getGlobalStats();
        return new CachePerformanceMetrics(
            global.globalHitRate,
            global.getTotalOperations(),
            getUptimeInSeconds(),
            global.totalErrors,
            cacheStatsMap.size()
        );
    }
    
    /**
     * 全局缓存统计信息类
     */
    public static class GlobalCacheStats {
        public final long totalHits;
        public final long totalMisses;
        public final long totalWrites;
        public final long totalDeletes;
        public final long totalErrors;
        public final long totalNullValues;
        public final double globalHitRate;
        public final long uptimeSeconds;
        public final long totalCacheCount;
        
        public GlobalCacheStats(long totalHits, long totalMisses, long totalWrites,
                              long totalDeletes, long totalErrors, long totalNullValues,
                              double globalHitRate, long uptimeSeconds, long totalCacheCount) {
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.totalWrites = totalWrites;
            this.totalDeletes = totalDeletes;
            this.totalErrors = totalErrors;
            this.totalNullValues = totalNullValues;
            this.globalHitRate = globalHitRate;
            this.uptimeSeconds = uptimeSeconds;
            this.totalCacheCount = totalCacheCount;
        }
        
        public long getTotalOperations() {
            return totalHits + totalMisses + totalWrites + totalDeletes;
        }
    }
    
    /**
     * 缓存性能指标类
     */
    public static class CachePerformanceMetrics {
        public final double hitRate;
        public final long totalOperations;
        public final long uptimeSeconds;
        public final long errorCount;
        public final int cacheCount;
        
        public CachePerformanceMetrics(double hitRate, long totalOperations, 
                                     long uptimeSeconds, long errorCount, int cacheCount) {
            this.hitRate = hitRate;
            this.totalOperations = totalOperations;
            this.uptimeSeconds = uptimeSeconds;
            this.errorCount = errorCount;
            this.cacheCount = cacheCount;
        }
        
        public double getOperationsPerSecond() {
            return uptimeSeconds > 0 ? (double) totalOperations / uptimeSeconds : 0;
        }
        
        public double getErrorRate() {
            return totalOperations > 0 ? (double) errorCount / totalOperations * 100 : 0;
        }
    }
}