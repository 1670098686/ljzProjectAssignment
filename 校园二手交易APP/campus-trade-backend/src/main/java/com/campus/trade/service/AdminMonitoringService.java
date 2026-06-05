package com.campus.trade.service;

import com.campus.trade.config.cache.LayeredCache;
import com.campus.trade.dto.admin.BusinessHealthResponse;
import com.campus.trade.dto.admin.PerformanceMetricsResponse;
import com.campus.trade.dto.admin.RuntimeMetricsResponse;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.Sample;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AdminMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(AdminMonitoringService.class);

    private final MetricsEndpoint metricsEndpoint;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MessageRepository messageRepository;

    public AdminMonitoringService(MetricsEndpoint metricsEndpoint,
                                  MeterRegistry meterRegistry,
                                  CacheManager cacheManager,
                                  OrderRepository orderRepository,
                                  ProductRepository productRepository,
                                  MessageRepository messageRepository) {
        this.metricsEndpoint = metricsEndpoint;
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.messageRepository = messageRepository;
    }

    public RuntimeMetricsResponse getRuntimeMetrics() {
        RuntimeMetricsResponse response = new RuntimeMetricsResponse();
        response.setUptimeSeconds(Math.round(readMetricValue("process.uptime")));
        response.setHeapUsedBytes(Math.round(readMetricValue("jvm.memory.used", "area:heap")));
        response.setHeapMaxBytes(Math.round(readMetricValue("jvm.memory.max", "area:heap")));
        response.setNonHeapUsedBytes(Math.round(readMetricValue("jvm.memory.used", "area:nonheap")));
        response.setThreadCount((int) Math.round(readMetricValue("jvm.threads.live")));
        response.setProcessCpuLoad(roundPercentMetric(readMetricValue("process.cpu.usage")));
        response.setSystemCpuLoad(roundPercentMetric(readMetricValue("system.cpu.usage")));
        return response;
    }

    public PerformanceMetricsResponse getPerformanceMetrics() {
        List<Timer> timers = meterRegistry.getMeters().stream()
                .filter(meter -> meter instanceof Timer)
                .map(meter -> (Timer) meter)
                .filter(timer -> "http.server.requests".equals(timer.getId().getName()))
                .toList();

        double totalCount = timers.stream().mapToDouble(Timer::count).sum();
        double totalTime = timers.stream().mapToDouble(timer -> timer.totalTime(TimeUnit.MILLISECONDS)).sum();
        double maxMillis = timers.stream().mapToDouble(timer -> safeValue(timer.max(TimeUnit.MILLISECONDS))).max().orElse(0D);

        PerformanceMetricsResponse response = new PerformanceMetricsResponse();
        response.setHttpRequestCount(totalCount);
        response.setHttpRequestMeanMillis(totalCount == 0 ? 0 : safeValue(totalTime / totalCount));
        response.setHttpRequestMaxMillis(maxMillis);
        response.setSlowestEndpoints(buildSlowEndpoints(timers));
        return response;
    }

    public BusinessHealthResponse getBusinessHealthSnapshot() {
        BusinessHealthResponse response = new BusinessHealthResponse();
        response.setPendingAudits(productRepository.countByAuditStatus(AuditStatus.PENDING));
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT)
                + orderRepository.countByStatus(OrderStatus.PENDING_SHIPMENT)
                + orderRepository.countByStatus(OrderStatus.PENDING_RECEIPT);
        response.setPendingOrders(pendingOrders);
        long refunds = orderRepository.countByRefundStatus(RefundStatus.REQUESTED)
                + orderRepository.countByRefundStatus(RefundStatus.PROCESSING);
        response.setRefundRequests(refunds);
        response.setCacheHitRatio(calculateCacheHitRatio());
        response.setMessageBacklog(messageRepository.countByReadFalse());
        return response;
    }

    public void triggerTestAlert() {
        log.warn("Admin requested a monitor alert test at {}", System.currentTimeMillis());
    }

    private double readMetricValue(String name, String... tags) {
        var response = metricsEndpoint.metric(name, buildTagList(tags));
        if (response == null || CollectionUtils.isEmpty(response.getMeasurements())) {
            return 0D;
        }
        return response.getMeasurements().stream()
                .map(Sample::getValue)
                .findFirst()
                .orElse(0D);
    }

    private List<String> buildTagList(String... tags) {
        if (tags == null || tags.length == 0) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (String tag : tags) {
            list.add(tag);
        }
        return list;
    }

    private double roundPercentMetric(double value) {
        double percent = value * 100D;
        return Math.round(percent * 100D) / 100D;
    }

    private double safeValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0D;
        }
        return Math.round(value * 100D) / 100D;
    }

    private List<PerformanceMetricsResponse.SlowEndpoint> buildSlowEndpoints(List<Timer> timers) {
        return timers.stream()
                .map(timer -> {
                    String uri = timer.getId().getTag("uri");
                    double max = safeValue(timer.max(TimeUnit.MILLISECONDS));
                    double mean = safeValue(timer.mean(TimeUnit.MILLISECONDS));
                    return new PerformanceMetricsResponse.SlowEndpoint(uri, max, mean);
                })
                .filter(item -> StringUtils.hasText(item.getUri()) && !"UNKNOWN".equalsIgnoreCase(item.getUri()))
                .sorted(Comparator.comparingDouble(PerformanceMetricsResponse.SlowEndpoint::getMaxMillis).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private double calculateCacheHitRatio() {
        double hits = 0D;
        double misses = 0D;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof LayeredCache layeredCache) {
                Cache local = layeredCache.getLocalCache();
                if (local instanceof CaffeineCache caffeineCache) {
                    CacheStats stats = caffeineCache.getNativeCache().stats();
                    hits += stats.hitCount();
                    misses += stats.missCount();
                }
            } else if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                hits += stats.hitCount();
                misses += stats.missCount();
            }
        }
        double total = hits + misses;
        if (total == 0) {
            return 0D;
        }
        return Math.round((hits / total * 100D) * 100D) / 100D;
    }
}
