package com.campus.trade.dto.user;

import com.campus.trade.model.enums.UserRole;

public class UserSummary {

    private Long id;
    private String username;
    private UserRole role;
    private String email;
    private String phone;
    private String avatar;
    private String school;
    private boolean emailVerified;

    public UserSummary() {
    }

    public UserSummary(Long id, String username, UserRole role, String email, String phone, String avatar, String school, boolean emailVerified) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.avatar = avatar;
        this.school = school;
        this.emailVerified = emailVerified;
    }

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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
