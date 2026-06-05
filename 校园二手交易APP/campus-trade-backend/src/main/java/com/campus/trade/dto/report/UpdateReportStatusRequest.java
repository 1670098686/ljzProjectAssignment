package com.campus.trade.dto.report;

import com.campus.trade.model.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateReportStatusRequest {

    @NotNull
    private ReportStatus status;

    @Size(max = 200)
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
