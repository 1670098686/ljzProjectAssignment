package com.campus.trade.dto.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminStatisticsResponse {

    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long pendingProducts;
    private long todayNewUsers;
    private long todayNewOrders;
    private List<DailyMetric> userTrends = new ArrayList<>();
    private List<DailyMetric> orderTrends = new ArrayList<>();

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingProducts() {
        return pendingProducts;
    }

    public void setPendingProducts(long pendingProducts) {
        this.pendingProducts = pendingProducts;
    }

    public long getTodayNewUsers() {
        return todayNewUsers;
    }

    public void setTodayNewUsers(long todayNewUsers) {
        this.todayNewUsers = todayNewUsers;
    }

    public long getTodayNewOrders() {
        return todayNewOrders;
    }

    public void setTodayNewOrders(long todayNewOrders) {
        this.todayNewOrders = todayNewOrders;
    }

    public List<DailyMetric> getUserTrends() {
        return userTrends;
    }

    public void setUserTrends(List<DailyMetric> userTrends) {
        this.userTrends = userTrends;
    }

    public List<DailyMetric> getOrderTrends() {
        return orderTrends;
    }

    public void setOrderTrends(List<DailyMetric> orderTrends) {
        this.orderTrends = orderTrends;
    }

    public static class DailyMetric {
        private LocalDate date;
        private long count;

        public DailyMetric() {
        }

        public DailyMetric(LocalDate date, long count) {
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
}
