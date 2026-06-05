package com.campus.trade.dto.auth;

import com.campus.trade.dto.user.UserSummary;

public class JwtLoginResponse {

    private String accessToken;
    private long expiresIn;
    private UserSummary user;

    public JwtLoginResponse() {
    }

    public JwtLoginResponse(String accessToken, long expiresIn, UserSummary user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
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

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }
}
