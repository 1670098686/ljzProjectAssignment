package com.campus.trade.controller.admin;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.AdminOrderReportResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize(AccessExpressions.ADMIN)
@Tag(name = "后台报表", description = "管理端报表统计接口")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping("/orders")
    @Operation(summary = "订单报表", description = "返回订单与交易相关的统计报表数据")
    public ApiResponse<AdminOrderReportResponse> orderReport(
            @Parameter(description = "统计天数，默认 7，最大 30") @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "Top N 排名条数，默认 5，最大 10") @RequestParam(defaultValue = "5") int top) {
        return ApiResponse.success(adminReportService.getOrderReport(days, top));
    }
}
