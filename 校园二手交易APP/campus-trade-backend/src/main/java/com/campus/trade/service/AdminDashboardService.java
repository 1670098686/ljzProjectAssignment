package com.campus.trade.service;

import com.campus.trade.dto.admin.DashboardOverviewResponse;
import com.campus.trade.dto.admin.DashboardOverviewResponse.MetricCard;
import com.campus.trade.dto.admin.DashboardOverviewResponse.RankingItem;
import com.campus.trade.dto.admin.DashboardOverviewResponse.StatusBreakdown;
import com.campus.trade.dto.admin.DashboardOverviewResponse.TrendPoint;
import com.campus.trade.dto.admin.DashboardOverviewResponse.TrendSeries;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.model.enums.ReviewStatus;
import com.campus.trade.repository.FavoriteRepository;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.ProductReviewRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.projection.OrderStatusCountView;
import com.campus.trade.repository.projection.SchoolCountView;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final FavoriteRepository favoriteRepository;
    private final MessageRepository messageRepository;
    private final ProductReviewRepository productReviewRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 ProductRepository productRepository,
                                 OrderRepository orderRepository,
                                 FavoriteRepository favoriteRepository,
                                 MessageRepository messageRepository,
                                 ProductReviewRepository productReviewRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.favoriteRepository = favoriteRepository;
        this.messageRepository = messageRepository;
        this.productReviewRepository = productReviewRepository;
    }

        @Cacheable(value = "admin:dashboard:overview",
            key = "T(com.campus.trade.util.CacheKeyUtils).dashboardOverviewKey(#days)")
        public DashboardOverviewResponse getOverview(int days) {
        int safeDays = clampDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart = now.minusDays(safeDays);
        LocalDateTime previousStart = currentStart.minusDays(safeDays);

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setUserMetrics(buildUserMetrics(now, currentStart, previousStart));
        response.setProductMetrics(buildProductMetrics(now, currentStart, previousStart));
        response.setOrderMetrics(buildOrderMetrics(now, currentStart, previousStart));
        response.setInteractionMetrics(buildInteractionMetrics(now, currentStart, previousStart));
        response.setOrderStatusBreakdown(buildStatusBreakdown());
        response.setCategoryRankings(buildProductCategoryRanking(5));
        response.setSchoolRankings(buildSchoolRanking(5));
        response.setTrends(buildTrendSeriesList(safeDays));
        return response;
    }

        @Cacheable(value = "admin:dashboard:trend",
            key = "T(com.campus.trade.util.CacheKeyUtils).dashboardTrendKey(#metric, #days)")
        public TrendSeries getTrendSeries(String metric, int days) {
        int safeDays = clampDays(days);
        String normalized = metric == null ? "" : metric.trim().toLowerCase();
        return switch (normalized) {
            case "users" -> buildTrendSeries("新增用户", safeDays,
                    (start, end) -> userRepository.countByCreateTimeBetween(start, end));
            case "orders" -> buildTrendSeries("订单量", safeDays,
                    (start, end) -> orderRepository.countByCreateTimeBetween(start, end));
            case "messages" -> buildTrendSeries("消息量", safeDays,
                    (start, end) -> messageRepository.countByCreateTimeBetween(start, end));
            case "gmv" -> buildTrendSeries("GMV", safeDays,
                    (start, end) -> orderRepository.sumTotalPriceByStatusAndCreateTimeBetween(OrderStatus.COMPLETED, start, end));
            default -> throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未知趋势指标: " + metric);
        };
    }

        @Cacheable(value = "admin:dashboard:ranking",
            key = "T(com.campus.trade.util.CacheKeyUtils).dashboardRankingKey(#type, #limit)")
        public List<RankingItem> getRankings(String type, int limit) {
        int safeLimit = Math.min(Math.max(limit, 3), 20);
        String normalized = type == null ? "" : type.trim().toLowerCase();
        return switch (normalized) {
            case "product-category" -> buildProductCategoryRanking(safeLimit);
            case "order-category" -> buildOrderCategoryRanking(safeLimit);
            case "school" -> buildSchoolRanking(safeLimit);
            default -> throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未知排行榜类型: " + type);
        };
    }

    private List<MetricCard> buildUserMetrics(LocalDateTime now,
                                              LocalDateTime currentStart,
                                              LocalDateTime previousStart) {
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.countByEmailVerifiedTrue();
        long newUsers = userRepository.countByCreateTimeBetween(currentStart, now);
        long prevNewUsers = userRepository.countByCreateTimeBetween(previousStart, currentStart);
        long activeUsers = userRepository.countByLastLoginAfter(now.minusDays(7));
        long deletionQueue = userRepository.countByDeleteRequestedTrue();

        List<MetricCard> cards = new ArrayList<>();
        cards.add(metric("累计用户", totalUsers, "", calcDelta(newUsers, prevNewUsers)));
        cards.add(metric("邮箱验证率", roundPercent(totalUsers, verifiedUsers), "%", 0));
        cards.add(metric("近7日活跃", activeUsers, "", 0));
        cards.add(metric("注销倒计时", deletionQueue, "", 0));
        return cards;
    }

    private List<MetricCard> buildProductMetrics(LocalDateTime now,
                                                 LocalDateTime currentStart,
                                                 LocalDateTime previousStart) {
        long onSale = productRepository.countByStatus(ProductStatus.ON_SALE);
        long sold = productRepository.countByStatus(ProductStatus.SOLD);
        long pendingAudit = productRepository.countByAuditStatus(AuditStatus.PENDING);
        long newProducts = productRepository.countByCreateTimeBetween(currentStart, now);
        long prevNewProducts = productRepository.countByCreateTimeBetween(previousStart, currentStart);

        List<MetricCard> cards = new ArrayList<>();
        cards.add(metric("在售商品", onSale, "", 0));
        cards.add(metric("待审核", pendingAudit, "", 0));
        cards.add(metric("已售罄", sold, "", 0));
        cards.add(metric("新增上架", newProducts, "", calcDelta(newProducts, prevNewProducts)));
        return cards;
    }

    private List<MetricCard> buildOrderMetrics(LocalDateTime now,
                                               LocalDateTime currentStart,
                                               LocalDateTime previousStart) {
        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);
        BigDecimal totalGmv = orderRepository.sumTotalPriceByStatus(OrderStatus.COMPLETED);
        BigDecimal currentGmv = orderRepository.sumTotalPriceByStatusAndCreateTimeBetween(OrderStatus.COMPLETED, currentStart, now);
        BigDecimal previousGmv = orderRepository.sumTotalPriceByStatusAndCreateTimeBetween(OrderStatus.COMPLETED, previousStart, currentStart);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT)
                + orderRepository.countByStatus(OrderStatus.PENDING_SHIPMENT)
                + orderRepository.countByStatus(OrderStatus.PENDING_RECEIPT);
        long refunding = orderRepository.countByRefundStatus(RefundStatus.REQUESTED)
                + orderRepository.countByRefundStatus(RefundStatus.PROCESSING);
        long newOrders = orderRepository.countByCreateTimeBetween(currentStart, now);
        long prevOrders = orderRepository.countByCreateTimeBetween(previousStart, currentStart);
        double completionRate = totalOrders == 0 ? 0D : (double) completedOrders / totalOrders * 100;

        List<MetricCard> cards = new ArrayList<>();
        cards.add(metric("GMV", totalGmv, "元", calcDelta(currentGmv, previousGmv)));
        cards.add(metric("订单完成率", roundDouble(completionRate), "%", 0));
        cards.add(metric("待处理订单", pendingOrders, "", 0));
        cards.add(metric("新增订单", newOrders, "", calcDelta(newOrders, prevOrders)));
        cards.add(metric("退款处理中", refunding, "", 0));
        return cards;
    }

    private List<MetricCard> buildInteractionMetrics(LocalDateTime now,
                                                     LocalDateTime currentStart,
                                                     LocalDateTime previousStart) {
        long messages = messageRepository.countByCreateTimeBetween(currentStart, now);
        long prevMessages = messageRepository.countByCreateTimeBetween(previousStart, currentStart);
        long favorites = favoriteRepository.countByCreateTimeBetween(currentStart, now);
        long prevFavorites = favoriteRepository.countByCreateTimeBetween(previousStart, currentStart);
        long reviews = productReviewRepository.countByStatusAndCreateTimeBetween(ReviewStatus.PUBLISHED, currentStart, now);
        long prevReviews = productReviewRepository.countByStatusAndCreateTimeBetween(ReviewStatus.PUBLISHED, previousStart, currentStart);

        List<MetricCard> cards = new ArrayList<>();
        cards.add(metric("消息量", messages, "", calcDelta(messages, prevMessages)));
        cards.add(metric("收藏量", favorites, "", calcDelta(favorites, prevFavorites)));
        cards.add(metric("评价发布", reviews, "", calcDelta(reviews, prevReviews)));
        return cards;
    }

    private List<StatusBreakdown> buildStatusBreakdown() {
        List<OrderStatusCountView> aggregates = orderRepository.aggregateStatusCounts();
        Map<OrderStatus, Long> statusMap = new EnumMap<>(OrderStatus.class);
        for (OrderStatusCountView view : aggregates) {
            statusMap.put(view.getStatus(), view.getTotal());
        }
        List<StatusBreakdown> breakdowns = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            long total = statusMap.getOrDefault(status, 0L);
            breakdowns.add(new StatusBreakdown(status.name(), total));
        }
        return breakdowns;
    }

    private List<RankingItem> buildProductCategoryRanking(int limit) {
        return productRepository.countApprovedProductsByCategory(AuditStatus.APPROVED, PageRequest.of(0, limit))
                .stream()
                .map(view -> new RankingItem(view.getCategory().name(), view.getTotal()))
                .toList();
    }

    private List<RankingItem> buildOrderCategoryRanking(int limit) {
        return orderRepository.topCategoriesByCompletedOrders(PageRequest.of(0, limit))
                .stream()
                .map(view -> new RankingItem(view.getCategory().name(), view.getTotal()))
                .toList();
    }

    private List<RankingItem> buildSchoolRanking(int limit) {
        return orderRepository.topSchoolsByOrders(PageRequest.of(0, limit))
                .stream()
                .map(view -> new RankingItem(view.getSchool(), view.getTotal()))
                .toList();
    }

    private List<TrendSeries> buildTrendSeriesList(int days) {
        int safeDays = clampDays(days);
        List<TrendSeries> series = new ArrayList<>();
        series.add(buildTrendSeries("新增用户", safeDays,
                (start, end) -> userRepository.countByCreateTimeBetween(start, end)));
        series.add(buildTrendSeries("订单量", safeDays,
                (start, end) -> orderRepository.countByCreateTimeBetween(start, end)));
        series.add(buildTrendSeries("消息量", safeDays,
                (start, end) -> messageRepository.countByCreateTimeBetween(start, end)));
        return series;
    }

    private TrendSeries buildTrendSeries(String name,
                                         int days,
                                         BiFunction<LocalDateTime, LocalDateTime, Number> counter) {
        int safeDays = clampDays(days);
        TrendSeries series = new TrendSeries(name);
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = startDate.plusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Number value = counter.apply(start, end);
            series.getPoints().add(new TrendPoint(date, value));
        }
        return series;
    }

    private MetricCard metric(String title, Number value, String unit, double delta) {
        MetricCard card = new MetricCard();
        card.setTitle(title);
        card.setValue(value);
        card.setUnit(unit);
        card.setDelta(roundDouble(delta));
        return card;
    }

    private double calcDelta(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100D : 0D;
        }
        return (double) (current - previous) / previous * 100D;
    }

    private double calcDelta(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;
        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) == 0 ? 0D : 100D;
        }
        BigDecimal diff = safeCurrent.subtract(safePrevious);
        return diff.divide(safePrevious, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double roundPercent(long total, long part) {
        if (total == 0) {
            return 0D;
        }
        double value = (double) part / total * 100D;
        return roundDouble(value);
    }

    private double roundDouble(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private int clampDays(int days) {
        return Math.min(Math.max(days, 1), 30);
    }

}
