package com.finance.service.impl;

import com.finance.context.UserContext;
import com.finance.dto.UpdateUserProfileRequest;
import com.finance.dto.UserProfileDto;
import com.finance.dto.UserPreferencesRequest;
import com.finance.dto.UserPreferencesResponse;
import com.finance.dto.UserThemeRequest;
import com.finance.dto.UserThemeResponse;
import com.finance.dto.UserNotificationRequest;
import com.finance.dto.UserNotificationResponse;
import com.finance.dto.UserExportRequest;
import com.finance.dto.UserExportResponse;
import com.finance.entity.UserProfile;
import com.finance.exception.BusinessException;
import com.finance.repository.UserProfileRepository;
import com.finance.service.DataExportService;
import com.finance.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional

public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);
    
    private final UserProfileRepository userProfileRepository;
    private final UserContext userContext;
    private final DataExportService dataExportService;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository,
                                  UserContext userContext,
                                  DataExportService dataExportService) {
        this.userProfileRepository = userProfileRepository;
        this.userContext = userContext;
        this.dataExportService = dataExportService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentProfile() {
        Long userId = userContext.getCurrentUserId();
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        return toDto(profile);
    }

    @Override
    public UserProfileDto updateProfile(UpdateUserProfileRequest request) {
        Objects.requireNonNull(request, "UpdateUserProfileRequest must not be null");
        Long userId = userContext.getCurrentUserId();
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        profile.setNickname(request.getNickname().trim());
        if (request.getRemindTime() != null) {
            validateRemindTime(request.getRemindTime());
            profile.setRemindTime(request.getRemindTime());
        } else {
            profile.setRemindTime(null);
        }

        return toDto(userProfileRepository.save(profile));
    }

    private UserProfile createDefaultProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname("新用户");
        profile.setRemindTime(null);
        profile.setBackupStatus(null);
        return userProfileRepository.save(profile);
    }

    private void validateRemindTime(String remindTime) {
        if (remindTime.isBlank()) {
            return;
        }
        if (!remindTime.matches("^([01]?\\d|2[0-3]):[0-5]\\d$")) {
            throw new BusinessException("remindTime must match HH:mm format");
        }
    }

    private UserProfileDto toDto(UserProfile profile) {
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setRemindTime(profile.getRemindTime());
        dto.setBackupStatus(profile.getBackupStatus());
        // 设置用户配置相关字段
        if (profile.getPreferences() != null) {
            dto.setPreferences(profile.getPreferences());
        }
        dto.setThemeMode(profile.getThemeMode());
        dto.setPrimaryColor(profile.getPrimaryColor());
        dto.setAccentColor(profile.getAccentColor());
        dto.setDarkModeEnabled(profile.getDarkModeEnabled());
        dto.setPushEnabled(profile.getPushEnabled());
        dto.setBudgetAlertEnabled(profile.getBudgetAlertEnabled());
        dto.setTransactionReminder(profile.getTransactionReminderEnabled());
        dto.setReminderFrequency(profile.getReminderFrequencyDays());
        dto.setExportFormat(profile.getExportFormat());
        dto.setCreatedTime(profile.getCreatedTime());
        dto.setUpdatedTime(profile.getUpdatedTime());
        return dto;
    }

    // ================================
    // 用户偏好设置方法实现
    // ================================

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences() {
        UserProfile profile = getCurrentProfileEntity();
        UserPreferencesResponse response = new UserPreferencesResponse();
        response.setSuccess(true);
        response.setMessage("获取用户偏好设置成功");
        response.setPreferences(profile.getPreferences() != null ? profile.getPreferences() : new HashMap<>());
        response.setCategory(profile.getCategory());
        response.setUpdatedTime(profile.getUpdatedTime());
        return response;
    }

    @Override
    public UserPreferencesResponse updatePreferences(UserPreferencesRequest request) {
        Objects.requireNonNull(request, "UserPreferencesRequest must not be null");
        Objects.requireNonNull(request.getPreferences(), "Preferences must not be null");
        
        UserProfile profile = getCurrentProfileEntity();
        profile.setPreferences(new HashMap<>(request.getPreferences()));
        profile.setCategory(request.getCategory());
        profile.setUpdatedTime(LocalDateTime.now());
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        UserPreferencesResponse response = new UserPreferencesResponse();
        response.setSuccess(true);
        response.setMessage("用户偏好设置更新成功");
        response.setPreferences(savedProfile.getPreferences());
        response.setCategory(savedProfile.getCategory());
        response.setUpdatedTime(savedProfile.getUpdatedTime());
        
        return response;
    }

    @Override
    public UserPreferencesResponse updatePreferenceItem(String category, Map<String, Object> preference) {
        Objects.requireNonNull(category, "Category must not be null");
        Objects.requireNonNull(preference, "Preference must not be null");
        
        UserProfile profile = getCurrentProfileEntity();
        Map<String, Object> preferences = profile.getPreferences();
        if (preferences == null) {
            preferences = new HashMap<>();
            profile.setPreferences(preferences);
        }
        
        preferences.put(category, preference);
        profile.setUpdatedTime(LocalDateTime.now());
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        UserPreferencesResponse response = new UserPreferencesResponse();
        response.setSuccess(true);
        response.setMessage("偏好项更新成功");
        response.setPreferences(savedProfile.getPreferences());
        response.setCategory(savedProfile.getCategory());
        response.setUpdatedTime(savedProfile.getUpdatedTime());
        
        return response;
    }

    // ================================
    // 主题配置方法实现
    // ================================

    @Override
    @Transactional(readOnly = true)
    public UserThemeResponse getTheme() {
        UserProfile profile = getCurrentProfileEntity();
        UserThemeResponse response = new UserThemeResponse();
        response.setSuccess(true);
        response.setMessage("获取主题配置成功");
        response.setThemeMode(profile.getThemeMode());
        response.setPrimaryColor(profile.getPrimaryColor());
        response.setAccentColor(profile.getAccentColor());
        response.setDarkModeEnabled(profile.getDarkModeEnabled());
        response.setUpdatedTime(profile.getUpdatedTime());
        return response;
    }

    @Override
    public UserThemeResponse updateTheme(UserThemeRequest request) {
        Objects.requireNonNull(request, "UserThemeRequest must not be null");
        
        UserProfile profile = getCurrentProfileEntity();
        profile.setThemeMode(request.getThemeMode());
        profile.setPrimaryColor(request.getPrimaryColor());
        profile.setAccentColor(request.getAccentColor());
        profile.setDarkModeEnabled(request.getDarkModeEnabled());
        profile.setUpdatedTime(LocalDateTime.now());
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        UserThemeResponse response = new UserThemeResponse();
        response.setSuccess(true);
        response.setMessage("主题配置更新成功");
        response.setThemeMode(savedProfile.getThemeMode());
        response.setPrimaryColor(savedProfile.getPrimaryColor());
        response.setAccentColor(savedProfile.getAccentColor());
        response.setDarkModeEnabled(savedProfile.getDarkModeEnabled());
        response.setUpdatedTime(savedProfile.getUpdatedTime());
        
        return response;
    }

    // ================================
    // 通知设置方法实现
    // ================================

    @Override
    @Transactional(readOnly = true)
    public UserNotificationResponse getNotificationSettings() {
        UserProfile profile = getCurrentProfileEntity();
        UserNotificationResponse response = new UserNotificationResponse();
        response.setSuccess(true);
        response.setMessage("获取通知设置成功");
        response.setPushEnabled(profile.getPushEnabled());
        response.setBudgetAlertEnabled(profile.getBudgetAlertEnabled());
        response.setTransactionReminder(profile.getTransactionReminderEnabled());
        response.setReminderFrequency(profile.getReminderFrequencyDays());
        response.setBudgetWarningThreshold(profile.getBudgetWarningThreshold() != null ? profile.getBudgetWarningThreshold().doubleValue() : null);
        response.setBudgetCriticalThreshold(profile.getBudgetCriticalThreshold() != null ? profile.getBudgetCriticalThreshold().doubleValue() : null);
        response.setBillReminderEnabled(profile.getBillReminderEnabled());
        response.setGoalAchievementEnabled(profile.getGoalAchievementReminderEnabled());
        response.setSyncReminderEnabled(profile.getDataSyncReminderEnabled());
        response.setUpdatedTime(profile.getUpdatedTime());
        return response;
    }

    @Override
    public UserNotificationResponse updateNotificationSettings(UserNotificationRequest request) {
        Objects.requireNonNull(request, "UserNotificationRequest must not be null");
        
        UserProfile profile = getCurrentProfileEntity();
        profile.setPushEnabled(request.getPushEnabled());
        profile.setBudgetAlertEnabled(request.getBudgetAlertEnabled());
        profile.setTransactionReminderEnabled(request.getTransactionReminder());
        profile.setReminderFrequencyDays(request.getReminderFrequency());
        profile.setBudgetWarningThreshold(request.getBudgetWarningThreshold() != null ? request.getBudgetWarningThreshold().intValue() : null);
        profile.setBudgetCriticalThreshold(request.getBudgetCriticalThreshold() != null ? request.getBudgetCriticalThreshold().intValue() : null);
        profile.setBillReminderEnabled(request.getBillReminderEnabled());
        profile.setGoalAchievementReminderEnabled(request.getGoalAchievementEnabled());
        profile.setDataSyncReminderEnabled(request.getSyncReminderEnabled());
        profile.setUpdatedTime(LocalDateTime.now());
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        UserNotificationResponse response = new UserNotificationResponse();
        response.setSuccess(true);
        response.setMessage("通知设置更新成功");
        response.setPushEnabled(savedProfile.getPushEnabled());
        response.setBudgetAlertEnabled(savedProfile.getBudgetAlertEnabled());
        response.setTransactionReminder(savedProfile.getTransactionReminderEnabled());
        response.setReminderFrequency(savedProfile.getReminderFrequencyDays());
        response.setBudgetWarningThreshold(savedProfile.getBudgetWarningThreshold() != null ? savedProfile.getBudgetWarningThreshold().doubleValue() : null);
        response.setBudgetCriticalThreshold(savedProfile.getBudgetCriticalThreshold() != null ? savedProfile.getBudgetCriticalThreshold().doubleValue() : null);
        response.setBillReminderEnabled(savedProfile.getBillReminderEnabled());
        response.setGoalAchievementEnabled(savedProfile.getGoalAchievementReminderEnabled());
        response.setSyncReminderEnabled(savedProfile.getDataSyncReminderEnabled());
        response.setUpdatedTime(savedProfile.getUpdatedTime());
        
        return response;
    }

    // ================================
    // 数据导出设置方法实现
    // ================================

    @Override
    @Transactional(readOnly = true)
    public UserExportResponse getExportSettings() {
        UserProfile profile = getCurrentProfileEntity();
        UserExportResponse response = new UserExportResponse();
        response.setSuccess(true);
        response.setMessage("获取导出设置成功");
        response.setExportFormat(profile.getExportFormat());
        response.setCategory(profile.getCategory());
        response.setUpdatedTime(profile.getUpdatedTime());
        return response;
    }

    @Override
    public UserExportResponse updateExportSettings(UserExportRequest request) {
        Objects.requireNonNull(request, "UserExportRequest must not be null");
        
        UserProfile profile = getCurrentProfileEntity();
        profile.setExportFormat(request.getExportFormat());
        profile.setCategory(request.getCategory());
        profile.setTimeRange(request.getTimeRange());
        profile.setExportPath(request.getExportPath());
        profile.setUpdatedTime(LocalDateTime.now());
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        UserExportResponse response = new UserExportResponse();
        response.setSuccess(true);
        response.setMessage("数据导出设置更新成功");
        response.setExportFormat(savedProfile.getExportFormat());
        response.setCategory(savedProfile.getCategory());
        response.setUpdatedTime(savedProfile.getUpdatedTime());
        
        return response;
    }

    @Override
    public String executeDataExport(String exportType) {
        try {
            // 使用数据导出服务执行实际的数据导出
            UserExportRequest request = new UserExportRequest();
            request.setExportFormat(exportType);
            UserExportResponse response = dataExportService.executeDataExport(request);
            return response.getExportPath();
        } catch (Exception e) {
            log.error("数据导出失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据导出失败: " + e.getMessage(), e);
        }
    }

    // ================================
    // 私有辅助方法
    // ================================

    /**
     * 获取当前用户档案实体，如果不存在则创建默认档案
     */
    private UserProfile getCurrentProfileEntity() {
        Long userId = userContext.getCurrentUserId();
        return userProfileRepository.findById(userId)
                .orElseGet(() -> createDefaultProfile(userId));
    }
}
