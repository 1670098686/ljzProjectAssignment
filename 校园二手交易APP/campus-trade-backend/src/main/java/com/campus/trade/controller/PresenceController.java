package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.presence.PresenceStatusResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/presence")
@Tag(name = "在线状态", description = "实时在线状态接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/online-users")
    @Operation(summary = "获取在线用户列表", description = "查询当前在线用户及最近活跃时间")
    public ApiResponse<List<PresenceStatusResponse>> onlineUsers() {
        return ApiResponse.success(presenceService.listOnlineUsers());
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "查询用户在线状态", description = "按用户ID查询在线/离线状态")
    public ApiResponse<PresenceStatusResponse> status(@PathVariable Long userId) {
        return ApiResponse.success(presenceService.getStatus(userId));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户在线状态", description = "返回当前登录用户的在线状态")
    public ApiResponse<PresenceStatusResponse> me() {
        return ApiResponse.success(presenceService.getStatusByUsername(SecurityUtils.getCurrentUsername()));
    }
}
