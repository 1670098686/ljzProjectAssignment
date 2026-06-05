package com.campus.trade.dto.report;

import com.campus.trade.model.enums.ReportStatus;

public class ReportStatusSummary {

    private ReportStatus status;
    private long count;

    public ReportStatusSummary() {
    }

    public ReportStatusSummary(ReportStatus status, long count) {
        this.status = status;
        this.count = count;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
