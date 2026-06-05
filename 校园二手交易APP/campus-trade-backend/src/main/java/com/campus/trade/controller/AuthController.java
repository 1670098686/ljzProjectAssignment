package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.auth.JwtLoginResponse;
import com.campus.trade.dto.auth.LoginRequest;
import com.campus.trade.dto.auth.PasswordResetCodeRequest;
import com.campus.trade.dto.auth.RegisterRequest;
import com.campus.trade.dto.auth.ResendVerificationEmailRequest;
import com.campus.trade.dto.auth.ResetPasswordRequest;
import com.campus.trade.dto.auth.VerifyEmailRequest;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证接口", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账户")
    public ApiResponse<UserSummary> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "已注册用户登录系统")
    public ApiResponse<JwtLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户退出登录")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    @PostMapping("/verify-email/request")
    @Operation(summary = "发送邮箱验证码", description = "向邮箱发送激活验证码")
    public ApiResponse<Map<String, String>> sendVerifyEmail(@Valid @RequestBody ResendVerificationEmailRequest request) {
        String code = authService.resendVerificationEmail(request);
        return ApiResponse.success(Map.of("verificationCode", code));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "邮箱验证", description = "使用验证码完成邮箱验证")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.success();
    }

    @PostMapping("/reset-password/request")
    @Operation(summary = "发送重置验证码", description = "向邮箱发送密码重置验证码")
    public ApiResponse<Map<String, String>> requestPasswordReset(@Valid @RequestBody PasswordResetCodeRequest request) {
        String code = authService.requestPasswordReset(request);
        return ApiResponse.success(Map.of("verificationCode", code));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "使用验证码重置账户密码")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("重置完成");
    }
}
