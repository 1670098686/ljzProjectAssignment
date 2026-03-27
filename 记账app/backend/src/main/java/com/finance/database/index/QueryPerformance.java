package com.finance.database.index;

/**
 * 查询性能信息
 */
public class QueryPerformance {
    
    private String queryPattern;          // 查询模式
    private long executionCount;          // 执行次数
    private double avgExecutionTime;      // 平均执行时间(秒)
    private double maxExecutionTime;      // 最大执行时间(秒)
    private long rowsExamined;            // 检查的行数
    private long rowsSent;                // 发送的行数
    private double efficiency;            // 效率 (rowsSent/rowsExamined)
    
    // Getters and Setters
    public String getQueryPattern() { return queryPattern; }
    public void setQueryPattern(String queryPattern) { this.queryPattern = queryPattern; }
    
    public long getExecutionCount() { return executionCount; }
    public void setExecutionCount(long executionCount) { this.executionCount = executionCount; }
    
    public double getAvgExecutionTime() { return avgExecutionTime; }
    public void setAvgExecutionTime(double avgExecutionTime) { this.avgExecutionTime = avgExecutionTime; }
    
    public double getMaxExecutionTime() { return maxExecutionTime; }
    public void setMaxExecutionTime(double maxExecutionTime) { this.maxExecutionTime = maxExecutionTime; }
    
    public long getRowsExamined() { return rowsExamined; }
    public void setRowsExamined(long rowsExamined) { this.rowsExamined = rowsExamined; }
    
    public long getRowsSent() { return rowsSent; }
    public void setRowsSent(long rowsSent) { this.rowsSent = rowsSent; }
    
    public double getEfficiency() { return efficiency; }
    public void setEfficiency(double efficiency) { this.efficiency = efficiency; }
    
    /**
     * 判断是否为慢查询
     */
    public boolean isSlowQuery() {
        return avgExecutionTime > 1.0; // 平均执行时间超过1秒
    }
}