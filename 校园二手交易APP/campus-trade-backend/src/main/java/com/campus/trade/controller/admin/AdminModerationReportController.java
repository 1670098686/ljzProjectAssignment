package com.campus.trade.controller.admin;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.report.ReportAdminQuery;
import com.campus.trade.dto.report.ReportAuditRequest;
import com.campus.trade.dto.report.ReportBatchAuditRequest;
import com.campus.trade.dto.report.ReportDetailResponse;
import com.campus.trade.dto.report.ReportResponse;
import com.campus.trade.dto.report.ReportStatsResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/moderation/reports")
@Tag(name = "举报审核", description = "管理端举报审核与统计")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminModerationReportController {

    private final ReportService reportService;

    public AdminModerationReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(summary = "举报列表", description = "按条件筛选举报记录")
    public ApiResponse<PaginatedResponse<ReportResponse>> reports(@Valid @ModelAttribute ReportAdminQuery query) {
        return ApiResponse.success(reportService.listReports(query));
    }

    @GetMapping("/stats")
    @Operation(summary = "举报统计", description = "返回举报状态、自动预警等统计信息")
    public ApiResponse<ReportStatsResponse> stats() {
        return ApiResponse.success(reportService.stats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "举报详情", description = "查看举报详情与证据")
    public ApiResponse<ReportDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(reportService.getReportDetail(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "处理举报", description = "审核并给出处理结论")
    public ApiResponse<Void> audit(@PathVariable Long id, @Valid @RequestBody ReportAuditRequest request) {
        reportService.auditReport(id, SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @PostMapping("/batch")
    @Operation(summary = "批量处理举报", description = "对多条举报同时给出处理结论")
    public ApiResponse<BatchOperationResult> batchAudit(@Valid @RequestBody ReportBatchAuditRequest request) {
        return ApiResponse.success(reportService.batchAudit(
                request.getIds(),
                SecurityUtils.getCurrentUsername(),
                request.toAuditRequest()));
    }
}
