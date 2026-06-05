package com.campus.trade.dto.user;

import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.UserRole;

import java.time.LocalDateTime;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String school;
    private String contactInfo;
    private UserRole role;
    private AccountStatus status;
    private boolean emailVerified;
    private boolean deleteRequested;
    private String deleteReason;
    private LocalDateTime deleteScheduleTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isDeleteRequested() {
        return deleteRequested;
    }

    public void setDeleteRequested(boolean deleteRequested) {
        this.deleteRequested = deleteRequested;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public LocalDateTime getDeleteScheduleTime() {
        return deleteScheduleTime;
    }

    public void setDeleteScheduleTime(LocalDateTime deleteScheduleTime) {
        this.deleteScheduleTime = deleteScheduleTime;
    }
}
