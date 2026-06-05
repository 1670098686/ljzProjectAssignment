package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.report.ReportAdminQuery;
import com.campus.trade.dto.report.ReportAuditRequest;
import com.campus.trade.dto.report.ReportCreateRequest;
import com.campus.trade.dto.report.ReportDetailResponse;
import com.campus.trade.dto.report.ReportListRequest;
import com.campus.trade.dto.report.ReportResponse;
import com.campus.trade.dto.report.ReportStatsResponse;

import java.util.List;

public interface ReportService {

    void createReport(String username, ReportCreateRequest request);

    PaginatedResponse<ReportResponse> listMyReports(String username, ReportListRequest request);

    ReportDetailResponse getMyReport(String username, Long id);

    PaginatedResponse<ReportResponse> listReports(ReportAdminQuery query);

    ReportDetailResponse getReportDetail(Long id);

    ReportStatsResponse stats();

    void auditReport(Long id, String adminUsername, ReportAuditRequest request);

    BatchOperationResult batchAudit(List<Long> ids, String adminUsername, ReportAuditRequest request);
}
