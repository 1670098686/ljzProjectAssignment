package com.campus.trade.model.entity;

import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import com.campus.trade.util.JsonListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reports")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_snapshot", length = 200)
    private String targetSnapshot;

    @Column(nullable = false, length = 200)
    private String reason;

    @Lob
    private String description;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "evidence_urls", columnDefinition = "json")
    private List<String> evidenceUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "contact_info", length = 100)
    private String contactInfo;

    @Column(length = 200)
    private String resolution;

    @Column(name = "handled_by", length = 50)
    private String handledBy;

    @Column(name = "handled_time")
    private LocalDateTime handledTime;

    @Column(name = "auto_flagged")
    private boolean autoFlagged;

    @Column(name = "auto_reason", length = 200)
    private String autoReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
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
}
