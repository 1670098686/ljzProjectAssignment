package com.campus.trade.controller.admin;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.admin.DashboardOverviewResponse;
import com.campus.trade.dto.admin.DashboardOverviewResponse.RankingItem;
import com.campus.trade.dto.admin.DashboardOverviewResponse.TrendSeries;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(dashboardService.getOverview(clampDays(days)));
    }

    @GetMapping("/trends")
    public ApiResponse<TrendSeries> trends(@RequestParam(defaultValue = "orders") String metric,
                                           @RequestParam(defaultValue = "7d") String period) {
        return ApiResponse.success(dashboardService.getTrendSeries(metric, parsePeriod(period)));
    }

    @GetMapping("/rankings")
    public ApiResponse<List<RankingItem>> rankings(@RequestParam(defaultValue = "product-category") String type,
                                                   @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(dashboardService.getRankings(type, limit));
    }

    private int clampDays(int days) {
        return Math.min(Math.max(days, 1), 30);
    }

    private int parsePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return 7;
        }
        String normalized = period.trim().toLowerCase();
        if (normalized.endsWith("d")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return clampDays(Integer.parseInt(normalized));
        } catch (NumberFormatException ignored) {
            return 7;
        }
    }
}
