package com.campus.trade.dto.report;

import com.campus.trade.model.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ReportBatchAuditRequest {

    @NotEmpty
    private List<Long> ids;

    @NotNull
    private ReportStatus status;

    @NotBlank
    @Size(max = 200)
    private String resolution;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
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

    public ReportAuditRequest toAuditRequest() {
        ReportAuditRequest auditRequest = new ReportAuditRequest();
        auditRequest.setStatus(this.status);
        auditRequest.setResolution(this.resolution);
        return auditRequest;
    }
}
