package com.finance.statistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统计数据实体类
 * 
 * 存储各类统计分析的结果数据
 * 支持不同维度的数据聚合
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "statistics_records",
    indexes = {
        @Index(name = "idx_stats_user_period", columnList = "user_id,period_type,period_value"),
        @Index(name = "idx_stats_period", columnList = "period_type,period_value"),
        @Index(name = "idx_stats_category", columnList = "user_id,category_id"),
        @Index(name = "idx_stats_created", columnList = "created_at")
    })
public class StatisticsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 统计类型：MONTHLY_SUMMARY, CATEGORY, TREND
     */
    @Column(name = "statistics_type", nullable = false, length = 50)
    private String statisticsType;

    /**
     * 时间周期类型：DAILY, MONTHLY, YEARLY
     */
    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType;

    /**
     * 时间周期值
     * 例如：2024-01 (月度), 2024 (年度)
     */
    @Column(name = "period_value", nullable = false)
    private String periodValue;

    /**
     * 分类ID（可选，用于分类统计）
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * 分类名称（冗余字段，用于展示）
     */
    @Column(name = "category_name", length = 100)
    private String categoryName;

    /**
     * 总收入
     */
    @Column(name = "total_income", precision = 15, scale = 2)
    private java.math.BigDecimal totalIncome;

    /**
     * 总支出
     */
    @Column(name = "total_expense", precision = 15, scale = 2)
    private java.math.BigDecimal totalExpense;

    /**
     * 交易数量
     */
    @Column(name = "transaction_count")
    private Integer transactionCount;

    /**
     * 环比增长率
     */
    @Column(name = "growth_rate", precision = 8, scale = 4)
    private java.math.BigDecimal growthRate;

    /**
     * 统计数据的详细JSON（可选，用于存储复杂统计结果）
     */
    @Column(name = "statistics_data", columnDefinition = "TEXT")
    private String statisticsData;

    /**
     * 缓存键（用于Redis缓存）
     */
    @Column(name = "cache_key", length = 255, unique = true)
    private String cacheKey;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 数据版本号（用于并发控制）
     */
    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 计算结余（收入-支出）
     */
    public java.math.BigDecimal getBalance() {
        if (totalIncome == null && totalExpense == null) {
            return java.math.BigDecimal.ZERO;
        }
        if (totalIncome == null) {
            return totalExpense != null ? totalExpense.negate() : java.math.BigDecimal.ZERO;
        }
        if (totalExpense == null) {
            return totalIncome;
        }
        return totalIncome.subtract(totalExpense);
    }

    /**
     * 检查数据是否为空
     */
    public boolean isEmpty() {
        return (totalIncome == null || totalIncome.compareTo(java.math.BigDecimal.ZERO) == 0) &&
               (totalExpense == null || totalExpense.compareTo(java.math.BigDecimal.ZERO) == 0) &&
               (transactionCount == null || transactionCount == 0);
    }
}