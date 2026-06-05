package com.finance.dto;

public class CategoryDto {
    private Long id;
    private String name;
    private String icon;
    private Integer type;
    private boolean defaultCategory;
    private Integer sortOrder;

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
