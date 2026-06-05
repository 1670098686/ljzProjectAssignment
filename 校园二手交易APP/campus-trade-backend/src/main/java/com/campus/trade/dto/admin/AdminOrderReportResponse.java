package com.campus.trade.dto.admin;

import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminOrderReportResponse {

    private OrderSummary summary = new OrderSummary();
    private List<OrderStatusMetric> statusDistribution = new ArrayList<>();
    private List<CategoryMetric> topCategories = new ArrayList<>();
    private List<SchoolMetric> topSchools = new ArrayList<>();
    private List<SalesTrendMetric> salesTrend = new ArrayList<>();
    private UserBehaviorAnalytics userBehavior = new UserBehaviorAnalytics();
    private ProductHeatAnalytics productHeat = new ProductHeatAnalytics();

    public OrderSummary getSummary() {
        return summary;
    }

    public void setSummary(OrderSummary summary) {
        this.summary = summary;
    }

    public List<OrderStatusMetric> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(List<OrderStatusMetric> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public List<CategoryMetric> getTopCategories() {
        return topCategories;
    }

    public void setTopCategories(List<CategoryMetric> topCategories) {
        this.topCategories = topCategories;
    }

    public List<SchoolMetric> getTopSchools() {
        return topSchools;
    }

    public void setTopSchools(List<SchoolMetric> topSchools) {
        this.topSchools = topSchools;
    }

    public List<SalesTrendMetric> getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(List<SalesTrendMetric> salesTrend) {
        this.salesTrend = salesTrend;
    }

    public UserBehaviorAnalytics getUserBehavior() {
        return userBehavior;
    }

    public void setUserBehavior(UserBehaviorAnalytics userBehavior) {
        this.userBehavior = userBehavior;
    }

    public ProductHeatAnalytics getProductHeat() {
        return productHeat;
    }

    public void setProductHeat(ProductHeatAnalytics productHeat) {
        this.productHeat = productHeat;
    }

    public static class OrderSummary {
        private long completedOrders;
        private long pendingPaymentOrders;
        private long pendingShipmentOrders;
        private long pendingReceiptOrders;
        private long cancelledOrders;
        private long refundedOrders;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal averageOrderValue = BigDecimal.ZERO;

        public long getCompletedOrders() {
            return completedOrders;
        }

        public void setCompletedOrders(long completedOrders) {
            this.completedOrders = completedOrders;
        }

        public long getPendingPaymentOrders() {
            return pendingPaymentOrders;
        }

        public void setPendingPaymentOrders(long pendingPaymentOrders) {
            this.pendingPaymentOrders = pendingPaymentOrders;
        }

        public long getPendingShipmentOrders() {
            return pendingShipmentOrders;
        }

        public void setPendingShipmentOrders(long pendingShipmentOrders) {
            this.pendingShipmentOrders = pendingShipmentOrders;
        }

        public long getPendingReceiptOrders() {
            return pendingReceiptOrders;
        }

        public void setPendingReceiptOrders(long pendingReceiptOrders) {
            this.pendingReceiptOrders = pendingReceiptOrders;
        }

        public long getCancelledOrders() {
            return cancelledOrders;
        }

        public void setCancelledOrders(long cancelledOrders) {
            this.cancelledOrders = cancelledOrders;
        }

        public long getRefundedOrders() {
            return refundedOrders;
        }

        public void setRefundedOrders(long refundedOrders) {
            this.refundedOrders = refundedOrders;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public BigDecimal getAverageOrderValue() {
            return averageOrderValue;
        }

        public void setAverageOrderValue(BigDecimal averageOrderValue) {
            this.averageOrderValue = averageOrderValue;
        }
    }

    public static class OrderStatusMetric {
        private OrderStatus status;
        private long count;

        public OrderStatusMetric() {
        }

        public OrderStatusMetric(OrderStatus status, long count) {
            this.status = status;
            this.count = count;
        }

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class CategoryMetric {
        private ProductCategory category;
        private long count;

        public CategoryMetric() {
        }

        public CategoryMetric(ProductCategory category, long count) {
            this.category = category;
            this.count = count;
        }

        public ProductCategory getCategory() {
            return category;
        }

        public void setCategory(ProductCategory category) {
            this.category = category;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class SchoolMetric {
        private String school;
        private long count;

        public SchoolMetric() {
        }

        public SchoolMetric(String school, long count) {
            this.school = school;
            this.count = count;
        }

        public String getSchool() {
            return school;
        }

        public void setSchool(String school) {
            this.school = school;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class SalesTrendMetric {
        private LocalDate date;
        private long orders;
        private BigDecimal revenue = BigDecimal.ZERO;

        public SalesTrendMetric() {
        }

        public SalesTrendMetric(LocalDate date, long orders, BigDecimal revenue) {
            this.date = date;
            this.orders = orders;
            this.revenue = revenue;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getOrders() {
            return orders;
        }

        public void setOrders(long orders) {
            this.orders = orders;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
    }

    public static class UserBehaviorAnalytics {
        private long newUsers;
        private long activeBuyers;
        private long activeSellers;
        private long ordersCreated;
        private long favoriteActions;
        private long messageInteractions;
        private List<DailyActionMetric> favoriteTrend = new ArrayList<>();
        private List<DailyActionMetric> messageTrend = new ArrayList<>();

        public long getNewUsers() {
            return newUsers;
        }

        public void setNewUsers(long newUsers) {
            this.newUsers = newUsers;
        }

        public long getActiveBuyers() {
            return activeBuyers;
        }

        public void setActiveBuyers(long activeBuyers) {
            this.activeBuyers = activeBuyers;
        }

        public long getActiveSellers() {
            return activeSellers;
        }

        public void setActiveSellers(long activeSellers) {
            this.activeSellers = activeSellers;
        }

        public long getOrdersCreated() {
            return ordersCreated;
        }

        public void setOrdersCreated(long ordersCreated) {
            this.ordersCreated = ordersCreated;
        }

        public long getFavoriteActions() {
            return favoriteActions;
        }

        public void setFavoriteActions(long favoriteActions) {
            this.favoriteActions = favoriteActions;
        }

        public long getMessageInteractions() {
            return messageInteractions;
        }

        public void setMessageInteractions(long messageInteractions) {
            this.messageInteractions = messageInteractions;
        }

        public List<DailyActionMetric> getFavoriteTrend() {
            return favoriteTrend;
        }

        public void setFavoriteTrend(List<DailyActionMetric> favoriteTrend) {
            this.favoriteTrend = favoriteTrend;
        }

        public List<DailyActionMetric> getMessageTrend() {
            return messageTrend;
        }

        public void setMessageTrend(List<DailyActionMetric> messageTrend) {
            this.messageTrend = messageTrend;
        }
    }

    public static class DailyActionMetric {
        private LocalDate date;
        private long count;

        public DailyActionMetric() {
        }

        public DailyActionMetric(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class ProductHeatAnalytics {
        private List<ProductHeatMetric> mostViewed = new ArrayList<>();
        private List<ProductHeatMetric> mostFavorited = new ArrayList<>();
        private List<ProductHeatMetric> bestSellers = new ArrayList<>();

        public List<ProductHeatMetric> getMostViewed() {
            return mostViewed;
        }

        public void setMostViewed(List<ProductHeatMetric> mostViewed) {
            this.mostViewed = mostViewed;
        }

        public List<ProductHeatMetric> getMostFavorited() {
            return mostFavorited;
        }

        public void setMostFavorited(List<ProductHeatMetric> mostFavorited) {
            this.mostFavorited = mostFavorited;
        }

        public List<ProductHeatMetric> getBestSellers() {
            return bestSellers;
        }

        public void setBestSellers(List<ProductHeatMetric> bestSellers) {
            this.bestSellers = bestSellers;
        }
    }

    public static class ProductHeatMetric {
        private Long productId;
        private String title;
        private long value;

        public ProductHeatMetric() {
        }

        public ProductHeatMetric(Long productId, String title, long value) {
            this.productId = productId;
            this.title = title;
            this.value = value;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }
}
