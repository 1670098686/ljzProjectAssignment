package com.campus.trade.dto.admin;

public class RuntimeMetricsResponse {

    private long uptimeSeconds;
    private long heapUsedBytes;
    private long heapMaxBytes;
    private long nonHeapUsedBytes;
    private int threadCount;
    private double processCpuLoad;
    private double systemCpuLoad;

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public long getHeapUsedBytes() {
        return heapUsedBytes;
    }

    public void setHeapUsedBytes(long heapUsedBytes) {
        this.heapUsedBytes = heapUsedBytes;
    }

    public long getHeapMaxBytes() {
        return heapMaxBytes;
    }

    public void setHeapMaxBytes(long heapMaxBytes) {
        this.heapMaxBytes = heapMaxBytes;
    }

    public long getNonHeapUsedBytes() {
        return nonHeapUsedBytes;
    }

    public void setNonHeapUsedBytes(long nonHeapUsedBytes) {
        this.nonHeapUsedBytes = nonHeapUsedBytes;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public double getProcessCpuLoad() {
        return processCpuLoad;
    }

    public void setProcessCpuLoad(double processCpuLoad) {
        this.processCpuLoad = processCpuLoad;
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(double systemCpuLoad) {
        this.systemCpuLoad = systemCpuLoad;
    }
}
