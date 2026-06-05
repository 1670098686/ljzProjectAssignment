package com.campus.trade.dto.admin;

import com.campus.trade.model.enums.AdminRole;

public class AdminRegisterResponse {
    private Long id;
    private String username;
    private String email;
    private AdminRole role;

    public AdminRegisterResponse() {
    }

    public AdminRegisterResponse(Long id, String username, String email, AdminRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }
}