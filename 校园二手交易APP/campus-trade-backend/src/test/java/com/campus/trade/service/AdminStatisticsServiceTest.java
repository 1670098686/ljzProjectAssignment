package com.campus.trade.service;

import com.campus.trade.dto.admin.AdminStatisticsResponse;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminStatisticsService statisticsService;

    @Test
    void getOverview_shouldAggregateCountsAndTrends() {
        when(userRepository.count()).thenReturn(120L);
        when(productRepository.count()).thenReturn(240L);
        when(orderRepository.count()).thenReturn(360L);
        when(productRepository.countByAuditStatus(AuditStatus.PENDING)).thenReturn(5L);
        when(userRepository.countByCreateTimeAfter(any())).thenReturn(3L);
        when(orderRepository.countByCreateTimeAfter(any())).thenReturn(4L);
        when(userRepository.countByCreateTimeBetween(any(), any())).thenReturn(2L);
        when(orderRepository.countByCreateTimeBetween(any(), any())).thenReturn(1L);

        int days = 5;
        AdminStatisticsResponse response = statisticsService.getOverview(days);

        assertEquals(120L, response.getTotalUsers());
        assertEquals(240L, response.getTotalProducts());
        assertEquals(360L, response.getTotalOrders());
        assertEquals(5L, response.getPendingProducts());
        assertEquals(3L, response.getTodayNewUsers());
        assertEquals(4L, response.getTodayNewOrders());
        assertEquals(days, response.getUserTrends().size());
        assertEquals(days, response.getOrderTrends().size());
        assertTrue(response.getUserTrends().stream().allMatch(metric -> metric.getCount() == 2L));
        assertTrue(response.getOrderTrends().stream().allMatch(metric -> metric.getCount() == 1L));

        LocalDate today = LocalDate.now();
        assertEquals(today.minusDays(days - 1L), response.getUserTrends().get(0).getDate());
        assertEquals(today, response.getUserTrends().get(days - 1).getDate());

        verify(userRepository, times(days)).countByCreateTimeBetween(any(), any());
        verify(orderRepository, times(days)).countByCreateTimeBetween(any(), any());
    }
}
