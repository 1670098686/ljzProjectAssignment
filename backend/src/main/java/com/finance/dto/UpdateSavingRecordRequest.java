package com.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新储蓄记录请求DTO
 */
public class UpdateSavingRecordRequest {

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @NotNull(message = "记录日期不能为空")
    private LocalDateTime recordDate;

    @Size(max = 50, message = "分类标签长度不能超过50个字符")
    private String category;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "记录类型不能为空")
    private String type;

    /**
     * 验证记录类型是否有效
     */
    public boolean isValidType() {
        return "DEPOSIT".equals(type) || "WITHDRAW".equals(type);
    }

    // Getter methods
    public String getDescription() {
        return description;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    // Setter methods
    public void setDescription(String description) {
        this.description = description;
    }

    public void setRecordDate(LocalDateTime recordDate) {
        this.recordDate = recordDate;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setType(String type) {
        this.type = type;
    }
}