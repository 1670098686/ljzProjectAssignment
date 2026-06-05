package com.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UserNotificationRequest {
    private Boolean pushEnabled;
    private Boolean budgetAlertEnabled;
    private Boolean transactionReminder;
    
    @Min(value = 1, message = "提醒频率不能小于1天")
    @Max(value = 7, message = "提醒频率不能大于7天")
    private Integer reminderFrequency;
    
    // 预算预警阈值设置
    private Double budgetWarningThreshold;  // 警告阈值(百分比)
    private Double budgetCriticalThreshold; // 严重阈值(百分比)
    
    // 账单提醒设置
    private Boolean billReminderEnabled;    // 账单提醒开关
    private Integer billReminderDays;       // 提前提醒天数
    
    // 目标达成提醒
    private Boolean goalAchievementEnabled; // 目标达成提醒开关
    
    // 数据同步提醒
    private Boolean syncReminderEnabled;    // 数据同步提醒开关

    // Getter methods
    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public Boolean getBudgetAlertEnabled() {
        return budgetAlertEnabled;
    }

    public Boolean getTransactionReminder() {
        return transactionReminder;
    }

    public Integer getReminderFrequency() {
        return reminderFrequency;
    }

    public Double getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    public Double getBudgetCriticalThreshold() {
        return budgetCriticalThreshold;
    }

    public Boolean getBillReminderEnabled() {
        return billReminderEnabled;
    }

    public Integer getBillReminderDays() {
        return billReminderDays;
    }

    public Boolean getGoalAchievementEnabled() {
        return goalAchievementEnabled;
    }

    public Boolean getSyncReminderEnabled() {
        return syncReminderEnabled;
    }

    // Setter methods
    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public void setBudgetAlertEnabled(Boolean budgetAlertEnabled) {
        this.budgetAlertEnabled = budgetAlertEnabled;
    }

    public void setTransactionReminder(Boolean transactionReminder) {
        this.transactionReminder = transactionReminder;
    }

    public void setReminderFrequency(Integer reminderFrequency) {
        this.reminderFrequency = reminderFrequency;
    }

    public void setBudgetWarningThreshold(Double budgetWarningThreshold) {
        this.budgetWarningThreshold = budgetWarningThreshold;
    }

    public void setBudgetCriticalThreshold(Double budgetCriticalThreshold) {
        this.budgetCriticalThreshold = budgetCriticalThreshold;
    }

    public void setBillReminderEnabled(Boolean billReminderEnabled) {
        this.billReminderEnabled = billReminderEnabled;
    }

    public void setBillReminderDays(Integer billReminderDays) {
        this.billReminderDays = billReminderDays;
    }

    public void setGoalAchievementEnabled(Boolean goalAchievementEnabled) {
        this.goalAchievementEnabled = goalAchievementEnabled;
    }

    public void setSyncReminderEnabled(Boolean syncReminderEnabled) {
        this.syncReminderEnabled = syncReminderEnabled;
    }
}