package com.campus.trade.service;

import com.campus.trade.dto.admin.AdminStatisticsResponse;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminStatisticsService(UserRepository userRepository,
                                  ProductRepository productRepository,
                                  OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public AdminStatisticsResponse getOverview(int days) {
        AdminStatisticsResponse response = new AdminStatisticsResponse();
        response.setTotalUsers(userRepository.count());
        response.setTotalProducts(productRepository.count());
        response.setTotalOrders(orderRepository.count());
        response.setPendingProducts(productRepository.countByAuditStatus(AuditStatus.PENDING));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        response.setTodayNewUsers(userRepository.countByCreateTimeAfter(startOfToday));
        response.setTodayNewOrders(orderRepository.countByCreateTimeAfter(startOfToday));

        LocalDate fromDate = LocalDate.now().minusDays(days - 1L);
        response.setUserTrends(buildTrends(fromDate, days,
            (start, end) -> userRepository.countByCreateTimeBetween(start, end)));
        response.setOrderTrends(buildTrends(fromDate, days,
            (start, end) -> orderRepository.countByCreateTimeBetween(start, end)));

        return response;
    }

    private List<AdminStatisticsResponse.DailyMetric> buildTrends(LocalDate fromDate,
                                                                  int days,
                                                                  TrendCounter counter) {
        List<AdminStatisticsResponse.DailyMetric> metrics = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = fromDate.plusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = counter.count(start, end);
            metrics.add(new AdminStatisticsResponse.DailyMetric(date, count));
        }
        return metrics;
    }

    @FunctionalInterface
    private interface TrendCounter {
        long count(LocalDateTime start, LocalDateTime end);
    }
}
