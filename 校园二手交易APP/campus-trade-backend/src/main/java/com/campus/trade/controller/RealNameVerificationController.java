package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.user.RealNameStatusResponse;
import com.campus.trade.dto.user.RealNameSubmitRequest;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.RealNameVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/real-name")
@Tag(name = "实名认证接口", description = "用户实名认证提交与状态查询")
@PreAuthorize(AccessExpressions.MEMBER)
public class RealNameVerificationController {

    private final RealNameVerificationService service;

    public RealNameVerificationController(RealNameVerificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "获取实名认证状态")
    public ApiResponse<RealNameStatusResponse> status() {
        return ApiResponse.success(service.getStatus(SecurityUtils.getCurrentUsername()));
    }

    @PostMapping
    @Operation(summary = "提交实名认证")
    public ApiResponse<RealNameStatusResponse> submit(@Valid @RequestBody RealNameSubmitRequest request) {
        return ApiResponse.success(service.submit(SecurityUtils.getCurrentUsername(), request));
    }
}
