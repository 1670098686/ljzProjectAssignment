package com.finance.alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预算预警配置实体
 * 
 * 存储用户的预算预警规则配置，包括预警阈值、通知方式等设置
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@Entity
@Table(name = "budget_alert_config")
public class BudgetAlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "warning_threshold", nullable = false, precision = 5, scale = 4)
    private Double warningThreshold; // 预警阈值 (0.8 = 80%)

    @Column(name = "critical_threshold", nullable = false, precision = 5, scale = 4)
    private Double criticalThreshold; // 严重预警阈值 (0.9 = 90%)

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true; // 是否启用推送通知

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true; // 是否启用邮件通知

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true; // 是否启用应用内通知

    @ElementCollection
    @CollectionTable(
        name = "budget_alert_config_categories",
        joinColumns = @JoinColumn(name = "config_id")
    )
    @Column(name = "category_id")
    private List<Long> categoryIds; // 配置的分类ID列表

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 构造函数
    public BudgetAlertConfig() {}

    public BudgetAlertConfig(Long userId, Double warningThreshold, Double criticalThreshold) {
        this.userId = userId;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.isActive = true;
    }

    // JPA生命周期回调
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
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

    public Double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(Double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public Double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(Double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(Boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // 工具方法
    public boolean isThresholdReached(Double usagePercentage) {
        return usagePercentage >= warningThreshold;
    }

    public boolean isCriticalThresholdReached(Double usagePercentage) {
        return usagePercentage >= criticalThreshold;
    }

    public AlertLevel determineAlertLevel(Double usagePercentage) {
        if (usagePercentage >= criticalThreshold) {
            return AlertLevel.CRITICAL;
        } else if (usagePercentage >= warningThreshold) {
            return AlertLevel.WARNING;
        } else {
            return AlertLevel.NORMAL;
        }
    }

    public boolean isNotificationEnabled(NotificationType type) {
        return switch (type) {
            case PUSH -> pushEnabled;
            case EMAIL -> emailEnabled;
            case IN_APP -> inAppEnabled;
        };
    }

    public enum AlertLevel {
        NORMAL("正常"),
        WARNING("预警"),
        CRITICAL("严重");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum NotificationType {
        PUSH("推送通知"),
        EMAIL("邮件通知"),
        IN_APP("应用内通知");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "BudgetAlertConfig{" +
                "id=" + id +
                ", userId=" + userId +
                ", warningThreshold=" + warningThreshold +
                ", criticalThreshold=" + criticalThreshold +
                ", pushEnabled=" + pushEnabled +
                ", emailEnabled=" + emailEnabled +
                ", inAppEnabled=" + inAppEnabled +
                ", categoryIds=" + categoryIds +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                ", isActive=" + isActive +
                '}';
    }
}