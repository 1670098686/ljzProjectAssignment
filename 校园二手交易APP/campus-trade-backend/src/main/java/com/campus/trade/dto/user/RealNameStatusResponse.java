package com.campus.trade.dto.user;

import com.campus.trade.model.enums.VerificationStatus;
import java.time.LocalDateTime;

public class RealNameStatusResponse {

    private VerificationStatus status;
    private String realName;
    private String idNumberLast4;
    private LocalDateTime submittedAt;
    private String rejectReason;

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdNumberLast4() {
        return idNumberLast4;
    }

    public void setIdNumberLast4(String idNumberLast4) {
        this.idNumberLast4 = idNumberLast4;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
