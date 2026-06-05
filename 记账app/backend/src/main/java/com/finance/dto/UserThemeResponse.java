package com.finance.dto;

import java.time.LocalDateTime;

public class UserThemeResponse {
    private Boolean success;
    private String message;
    
    private String themeMode;
    private String primaryColor;
    private String accentColor;
    private Boolean darkModeEnabled;
    private String fontSize;
    private Boolean roundedCorners;
    private Boolean animationsEnabled;
    
    private LocalDateTime updatedTime;

    // Getter methods
    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

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

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    // Setter methods
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}