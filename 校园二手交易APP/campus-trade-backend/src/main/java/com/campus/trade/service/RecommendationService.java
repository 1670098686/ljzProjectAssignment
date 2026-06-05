package com.campus.trade.service;

import com.campus.trade.dto.product.RecommendationEventRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.RecommendationEvent;
import com.campus.trade.model.entity.RecommendationSnapshot;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.RecommendationScene;
import com.campus.trade.repository.FavoriteRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.RecommendationEventRepository;
import com.campus.trade.repository.RecommendationSnapshotRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.spec.ProductSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final int SNAPSHOT_LIMIT = 30;
    private static final int CANDIDATE_MULTIPLIER = 3;

    private final RecommendationSnapshotRepository snapshotRepository;
    private final RecommendationEventRepository eventRepository;
    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public RecommendationService(RecommendationSnapshotRepository snapshotRepository,
                                 RecommendationEventRepository eventRepository,
                                 ProductRepository productRepository,
                                 FavoriteRepository favoriteRepository,
                                 OrderRepository orderRepository,
                                 UserRepository userRepository) {
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
        this.productRepository = productRepository;
        this.favoriteRepository = favoriteRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getRecommendations(String username,
                                            RecommendationScene scene,
                                            ProductCategory focusCategory,
                                            int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Optional<User> userOpt = resolveUser(username);
        User user = userOpt.orElse(null);
        Set<Long> excluded = buildExclusionSet(user, scene);
        Set<Long> seen = new LinkedHashSet<>();
        List<Product> recommendations = new ArrayList<>();
        int fetchSize = safeLimit * 2;

        appendCandidates(recommendations,
                loadFromSnapshots(user, scene, focusCategory, fetchSize),
                user, excluded, seen, safeLimit);
        if (recommendations.size() < safeLimit) {
            appendCandidates(recommendations,
                    loadFromSnapshots(null, scene, focusCategory, fetchSize),
                    user, excluded, seen, safeLimit);
        }
        if (recommendations.size() < safeLimit) {
            appendCandidates(recommendations,
                    fallbackTrending(focusCategory, fetchSize),
                    user, excluded, seen, safeLimit);
        }
        return recommendations;
    }

    @Transactional
    public void recordEvent(String username, RecommendationEventRequest request) {
        Optional<User> userOpt = resolveUser(username);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        RecommendationEvent event = new RecommendationEvent();
        event.setUser(userOpt.orElse(null));
        event.setProduct(product);
        event.setScene(request.getScene() == null ? RecommendationScene.HOME : request.getScene());
        event.setEventType(request.getEventType());
        event.setMetadata(request.getMetadata());
        eventRepository.save(event);
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void rebuildSnapshotsJob() {
        log.info("Rebuilding recommendation snapshots...");
        LocalDateTime now = LocalDateTime.now();
        snapshotRepository.deleteByExpireTimeBefore(now.minusDays(1));
        LocalDateTime expireTime = now.plusHours(12);
        generateGlobalSnapshots(expireTime);
        List<User> activeUsers = userRepository.findByStatus(AccountStatus.ACTIVE);
        for (User user : activeUsers) {
            generateUserSnapshots(user, RecommendationScene.HOME, expireTime);
            if (StringUtils.hasText(user.getSchool())) {
                generateUserSnapshots(user, RecommendationScene.SCHOOL, expireTime);
            }
        }
        log.info("Recommendation snapshots refreshed for {} users", activeUsers.size());
    }

    private void generateUserSnapshots(User user, RecommendationScene scene, LocalDateTime expireTime) {
        Map<ProductCategory, Double> categoryWeights = buildCategoryWeights(user.getId());
        List<Product> candidates = collectCandidates(categoryWeights.keySet(), scene,
            SNAPSHOT_LIMIT * CANDIDATE_MULTIPLIER, user.getSchool());
        List<RecommendationSnapshot> snapshots = scoreAndBuildSnapshots(candidates, user, scene, categoryWeights, expireTime);
        saveSnapshots(user, scene, snapshots);
    }

    private void generateGlobalSnapshots(LocalDateTime expireTime) {
        Map<ProductCategory, Double> weights = Collections.emptyMap();
        List<Product> trending = fallbackTrending(null, SNAPSHOT_LIMIT * CANDIDATE_MULTIPLIER);
        List<RecommendationSnapshot> snapshots = scoreAndBuildSnapshots(trending, null, RecommendationScene.HOME, weights, expireTime);
        saveSnapshots(null, RecommendationScene.HOME, snapshots);
    }

    private void saveSnapshots(User user, RecommendationScene scene, List<RecommendationSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        if (user == null) {
            List<RecommendationSnapshot> existing = snapshotRepository.findByUserIsNullAndSceneAndExpireTimeAfterOrderByScoreDesc(scene, LocalDateTime.now());
            snapshotRepository.deleteAll(existing);
        } else {
            List<RecommendationSnapshot> existing = snapshotRepository.findByUserIdAndSceneAndExpireTimeAfterOrderByScoreDesc(user.getId(), scene, LocalDateTime.now());
            snapshotRepository.deleteAll(existing);
        }
        snapshotRepository.saveAll(snapshots);
    }

    private List<RecommendationSnapshot> scoreAndBuildSnapshots(List<Product> candidates,
                                                                User user,
                                                                RecommendationScene scene,
                                                                Map<ProductCategory, Double> categoryWeights,
                                                                LocalDateTime expireTime) {
        if (CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        LocalDateTime now = LocalDateTime.now();
        List<RecommendationSnapshot> snapshots = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (Product product : candidates) {
            if (product.getId() == null || !seen.add(product.getId())) {
                continue;
            }
            RecommendationSnapshot snapshot = new RecommendationSnapshot();
            snapshot.setUser(user);
            snapshot.setProduct(product);
            snapshot.setScene(scene);
            double score = calculateScore(product, categoryWeights, now);
            snapshot.setScore(score);
            snapshot.setReason(buildReason(product, categoryWeights));
            snapshot.setExpireTime(expireTime);
            snapshots.add(snapshot);
        }
        snapshots.sort(Comparator.comparingDouble(RecommendationSnapshot::getScore).reversed());
        return snapshots.stream().limit(SNAPSHOT_LIMIT).collect(Collectors.toList());
    }

    private double calculateScore(Product product,
                                  Map<ProductCategory, Double> categoryWeights,
                                  LocalDateTime now) {
        double weight = categoryWeights.getOrDefault(product.getCategory(), 1.0);
        double likeCount = product.getLikeCount() == null ? 0 : product.getLikeCount();
        double viewCount = product.getViewCount() == null ? 0 : product.getViewCount();
        double engagement = likeCount * 0.6 + viewCount * 0.4;
        LocalDateTime updateTime = product.getUpdateTime();
        double hours = updateTime == null ? 48D : Math.max(Duration.between(updateTime, now).toHours(), 1D);
        double recency = 1D / hours;
        return weight * 0.5 + engagement * 0.3 + recency * 0.2;
    }

    private String buildReason(Product product, Map<ProductCategory, Double> categoryWeights) {
        if (categoryWeights.containsKey(product.getCategory())) {
            return "偏好品类 " + product.getCategory().name();
        }
        return "热门推荐";
    }

    private void appendCandidates(List<Product> target,
                                  List<Product> candidates,
                                  User user,
                                  Set<Long> excluded,
                                  Set<Long> seen,
                                  int limit) {
        if (CollectionUtils.isEmpty(candidates) || target.size() >= limit) {
            return;
        }
        for (Product product : candidates) {
            if (target.size() >= limit) {
                break;
            }
            if (shouldSkipProduct(product, user, excluded, seen)) {
                continue;
            }
            target.add(product);
            seen.add(product.getId());
        }
    }

    private boolean shouldSkipProduct(Product product,
                                      User user,
                                      Set<Long> excluded,
                                      Set<Long> seen) {
        if (product == null || product.getId() == null) {
            return true;
        }
        if (seen.contains(product.getId())) {
            return true;
        }
        if (excluded.contains(product.getId())) {
            return true;
        }
        if (product.getStatus() != ProductStatus.ON_SALE || product.getAuditStatus() != AuditStatus.APPROVED) {
            return true;
        }
        if (product.getSeller() != null) {
            product.getSeller().getId();
        }
        if (user != null && product.getSeller() != null &&
                Objects.equals(product.getSeller().getId(), user.getId())) {
            return true;
        }
        return false;
    }

    private Set<Long> buildExclusionSet(User user, RecommendationScene scene) {
        if (user == null) {
            return Collections.emptySet();
        }
        Set<Long> excluded = new LinkedHashSet<>();
        List<Long> favorites = favoriteRepository.findProductIdsByUserId(user.getId());
        if (!CollectionUtils.isEmpty(favorites)) {
            excluded.addAll(favorites);
        }
        List<Long> purchased = orderRepository.findCompletedProductIdsByBuyer(user.getId());
        if (!CollectionUtils.isEmpty(purchased)) {
            excluded.addAll(purchased);
        }
        LocalDateTime since = LocalDateTime.now().minusHours(12);
        List<Long> recent = eventRepository.findRecentProductIds(user.getId(), scene, since);
        if (!CollectionUtils.isEmpty(recent)) {
            excluded.addAll(recent);
        }
        return excluded;
    }

    private Map<ProductCategory, Double> buildCategoryWeights(Long userId) {
        Map<ProductCategory, Double> weights = new EnumMap<>(ProductCategory.class);
        List<CategoryCountView> favoriteCategories = favoriteRepository.countFavoriteCategories(userId);
        aggregateWeights(weights, favoriteCategories, 1.0);
        List<CategoryCountView> orderCategories = orderRepository.topCategoriesByBuyer(userId, PageRequest.of(0, 5));
        aggregateWeights(weights, orderCategories, 1.5);
        return weights;
    }

    private void aggregateWeights(Map<ProductCategory, Double> weights,
                                  List<CategoryCountView> views,
                                  double factor) {
        if (CollectionUtils.isEmpty(views)) {
            return;
        }
        for (CategoryCountView view : views) {
            weights.merge(view.getCategory(), view.getTotal() * factor, Double::sum);
        }
    }

    private List<Product> collectCandidates(Collection<ProductCategory> categories,
                                            RecommendationScene scene,
                                            int target,
                                            String school) {
        Pageable pageable = PageRequest.of(0, Math.max(target, 10), Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createTime")));
        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasStatus(ProductStatus.ON_SALE))
                .and(ProductSpecifications.hasAuditStatus(AuditStatus.APPROVED));
        if (!CollectionUtils.isEmpty(categories)) {
            spec = spec.and(ProductSpecifications.hasCategories(categories));
        }
        if (scene == RecommendationScene.SCHOOL && StringUtils.hasText(school)) {
            spec = spec.and(ProductSpecifications.sellerSchoolEquals(school));
        }
        Page<Product> page = productRepository.findAll(spec, pageable);
        return new ArrayList<>(page.getContent());
    }

    private List<Product> fallbackTrending(ProductCategory focusCategory, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(0, Math.max(limit, 10), Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("viewCount")));
        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasStatus(ProductStatus.ON_SALE))
                .and(ProductSpecifications.hasAuditStatus(AuditStatus.APPROVED));
        if (focusCategory != null) {
            spec = spec.and(ProductSpecifications.hasCategory(focusCategory));
        }
        Page<Product> page = productRepository.findAll(spec, pageable);
        return new ArrayList<>(page.getContent());
    }

    private List<Product> loadFromSnapshots(User user,
                                            RecommendationScene scene,
                                            ProductCategory focusCategory,
                                            int limit) {
        List<RecommendationSnapshot> snapshots;
        LocalDateTime now = LocalDateTime.now();
        if (user != null) {
            snapshots = snapshotRepository.findByUserIdAndSceneAndExpireTimeAfterOrderByScoreDesc(user.getId(), scene, now);
        } else {
            snapshots = snapshotRepository.findByUserIsNullAndSceneAndExpireTimeAfterOrderByScoreDesc(scene, now);
        }
        if (CollectionUtils.isEmpty(snapshots)) {
            return new ArrayList<>();
        }
        return snapshots.stream()
                .map(RecommendationSnapshot::getProduct)
                .filter(product -> focusCategory == null || product.getCategory() == focusCategory)
                .limit(limit)
                .map(product -> {
                    product.getSeller().getId();
                    return product;
                })
                .collect(Collectors.toList());
    }

    private Optional<User> resolveUser(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }
}
