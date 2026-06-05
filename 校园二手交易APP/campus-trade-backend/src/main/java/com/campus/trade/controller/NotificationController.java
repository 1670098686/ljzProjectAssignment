package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.message.SystemNotificationResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "系统通知", description = "系统通知列表与已读状态接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "我的系统通知", description = "分页获取当前用户的系统通知列表")
    public ApiResponse<PaginatedResponse<SystemNotificationResponse>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") @Min(1) int size) {
        String username = SecurityUtils.getCurrentUsername();
        return ApiResponse.success(notificationService.listNotifications(username, page, size));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    public ApiResponse<Void> markRead(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        notificationService.markRead(username, id);
        return ApiResponse.success();
    }
}
