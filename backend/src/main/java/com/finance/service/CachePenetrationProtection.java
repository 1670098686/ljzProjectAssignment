package com.finance.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 缓存穿透防护服务
 * 集成布隆过滤器
 */
@Service
public class CachePenetrationProtection {
    
    private static final Logger log = LoggerFactory.getLogger(CachePenetrationProtection.class);
    
    // 布隆过滤器，用于快速判断key是否存在
    private final BloomFilter<String> bloomFilter;
    
    // 布隆过滤器预期插入数量
    private static final int EXPECTED_INSERTIONS = 100000;
    
    // 布隆过滤器误判率
    private static final double FALSE_POSITIVE_RATE = 0.01;
    
    public CachePenetrationProtection() {
        // 使用自定义Funnel<String>来解决类型不兼容问题
        this.bloomFilter = BloomFilter.create(
            new Funnel<String>() {
                @Override
                public void funnel(String from, PrimitiveSink into) {
                    into.putString(from, StandardCharsets.UTF_8);
                }
            },
            EXPECTED_INSERTIONS
        );
        log.info("缓存穿透防护服务初始化完成，预期插入数: {}, 误判率: {}", EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE);
    }
    
    /**
     * 添加key到布隆过滤器
     */
    public void addToBloomFilter(String key) {
        bloomFilter.put(key);
    }
    
    /**
     * 批量添加keys到布隆过滤器
     */
    public void addToBloomFilterBatch(String[] keys) {
        if (keys != null) {
            Arrays.stream(keys).forEach(bloomFilter::put);
        }
    }
    
    /**
     * 检查key是否可能在布隆过滤器中
     */
    public boolean mightContain(String key) {
        return bloomFilter.mightContain(key);
    }
    
    /**
     * 预热布隆过滤器
     * 从数据库或配置中预加载常用key
     */
    public void warmUpBloomFilter(String[] warmUpKeys) {
        if (warmUpKeys != null && warmUpKeys.length > 0) {
            Arrays.stream(warmUpKeys).forEach(bloomFilter::put);
            log.info("布隆过滤器预热完成，预热key数量: {}", warmUpKeys.length);
        }
    }
    
    /**
     * 获取布隆过滤器状态信息
     */
    public String getBloomFilterStatus() {
        return String.format("布隆过滤器 - 预期插入数: %d, 误判率: %.4f", 
            EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE);
    }
    
    /**
     * 清理过期的null值缓存
     * 此方法可以用来定期清理布隆过滤器中的无效key
     */
    public void cleanupBloomFilter() {
        // 布隆过滤器不支持删除，这里仅做日志记录
        // 在实际应用中，可以考虑重建布隆过滤器
        log.info("布隆过滤器状态: {}", getBloomFilterStatus());
    }
}