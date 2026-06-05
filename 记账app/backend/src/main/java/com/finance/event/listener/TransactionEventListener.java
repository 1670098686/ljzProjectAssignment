package com.finance.event.listener;

import com.finance.event.TransactionCreatedEvent;
import com.finance.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 交易事件监听器 - 处理交易相关的异步业务逻辑
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheService cacheService;

    /**
     * 处理交易创建事件
     * 执行以下异步业务逻辑：
     * 1. 清除相关统计数据缓存
     * 2. 触发预算检查
     * 3. 更新用户消费习惯分析
     */
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        log.info("处理交易创建事件: 用户={}, 交易ID={}, 分类={}, 金额={}", 
                event.getUserId(), event.getTransactionId(), event.getCategoryId(), event.getAmount());

        try {
            // 1. 清除相关缓存以触发数据重新计算
            clearTransactionRelatedCaches(event);
            
            // 2. 触发预算检查逻辑
            triggerBudgetCheck(event);
            
            // 3. 更新用户消费分析数据
            updateUserSpendingAnalysis(event);
            
            // 4. 记录交易事件到审计日志
            logTransactionEvent(event);
            
        } catch (Exception e) {
            log.error("处理交易事件时发生错误: {}", event.getTransactionId(), e);
        }
    }

    private void clearTransactionRelatedCaches(TransactionCreatedEvent event) {
        try {
            // 清除用户统计缓存
            cacheService.clearCache("user_statistics");
            
            // 清除分类统计缓存
            cacheService.clearCache("category_statistics");
            
            // 清除趋势分析缓存
            cacheService.clearCache("trend_statistics");
            
            log.debug("已清除用户{}交易相关的缓存数据", event.getUserId());
        } catch (Exception e) {
            log.warn("清除缓存失败: {}", e.getMessage());
        }
    }

    private void triggerBudgetCheck(TransactionCreatedEvent event) {
        // 如果是支出交易，触发预算检查
        if (event.getType() == 2) { // 支出类型
            log.debug("触发预算检查: 用户={}, 分类={}, 金额={}", 
                    event.getUserId(), event.getCategoryId(), event.getAmount());
            // 预算检查逻辑将通过BudgetAlertService异步处理
        }
    }

    private void updateUserSpendingAnalysis(TransactionCreatedEvent event) {
        try {
            // 更新用户消费习惯分析数据
            Cache spendingCache = cacheManager.getCache("user_spending_analysis");
            if (spendingCache != null) {
                spendingCache.evictIfPresent(event.getUserId());
            }
            log.debug("已更新用户消费分析数据: {}", event.getUserId());
        } catch (Exception e) {
            log.warn("更新用户消费分析失败: {}", e.getMessage());
        }
    }

    private void logTransactionEvent(TransactionCreatedEvent event) {
        // 记录交易事件详情，用于后续分析和审计
        log.info("交易事件详情 - 用户ID:{}, 交易ID:{}, 交易类型:{}, 分类ID:{}, 金额:{}, 交易日期:{}", 
                event.getUserId(), 
                event.getTransactionId(), 
                event.getType() == 1 ? "收入" : "支出",
                event.getCategoryId(), 
                event.getAmount(), 
                event.getTransactionDate());
    }
}
