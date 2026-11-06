package com.finance.dto;

public class IconPresetDto {
    private String name;
    private String iconName;
    private String displayName;
    private String category;
    private String description;
    
    // Constructor
    public IconPresetDto() {
    }
    
    public IconPresetDto(String name, String iconName, String displayName, String category, String description) {
        this.name = name;
        this.iconName = iconName;
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }
    
    // Getter methods
    public String getName() {
        return name;
    }
    
    public String getIconName() {
        return iconName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getDescription() {
        return description;
    }
    
    // Setter methods
    public void setName(String name) {
        this.name = name;
    }
    
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}