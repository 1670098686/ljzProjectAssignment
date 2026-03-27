package com.finance.entity;

import com.finance.converter.AmountEncryptConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_user_date", columnList = "user_id,transaction_date"),
        @Index(name = "idx_transactions_user_type_category", columnList = "user_id,type,category_id"),
        @Index(name = "idx_user_recent", columnList = "user_id,transaction_date DESC")
    })
public class TransactionRecord extends UserScopedAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 1 = income, 2 = expense
     */
    @Column(nullable = false)
    private Integer type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_category"))
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    @Convert(converter = AmountEncryptConverter.class)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(length = 100)
    private String remark;

    public TransactionRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
