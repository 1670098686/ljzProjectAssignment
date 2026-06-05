package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.user.NotificationSettingRequest;
import com.campus.trade.dto.user.NotificationSettingResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/notification-settings")
@Tag(name = "通知设置接口", description = "用户通知偏好设置")
@PreAuthorize(AccessExpressions.MEMBER)
public class UserNotificationSettingController {

    private final NotificationSettingService service;

    public UserNotificationSettingController(NotificationSettingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "获取通知设置")
    public ApiResponse<NotificationSettingResponse> get() {
        return ApiResponse.success(service.getOrCreate(SecurityUtils.getCurrentUsername()));
    }

    @PutMapping
    @Operation(summary = "更新通知设置")
    public ApiResponse<NotificationSettingResponse> update(@RequestBody NotificationSettingRequest request) {
        return ApiResponse.success(service.update(SecurityUtils.getCurrentUsername(), request));
    }
}
