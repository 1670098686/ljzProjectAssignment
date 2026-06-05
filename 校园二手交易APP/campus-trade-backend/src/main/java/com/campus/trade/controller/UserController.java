package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.user.AccountStatusResponse;
import com.campus.trade.dto.user.ChangePasswordRequest;
import com.campus.trade.dto.user.DeleteAccountRequest;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.FavoriteService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.UserService;
import com.campus.trade.service.IdempotencyService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户接口", description = "普通用户相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class UserController {

    private final UserService userService;
    private final IdempotencyService idempotencyService;
    private final ProductService productService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService, IdempotencyService idempotencyService, ProductService productService, FavoriteService favoriteService) {
        this.userService = userService;
        this.idempotencyService = idempotencyService;
        this.productService = productService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的简要信息")
    public ApiResponse<UserSummary> currentUser() {
        return ApiResponse.success(userService.getCurrentUserSummary(SecurityUtils.getCurrentUsername()));
    }

    @GetMapping("/me/account-status")
    @Operation(summary = "获取账户状态")
    public ApiResponse<AccountStatusResponse> accountStatus() {
        return ApiResponse.success(userService.getAccountStatus(SecurityUtils.getCurrentUsername()));
    }

    @PostMapping("/me/delete-request")
    @Operation(summary = "申请注销账号")
    public ApiResponse<AccountStatusResponse> deleteRequest(@RequestBody(required = false) DeleteAccountRequest request) {
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(userService.requestDelete(SecurityUtils.getCurrentUsername(), reason));
    }

    @PostMapping("/me/cancel-delete")
    @Operation(summary = "取消注销申请")
    public ApiResponse<AccountStatusResponse> cancelDelete() {
        return ApiResponse.success(userService.cancelDelete(SecurityUtils.getCurrentUsername()));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    public ApiResponse<Void> changePassword(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                           @Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        idempotencyService.execute(
                idempotencyKey,
                username,
                "USER_CHANGE_PASSWORD",
                request,
                () -> {
                    userService.changePassword(username, request);
                    return null;
                },
                Void.class);
        return ApiResponse.success();
    }

    @GetMapping("/me/products")
    @Operation(summary = "我的商品", description = "获取当前用户发布的商品列表")
    @Timed(value = "api.users.me.products", histogram = true)
    public ApiResponse<PaginatedResponse<ProductResponse>> getMyProducts(
            @Parameter(description = "商品状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        // 转换字符串参数为枚举类型
        ProductStatus productStatus = null;
        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            try {
                productStatus = ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果枚举值无效，忽略该参数
            }
        }
        PaginatedResponse<ProductResponse> response = productService.listMyProducts(
                SecurityUtils.getCurrentUsername(), productStatus, page, size);
        attachFavoriteFlags(response.getItems());
        return ApiResponse.success(response);
    }

    private void attachFavoriteFlags(java.util.List<ProductResponse> responses) {
        favoriteService.attachFavoriteFlags(SecurityUtils.getCurrentUsername(), responses);
    }
}
