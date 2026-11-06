package com.finance.dto;

/**
 * 用户数据导出请求DTO
 */
public class UserExportRequest {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 导出格式（csv, json）
     */
    private String exportFormat;
    
    /**
     * 导出分类（transactions, budgets, saving_goals, statistics, all）
     */
    private String category;
    
    /**
     * 时间范围
     */
    private String timeRange;
    
    /**
     * 导出路径
     */
    private String exportPath;
    
    /**
     * 是否压缩导出
     */
    private boolean compressExport;

    // Getter methods
    public Long getUserId() {
        return userId;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public String getCategory() {
        return category;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getExportPath() {
        return exportPath;
    }

    public boolean isCompressExport() {
        return compressExport;
    }

    // Setter methods
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public void setCompressExport(boolean compressExport) {
        this.compressExport = compressExport;
    }
}