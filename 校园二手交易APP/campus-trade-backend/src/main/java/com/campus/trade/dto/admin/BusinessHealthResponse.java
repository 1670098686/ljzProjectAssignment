package com.campus.trade.dto.admin;

public class BusinessHealthResponse {

    private long pendingAudits;
    private long pendingOrders;
    private long refundRequests;
    private double cacheHitRatio;
    private long messageBacklog;

    public long getPendingAudits() {
        return pendingAudits;
    }

    public void setPendingAudits(long pendingAudits) {
        this.pendingAudits = pendingAudits;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getRefundRequests() {
        return refundRequests;
    }

    public void setRefundRequests(long refundRequests) {
        this.refundRequests = refundRequests;
    }

    public double getCacheHitRatio() {
        return cacheHitRatio;
    }

    public void setCacheHitRatio(double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    public long getMessageBacklog() {
        return messageBacklog;
    }

    public void setMessageBacklog(long messageBacklog) {
        this.messageBacklog = messageBacklog;
    }
}
