package com.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserThemeRequest {
    @Pattern(regexp = "^(light|dark|system)$", message = "themeMode必须是light、dark或system")
    private String themeMode;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "primaryColor必须是6位十六进制颜色值")
    private String primaryColor;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "accentColor必须是6位十六进制颜色值")
    private String accentColor;
    
    private Boolean darkModeEnabled;
    
    // 可选：字体大小
    private String fontSize; // small, medium, large
    
    // 可选：圆角设置
    private Boolean roundedCorners;
    
    // 可选：动画效果
    private Boolean animationsEnabled;

    // Getter methods
    public String getThemeMode() {
        return themeMode;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public Boolean getDarkModeEnabled() {
        return darkModeEnabled;
    }

    public String getFontSize() {
        return fontSize;
    }

    public Boolean getRoundedCorners() {
        return roundedCorners;
    }

    public Boolean getAnimationsEnabled() {
        return animationsEnabled;
    }

    // Setter methods
    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public void setDarkModeEnabled(Boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public void setRoundedCorners(Boolean roundedCorners) {
        this.roundedCorners = roundedCorners;
    }

    public void setAnimationsEnabled(Boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }
}