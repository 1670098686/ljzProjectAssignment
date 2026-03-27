package com.finance.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alert_history")
public class BudgetAlertHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "budget_id", nullable = false)
    private Long budgetId;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month;
    
    @Column(name = "budget_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal budgetAmount;
    
    @Column(name = "spent_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal spentAmount;
    
    @Column(name = "usage_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal usageRate;
    
    @Column(name = "alert_level", nullable = false)
    private String alertLevel;
    
    @Column(name = "triggered_threshold", precision = 5, scale = 4, nullable = false)
    private BigDecimal triggeredThreshold;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent;
    
    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;
    
    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime; // 预警解除时间

    // Constructor
    public BudgetAlertHistory() {
        // Default constructor
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public BigDecimal getUsageRate() {
        return usageRate;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public BigDecimal getTriggeredThreshold() {
        return triggeredThreshold;
    }

    public String getMessage() {
        return message;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public void setUsageRate(BigDecimal usageRate) {
        this.usageRate = usageRate;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public void setTriggeredThreshold(BigDecimal triggeredThreshold) {
        this.triggeredThreshold = triggeredThreshold;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }
}