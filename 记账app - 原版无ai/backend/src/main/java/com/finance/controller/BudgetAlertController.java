package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.dto.BudgetAlertConfigRequest;
import com.finance.dto.BudgetAlertConfigResponse;
import com.finance.dto.BudgetAlertDto;
import com.finance.dto.BudgetAlertHistoryRequest;
import com.finance.dto.BudgetAlertHistoryResponse;
import com.finance.service.BudgetAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budget/alert")
@Tag(name = "预算预警管理", description = "预算预警管理接口，提供预算超支预警检查、预警列表查询、预警规则配置、预警历史记录等功能")
public class BudgetAlertController {

    private final BudgetAlertService budgetAlertService;

    public BudgetAlertController(BudgetAlertService budgetAlertService) {
        this.budgetAlertService = budgetAlertService;
    }

    @GetMapping
    @Operation(
        summary = "获取预算预警列表", 
        description = "获取当前月份的预算预警信息，包含已超预算和接近预算的分类预警"
    )
    @OperationLog(value = "GET_BUDGET_ALERTS", description = "获取预算预警列表", recordParams = false, recordResult = false)
    public ApiResponse<List<BudgetAlertDto>> getBudgetAlerts() {
        // 使用当前年月，默认阈值，不推送通知
        YearMonth now = YearMonth.now();
        return ApiResponse.success(budgetAlertService.getBudgetAlerts(
                now.getYear(), now.getMonthValue(), 
                new BigDecimal("0.8"), new BigDecimal("0.9"), false
        ));
    }

    @GetMapping("/check")
    @Operation(
        summary = "检查预算预警", 
        description = "检查当前月份的预算使用情况并生成预警，返回新生成的预警信息"
    )
    @OperationLog(value = "CHECK_BUDGET_ALERTS", description = "检查预算预警", businessType = "BUDGET_ALERT")
    public ApiResponse<List<BudgetAlertDto>> checkBudgetAlerts() {
        // 使用当前年月，默认阈值，推送通知
        YearMonth now = YearMonth.now();
        return ApiResponse.success(budgetAlertService.getBudgetAlerts(
                now.getYear(), now.getMonthValue(), 
                new BigDecimal("0.8"), new BigDecimal("0.9"), true
        ));
    }

    @PostMapping("/config")
    @Operation(
        summary = "配置预警规则", 
        description = "设置预算预警规则，包括预警阈值、推送通知配置等"
    )
    @OperationLog(value = "CONFIGURE_BUDGET_ALERT", description = "配置预警规则", businessType = "BUDGET_ALERT")
    public ApiResponse<BudgetAlertConfigResponse> configureBudgetAlert(
            @Parameter(description = "预警规则配置请求体") @Valid @RequestBody BudgetAlertConfigRequest request) {
        
        BudgetAlertConfigResponse config = budgetAlertService.configureAlertRule(request);
        return ApiResponse.success(config);
    }

    @GetMapping("/config")
    @Operation(
        summary = "获取预警规则配置", 
        description = "获取当前用户的预警规则配置"
    )
    @OperationLog(value = "GET_ALERT_CONFIG", description = "获取预警规则配置", recordParams = false, recordResult = false)
    public ApiResponse<BudgetAlertConfigResponse> getAlertConfig() {
        BudgetAlertConfigResponse config = budgetAlertService.getAlertConfig();
        return ApiResponse.success(config);
    }

    @GetMapping("/history")
    @Operation(
        summary = "查询预警历史记录", 
        description = "分页查询预算预警历史记录，支持按时间、级别、分类等条件筛选"
    )
    @OperationLog(value = "GET_ALERT_HISTORY", description = "查询预警历史记录", recordParams = false, recordResult = false)
    public ApiResponse<BudgetAlertHistoryResponse.BudgetAlertHistoryListResponse> getAlertHistory(
            @Parameter(description = "预警历史查询请求体") @Valid @ModelAttribute BudgetAlertHistoryRequest request) {
        
        BudgetAlertHistoryResponse.BudgetAlertHistoryListResponse history = budgetAlertService.getAlertHistory(request);
        return ApiResponse.success(history);
    }
}
