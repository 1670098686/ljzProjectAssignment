package com.campus.trade.dto.report;

import java.util.ArrayList;
import java.util.List;

public class ReportStatsResponse {

    private long totalReports;
    private long pendingReports;
    private long inProgressReports;
    private long resolvedReports;
    private long rejectedReports;
    private long autoFlaggedReports;
    private long todayReports;
    private List<ReportStatusSummary> distribution = new ArrayList<>();

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(long pendingReports) {
        this.pendingReports = pendingReports;
    }

    public long getInProgressReports() {
        return inProgressReports;
    }

    public void setInProgressReports(long inProgressReports) {
        this.inProgressReports = inProgressReports;
    }

    public long getResolvedReports() {
        return resolvedReports;
    }

    public void setResolvedReports(long resolvedReports) {
        this.resolvedReports = resolvedReports;
    }

    public long getRejectedReports() {
        return rejectedReports;
    }

    public void setRejectedReports(long rejectedReports) {
        this.rejectedReports = rejectedReports;
    }

    public long getAutoFlaggedReports() {
        return autoFlaggedReports;
    }

    public void setAutoFlaggedReports(long autoFlaggedReports) {
        this.autoFlaggedReports = autoFlaggedReports;
    }

    public long getTodayReports() {
        return todayReports;
    }

    public void setTodayReports(long todayReports) {
        this.todayReports = todayReports;
    }

    public List<ReportStatusSummary> getDistribution() {
        return distribution;
    }

    public void setDistribution(List<ReportStatusSummary> distribution) {
        this.distribution = distribution;
    }
}
