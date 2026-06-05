package com.campus.trade.dto.report;

import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ReportListRequest {

    private ReportStatus status;
    private ReportTargetType targetType;

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(50)
    private int size = 10;

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
