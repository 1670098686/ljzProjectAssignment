package com.campus.trade.dto.message;

import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.enums.ReportStatus;

import java.time.LocalDateTime;

public class MessageReportResponse {

    private Long id;
    private Long messageId;
    private UserSummary reporter;
    private String reason;
    private String evidenceUrl;
    private ReportStatus status;
    private String resolution;
    private LocalDateTime createTime;
    private LocalDateTime resolvedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public UserSummary getReporter() {
        return reporter;
    }

    public void setReporter(UserSummary reporter) {
        this.reporter = reporter;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }
}
