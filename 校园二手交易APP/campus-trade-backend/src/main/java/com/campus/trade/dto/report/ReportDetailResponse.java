package com.campus.trade.dto.report;

import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import com.campus.trade.dto.user.UserSummary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportDetailResponse {

    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private String targetSnapshot;
    private String reason;
    private String description;
    private List<String> evidenceUrls = new ArrayList<>();
    private ReportStatus status;
    private String contactInfo;
    private String resolution;
    private String handledBy;
    private LocalDateTime handledTime;
    private LocalDateTime createTime;
    private boolean autoFlagged;
    private String autoReason;
    private UserSummary reporter;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ReportTargetType targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetSnapshot() {
        return targetSnapshot;
    }

    public void setTargetSnapshot(String targetSnapshot) {
        this.targetSnapshot = targetSnapshot;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(List<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledTime() {
        return handledTime;
    }

    public void setHandledTime(LocalDateTime handledTime) {
        this.handledTime = handledTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean isAutoFlagged() {
        return autoFlagged;
    }

    public void setAutoFlagged(boolean autoFlagged) {
        this.autoFlagged = autoFlagged;
    }

    public String getAutoReason() {
        return autoReason;
    }

    public void setAutoReason(String autoReason) {
        this.autoReason = autoReason;
    }

    public UserSummary getReporter() {
        return reporter;
    }

    public void setReporter(UserSummary reporter) {
        this.reporter = reporter;
    }
}
