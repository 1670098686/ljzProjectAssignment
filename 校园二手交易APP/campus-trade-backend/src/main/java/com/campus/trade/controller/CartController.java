package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.cart.AddCartItemRequest;
import com.campus.trade.dto.cart.CartCountResponse;
import com.campus.trade.dto.cart.CartItemResponse;
import com.campus.trade.dto.cart.CartSummaryResponse;
import com.campus.trade.dto.cart.UpdateCartItemRequest;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart/items")
@Tag(name = "购物车接口", description = "购物车相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @Operation(summary = "添加商品到购物车")
    public ApiResponse<CartItemResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addItem(SecurityUtils.getCurrentUsername(), request));
    }

    @GetMapping
    @Operation(summary = "查看购物车列表")
    public ApiResponse<PaginatedResponse<CartItemResponse>> listItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(cartService.listItems(SecurityUtils.getCurrentUsername(), page, size));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新购物车商品数量")
    public ApiResponse<CartItemResponse> updateQuantity(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateQuantity(SecurityUtils.getCurrentUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移除购物车商品")
    public ApiResponse<Void> removeItem(@PathVariable Long id) {
        cartService.removeItem(SecurityUtils.getCurrentUsername(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "根据商品移除购物车项")
    public ApiResponse<Void> removeByProduct(@PathVariable Long productId) {
        cartService.removeItemByProduct(SecurityUtils.getCurrentUsername(), productId);
        return ApiResponse.success();
    }

    @DeleteMapping
    @Operation(summary = "清空购物车")
    public ApiResponse<Void> clearCart() {
        cartService.clearCart(SecurityUtils.getCurrentUsername());
        return ApiResponse.success();
    }

    @GetMapping("/summary")
    @Operation(summary = "购物车汇总信息")
    public ApiResponse<CartSummaryResponse> summary() {
        return ApiResponse.success(cartService.getSummary(SecurityUtils.getCurrentUsername()));
    }

    @GetMapping("/count")
    @Operation(summary = "购物车商品数量")
    public ApiResponse<CartCountResponse> count() {
        long totalItems = cartService.countItems(SecurityUtils.getCurrentUsername());
        return ApiResponse.success(new CartCountResponse(totalItems));
    }
}
