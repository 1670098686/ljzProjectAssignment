package com.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "交易记录响应对象")
public class TransactionDto {

    @Schema(description = "交易ID", example = "1001")
    private Long id;

    @Schema(description = "交易类型：1=收入，2=支出", example = "2")
    private Integer type;

    @Schema(description = "分类ID", example = "501")
    private Long categoryId;

    @Schema(description = "分类名称", example = "餐饮")
    private String categoryName;

    @Schema(description = "交易金额", example = "128.50")
    private BigDecimal amount;

    @Schema(description = "交易日期", example = "2025-10-21")
    private LocalDate transactionDate;

    @Schema(description = "备注信息", example = "朋友聚餐")
    private String remark;

    // Getter methods
    public Long getId() {
        return id;
    }

    public Integer getType() {
        return type;
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getRemark() {
        return remark;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
