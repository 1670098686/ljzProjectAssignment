package com.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "预算响应对象")
public class BudgetDto {

    @Schema(description = "预算ID", example = "2001")
    private Long id;

    @Schema(description = "所属分类ID", example = "501")
    private Long categoryId;

    @Schema(description = "分类名称", example = "餐饮")
    private String categoryName;

    @Schema(description = "预算金额", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "预算年份", example = "2025")
    private Integer year;

    @Schema(description = "预算月份", example = "11")
    private Integer month;

    // Getter methods
    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
