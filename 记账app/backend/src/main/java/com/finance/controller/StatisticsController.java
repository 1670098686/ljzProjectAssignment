package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.dto.CategoryStatisticsDto;
import com.finance.dto.StatisticsGranularity;
import com.finance.dto.SummaryStatisticsDto;
import com.finance.dto.TrendPointDto;
import com.finance.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "统计报表管理", description = "财务数据统计和报表分析接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "获取月度汇总统计", description = "获取指定月份的收入、支出、结余等汇总统计信息")
    @OperationLog(value = "GET_MONTHLY_SUMMARY", description = "获取月度汇总统计", recordParams = false, recordResult = false)
    public ApiResponse<SummaryStatisticsDto> getSummary(
            @Parameter(description = "年份，格式：yyyy") @RequestParam int year,
            @Parameter(description = "月份，1-12") @RequestParam int month) {
        return ApiResponse.success(statisticsService.getMonthlySummary(year, month));
    }

    @GetMapping("/by-category")
    @Operation(
        summary = "获取分类统计", 
        description = "按分类统计指定时间范围内的交易数据，按收入支出类型分类汇总"
    )
    @OperationLog(value = "GET_CATEGORY_STATISTICS", description = "获取分类统计", recordParams = false, recordResult = false)
    public ApiResponse<List<CategoryStatisticsDto>> getByCategory(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd，示例：2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd，示例：2024-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "交易类型：1=收入，2=支出") @RequestParam(required = false) Integer type) {
        return ApiResponse.success(statisticsService.getCategoryStatistics(startDate, endDate, type));
    }

    @GetMapping("/trend")
    @Operation(summary = "获取趋势分析", description = "获取指定时间范围内的交易趋势数据，支持按日、周、月粒度分析")
    @OperationLog(value = "GET_TREND_ANALYSIS", description = "获取趋势分析", recordParams = false, recordResult = false)
    public ApiResponse<List<TrendPointDto>> getTrend(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "统计粒度：daily=日，weekly=周，monthly=月，默认daily") @RequestParam(value = "granularity", defaultValue = "daily") String granularity) {
        return ApiResponse.success(statisticsService.getTrend(startDate, endDate, StatisticsGranularity.from(granularity)));
    }
}
