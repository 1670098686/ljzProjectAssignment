package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.product.ProductSearchFilter;
import com.campus.trade.dto.product.SearchSuggestionResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.RecommendationScene;
import com.campus.trade.model.entity.Category;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.spec.ProductSpecifications;
import com.campus.trade.util.ProductMapper;
import com.campus.trade.util.SearchKeywordParser;
import com.campus.trade.service.HotProductCacheService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductReviewService productReviewService;
    private final RecommendationService recommendationService;
    private final HotProductCacheService hotProductCacheService;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository,
                          UserRepository userRepository,
                          ProductReviewService productReviewService,
                          RecommendationService recommendationService,
                          HotProductCacheService hotProductCacheService,
                          CategoryService categoryService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productReviewService = productReviewService;
        this.recommendationService = recommendationService;
        this.hotProductCacheService = hotProductCacheService;
        this.categoryService = categoryService;
    }

        @Transactional
        @Caching(evict = {
            @CacheEvict(value = "product:list", allEntries = true),
            @CacheEvict(value = "product:detail", key = "#result.id", condition = "#result != null"),
            @CacheEvict(value = "search:suggestion", allEntries = true)
        })
    public ProductResponse createProduct(String username, ProductRequest request) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getCategoryId() == null && request.getCategory() == null) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_INVALID, "缺少商品分类");
        }

        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        Category categoryEntity = null;
        if (request.getCategoryId() != null) {
            categoryEntity = categoryService.getEnabledEntity(request.getCategoryId());
            product.setCategoryEntity(categoryEntity);
        }

        ProductCategory resolvedCategory = request.getCategory();
        if (resolvedCategory == null && categoryEntity != null) {
            resolvedCategory = mapLegacyCategory(categoryEntity.getCode());
        }
        product.setCategory(resolvedCategory == null ? ProductCategory.OTHER : resolvedCategory);
        product.setImages(request.getImages());
        product.setTags(request.getTags());
        product.setContactInfo(request.getContactInfo());
        product.setLocation(request.getLocation());
        product.setSeller(seller);
        product.setStatus(request.isDraft() ? ProductStatus.DRAFT : ProductStatus.ON_SALE);
        product.setAuditStatus(AuditStatus.PENDING);
        productRepository.save(product);
        ProductResponse response = ProductMapper.toResponse(product);
        productReviewService.attachRatingSummary(response);
        hotProductCacheService.evictHotSearchCaches();
        return response;
    }

    private ProductCategory mapLegacyCategory(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            return ProductCategory.OTHER;
        }
        try {
            return ProductCategory.valueOf(categoryCode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ProductCategory.OTHER;
        }
    }

        @Transactional
        @Caching(
            cacheable = @Cacheable(value = "product:detail", key = "#id", condition = "!#increaseView"),
            evict = @CacheEvict(value = "product:detail", key = "#id", condition = "#increaseView")
        )
    public ProductResponse getProduct(Long id, boolean increaseView) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (increaseView) {
            product.setViewCount(product.getViewCount() + 1);
        }
        ProductResponse response = ProductMapper.toResponse(product);
        productReviewService.attachRatingSummary(response);
        return response;
    }

        @Transactional(readOnly = true)
        @Cacheable(value = "product:list",
            key = "T(com.campus.trade.util.CacheKeyUtils).productListKey(#category, #keyword, #status, #page, #size, #sort)")
    public PaginatedResponse<ProductResponse> listProducts(ProductCategory category,
                                                           String keyword,
                                                           ProductStatus status,
                                                           int page,
                                                           int size,
                                                           Sort sort) {
        ProductSearchFilter filter = ProductSearchFilter.builder()
                .category(category)
                .keyword(keyword)
                .status(status)
                .includeInactive(status != null && status != ProductStatus.ON_SALE)
                .build();
        return queryProducts(filter, page, size, sort);
    }

        @Transactional(readOnly = true)
        @Cacheable(value = "product:list",
            key = "T(com.campus.trade.util.CacheKeyUtils).productSearchKey(#filter, #page, #size, #sort)")
    public PaginatedResponse<ProductResponse> searchProducts(ProductSearchFilter filter,
                                                             int page,
                                                             int size,
                                                             Sort sort) {
        return queryProducts(filter, page, size, sort);
    }

    private PaginatedResponse<ProductResponse> queryProducts(ProductSearchFilter filter,
                                                             int page,
                                                             int size,
                                                             Sort sort) {
        Sort effectiveSort = sort == null ? Sort.by(Sort.Direction.DESC, "createTime") : sort;
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, effectiveSort);
        Specification<Product> spec = buildSpecification(filter);
        Page<Product> pageResult = productRepository.findAll(spec, pageable);

        List<Product> products = pageResult.getContent();
        for (Product product : products) {
            product.getSeller().getId();
        }

        List<ProductResponse> responses = products.stream()
            .map(ProductMapper::toResponse)
            .collect(Collectors.toList());
        productReviewService.attachRatingSummary(responses);
        return PaginatedResponse.of(responses, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "search:suggestion",
            key = "T(com.campus.trade.util.CacheKeyUtils).searchSuggestionKey(#keyword, #limit)")
    public SearchSuggestionResponse getSearchSuggestions(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        Specification<Product> baseSpec = Specification.where(ProductSpecifications.hasStatus(ProductStatus.ON_SALE))
                .and(ProductSpecifications.hasAuditStatus(AuditStatus.APPROVED));
        Specification<Product> keywordSpec = baseSpec.and(ProductSpecifications.titleLike(keyword));
        Sort sort = Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createTime"));
        LinkedHashSet<String> suggestionSet = new LinkedHashSet<>();
        Page<Product> keywordPage = productRepository.findAll(keywordSpec,
                PageRequest.of(0, safeLimit, sort));
        keywordPage.forEach(product -> suggestionSet.add(product.getTitle()));

        if (suggestionSet.size() < safeLimit) {
            List<String> hotTitles = hotProductCacheService.getHotSearchTitles(safeLimit);
            for (String title : hotTitles) {
                if (suggestionSet.size() >= safeLimit) {
                    break;
                }
                suggestionSet.add(title);
            }
        }
        SearchSuggestionResponse response = new SearchSuggestionResponse();
        response.setKeyword(keyword);
        response.setSuggestions(new ArrayList<>(suggestionSet));
        response.setHasMore(keywordPage.hasNext());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRecommendations(String username,
                                                    RecommendationScene scene,
                                                    ProductCategory focusCategory,
                                                    int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 30);
        List<Product> products = recommendationService.getRecommendations(username, scene, focusCategory, safeLimit);
        products.forEach(product -> product.getSeller().getId());
        List<ProductResponse> responses = products.stream()
                .limit(safeLimit)
                .map(ProductMapper::toResponse)
                .collect(Collectors.toList());
        productReviewService.attachRatingSummary(responses);
        return responses;
    }

    private Specification<Product> buildSpecification(ProductSearchFilter filter) {
        Specification<Product> spec = Specification.where(ProductSpecifications.alwaysTrue());
        if (filter == null) {
            return spec;
        }
        // 草稿仅对卖家可见：任何公开列表/搜索都不返回草稿
        spec = spec.and(ProductSpecifications.excludeStatus(ProductStatus.DRAFT));
        // 同时支持根据category枚举和categoryEntity实体进行过滤
        spec = spec.and(ProductSpecifications.hasCategoryId(filter.getCategoryId()));
        spec = spec.and(ProductSpecifications.hasCategoryIds(filter.getCategoryIds()));
        spec = spec.and(ProductSpecifications.hasCategory(filter.getCategory()));
        spec = spec.and(ProductSpecifications.hasCategories(filter.getCategories()));
        if (filter.getStatus() != null) {
            spec = spec.and(ProductSpecifications.hasStatus(filter.getStatus()));
        } else if (!filter.isIncludeInactive()) {
            spec = spec.and(ProductSpecifications.hasStatus(ProductStatus.ON_SALE));
        }
        spec = spec.and(ProductSpecifications.hasStatuses(filter.getStatuses()));
        // 对于公开搜索，只返回已审核通过的商品
        if (!filter.isIncludeInactive()) {
            spec = spec.and(ProductSpecifications.hasAuditStatus(AuditStatus.APPROVED));
        }
        spec = spec.and(ProductSpecifications.priceBetween(filter.getMinPrice(), filter.getMaxPrice()));
        List<String> locationTokens = filter.getLocationKeywords();
        if (CollectionUtils.isEmpty(locationTokens) && StringUtils.hasText(filter.getLocation())) {
            locationTokens = SearchKeywordParser.parse(filter.getLocation());
        }
        spec = spec.and(ProductSpecifications.locationLike(filter.getLocation()));
        spec = spec.and(ProductSpecifications.locationContainsAll(locationTokens));
        spec = spec.and(ProductSpecifications.sellerSchoolLike(filter.getSellerSchool()));
        spec = spec.and(ProductSpecifications.hasImages(filter.getOnlyWithImages()));
        LocalDateTime publishedAfter = resolvePublishedAfter(filter);
        spec = spec.and(ProductSpecifications.createdAfter(publishedAfter != null ? publishedAfter : filter.getPublishedAfter()));
        List<String> keywords = filter.getKeywordTokens();
        if (keywords == null || keywords.isEmpty()) {
            keywords = SearchKeywordParser.parse(filter.getKeyword());
        }
        spec = spec.and(ProductSpecifications.matchKeywords(keywords));
        spec = spec.and(ProductSpecifications.matchClauses(filter.getClauses()));
        return spec;
    }

    private LocalDateTime resolvePublishedAfter(ProductSearchFilter filter) {
        if (filter == null || filter.getPublishedWithinDays() == null || filter.getPublishedWithinDays() <= 0) {
            return null;
        }
        return LocalDateTime.now().minusDays(filter.getPublishedWithinDays());
    }

    public PaginatedResponse<ProductResponse> listMyProducts(String username, ProductStatus status, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Product> result;
        
        // 统一使用Specification构建查询条件，通过Join关联seller表，匹配seller_id
        Specification<Product> spec = Specification.where((root, query, cb) -> cb.equal(root.join("seller").get("id"), user.getId()));
        if (status != null) {
            spec = spec.and(ProductSpecifications.hasStatus(status));
        }
        // 对于卖家自己的商品列表，返回所有审核状态的商品，包括待审核状态
        // 不添加 hasAuditStatus(AuditStatus.APPROVED) 过滤条件
        
        result = productRepository.findAll(spec, pageable);
        
        // 手动加载关联字段以避免懒加载问题
        List<Product> products = result.getContent();
        for (Product product : products) {
            // 强制加载seller字段
            product.getSeller().getId();
            // 强制加载categoryEntity字段
            if (product.getCategoryEntity() != null) {
                product.getCategoryEntity().getId();
            }
        }
        
        List<ProductResponse> responses = products.stream()
            .map(ProductMapper::toResponse)
            .collect(Collectors.toList());
        productReviewService.attachRatingSummary(responses);
        return PaginatedResponse.of(responses, page, size, result.getTotalElements());
    }

        @Transactional
    @Caching(evict = {
        @CacheEvict(value = "product:list", allEntries = true),
        @CacheEvict(value = "product:detail", key = "#productId"),
        @CacheEvict(value = "search:suggestion", allEntries = true)
    })
    public void updateStatus(String username, Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.getSeller().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "无权操作该商品");
        }
        product.setStatus(status);
        hotProductCacheService.evictHotSearchCaches();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "product:list", allEntries = true),
        @CacheEvict(value = "product:detail", key = "#productId"),
        @CacheEvict(value = "search:suggestion", allEntries = true)
    })
    public ProductResponse updateProduct(String username, Long productId, ProductRequest request) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        // 验证商品所属权
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "无权操作该商品");
        }
        
        // 更新商品信息
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        
        // 更新分类信息
        Category categoryEntity = null;
        ProductCategory resolvedCategory = request.getCategory();
        
        // 优先处理categoryId
        if (request.getCategoryId() != null) {
            categoryEntity = categoryService.getEnabledEntity(request.getCategoryId());
            product.setCategoryEntity(categoryEntity);
            // 从categoryEntity获取code映射到ProductCategory枚举
            if (categoryEntity != null) {
                resolvedCategory = mapLegacyCategory(categoryEntity.getCode());
            }
        } 
        // 如果没有categoryId但有category枚举值
        else if (resolvedCategory != null) {
            // 清空categoryEntity，因为用户选择了枚举分类而非动态分类
            product.setCategoryEntity(null);
        }
        // 如果都没有，保持原有分类不变
        else {
            resolvedCategory = product.getCategory();
        }
        
        // 设置最终的category枚举值
        product.setCategory(resolvedCategory == null ? ProductCategory.OTHER : resolvedCategory);
        
        product.setImages(request.getImages());
        product.setTags(request.getTags());
        product.setContactInfo(request.getContactInfo());
        product.setLocation(request.getLocation());
        
        // 如果商品状态从草稿变为上架，更新审核状态
        if (product.getStatus() == ProductStatus.DRAFT && !request.isDraft()) {
            product.setStatus(ProductStatus.ON_SALE);
            product.setAuditStatus(AuditStatus.PENDING);
        } else if (request.isDraft()) {
            product.setStatus(ProductStatus.DRAFT);
        }
        
        productRepository.save(product);
        ProductResponse response = ProductMapper.toResponse(product);
        productReviewService.attachRatingSummary(response);
        hotProductCacheService.evictHotSearchCaches();
        return response;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "product:list", allEntries = true),
        @CacheEvict(value = "search:suggestion", allEntries = true)
    })
    public BatchOperationResult batchUpdateStatus(String username, List<Long> productIds, ProductStatus status) {
        if (productIds == null || productIds.isEmpty()) {
            return BatchOperationResult.builder()
                .successCount(0)
                .failedCount(0)
                .totalCount(0)
                .message("无待处理商品")
                .build();
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Product> products = productRepository.findByIdInAndSellerId(productIds, user.getId());
        Set<Long> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        long failed = productIds.stream().filter(id -> !foundIds.contains(id)).count();

        for (Product product : products) {
            product.setStatus(status);
        }
        hotProductCacheService.evictHotSearchCaches();
        return BatchOperationResult.builder()
            .successCount(products.size())
            .failedCount(failed)
            .totalCount(productIds.size())
            .message("批量更新完成")
            .build();
    }

    public PaginatedResponse<ProductResponse> listPendingProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Specification<Product> spec = Specification.where(ProductSpecifications.hasAuditStatus(AuditStatus.PENDING))
            .and(ProductSpecifications.excludeStatus(ProductStatus.DRAFT));
        Page<Product> result = productRepository.findAll(spec, pageable);
        
        // 手动加载seller字段以避免懒加载问题
        List<Product> products = result.getContent();
        for (Product product : products) {
            // 强制加载seller字段
            product.getSeller().getId();
        }
        
        List<ProductResponse> responses = products.stream()
            .map(ProductMapper::toResponse)
            .collect(Collectors.toList());
        productReviewService.attachRatingSummary(responses);
        return PaginatedResponse.of(responses, page, size, result.getTotalElements());
    }

        @Transactional
        @Caching(evict = {
            @CacheEvict(value = "product:list", allEntries = true),
            @CacheEvict(value = "product:detail", key = "#productId"),
            @CacheEvict(value = "search:suggestion", allEntries = true)
        })
    public ProductResponse reviewProduct(Long productId, boolean approved, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setAuditStatus(approved ? AuditStatus.APPROVED : AuditStatus.REJECTED);
        product.setRemark(reason);
        if (!approved) {
            product.setStatus(ProductStatus.OFF_SALE);
        }
        ProductResponse response = ProductMapper.toResponse(product);
        productReviewService.attachRatingSummary(response);
        hotProductCacheService.evictHotSearchCaches();
        return response;
    }
}
