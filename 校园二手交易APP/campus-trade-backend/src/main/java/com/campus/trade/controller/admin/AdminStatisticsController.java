package com.campus.trade.controller.admin;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.AdminStatisticsResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.AdminStatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/statistics/detailed")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    public AdminStatisticsController(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ApiResponse<AdminStatisticsResponse> getOverview(
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 30);
        return ApiResponse.success(statisticsService.getOverview(safeDays));
    }
}
