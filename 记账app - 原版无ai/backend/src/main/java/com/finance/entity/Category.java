package com.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_user_name_type", columnNames = {"user_id", "name", "type"}),
        indexes = {
                @Index(name = "idx_categories_user", columnList = "user_id"),
                @Index(name = "idx_categories_user_type", columnList = "user_id,type")
        })
public class Category extends UserScopedAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 100, nullable = false)
    private String icon = "default_icon.png";

    /**
     * 1 = income, 2 = expense
     */
    @Column(nullable = false)
    private Integer type;

    @Column(name = "is_default", nullable = false)
    private boolean defaultCategory = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Constructor
    public Category() {
        // Default constructor
    }

    // Getter methods
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getType() {
        return type;
    }

    public boolean isDefaultCategory() {
        return defaultCategory;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setDefaultCategory(boolean defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
