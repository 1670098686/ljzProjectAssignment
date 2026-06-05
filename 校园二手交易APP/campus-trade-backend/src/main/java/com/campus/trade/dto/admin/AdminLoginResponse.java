package com.campus.trade.dto.admin;

import com.campus.trade.model.enums.AdminRole;

import java.util.List;

public class AdminLoginResponse {

    private String accessToken;
    private long expiresIn;
    private AdminRole role;
    private List<String> permissions;

    public AdminLoginResponse() {
    }

    public AdminLoginResponse(String accessToken, long expiresIn, AdminRole role, List<String> permissions) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.role = role;
        this.permissions = permissions;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
