package com.finance.entity;

import com.finance.converter.AmountEncryptConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budgets",
    uniqueConstraints = @UniqueConstraint(name = "uk_budgets_user_category_month", columnNames = {"user_id", "category_id", "year", "month"}),
    indexes = {
        @Index(name = "idx_budgets_user", columnList = "user_id"),
        @Index(name = "idx_budgets_user_category", columnList = "user_id,category_id")
    })
public class Budget extends UserScopedAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_budget_category"))
    private Category category;

    @Column(name = "monthly_budget", nullable = false, precision = 10, scale = 2)
    @Convert(converter = AmountEncryptConverter.class)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    // Constructor
    public Budget() {
        // Default constructor
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
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

    public void setCategory(Category category) {
        this.category = category;
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
