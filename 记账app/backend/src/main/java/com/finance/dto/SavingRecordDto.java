package com.finance.dto;

import com.finance.entity.SavingRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储蓄记录数据传输对象
 */
public class SavingRecordDto {

    private Long id;
    private Long goalId;
    private String type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime recordDate;
    private String category;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getter methods
    public Long getId() {
        return id;
    }

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

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long goalId;
        private String type;
        private BigDecimal amount;
        private String description;
        private LocalDateTime recordDate;
        private String category;
        private Long userId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

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

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SavingRecordDto build() {
            SavingRecordDto dto = new SavingRecordDto();
            dto.id = this.id;
            dto.goalId = this.goalId;
            dto.type = this.type;
            dto.amount = this.amount;
            dto.description = this.description;
            dto.recordDate = this.recordDate;
            dto.category = this.category;
            dto.userId = this.userId;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            return dto;
        }
    }

    /**
     * 从实体类转换
     */
    public static SavingRecordDto fromEntity(SavingRecord entity) {
        if (entity == null) {
            return null;
        }
        
        SavingRecordDto dto = new SavingRecordDto();
        dto.setId(entity.getId());
        dto.setGoalId(entity.getGoalId());
        dto.setType(entity.getType());
        dto.setAmount(entity.getAmount());
        dto.setDescription(entity.getDescription());
        dto.setRecordDate(entity.getRecordDate());
        dto.setCategory(entity.getCategory());
        dto.setUserId(entity.getUserId());
        dto.setCreatedAt(entity.getCreateTime());
        dto.setUpdatedAt(entity.getUpdateTime());
        
        return dto;
    }

    /**
     * 获取记录类型显示名称
     */
    public String getTypeDisplay() {
        return "DEPOSIT".equals(type) ? "存款" : "取款";
    }

    /**
     * 判断是否为存款记录
     */
    public boolean isDeposit() {
        return "DEPOSIT".equals(type);
    }

    /**
     * 判断是否为取款记录
     */
    public boolean isWithdraw() {
        return "WITHDRAW".equals(type);
    }

    /**
     * 获取带符号的金额（正数表示存款，负数表示取款）
     */
    public BigDecimal getSignedAmount() {
        return isDeposit() ? amount : amount.negate();
    }

    /**
     * 获取金额显示（带符号）
     */
    public String getAmountDisplay() {
        return (isDeposit() ? "+" : "-") + amount;
    }
}