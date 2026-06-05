package com.campus.trade.controller.admin;

import com.campus.trade.annotation.OperationLog;
import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.SystemConfigCreateRequest;
import com.campus.trade.dto.admin.SystemConfigResponse;
import com.campus.trade.dto.admin.SystemConfigUpdateRequest;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.model.enums.SystemConfigScope;
import com.campus.trade.model.enums.SystemConfigStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system-configs")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminSystemConfigController {

    private final SystemConfigService service;

    public AdminSystemConfigController(SystemConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SystemConfigResponse>> list(
            @RequestParam(required = false) SystemConfigScope scope,
            @RequestParam(required = false) SystemConfigStatus status) {
        return ApiResponse.success(service.list(scope, status));
    }

    @PostMapping
    @OperationLog(title = "系统配置", action = "SYSTEM_CONFIG_CREATE", type = OperationType.CREATE)
    public ApiResponse<SystemConfigResponse> create(@Valid @RequestBody SystemConfigCreateRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PatchMapping("/{id}")
    @OperationLog(title = "系统配置", action = "SYSTEM_CONFIG_UPDATE", type = OperationType.UPDATE)
    public ApiResponse<SystemConfigResponse> update(@PathVariable Long id, @Valid @RequestBody SystemConfigUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
}
