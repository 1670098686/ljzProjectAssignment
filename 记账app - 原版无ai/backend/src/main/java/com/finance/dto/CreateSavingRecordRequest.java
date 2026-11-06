package com.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建储蓄记录请求DTO
 */
public class CreateSavingRecordRequest {

    @NotNull(message = "储蓄目标ID不能为空")
    private Long goalId;

    @NotBlank(message = "记录类型不能为空")
    @Size(max = 20, message = "记录类型长度不能超过20个字符")
    private String type; // DEPOSIT 或 WITHDRAW

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @NotNull(message = "记录日期不能为空")
    private LocalDateTime recordDate;

    @Size(max = 50, message = "分类标签长度不能超过50个字符")
    private String category;

    /**
     * 验证记录类型是否有效
     */
    public boolean isValidType() {
        return "DEPOSIT".equals(type) || "WITHDRAW".equals(type);
    }

    // Getter methods
    public Long getGoalId() {
        return goalId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public String getCategory() {
        return category;
    }

    // Setter methods
    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRecordDate(LocalDateTime recordDate) {
        this.recordDate = recordDate;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long goalId;
        private String type;
        private BigDecimal amount;
        private String description;
        private LocalDateTime recordDate;
        private String category;

        public Builder goalId(Long goalId) {
            this.goalId = goalId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder recordDate(LocalDateTime recordDate) {
            this.recordDate = recordDate;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public CreateSavingRecordRequest build() {
            CreateSavingRecordRequest request = new CreateSavingRecordRequest();
            request.goalId = this.goalId;
            request.type = this.type;
            request.amount = this.amount;
            request.description = this.description;
            request.recordDate = this.recordDate;
            request.category = this.category;
            return request;
        }
    }
}