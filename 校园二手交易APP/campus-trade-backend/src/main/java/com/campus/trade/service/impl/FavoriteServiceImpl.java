package com.campus.trade.service.impl;

import com.campus.trade.common.PageMeta;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.favorite.FavoriteCategoryStat;
import com.campus.trade.dto.favorite.FavoriteListQuery;
import com.campus.trade.dto.favorite.FavoriteStatusResponse;
import com.campus.trade.dto.favorite.FavoriteSummaryResponse;
import com.campus.trade.dto.favorite.FavoriteTopProductResponse;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Favorite;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.FavoriteSortMode;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.projection.FavoriteCountView;
import com.campus.trade.repository.projection.FavoriteStatusCountView;
import com.campus.trade.repository.projection.ProductHeatView;
import org.springframework.data.domain.Pageable;
import com.campus.trade.repository.FavoriteRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.service.FavoriteService;
import com.campus.trade.service.ProductReviewService;
import com.campus.trade.util.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductReviewService productReviewService;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                               UserRepository userRepository,
                               ProductRepository productRepository,
                               ProductReviewService productReviewService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productReviewService = productReviewService;
    }

    @Override
    @Transactional
    public void addFavorite(String username, Long productId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "商品已下架或售罄");
        }

        if (product.getSeller() != null && product.getSeller().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能收藏自己的商品");
        }

        if (favoriteRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        favoriteRepository.save(favorite);

        product.setLikeCount(safeIncrement(product.getLikeCount()));
    }

    @Override
    @Transactional
    public void removeFavorite(String username, Long productId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!favoriteRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return;
        }

        favoriteRepository.deleteByUserIdAndProductId(user.getId(), productId);

        int currentLikes = safeValue(product.getLikeCount());
        if (currentLikes > 0) {
            product.setLikeCount(currentLikes - 1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> listMyFavorites(String username, FavoriteListQuery query) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FavoriteListQuery effective = sanitizeQuery(query);
        PageRequest pageRequest = PageRequest.of(effective.getPage() - 1, effective.getSize(), buildSort(effective));
        Page<Favorite> favorites = favoriteRepository.findAll(buildSpecification(user.getId(), effective), pageRequest);

        PageMeta meta = new PageMeta(effective.getPage(), effective.getSize(), favorites.getTotalElements());

        List<ProductResponse> responses = favorites.getContent().stream()
                .map(Favorite::getProduct)
                .filter(Objects::nonNull)
                .filter(product -> effective.getOnlyAvailable() == null || !effective.getOnlyAvailable() || product.getStatus() == ProductStatus.ON_SALE)
                .map(product -> {
                    ProductResponse response = ProductMapper.toResponse(product);
                    response.setFavorited(true);
                    return response;
                })
                .collect(Collectors.toList());
        productReviewService.attachRatingSummary(responses);
        syncLikeCounters(responses);
        return new PaginatedResponse<>(responses, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public void attachFavoriteFlags(String username, List<ProductResponse> responses) {
        if (CollectionUtils.isEmpty(responses) || isAnonymous(username)) {
            return;
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }
        List<Long> productIds = responses.stream()
                .map(ProductResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return;
        }
        Set<Long> favoriteIds = new HashSet<>(favoriteRepository.findProductIdsByUserIdAndProductIdIn(user.getId(), productIds));
        responses.stream()
            .filter(response -> response.getId() != null)
            .forEach(response -> response.setFavorited(favoriteIds.contains(response.getId())));
        syncLikeCounters(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteStatusResponse> getFavoriteStatus(String username, List<Long> productIds) {
        List<FavoriteStatusResponse> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(productIds)) {
            return result;
        }
        if (isAnonymous(username)) {
            productIds.stream().distinct().forEach(id -> result.add(new FavoriteStatusResponse(id, false)));
            return result;
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            productIds.stream().distinct().forEach(id -> result.add(new FavoriteStatusResponse(id, false)));
            return result;
        }
        Set<Long> favorites = new HashSet<>(favoriteRepository.findProductIdsByUserIdAndProductIdIn(
                user.getId(), productIds));
        productIds.stream()
                .distinct()
                .forEach(id -> result.add(new FavoriteStatusResponse(id, favorites.contains(id))));
        return result;
    }

        @Override
        @Transactional(readOnly = true)
        public FavoriteSummaryResponse getFavoriteSummary(String username, int topN) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        FavoriteSummaryResponse response = new FavoriteSummaryResponse();
        response.setTotalFavorites(favoriteRepository.countByUserId(user.getId()));
            Map<ProductStatus, Long> statusMap = favoriteRepository.countFavoriteStatus(user.getId()).stream()
                .filter(view -> view.getStatus() != null)
                .collect(Collectors.toMap(FavoriteStatusCountView::getStatus, FavoriteStatusCountView::getTotal));
        response.setOnSaleFavorites(statusMap.getOrDefault(ProductStatus.ON_SALE, 0L));
        response.setSoldOutFavorites(statusMap.getOrDefault(ProductStatus.SOLD, 0L));
        response.setRemovedFavorites(statusMap.getOrDefault(ProductStatus.DELETED, 0L) +
            statusMap.getOrDefault(ProductStatus.OFF_SALE, 0L));
        Favorite last = favoriteRepository.findTopByUserIdOrderByCreateTimeDesc(user.getId());
        response.setLastUpdatedAt(last == null ? null : last.getCreateTime());

        List<CategoryCountView> categories = favoriteRepository.countFavoriteCategories(user.getId());
        response.setCategoryDistribution(mapCategoryStats(categories, response.getTotalFavorites()));

            Pageable pageable = PageRequest.of(0, Math.min(Math.max(topN, 3), 20), Sort.unsorted());
        List<ProductHeatView> trending = favoriteRepository.topFavoritedProducts(pageable);
        response.setTrendingProducts(mapTrendingProducts(trending));
        return response;
        }

    private FavoriteListQuery sanitizeQuery(FavoriteListQuery query) {
        FavoriteListQuery effective = query == null ? new FavoriteListQuery() : query;
        effective.setPage(Math.max(1, effective.getPage()));
        effective.setSize(Math.max(1, Math.min(50, effective.getSize())));
        if (effective.getSortMode() == null) {
            effective.setSortMode(FavoriteSortMode.LATEST);
        }
        if (StringUtils.hasText(effective.getKeyword())) {
            effective.setKeyword(effective.getKeyword().trim());
        }
        return effective;
    }

    private Sort buildSort(FavoriteListQuery query) {
        return switch (query.getSortMode()) {
            case PRICE_ASC -> Sort.by(Sort.Order.asc("product.price"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("product.price"));
            case POPULARITY -> Sort.by(Sort.Order.desc("product.likeCount"), Sort.Order.desc("product.viewCount"));
            default -> Sort.by(Sort.Order.desc("createTime"));
        };
    }

    private Specification<Favorite> buildSpecification(Long userId, FavoriteListQuery query) {
        Specification<Favorite> spec = Specification.where((root, cq, cb) -> cb.equal(root.get("user").get("id"), userId));
        if (query.getCategory() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("product").get("category"), query.getCategory()));
        }
        if (query.getStatus() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("product").get("status"), query.getStatus()));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String like = "%" + query.getKeyword().trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("product").get("title")), like),
                cb.like(cb.lower(root.get("product").get("description")), like)
            ));
        }
        if (Boolean.TRUE.equals(query.getOnlyAvailable())) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("product").get("status"), ProductStatus.ON_SALE));
        }
        return spec;
    }

    private List<FavoriteCategoryStat> mapCategoryStats(List<CategoryCountView> categories, long total) {
        if (CollectionUtils.isEmpty(categories) || total <= 0) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(view -> new FavoriteCategoryStat(view.getCategory(), view.getTotal(), roundRatio(view.getTotal(), total)))
                .collect(Collectors.toList());
    }

    private double roundRatio(long count, long total) {
        return Math.round((count * 1000D / total)) / 10D;
    }

    private List<FavoriteTopProductResponse> mapTrendingProducts(List<ProductHeatView> views) {
        if (CollectionUtils.isEmpty(views)) {
            return Collections.emptyList();
        }
        return views.stream()
                .map(view -> new FavoriteTopProductResponse(view.getProductId(), view.getProductTitle(), view.getTotal()))
                .collect(Collectors.toList());
    }

    private void syncLikeCounters(List<ProductResponse> responses) {
        if (CollectionUtils.isEmpty(responses)) {
            return;
        }
        List<Long> productIds = responses.stream()
                .map(ProductResponse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return;
        }
        Map<Long, Long> counts = favoriteRepository.countByProductIds(productIds).stream()
                .collect(Collectors.toMap(FavoriteCountView::getProductId, FavoriteCountView::getTotal));
        responses.forEach(response -> {
            Long count = counts.get(response.getId());
            if (count != null) {
                response.setLikeCount(count.intValue());
            }
        });
    }

    private boolean isAnonymous(String username) {
        return !StringUtils.hasText(username) || "anonymousUser".equalsIgnoreCase(username);
    }

    private int safeIncrement(Integer current) {
        return safeValue(current) + 1;
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }
}
