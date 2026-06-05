package com.finance.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class UserProfileDto {
    private Long userId;
    private String nickname;
    private String remindTime;        // 保持向后兼容
    private String reminderTime;      // 新增标准字段
    private LocalDateTime backupStatus;  // 保持向后兼容
    private Boolean backupEnabled;    // 新增标准字段
    
    // 用户偏好设置
    private Map<String, Object> preferences;
    
    // 主题配置
    private String themeMode;        // light, dark, system
    private String primaryColor;     // 主色调
    private String accentColor;      // 强调色
    private Boolean darkModeEnabled; // 深色模式开关
    
    // 通知设置
    private Boolean pushEnabled;           // 推送通知开关
    private Boolean budgetAlertEnabled;    // 预算预警开关
    private Boolean transactionReminder;   // 交易提醒开关
    private Integer reminderFrequency;     // 提醒频率(1-7天)
    
    // 数据导出设置
    private String exportFormat;      // csv, excel, json
    private Boolean autoBackup;       // 自动备份开关
    private Integer backupFrequency;  // 备份频率(1-30天)
    private String defaultExportPath; // 默认导出路径
    
    // 创建和更新时间
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // Getter methods
    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRemindTime() {
        return remindTime;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public LocalDateTime getBackupStatus() {
        return backupStatus;
    }

    public Boolean getBackupEnabled() {
        return backupEnabled;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public Boolean getDarkModeEnabled() {
        return darkModeEnabled;
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

    public String getExportFormat() {
        return exportFormat;
    }

    public Boolean getAutoBackup() {
        return autoBackup;
    }

    public Integer getBackupFrequency() {
        return backupFrequency;
    }

    public String getDefaultExportPath() {
        return defaultExportPath;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    // Setter methods
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime = remindTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public void setBackupStatus(LocalDateTime backupStatus) {
        this.backupStatus = backupStatus;
    }

    public void setBackupEnabled(Boolean backupEnabled) {
        this.backupEnabled = backupEnabled;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public void setDarkModeEnabled(Boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
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

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public void setAutoBackup(Boolean autoBackup) {
        this.autoBackup = autoBackup;
    }

    public void setBackupFrequency(Integer backupFrequency) {
        this.backupFrequency = backupFrequency;
    }

    public void setDefaultExportPath(String defaultExportPath) {
        this.defaultExportPath = defaultExportPath;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
