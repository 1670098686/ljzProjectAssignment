package com.finance.alert.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算预警历史记录实体
 * 
 * 存储用户的预算预警历史记录，包括预警级别、触发时间、分类等信息
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@Entity
@Table(name = "budget_alert_history")
public class BudgetAlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "budget_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "spent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal spentAmount;

    @Column(name = "usage_percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal usagePercentage;

    @Column(name = "alert_level", nullable = false)
    private String alertLevel;

    @Column(name = "alert_message", nullable = false, length = 500)
    private String alertMessage;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    @Column(name = "notification_time")
    private LocalDateTime notificationTime;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON格式的额外数据

    // 构造函数
    public BudgetAlertHistory() {}

    public BudgetAlertHistory(Long userId, Long categoryId, String categoryName,
                            BigDecimal budgetAmount, BigDecimal spentAmount,
                            BigDecimal usagePercentage, String alertLevel, 
                            String alertMessage, Integer year, Integer month) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.usagePercentage = usagePercentage;
        this.alertLevel = alertLevel;
        this.alertMessage = alertMessage;
        this.alertTime = LocalDateTime.now();
        this.year = year;
        this.month = month;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.isResolved = false;
        this.notificationSent = false;
    }

    // JPA生命周期回调
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
        if (alertTime == null) {
            alertTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    // 获取和设置方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(BigDecimal usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
        if (isResolved && resolvedTime == null) {
            resolvedTime = LocalDateTime.now();
        }
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
        if (notificationSent && notificationTime == null) {
            notificationTime = LocalDateTime.now();
        }
    }

    public LocalDateTime getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(LocalDateTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    // 工具方法
    public boolean isCriticalAlert() {
        return "CRITICAL".equals(alertLevel);
    }

    public boolean isWarningAlert() {
        return "WARNING".equals(alertLevel);
    }

    public boolean isResolved() {
        return isResolved;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public BigDecimal getRemainingBudget() {
        return budgetAmount.subtract(spentAmount);
    }

    public BigDecimal getRemainingPercentage() {
        return BigDecimal.ONE.subtract(usagePercentage);
    }

    public void resolveAlert() {
        this.isResolved = true;
        this.resolvedTime = LocalDateTime.now();
    }

    public void markNotificationSent() {
        this.notificationSent = true;
        this.notificationTime = LocalDateTime.now();
    }

    public void setMetadataValue(String key, String value) {
        // 简单的metadata处理，实际项目中建议使用JSON库
        if (metadata == null || metadata.isEmpty()) {
            metadata = "{\"" + key + "\":\"" + value + "\"}";
        } else {
            metadata = metadata.substring(0, metadata.length() - 1) + ",\"" + key + "\":\"" + value + "\"}";
        }
    }

    public String getMetadataValue(String key) {
        // 简单的metadata解析，实际项目中建议使用JSON库
        if (metadata == null) return null;
        
        String searchKey = "\"" + key + "\":";
        int startIndex = metadata.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        int valueStart = startIndex + searchKey.length() + 1; // 跳过"和:
        int valueEnd = metadata.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return metadata.substring(valueStart, valueEnd);
    }

    @Override
    public String toString() {
        return "BudgetAlertHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", budgetAmount=" + budgetAmount +
                ", spentAmount=" + spentAmount +
                ", usagePercentage=" + usagePercentage +
                ", alertLevel='" + alertLevel + '\'' +
                ", alertMessage='" + alertMessage + '\'' +
                ", alertTime=" + alertTime +
                ", year=" + year +
                ", month=" + month +
                ", isResolved=" + isResolved +
                ", resolvedTime=" + resolvedTime +
                ", notificationSent=" + notificationSent +
                ", notificationTime=" + notificationTime +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                ", metadata='" + metadata + '\'' +
                '}';
    }
}