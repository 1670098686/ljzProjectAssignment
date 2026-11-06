package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
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
import com.finance.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "用户档案管理", description = "用户个人档案和配置管理接口，支持用户基本信息的查询和更新、偏好设置、主题配置等")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    @Operation(
        summary = "获取用户档案", 
        description = "获取当前登录用户的个人档案信息，包括用户名、邮箱、头像等个人信息"
    )
    @OperationLog(value = "GET_USER_PROFILE", description = "获取用户档案", recordParams = false, recordResult = false)
    public ApiResponse<UserProfileDto> getProfile() {
        return ApiResponse.success(userProfileService.getCurrentProfile());
    }

    @PutMapping("/profile")
    @Operation(
        summary = "更新用户档案", 
        description = "更新当前登录用户的个人档案信息，支持修改用户名、邮箱、头像等个人信息"
    )
    @OperationLog(value = "UPDATE_USER_PROFILE", description = "更新用户档案", businessType = "USER_PROFILE")
    public ApiResponse<UserProfileDto> updateProfile(
            @Parameter(description = "用户档案更新请求体，包含需要更新的用户信息") @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userProfileService.updateProfile(request));
    }

    // ==== 用户偏好设置接口 ====

    @GetMapping("/preferences")
    @Operation(
        summary = "获取用户偏好设置",
        description = "获取当前登录用户的偏好设置，包括应用行为偏好、功能开关等"
    )
    @OperationLog(value = "GET_USER_PREFERENCES", description = "获取用户偏好设置", recordParams = false, recordResult = false)
    public ApiResponse<UserPreferencesResponse> getPreferences() {
        return ApiResponse.success(userProfileService.getPreferences());
    }

    @PutMapping("/preferences")
    @Operation(
        summary = "更新用户偏好设置",
        description = "更新当前登录用户的偏好设置，支持批量或分类更新"
    )
    @OperationLog(value = "UPDATE_USER_PREFERENCES", description = "更新用户偏好设置", businessType = "USER_PROFILE")
    public ApiResponse<UserPreferencesResponse> updatePreferences(
            @Parameter(description = "用户偏好设置请求体") @Valid @RequestBody UserPreferencesRequest request) {
        return ApiResponse.success(userProfileService.updatePreferences(request));
    }

    @PatchMapping("/preferences")
    @Operation(
        summary = "局部更新用户偏好设置",
        description = "局部更新用户偏好设置中的特定配置项"
    )
    @OperationLog(value = "UPDATE_USER_PREFERENCE_ITEM", description = "更新用户偏好项", businessType = "USER_PROFILE")
    public ApiResponse<UserPreferencesResponse> updatePreferenceItem(
            @Parameter(description = "配置项类别") @RequestParam String category,
            @Parameter(description = "配置项内容") @RequestBody Map<String, Object> preference) {
        return ApiResponse.success(userProfileService.updatePreferenceItem(category, preference));
    }

    // ==== 主题配置接口 ====

    @GetMapping("/theme")
    @Operation(
        summary = "获取用户主题配置",
        description = "获取当前登录用户的主题配置，包括颜色主题、字体设置等"
    )
    @OperationLog(value = "GET_USER_THEME", description = "获取用户主题", recordParams = false, recordResult = false)
    public ApiResponse<UserThemeResponse> getTheme() {
        return ApiResponse.success(userProfileService.getTheme());
    }

    @PutMapping("/theme")
    @Operation(
        summary = "更新用户主题配置",
        description = "更新当前登录用户的主题配置，支持自定义颜色、字体等设置"
    )
    @OperationLog(value = "UPDATE_USER_THEME", description = "更新用户主题", businessType = "USER_PROFILE")
    public ApiResponse<UserThemeResponse> updateTheme(
            @Parameter(description = "用户主题配置请求体") @Valid @RequestBody UserThemeRequest request) {
        return ApiResponse.success(userProfileService.updateTheme(request));
    }

    // ==== 通知设置接口 ====

    @GetMapping("/notification")
    @Operation(
        summary = "获取用户通知设置",
        description = "获取当前登录用户的通知设置，包括推送开关、提醒频率等"
    )
    @OperationLog(value = "GET_USER_NOTIFICATION", description = "获取用户通知设置", recordParams = false, recordResult = false)
    public ApiResponse<UserNotificationResponse> getNotificationSettings() {
        return ApiResponse.success(userProfileService.getNotificationSettings());
    }

    @PutMapping("/notification")
    @Operation(
        summary = "更新用户通知设置",
        description = "更新当前登录用户的通知设置，支持配置各种提醒和推送选项"
    )
    @OperationLog(value = "UPDATE_USER_NOTIFICATION", description = "更新用户通知设置", businessType = "USER_PROFILE")
    public ApiResponse<UserNotificationResponse> updateNotificationSettings(
            @Parameter(description = "用户通知设置请求体") @Valid @RequestBody UserNotificationRequest request) {
        return ApiResponse.success(userProfileService.updateNotificationSettings(request));
    }

    // ==== 数据导出设置接口 ====

    @GetMapping("/export")
    @Operation(
        summary = "获取数据导出设置",
        description = "获取当前登录用户的数据导出配置，包括导出格式、频率等设置"
    )
    @OperationLog(value = "GET_USER_EXPORT", description = "获取数据导出设置", recordParams = false, recordResult = false)
    public ApiResponse<UserExportResponse> getExportSettings() {
        return ApiResponse.success(userProfileService.getExportSettings());
    }

    @PutMapping("/export")
    @Operation(
        summary = "更新数据导出设置",
        description = "更新当前登录用户的数据导出配置，支持自定义导出选项和频率"
    )
    @OperationLog(value = "UPDATE_USER_EXPORT", description = "更新数据导出设置", businessType = "USER_PROFILE")
    public ApiResponse<UserExportResponse> updateExportSettings(
            @Parameter(description = "数据导出设置请求体") @Valid @RequestBody UserExportRequest request) {
        return ApiResponse.success(userProfileService.updateExportSettings(request));
    }

    @PostMapping("/export/execute")
    @Operation(
        summary = "执行数据导出",
        description = "根据当前用户的导出设置执行数据导出操作"
    )
    @OperationLog(value = "EXECUTE_DATA_EXPORT", description = "执行数据导出", businessType = "USER_PROFILE", recordParams = true, recordExecutionTime = true)
    public ApiResponse<String> executeDataExport(
            @Parameter(description = "导出类型选择") @RequestParam(required = false) String exportType) {
        return ApiResponse.success(userProfileService.executeDataExport(exportType));
    }
}
