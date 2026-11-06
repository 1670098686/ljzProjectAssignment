package com.finance.service;

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
import java.util.Map;

/**
 * 用户档案服务接口
 * 提供用户基本信息和配置管理相关的服务方法
 */
public interface UserProfileService {

    /**
     * 获取当前用户的基本档案信息
     * @return 用户档案信息
     */
    UserProfileDto getCurrentProfile();

    /**
     * 更新当前用户的基本档案信息
     * @param request 用户档案更新请求
     * @return 更新后的用户档案信息
     */
    UserProfileDto updateProfile(UpdateUserProfileRequest request);

    // ==== 用户偏好设置服务方法 ====

    /**
     * 获取用户偏好设置
     * @return 用户偏好设置响应
     */
    UserPreferencesResponse getPreferences();

    /**
     * 批量更新用户偏好设置
     * @param request 偏好设置请求
     * @return 更新后的偏好设置响应
     */
    UserPreferencesResponse updatePreferences(UserPreferencesRequest request);

    /**
     * 局部更新用户偏好设置中的特定配置项
     * @param category 配置项类别
     * @param preference 配置项内容
     * @return 更新后的偏好设置响应
     */
    UserPreferencesResponse updatePreferenceItem(String category, Map<String, Object> preference);

    // ==== 主题配置服务方法 ====

    /**
     * 获取用户主题配置
     * @return 用户主题配置响应
     */
    UserThemeResponse getTheme();

    /**
     * 更新用户主题配置
     * @param request 主题配置请求
     * @return 更新后的主题配置响应
     */
    UserThemeResponse updateTheme(UserThemeRequest request);

    // ==== 通知设置服务方法 ====

    /**
     * 获取用户通知设置
     * @return 用户通知设置响应
     */
    UserNotificationResponse getNotificationSettings();

    /**
     * 更新用户通知设置
     * @param request 通知设置请求
     * @return 更新后的通知设置响应
     */
    UserNotificationResponse updateNotificationSettings(UserNotificationRequest request);

    // ==== 数据导出设置服务方法 ====

    /**
     * 获取用户数据导出设置
     * @return 用户导出设置响应
     */
    UserExportResponse getExportSettings();

    /**
     * 更新用户数据导出设置
     * @param request 导出设置请求
     * @return 更新后的导出设置响应
     */
    UserExportResponse updateExportSettings(UserExportRequest request);

    /**
     * 执行数据导出操作
     * @param exportType 导出类型选择
     * @return 导出结果信息
     */
    String executeDataExport(String exportType);
}
