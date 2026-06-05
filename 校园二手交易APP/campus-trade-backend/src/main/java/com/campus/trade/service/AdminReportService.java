package com.campus.trade.service;

import com.campus.trade.dto.admin.AdminOrderReportResponse;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.repository.FavoriteRepository;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.projection.CategoryCountView;
import com.campus.trade.repository.projection.OrderStatusCountView;
import com.campus.trade.repository.projection.ProductHeatView;
import com.campus.trade.repository.projection.SchoolCountView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    private static final int MAX_DAYS = 30;
    private static final int MAX_TOP = 10;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;
    private final MessageRepository messageRepository;

    public AdminReportService(OrderRepository orderRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository,
                              FavoriteRepository favoriteRepository,
                              MessageRepository messageRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.favoriteRepository = favoriteRepository;
        this.messageRepository = messageRepository;
    }

        @Transactional(readOnly = true)
        @Cacheable(value = "admin:order-report",
            key = "T(com.campus.trade.util.CacheKeyUtils).adminOrderReportKey(#days, #top)")
        public AdminOrderReportResponse getOrderReport(int days, int top) {
        int safeDays = Math.min(Math.max(days, 1), MAX_DAYS);
        int safeTop = Math.min(Math.max(top, 1), MAX_TOP);
        AdminOrderReportResponse response = new AdminOrderReportResponse();
        response.setSummary(buildSummary());
        response.setStatusDistribution(buildStatusDistribution());
        response.setTopCategories(buildTopCategories(safeTop));
        response.setTopSchools(buildTopSchools(safeTop));
        response.setSalesTrend(buildSalesTrend(safeDays));
        LocalDate rangeStartDate = LocalDate.now().minusDays(safeDays - 1L);
        LocalDateTime rangeStart = rangeStartDate.atStartOfDay();
        LocalDateTime rangeEnd = rangeStartDate.plusDays(safeDays).atStartOfDay();
        response.setUserBehavior(buildUserBehaviorAnalytics(rangeStartDate, rangeStart, rangeEnd, safeDays));
        response.setProductHeat(buildProductHeatAnalytics(safeTop));
        return response;
    }

    private AdminOrderReportResponse.OrderSummary buildSummary() {
        AdminOrderReportResponse.OrderSummary summary = new AdminOrderReportResponse.OrderSummary();
        long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long pendingPayment = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long pendingShipment = orderRepository.countByStatus(OrderStatus.PENDING_SHIPMENT);
        long pendingReceipt = orderRepository.countByStatus(OrderStatus.PENDING_RECEIPT);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long refunded = orderRepository.countByRefundStatus(RefundStatus.REFUNDED);
        BigDecimal revenue = defaultZero(orderRepository.sumTotalPriceByStatus(OrderStatus.COMPLETED));
        BigDecimal average = completed > 0
                ? revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        summary.setCompletedOrders(completed);
        summary.setPendingPaymentOrders(pendingPayment);
        summary.setPendingShipmentOrders(pendingShipment);
        summary.setPendingReceiptOrders(pendingReceipt);
        summary.setCancelledOrders(cancelled);
        summary.setRefundedOrders(refunded);
        summary.setTotalRevenue(revenue);
        summary.setAverageOrderValue(average);
        return summary;
    }

    private List<AdminOrderReportResponse.OrderStatusMetric> buildStatusDistribution() {
        Map<OrderStatus, Long> distribution = new EnumMap<>(OrderStatus.class);
        Arrays.stream(OrderStatus.values()).forEach(status -> distribution.put(status, 0L));
        List<OrderStatusCountView> aggregates = orderRepository.aggregateStatusCounts();
        aggregates.forEach(view -> distribution.put(view.getStatus(), view.getTotal()));
        List<AdminOrderReportResponse.OrderStatusMetric> metrics = new ArrayList<>();
        distribution.forEach((status, count) ->
                metrics.add(new AdminOrderReportResponse.OrderStatusMetric(status, count)));
        metrics.sort((a, b) -> Integer.compare(a.getStatus().ordinal(), b.getStatus().ordinal()));
        return metrics;
    }

    private List<AdminOrderReportResponse.CategoryMetric> buildTopCategories(int top) {
        List<CategoryCountView> views = orderRepository.topCategoriesByCompletedOrders(PageRequest.of(0, top));
        List<AdminOrderReportResponse.CategoryMetric> metrics = new ArrayList<>();
        for (CategoryCountView view : views) {
            ProductCategory category = view.getCategory();
            metrics.add(new AdminOrderReportResponse.CategoryMetric(category, view.getTotal()));
        }
        return metrics;
    }

    private List<AdminOrderReportResponse.SchoolMetric> buildTopSchools(int top) {
        List<SchoolCountView> views = orderRepository.topSchoolsByOrders(PageRequest.of(0, top));
        List<AdminOrderReportResponse.SchoolMetric> metrics = new ArrayList<>();
        for (SchoolCountView view : views) {
            metrics.add(new AdminOrderReportResponse.SchoolMetric(view.getSchool(), view.getTotal()));
        }
        return metrics;
    }

    private List<AdminOrderReportResponse.SalesTrendMetric> buildSalesTrend(int days) {
        List<AdminOrderReportResponse.SalesTrendMetric> trend = new ArrayList<>();
        LocalDate fromDate = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate date = fromDate.plusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long orders = orderRepository.countByStatusAndCreateTimeBetween(OrderStatus.COMPLETED, start, end);
            BigDecimal revenue = defaultZero(orderRepository
                    .sumTotalPriceByStatusAndCreateTimeBetween(OrderStatus.COMPLETED, start, end));
            trend.add(new AdminOrderReportResponse.SalesTrendMetric(date, orders, revenue));
        }
        return trend;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private AdminOrderReportResponse.UserBehaviorAnalytics buildUserBehaviorAnalytics(LocalDate rangeStartDate,
                                                                                      LocalDateTime rangeStart,
                                                                                      LocalDateTime rangeEnd,
                                                                                      int days) {
        AdminOrderReportResponse.UserBehaviorAnalytics analytics = new AdminOrderReportResponse.UserBehaviorAnalytics();
        analytics.setNewUsers(userRepository.countByCreateTimeBetween(rangeStart, rangeEnd));
        analytics.setOrdersCreated(orderRepository.countByCreateTimeBetween(rangeStart, rangeEnd));
        analytics.setActiveBuyers(orderRepository.countDistinctBuyersBetween(rangeStart, rangeEnd));
        analytics.setActiveSellers(orderRepository.countDistinctSellersBetween(rangeStart, rangeEnd));
        analytics.setFavoriteActions(favoriteRepository.countByCreateTimeBetween(rangeStart, rangeEnd));
        analytics.setMessageInteractions(messageRepository.countByCreateTimeBetween(rangeStart, rangeEnd));
        analytics.setFavoriteTrend(buildDailyActionMetrics(rangeStartDate, days,
                (start, end) -> favoriteRepository.countByCreateTimeBetween(start, end)));
        analytics.setMessageTrend(buildDailyActionMetrics(rangeStartDate, days,
                (start, end) -> messageRepository.countByCreateTimeBetween(start, end)));
        return analytics;
    }

    private List<AdminOrderReportResponse.DailyActionMetric> buildDailyActionMetrics(LocalDate fromDate,
                                                                                    int days,
                                                                                    ActionCounter counter) {
        List<AdminOrderReportResponse.DailyActionMetric> metrics = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = fromDate.plusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = counter.count(start, end);
            metrics.add(new AdminOrderReportResponse.DailyActionMetric(date, count));
        }
        return metrics;
    }

    private AdminOrderReportResponse.ProductHeatAnalytics buildProductHeatAnalytics(int top) {
        AdminOrderReportResponse.ProductHeatAnalytics analytics = new AdminOrderReportResponse.ProductHeatAnalytics();
        analytics.setMostViewed(mapHeatMetrics(productRepository.topViewedProducts(PageRequest.of(0, top))));
        analytics.setMostFavorited(mapHeatMetrics(favoriteRepository.topFavoritedProducts(PageRequest.of(0, top))));
        analytics.setBestSellers(mapHeatMetrics(orderRepository.topProductsByCompletedOrders(PageRequest.of(0, top))));
        return analytics;
    }

    private List<AdminOrderReportResponse.ProductHeatMetric> mapHeatMetrics(List<ProductHeatView> views) {
        if (views == null || views.isEmpty()) {
            return Collections.emptyList();
        }
        return views.stream()
                .map(view -> new AdminOrderReportResponse.ProductHeatMetric(
                        view.getProductId(), view.getProductTitle(), view.getTotal()))
                .collect(Collectors.toList());
    }

    @FunctionalInterface
    private interface ActionCounter {
        long count(LocalDateTime start, LocalDateTime end);
    }
}
