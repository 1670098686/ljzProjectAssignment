package com.campus.trade.dto.report;

import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class ReportAdminQuery {

    private ReportStatus status;
    private ReportTargetType targetType;
    private String reporterKeyword;
    private Boolean autoFlaggedOnly;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ReportTargetType targetType) {
        this.targetType = targetType;
    }

    public String getReporterKeyword() {
        return reporterKeyword;
    }

    public void setReporterKeyword(String reporterKeyword) {
        this.reporterKeyword = reporterKeyword;
    }

    public Boolean getAutoFlaggedOnly() {
        return autoFlaggedOnly;
    }

    public void setAutoFlaggedOnly(Boolean autoFlaggedOnly) {
        this.autoFlaggedOnly = autoFlaggedOnly;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
