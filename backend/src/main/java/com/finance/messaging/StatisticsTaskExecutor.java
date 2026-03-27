package com.finance.messaging;

import com.finance.dto.StatisticsGranularity;
import com.finance.messaging.payload.StatisticsRecomputeMessage;
import com.finance.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Executes heavier statistics refresh work outside the request thread so
 * API latency stays predictable. Currently it warms the cache by directly
 * invoking {@link StatisticsService}; in the future it can be replaced by a
 * dedicated analytics microservice.
 */
@Component
public class StatisticsTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(StatisticsTaskExecutor.class);

    private final StatisticsService statisticsService;

    public StatisticsTaskExecutor(StatisticsService statisticsService) {
        this.statisticsService = Objects.requireNonNull(statisticsService, "statisticsService");
    }

    public void recompute(StatisticsRecomputeMessage message) {
        LocalDate date = message.getTransactionDate();
        log.info("[StatisticsTask] Recomputing aggregates for user {} transaction {} on {}",
            message.getUserId(), message.getTransactionId(), date);

        statisticsService.getMonthlySummary(date.getYear(), date.getMonthValue());
        statisticsService.getCategoryStatistics(date.minusDays(29), date, null);
        statisticsService.getTrend(date.minusDays(29), date, StatisticsGranularity.DAILY);
    }
}
