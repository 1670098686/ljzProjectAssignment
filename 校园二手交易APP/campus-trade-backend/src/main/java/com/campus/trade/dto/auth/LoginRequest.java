package com.campus.trade.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // 用于支持用户名或邮箱登录
    public String getUsernameOrEmail() {
        return email;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.email = usernameOrEmail;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
