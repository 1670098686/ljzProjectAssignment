package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.product.AdvancedProductSearchRequest;
import com.campus.trade.dto.product.BatchUpdateProductStatusRequest;
import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.product.ProductSearchFilter;
import com.campus.trade.dto.product.RecommendationEventRequest;
import com.campus.trade.dto.product.SearchSuggestionResponse;
import com.campus.trade.dto.product.UpdateProductStatusRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductSearchSort;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.RecommendationScene;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.FavoriteService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.IdempotencyService;
import com.campus.trade.service.RecommendationService;
import com.campus.trade.util.ProductSortResolver;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "商品管理", description = "商品相关接口")
public class ProductController {

    private final ProductService productService;
    private final IdempotencyService idempotencyService;
    private final RecommendationService recommendationService;
    private final FavoriteService favoriteService;

    public ProductController(ProductService productService,
                             IdempotencyService idempotencyService,
                             RecommendationService recommendationService,
                             FavoriteService favoriteService) {
        this.productService = productService;
        this.idempotencyService = idempotencyService;
        this.recommendationService = recommendationService;
        this.favoriteService = favoriteService;
    }

    @PostMapping
    @Operation(summary = "创建商品", description = "用户发布新的商品信息")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<ProductResponse> createProduct(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                     @Valid @RequestBody ProductRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        ProductResponse response = idempotencyService.execute(
                idempotencyKey,
                username,
                "PRODUCT_CREATE",
                request,
                () -> productService.createProduct(username, request),
                ProductResponse.class);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "商品列表", description = "获取商品列表，支持分类、状态筛选和关键词搜索")
    @Timed(value = "api.products.list", histogram = true)
    public ApiResponse<PaginatedResponse<ProductResponse>> listProducts(
            @Parameter(description = "商品分类") @RequestParam(required = false) String category,
            @Parameter(description = "商品分类ID（动态分类）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "商品状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        // 转换字符串参数为枚举类型
        ProductCategory productCategory = null;
        ProductStatus productStatus = null;
        
        if (category != null && !category.isEmpty()) {
            // 处理"全部"分类选项
            if ("全部".equals(category) || "all".equalsIgnoreCase(category)) {
                productCategory = null; // 不应用分类过滤
            } else {
                // 中文分类名称映射到枚举值
                switch (category) {
                    case "书籍":
                    case "教材书籍":
                        productCategory = ProductCategory.BOOKS;
                        break;
                    case "数码电子":
                    case "电子产品":
                        productCategory = ProductCategory.ELECTRONICS;
                        break;
                    case "服装服饰":
                    case "服装":
                        productCategory = ProductCategory.CLOTHING;
                        break;
                    case "运动户外":
                    case "运动用品":
                        productCategory = ProductCategory.SPORTS;
                        break;
                    case "生活用品":
                    case "日用品":
                        productCategory = ProductCategory.DAILY;
                        break;
                    default:
                        // 尝试直接转换（英文枚举值）
                        try {
                            productCategory = ProductCategory.valueOf(category.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            // 如果枚举值无效，忽略该参数
                        }
                        break;
                }
            }
        }
        
        if (status != null && !status.isEmpty()) {
            try {
                productStatus = ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果枚举值无效，忽略该参数
            }
        }
        
        Sort sort = ProductSortResolver.resolve(null, keyword, direction, sortBy);
        PaginatedResponse<ProductResponse> response = productService.searchProducts(
            ProductSearchFilter.builder()
                .category(productCategory)
                .categoryId(categoryId)
                .keyword(keyword)
                .status(productStatus)
                .includeInactive(productStatus != null && productStatus != ProductStatus.ON_SALE)
                .build(),
            page,
            size,
            sort);
        attachFavoriteFlags(response.getItems());
        return ApiResponse.success(response);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "根据关键词搜索商品")
    @Timed(value = "api.products.search", histogram = true)
    public ApiResponse<PaginatedResponse<ProductResponse>> searchProducts(
            @Parameter(description = "商品分类") @RequestParam(required = false) String category,
            @Parameter(description = "商品分类ID（动态分类）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "商品状态") @RequestParam(required = false) String status,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "交易地点（模糊匹配）") @RequestParam(required = false) String location,
            @Parameter(description = "卖家所在学校（模糊匹配）") @RequestParam(name = "sellerSchool", required = false) String sellerSchool,
            @Parameter(description = "是否包含下架/待审核商品（默认仅展示可售）") @RequestParam(defaultValue = "false") boolean includeInactive,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "搜索排序模式") @RequestParam(required = false) ProductSearchSort sortMode) {
        // 转换字符串参数为枚举类型
        ProductCategory productCategory = null;
        ProductStatus productStatus = null;
        
        if (category != null && !category.isEmpty()) {
            // 处理"全部"分类选项
            if ("全部".equals(category) || "all".equalsIgnoreCase(category)) {
                productCategory = null; // 不应用分类过滤
            } else {
                // 中文分类名称映射到枚举值
                switch (category) {
                    case "书籍":
                    case "教材书籍":
                        productCategory = ProductCategory.BOOKS;
                        break;
                    case "数码电子":
                    case "电子产品":
                        productCategory = ProductCategory.ELECTRONICS;
                        break;
                    case "服装服饰":
                    case "服装":
                        productCategory = ProductCategory.CLOTHING;
                        break;
                    case "运动户外":
                    case "运动用品":
                        productCategory = ProductCategory.SPORTS;
                        break;
                    case "生活用品":
                    case "日用品":
                        productCategory = ProductCategory.DAILY;
                        break;
                    default:
                        // 尝试直接转换（英文枚举值）
                        try {
                            productCategory = ProductCategory.valueOf(category.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            // 如果枚举值无效，忽略该参数
                        }
                        break;
                }
            }
        }
        
        if (status != null && !status.isEmpty()) {
            try {
                productStatus = ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果枚举值无效，忽略该参数
            }
        }
        
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }
        boolean allowInactive = includeInactive || (productStatus != null && productStatus != ProductStatus.ON_SALE);
        ProductSearchFilter filter = ProductSearchFilter.builder()
            .category(productCategory)
            .categoryId(categoryId)
            .keyword(keyword)
            .status(productStatus)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .location(location)
            .sellerSchool(sellerSchool)
            .includeInactive(allowInactive)
            .build();
        Sort sort = ProductSortResolver.resolve(sortMode, keyword, direction, sortBy);
        PaginatedResponse<ProductResponse> response = productService.searchProducts(filter, page, size, sort);
        attachFavoriteFlags(response.getItems());
        return ApiResponse.success(response);
    }

    @PostMapping("/search/advanced")
    @Operation(summary = "高级搜索", description = "支持多条件组合筛选与排序")
    @Timed(value = "api.products.search.advanced", histogram = true)
    public ApiResponse<PaginatedResponse<ProductResponse>> advancedSearch(@Valid @RequestBody AdvancedProductSearchRequest request) {
        request.normalize();
        ProductSearchFilter filter = request.toFilter();
        Sort sort = ProductSortResolver.resolve(request.getSortMode(), request.getKeyword(),
            request.getDirection(), request.getSortBy());
        int page = request.getPage();
        int size = request.getSize();
        PaginatedResponse<ProductResponse> response = productService.searchProducts(filter, page, size, sort);
        attachFavoriteFlags(response.getItems());
        return ApiResponse.success(response);
    }

    @GetMapping("/search/suggestions")
    @Operation(summary = "搜索联想", description = "根据关键词返回热门提示")
    @Timed(value = "api.products.search.suggestions", histogram = true)
    public ApiResponse<SearchSuggestionResponse> searchSuggestions(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(productService.getSearchSuggestions(keyword, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情", description = "获取指定商品的详细信息")
    @Timed(value = "api.products.detail", histogram = true)
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "是否增加浏览量") @RequestParam(defaultValue = "true") boolean increaseView) {
        ProductResponse response = productService.getProduct(id, increaseView);
        attachFavoriteFlag(response);
        return ApiResponse.success(response);
    }

    @GetMapping("/recommendations")
    @Operation(summary = "个性化推荐", description = "根据场景返回推荐商品列表")
    @Timed(value = "api.products.recommendations", histogram = true)
    public ApiResponse<List<ProductResponse>> recommendations(
            @RequestParam(defaultValue = "HOME") String scene,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "10") int limit) {
        RecommendationScene resolvedScene = parseScene(scene);
        String username = SecurityUtils.getCurrentUsername();
        List<ProductResponse> responses = productService.getRecommendations(username, resolvedScene, category, limit);
        favoriteService.attachFavoriteFlags(username, responses);
        return ApiResponse.success(responses);
    }

    @PostMapping("/recommendations/events")
    @Operation(summary = "上报推荐事件", description = "记录推荐曝光、点击、下单等交互")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<Void> recordRecommendationEvent(@Valid @RequestBody RecommendationEventRequest request) {
        recommendationService.recordEvent(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    private RecommendationScene parseScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return RecommendationScene.HOME;
        }
        try {
            return RecommendationScene.valueOf(scene.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return RecommendationScene.HOME;
        }
    }

    private void attachFavoriteFlags(List<ProductResponse> responses) {
        favoriteService.attachFavoriteFlags(SecurityUtils.getCurrentUsername(), responses);
    }

    private void attachFavoriteFlag(ProductResponse response) {
        if (response == null) {
            return;
        }
        favoriteService.attachFavoriteFlags(SecurityUtils.getCurrentUsername(), List.of(response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新商品", description = "卖家更新已发布商品的信息")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<ProductResponse> updateProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        ProductResponse response = productService.updateProduct(username, id, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新商品状态", description = "更新商品的状态（如上架、下架等）")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        productService.updateStatus(SecurityUtils.getCurrentUsername(), id, request.getStatus());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "删除指定的商品")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<Void> deleteProduct(@Parameter(description = "商品ID") @PathVariable Long id) {
        productService.updateStatus(SecurityUtils.getCurrentUsername(), id, ProductStatus.DELETED);
        return ApiResponse.success();
    }

    @PostMapping("/batch/status")
    @Operation(summary = "批量更新商品状态", description = "卖家批量上/下架或批量删除（状态置为DELETED）")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<BatchOperationResult> batchUpdateStatus(@Valid @RequestBody BatchUpdateProductStatusRequest request) {
        ProductStatus status;
        try {
            status = ProductStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "无效的商品状态");
        }
        return ApiResponse.success(productService.batchUpdateStatus(SecurityUtils.getCurrentUsername(), request.getIds(), status));
    }

}
