package com.finance.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateUserProfileRequest {
    @NotBlank
    private String nickname;

    private String remindTime;

    // Getter methods
    public String getNickname() {
        return nickname;
    }

    public String getRemindTime() {
        return remindTime;
    }

    // Setter methods
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime = remindTime;
    }
}
