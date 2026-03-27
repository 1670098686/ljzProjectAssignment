package com.finance.dto;

import java.time.LocalDateTime;

public class UserNotificationResponse {
    private Boolean success;
    private String message;
    
    private Boolean pushEnabled;
    private Boolean budgetAlertEnabled;
    private Boolean transactionReminder;
    private Integer reminderFrequency;
    private Double budgetWarningThreshold;
    private Double budgetCriticalThreshold;
    private Boolean billReminderEnabled;
    private Integer billReminderDays;
    private Boolean goalAchievementEnabled;
    private Boolean syncReminderEnabled;
    
    private LocalDateTime updatedTime;

    // Getter methods
    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

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

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    // Setter methods
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}