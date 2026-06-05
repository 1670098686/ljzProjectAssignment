package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.user.AccountStatusResponse;
import com.campus.trade.dto.user.DeleteAccountRequest;
import com.campus.trade.dto.user.UpdateProfileRequest;
import com.campus.trade.dto.user.UserProfileResponse;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "个人资料接口", description = "用户个人资料相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class ProfileController {

    private final UserService userService;
    private final ProductService productService;

    public ProfileController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "获取个人资料", description = "获取当前用户的个人资料信息")
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.success(userService.getProfile(SecurityUtils.getCurrentUsername()));
    }

    @PutMapping
    @Operation(summary = "更新个人资料", description = "更新当前用户的个人资料信息")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @GetMapping("/account-status")
    @Operation(summary = "获取账户状态", description = "获取当前用户的账户状态信息")
    public ApiResponse<AccountStatusResponse> accountStatus() {
        return ApiResponse.success(userService.getAccountStatus(SecurityUtils.getCurrentUsername()));
    }

    @PostMapping("/delete-request")
    @Operation(summary = "申请删除账户", description = "申请删除当前用户账户")
    public ApiResponse<AccountStatusResponse> deleteRequest(@RequestBody(required = false) DeleteAccountRequest request) {
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(userService.requestDelete(SecurityUtils.getCurrentUsername(), reason));
    }

    @PostMapping("/cancel-delete")
    @Operation(summary = "取消删除账户", description = "取消之前提交的删除账户申请")
    public ApiResponse<AccountStatusResponse> cancelDelete() {
        return ApiResponse.success(userService.cancelDelete(SecurityUtils.getCurrentUsername()));
    }

    @GetMapping("/products")
    @Operation(summary = "我的商品", description = "获取当前用户发布的商品列表")
    public ApiResponse<PaginatedResponse<ProductResponse>> myProducts(
            @Parameter(description = "商品状态") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(productService.listMyProducts(SecurityUtils.getCurrentUsername(), status, page, size));
    }
}
