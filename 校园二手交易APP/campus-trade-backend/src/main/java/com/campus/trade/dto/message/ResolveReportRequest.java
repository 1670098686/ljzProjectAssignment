package com.campus.trade.dto.message;

import com.campus.trade.model.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public class ResolveReportRequest {

    @NotNull
    private ReportStatus status;

    private String resolution;

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
}
