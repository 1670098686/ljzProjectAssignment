package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.report.ReportCreateRequest;
import com.campus.trade.dto.report.ReportDetailResponse;
import com.campus.trade.dto.report.ReportListRequest;
import com.campus.trade.dto.report.ReportResponse;
import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "举报反馈", description = "举报提交、进度查询与结果反馈接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "提交举报", description = "举报违规商品、用户、订单或聊天消息")
    public ApiResponse<Void> createReport(@Valid @RequestBody ReportCreateRequest request) {
        reportService.createReport(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @GetMapping
    @Operation(summary = "我的举报", description = "分页查看当前用户发起的举报记录")
    public ApiResponse<PaginatedResponse<ReportResponse>> listReports(
            @Parameter(description = "举报状态过滤") @RequestParam(required = false) ReportStatus status,
            @Parameter(description = "举报对象类型") @RequestParam(required = false) ReportTargetType targetType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        ReportListRequest query = new ReportListRequest();
        query.setStatus(status);
        query.setTargetType(targetType);
        query.setPage(page);
        query.setSize(size);
        return ApiResponse.success(reportService.listMyReports(SecurityUtils.getCurrentUsername(), query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "举报详情", description = "查看指定举报的处理进度与结果")
    public ApiResponse<ReportDetailResponse> getReport(@PathVariable Long id) {
        return ApiResponse.success(reportService.getMyReport(SecurityUtils.getCurrentUsername(), id));
    }
}
