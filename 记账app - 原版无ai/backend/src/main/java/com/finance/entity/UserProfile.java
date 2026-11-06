package com.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.finance.converter.MapConverter;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "remind_time", length = 10)
    private String remindTime;

    @Column(name = "backup_status")
    private LocalDateTime backupStatus;

    // 用户偏好设置
    @Lob
    @Column(columnDefinition = "TEXT")
    @Convert(converter = MapConverter.class)
    private Map<String, Object> preferences;

    @Column(name = "category")
    private String category;

    // 主题配置
    @Column(name = "theme_mode")
    private String themeMode;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "accent_color")
    private String accentColor;

    @Column(name = "dark_mode_enabled")
    private Boolean darkModeEnabled;

    // 通知设置
    @Column(name = "push_enabled")
    private Boolean pushEnabled;

    @Column(name = "budget_alert_enabled")
    private Boolean budgetAlertEnabled;

    @Column(name = "transaction_reminder_enabled")
    private Boolean transactionReminderEnabled;

    @Column(name = "reminder_frequency_days")
    private Integer reminderFrequencyDays;

    @Column(name = "budget_warning_threshold")
    private Integer budgetWarningThreshold;

    @Column(name = "budget_critical_threshold")
    private Integer budgetCriticalThreshold;

    @Column(name = "bill_reminder_enabled")
    private Boolean billReminderEnabled;

    @Column(name = "goal_achievement_reminder_enabled")
    private Boolean goalAchievementReminderEnabled;

    @Column(name = "data_sync_reminder_enabled")
    private Boolean dataSyncReminderEnabled;

    // 数据导出设置
    @Column(name = "export_format")
    private String exportFormat;

    @Column(name = "auto_backup_enabled")
    private Boolean autoBackupEnabled;

    @Column(name = "backup_frequency")
    private String backupFrequency;

    @Column(name = "export_path")
    private String exportPath;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fieldSelection;

    @Column(name = "time_range")
    private String timeRange;

    @Column(name = "include_attachments")
    private Boolean includeAttachments;

    @Column(name = "include_images")
    private Boolean includeImages;

    @Column(name = "compress_export")
    private Boolean compressExport;

    // 时间戳字段
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdTime = now;
        this.updatedTime = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedTime = LocalDateTime.now();
    }

    // Getter and Setter methods
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime = remindTime;
    }

    public LocalDateTime getBackupStatus() {
        return backupStatus;
    }

    public void setBackupStatus(LocalDateTime backupStatus) {
        this.backupStatus = backupStatus;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public Boolean getDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(Boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getBudgetAlertEnabled() {
        return budgetAlertEnabled;
    }

    public void setBudgetAlertEnabled(Boolean budgetAlertEnabled) {
        this.budgetAlertEnabled = budgetAlertEnabled;
    }

    public Boolean getTransactionReminderEnabled() {
        return transactionReminderEnabled;
    }

    public void setTransactionReminderEnabled(Boolean transactionReminderEnabled) {
        this.transactionReminderEnabled = transactionReminderEnabled;
    }

    public Integer getReminderFrequencyDays() {
        return reminderFrequencyDays;
    }

    public void setReminderFrequencyDays(Integer reminderFrequencyDays) {
        this.reminderFrequencyDays = reminderFrequencyDays;
    }

    public Integer getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    public void setBudgetWarningThreshold(Integer budgetWarningThreshold) {
        this.budgetWarningThreshold = budgetWarningThreshold;
    }

    public Integer getBudgetCriticalThreshold() {
        return budgetCriticalThreshold;
    }

    public void setBudgetCriticalThreshold(Integer budgetCriticalThreshold) {
        this.budgetCriticalThreshold = budgetCriticalThreshold;
    }

    public Boolean getBillReminderEnabled() {
        return billReminderEnabled;
    }

    public void setBillReminderEnabled(Boolean billReminderEnabled) {
        this.billReminderEnabled = billReminderEnabled;
    }

    public Boolean getGoalAchievementReminderEnabled() {
        return goalAchievementReminderEnabled;
    }

    public void setGoalAchievementReminderEnabled(Boolean goalAchievementReminderEnabled) {
        this.goalAchievementReminderEnabled = goalAchievementReminderEnabled;
    }

    public Boolean getDataSyncReminderEnabled() {
        return dataSyncReminderEnabled;
    }

    public void setDataSyncReminderEnabled(Boolean dataSyncReminderEnabled) {
        this.dataSyncReminderEnabled = dataSyncReminderEnabled;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public Boolean getAutoBackupEnabled() {
        return autoBackupEnabled;
    }

    public void setAutoBackupEnabled(Boolean autoBackupEnabled) {
        this.autoBackupEnabled = autoBackupEnabled;
    }

    public String getBackupFrequency() {
        return backupFrequency;
    }

    public void setBackupFrequency(String backupFrequency) {
        this.backupFrequency = backupFrequency;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public String getFieldSelection() {
        return fieldSelection;
    }

    public void setFieldSelection(String fieldSelection) {
        this.fieldSelection = fieldSelection;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public Boolean getIncludeAttachments() {
        return includeAttachments;
    }

    public void setIncludeAttachments(Boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }

    public Boolean getIncludeImages() {
        return includeImages;
    }

    public void setIncludeImages(Boolean includeImages) {
        this.includeImages = includeImages;
    }

    public Boolean getCompressExport() {
        return compressExport;
    }

    public void setCompressExport(Boolean compressExport) {
        this.compressExport = compressExport;
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
}
