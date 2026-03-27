package com.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 交易记录导出请求
 */
@Schema(description = "交易记录导出请求")
public class ExportRequest {

    /**
     * 导出格式
     */
    @Schema(description = "导出格式：CSV, EXCEL", example = "CSV")
    @NotNull(message = "导出格式不能为空")
    private ExportFormat format;

    /**
     * 开始日期（可选）
     */
    @Schema(description = "开始日期", example = "2024-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期（可选）
     */
    @Schema(description = "结束日期", example = "2024-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 交易类型过滤：1=收入，2=支出
     */
    @Schema(description = "交易类型：1=收入，2=支出", example = "2")
    @PositiveOrZero(message = "交易类型必须为正数或零")
    private Integer type;

    /**
     * 分类过滤（可选）
     */
    @Schema(description = "分类名称", example = "餐饮")
    private String category;

    /**
     * 最小金额（可选）
     */
    @Schema(description = "最小金额", example = "0.00")
    private BigDecimal minAmount;

    /**
     * 最大金额（可选）
     */
    @Schema(description = "最大金额", example = "1000.00")
    private BigDecimal maxAmount;

    /**
     * 备注关键词（可选）
     */
    @Schema(description = "备注关键词", example = "午餐")
    private String remarkKeyword;

    /**
     * 包含的分类列表（可选）
     */
    @Schema(description = "包含的分类列表", example = "[\"餐饮\", \"交通\"]")
    private List<String> includeCategories;

    /**
     * 排除的分类列表（可选）
     */
    @Schema(description = "排除的分类列表", example = "[\"投资\"]")
    private List<String> excludeCategories;

    /**
     * 是否包含删除的交易记录
     */
    @Schema(description = "是否包含已删除的交易记录", example = "false")
    private Boolean includeDeleted = false;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段：date, amount, category, type, createdAt", example = "date")
    private String sortBy = "date";

    /**
     * 排序方向
     */
    @Schema(description = "排序方向：ASC, DESC", example = "DESC")
    private SortDirection sortDirection = SortDirection.DESC;

    public ExportRequest() {
    }

    public ExportRequest(ExportFormat format) {
        this.format = format;
    }

    public ExportRequest(ExportFormat format, LocalDate startDate, LocalDate endDate) {
        this.format = format;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getRemarkKeyword() {
        return remarkKeyword;
    }

    public void setRemarkKeyword(String remarkKeyword) {
        this.remarkKeyword = remarkKeyword;
    }

    public List<String> getIncludeCategories() {
        return includeCategories;
    }

    public void setIncludeCategories(List<String> includeCategories) {
        this.includeCategories = includeCategories;
    }

    public List<String> getExcludeCategories() {
        return excludeCategories;
    }

    public void setExcludeCategories(List<String> excludeCategories) {
        this.excludeCategories = excludeCategories;
    }

    public Boolean getIncludeDeleted() {
        return includeDeleted;
    }

    public void setIncludeDeleted(Boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * 导出格式枚举
     */
    public enum ExportFormat {
        CSV, EXCEL, JSON
    }
}