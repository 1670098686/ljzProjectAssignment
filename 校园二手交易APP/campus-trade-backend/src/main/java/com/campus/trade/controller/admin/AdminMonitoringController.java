package com.campus.trade.controller.admin;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.BusinessHealthResponse;
import com.campus.trade.dto.admin.PerformanceMetricsResponse;
import com.campus.trade.dto.admin.RuntimeMetricsResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.AdminMonitoringService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/monitor")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminMonitoringController {

    private final AdminMonitoringService monitoringService;

    public AdminMonitoringController(AdminMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/runtime")
    public ApiResponse<RuntimeMetricsResponse> runtime() {
        return ApiResponse.success(monitoringService.getRuntimeMetrics());
    }

    @GetMapping("/performance")
    public ApiResponse<PerformanceMetricsResponse> performance() {
        return ApiResponse.success(monitoringService.getPerformanceMetrics());
    }

    @GetMapping("/business")
    public ApiResponse<BusinessHealthResponse> business() {
        return ApiResponse.success(monitoringService.getBusinessHealthSnapshot());
    }

    @PostMapping("/alerts/test")
    public ApiResponse<String> triggerAlertSimulation() {
        monitoringService.triggerTestAlert();
        return ApiResponse.successMessage("告警链路触发成功");
    }
}
