package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.favorite.FavoriteListQuery;
import com.campus.trade.dto.favorite.FavoriteStatusResponse;
import com.campus.trade.dto.favorite.FavoriteSummaryResponse;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.model.enums.FavoriteSortMode;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "收藏接口", description = "商品收藏相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{productId}")
    @Operation(summary = "收藏商品", description = "将指定商品加入收藏")
    public ApiResponse<Void> addFavorite(@PathVariable Long productId) {
        favoriteService.addFavorite(SecurityUtils.getCurrentUsername(), productId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "取消收藏", description = "取消对指定商品的收藏")
    public ApiResponse<Void> removeFavorite(@PathVariable Long productId) {
        favoriteService.removeFavorite(SecurityUtils.getCurrentUsername(), productId);
        return ApiResponse.success();
    }

    @GetMapping
    @Operation(summary = "我的收藏", description = "获取当前用户收藏的商品列表")
    public ApiResponse<PaginatedResponse<ProductResponse>> listMyFavorites(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "品类过滤") @RequestParam(required = false) ProductCategory category,
            @Parameter(description = "商品状态过滤") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "排序模式") @RequestParam(defaultValue = "LATEST") FavoriteSortMode sortMode,
            @Parameter(description = "仅显示可售商品") @RequestParam(required = false) Boolean onlyAvailable) {
        FavoriteListQuery query = FavoriteListQuery.of(page, size, category, status, keyword, sortMode, onlyAvailable);
        return ApiResponse.success(favoriteService.listMyFavorites(SecurityUtils.getCurrentUsername(), query));
    }

    @GetMapping("/status")
    @Operation(summary = "收藏状态查询", description = "批量查询指定商品是否已收藏")
    public ApiResponse<List<FavoriteStatusResponse>> favoriteStatus(
            @Parameter(description = "商品ID列表") @RequestParam("productIds") List<Long> productIds) {
        return ApiResponse.success(favoriteService.getFavoriteStatus(SecurityUtils.getCurrentUsername(), productIds));
    }

    @GetMapping("/summary")
    @Operation(summary = "收藏统计", description = "返回当前用户的收藏汇总信息")
    public ApiResponse<FavoriteSummaryResponse> favoriteSummary(
            @Parameter(description = "热门商品数量") @RequestParam(defaultValue = "5") int top) {
        return ApiResponse.success(favoriteService.getFavoriteSummary(SecurityUtils.getCurrentUsername(), top));
    }
}
