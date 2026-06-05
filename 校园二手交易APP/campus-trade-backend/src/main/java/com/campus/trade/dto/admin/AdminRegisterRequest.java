package com.campus.trade.dto.admin;

import com.campus.trade.model.enums.AdminPermission;
import com.campus.trade.model.enums.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminRegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @Email
    @NotBlank
    private String email;

    @NotNull
    private AdminRole role;

    private List<AdminPermission> permissions;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public List<AdminPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<AdminPermission> permissions) {
        this.permissions = permissions;
    }
}
