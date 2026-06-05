package com.campus.trade.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO used by the admin dashboard to render metric cards, status breakdowns, trend series and rankings.
 */
public class DashboardOverviewResponse {

    private List<MetricCard> userMetrics = new ArrayList<>();
    private List<MetricCard> productMetrics = new ArrayList<>();
    private List<MetricCard> orderMetrics = new ArrayList<>();
    private List<MetricCard> interactionMetrics = new ArrayList<>();
    private List<StatusBreakdown> orderStatusBreakdown = new ArrayList<>();
    private List<RankingItem> categoryRankings = new ArrayList<>();
    private List<RankingItem> schoolRankings = new ArrayList<>();
    private List<TrendSeries> trends = new ArrayList<>();

    public List<MetricCard> getUserMetrics() {
        return userMetrics;
    }

    public void setUserMetrics(List<MetricCard> userMetrics) {
        this.userMetrics = userMetrics;
    }

    public List<MetricCard> getProductMetrics() {
        return productMetrics;
    }

    public void setProductMetrics(List<MetricCard> productMetrics) {
        this.productMetrics = productMetrics;
    }

    public List<MetricCard> getOrderMetrics() {
        return orderMetrics;
    }

    public void setOrderMetrics(List<MetricCard> orderMetrics) {
        this.orderMetrics = orderMetrics;
    }

    public List<MetricCard> getInteractionMetrics() {
        return interactionMetrics;
    }

    public void setInteractionMetrics(List<MetricCard> interactionMetrics) {
        this.interactionMetrics = interactionMetrics;
    }

    public List<StatusBreakdown> getOrderStatusBreakdown() {
        return orderStatusBreakdown;
    }

    public void setOrderStatusBreakdown(List<StatusBreakdown> orderStatusBreakdown) {
        this.orderStatusBreakdown = orderStatusBreakdown;
    }

    public List<RankingItem> getCategoryRankings() {
        return categoryRankings;
    }

    public void setCategoryRankings(List<RankingItem> categoryRankings) {
        this.categoryRankings = categoryRankings;
    }

    public List<RankingItem> getSchoolRankings() {
        return schoolRankings;
    }

    public void setSchoolRankings(List<RankingItem> schoolRankings) {
        this.schoolRankings = schoolRankings;
    }

    public List<TrendSeries> getTrends() {
        return trends;
    }

    public void setTrends(List<TrendSeries> trends) {
        this.trends = trends;
    }

    public static class MetricCard {
        private String title;
        private Number value;
        private String unit;
        private double delta;

        public MetricCard() {
        }

        public MetricCard(String title, Number value, String unit, double delta) {
            this.title = title;
            this.value = value;
            this.unit = unit;
            this.delta = delta;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Number getValue() {
            return value;
        }

        public void setValue(Number value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public double getDelta() {
            return delta;
        }

        public void setDelta(double delta) {
            this.delta = delta;
        }
    }

    public static class StatusBreakdown {
        private String label;
        private long count;

        public StatusBreakdown() {
        }

        public StatusBreakdown(String label, long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class RankingItem {
        private String label;
        private long value;

        public RankingItem() {
        }

        public RankingItem(String label, long value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    public static class TrendSeries {
        private String name;
        private List<TrendPoint> points = new ArrayList<>();

        public TrendSeries() {
        }

        public TrendSeries(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<TrendPoint> getPoints() {
            return points;
        }

        public void setPoints(List<TrendPoint> points) {
            this.points = points;
        }
    }

    public static class TrendPoint {
        private LocalDate date;
        private Number value;

        public TrendPoint() {
        }

        public TrendPoint(LocalDate date, Number value) {
            this.date = date;
            this.value = value;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Number getValue() {
            return value;
        }

        public void setValue(Number value) {
            this.value = value;
        }
    }
}
