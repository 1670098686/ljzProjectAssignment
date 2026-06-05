package com.finance.entity;

import com.finance.converter.AmountEncryptConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储蓄记录实体类
 * 用于记录用户的具体储蓄操作，如存款、取款等
 */
@Entity
@Table(name = "saving_records",
    indexes = {
        @Index(name = "idx_saving_records_user", columnList = "user_id"),
        @Index(name = "idx_saving_records_goal", columnList = "goal_id"),
        @Index(name = "idx_saving_records_date", columnList = "record_date"),
        @Index(name = "idx_saving_records_user_type", columnList = "user_id,type")
    })
public class SavingRecord extends UserScopedAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id")
    private Long goalId;

    @Column(nullable = false, length = 20)
    private String type; // 记录类型：DEPOSIT=存款, WITHDRAW=取款

    @Column(nullable = false, precision = 10, scale = 2)
    @Convert(converter = AmountEncryptConverter.class)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;

    @Column(length = 50)
    private String category; // 分类标签

    // Constructor
    public SavingRecord() {
        // Default constructor
    }

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
     * 获取记录类型显示名称
     */
    public String getTypeDisplay() {
        return isDeposit() ? "存款" : "取款";
    }

    /**
     * 获取记录金额（正数表示存款，负数表示取款）
     */
    public BigDecimal getSignedAmount() {
        return isDeposit() ? amount : amount.negate();
    }


    
    @Override
    public String toString() {
        return "SavingRecord{" +
                "id=" + id +
                ", goalId=" + goalId +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", recordDate=" + recordDate +
                ", category='" + category + '\'' +
                ", userId=" + getUserId() +
                '}';
    }
}