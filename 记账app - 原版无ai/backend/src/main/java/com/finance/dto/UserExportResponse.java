package com.finance.dto;

/**
 * 用户数据导出响应DTO
 */
public class UserExportResponse {
    
    /**
     * 导出是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 导出文件路径
     */
    private String exportPath;
    
    /**
     * 导出格式
     */
    private String exportFormat;
    
    /**
     * 导出分类
     */
    private String category;
    
    /**
     * 更新时间
     */
    private java.time.LocalDateTime updatedTime;

    // Getter methods
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getExportPath() {
        return exportPath;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public String getCategory() {
        return category;
    }

    // Setter methods
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    public java.time.LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
    
    public void setUpdatedTime(java.time.LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}