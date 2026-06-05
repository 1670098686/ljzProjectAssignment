package com.finance.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "budget_alert_config")
public class BudgetAlertConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "warning_threshold", precision = 5, scale = 4, nullable = false)
    private BigDecimal warningThreshold;
    
    @Column(name = "critical_threshold", precision = 5, scale = 4, nullable = false)
    private BigDecimal criticalThreshold;
    
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled;
    
    @Column(name = "enable_all_categories", nullable = false)
    private Boolean enableAllCategories;
    
    @ElementCollection
    @CollectionTable(name = "budget_alert_config_categories", 
                     joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "category_id")
    private List<Long> categoryIds;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    // Manual getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(BigDecimal warningThreshold) { this.warningThreshold = warningThreshold; }
    public BigDecimal getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(BigDecimal criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public Boolean getEnableAllCategories() { return enableAllCategories; }
    public void setEnableAllCategories(Boolean enableAllCategories) { this.enableAllCategories = enableAllCategories; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
    
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }
}