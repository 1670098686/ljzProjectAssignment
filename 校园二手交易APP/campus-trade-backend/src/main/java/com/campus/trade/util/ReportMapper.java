package com.campus.trade.util;

import com.campus.trade.dto.report.ReportDetailResponse;
import com.campus.trade.dto.report.ReportResponse;
import com.campus.trade.model.entity.Report;

public final class ReportMapper {

    private ReportMapper() {
    }

    public static ReportResponse toResponse(Report report) {
        if (report == null) {
            return null;
        }
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setTargetSnapshot(report.getTargetSnapshot());
        response.setReason(report.getReason());
        response.setDescription(report.getDescription());
        response.setEvidenceUrls(report.getEvidenceUrls());
        response.setStatus(report.getStatus());
        response.setContactInfo(report.getContactInfo());
        response.setResolution(report.getResolution());
        response.setHandledBy(report.getHandledBy());
        response.setHandledTime(report.getHandledTime());
        response.setCreateTime(report.getCreateTime());
        response.setAutoFlagged(report.isAutoFlagged());
        response.setAutoReason(report.getAutoReason());
        response.setReporter(UserMapper.toMaskedSummary(report.getReporter()));
        return response;
    }

    public static ReportDetailResponse toDetail(Report report) {
        if (report == null) {
            return null;
        }
        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(report.getId());
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setTargetSnapshot(report.getTargetSnapshot());
        response.setReason(report.getReason());
        response.setDescription(report.getDescription());
        response.setEvidenceUrls(report.getEvidenceUrls());
        response.setStatus(report.getStatus());
        response.setContactInfo(report.getContactInfo());
        response.setResolution(report.getResolution());
        response.setHandledBy(report.getHandledBy());
        response.setHandledTime(report.getHandledTime());
        response.setCreateTime(report.getCreateTime());
        response.setAutoFlagged(report.isAutoFlagged());
        response.setAutoReason(report.getAutoReason());
        response.setReporter(UserMapper.toMaskedSummary(report.getReporter()));
        return response;
    }
}
