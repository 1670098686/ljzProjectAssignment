package com.finance.service.impl;

import com.finance.cache.CacheNames;
import com.finance.context.UserContext;
import com.finance.dto.CategoryStatisticsDto;
import com.finance.dto.StatisticsGranularity;
import com.finance.dto.SummaryStatisticsDto;
import com.finance.dto.TrendPointDto;
import com.finance.exception.BusinessException;
import com.finance.repository.TransactionRecordRepository;
import com.finance.service.StatisticsService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final int TYPE_INCOME = 1;
    private static final int TYPE_EXPENSE = 2;

    private final TransactionRecordRepository transactionRecordRepository;
    private final UserContext userContext;
    private final Cache summaryCache;
    private final Cache categoryCache;
    private final Cache trendCache;

    public StatisticsServiceImpl(TransactionRecordRepository transactionRecordRepository,
                                 UserContext userContext,
                                 CacheManager cacheManager) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.userContext = userContext;
        this.summaryCache = cacheManager.getCache(CacheNames.STATISTICS_SUMMARY);
        this.categoryCache = cacheManager.getCache(CacheNames.STATISTICS_CATEGORY);
        this.trendCache = cacheManager.getCache(CacheNames.STATISTICS_TREND);
    }

    @Override
    public SummaryStatisticsDto getMonthlySummary(int year, int month) {
        validateYearMonth(year, month);
        Long userId = userContext.getCurrentUserId();
        String cacheKey = String.join(":", String.valueOf(userId), String.valueOf(year), String.valueOf(month));
        
        // 尝试从缓存获取
        Cache.ValueWrapper wrapper = summaryCache.get(cacheKey);
        if (wrapper != null) {
            return (SummaryStatisticsDto) wrapper.get();
        }
        
        // 缓存不存在，加载数据
        SummaryStatisticsDto result = buildMonthlySummary(userId, year, month);
        // 存入缓存
        summaryCache.put(cacheKey, result);
        
        return result;
    }

    @Override
    public List<CategoryStatisticsDto> getCategoryStatistics(LocalDate startDate, LocalDate endDate, Integer type) {
        validateDateRange(startDate, endDate);
        if (type != null) {
            validateType(type);
        }
        Long userId = userContext.getCurrentUserId();
        String cacheKey = String.join(":",
                String.valueOf(userId),
                startDate.toString(),
                endDate.toString(),
                type == null ? "all" : type.toString());
        
        // 尝试从缓存获取
        Cache.ValueWrapper wrapper = categoryCache.get(cacheKey);
        if (wrapper != null) {
            return (List<CategoryStatisticsDto>) wrapper.get();
        }
        
        // 缓存不存在，加载数据
        List<CategoryStatisticsDto> result = transactionRecordRepository.aggregateByCategory(userId, startDate, endDate, type).stream()
                .map(row -> {
                    String categoryName = (String) row[0];
                    BigDecimal amount = defaultZero((BigDecimal) row[1]);
                    return new CategoryStatisticsDto(categoryName, amount);
                })
                .collect(Collectors.toList());
        
        // 存入缓存
        categoryCache.put(cacheKey, result);
        
        return result;
    }

    @Override
    public List<TrendPointDto> getTrend(LocalDate startDate, LocalDate endDate, StatisticsGranularity granularity) {
        validateDateRange(startDate, endDate);
        StatisticsGranularity effectiveGranularity = granularity == null ? StatisticsGranularity.DAILY : granularity;
        Long userId = userContext.getCurrentUserId();
        String cacheKey = String.join(":",
                String.valueOf(userId),
                startDate.toString(),
                endDate.toString(),
                effectiveGranularity.name());

        // 尝试从缓存获取
        Cache.ValueWrapper wrapper = trendCache.get(cacheKey);
        if (wrapper != null) {
            return (List<TrendPointDto>) wrapper.get();
        }
        
        // 缓存不存在，加载数据
        List<TrendPointDto> dailyPoints = transactionRecordRepository.aggregateTrend(userId, startDate, endDate).stream()
                .map(row -> {
                    LocalDate date = (LocalDate) row[0];
                    BigDecimal income = defaultZero((BigDecimal) row[1]);
                    BigDecimal expense = defaultZero((BigDecimal) row[2]);
                    return new TrendPointDto(date, income, expense);
                })
                .collect(Collectors.toList());
        
        List<TrendPointDto> result = aggregateTrendPoints(dailyPoints, effectiveGranularity);
        
        // 存入缓存
        trendCache.put(cacheKey, result);
        
        return result;
    }

    private SummaryStatisticsDto buildMonthlySummary(Long userId, int year, int month) {
        BigDecimal income = defaultZero(transactionRecordRepository.getMonthlyIncome(userId, year, month));
        BigDecimal expense = defaultZero(transactionRecordRepository.getMonthlyExpense(userId, year, month));
        BigDecimal balance = income.subtract(expense);
        return new SummaryStatisticsDto(income, expense, balance);
    }

    private List<TrendPointDto> aggregateTrendPoints(List<TrendPointDto> dailyPoints,
                                                     StatisticsGranularity granularity) {
        if (granularity == StatisticsGranularity.DAILY) {
            return dailyPoints;
        }
        Map<LocalDate, TrendTotals> buckets = new LinkedHashMap<>();
        for (TrendPointDto point : dailyPoints) {
            LocalDate bucketDate = resolveBucketDate(point.getDate(), granularity);
            TrendTotals totals = buckets.computeIfAbsent(bucketDate, key -> new TrendTotals());
            totals.add(point);
        }
        return buckets.entrySet().stream()
                .map(entry -> {
                    return new TrendPointDto(entry.getKey(), entry.getValue().income, entry.getValue().expense);
                })
                .collect(Collectors.toList());
    }

    private LocalDate resolveBucketDate(LocalDate date, StatisticsGranularity granularity) {
        return switch (granularity) {
            case WEEKLY -> startOfWeek(date);
            case MONTHLY -> date.withDayOfMonth(1);
            case QUARTERLY -> firstDayOfQuarter(date);
            default -> date;
        };
    }

    private LocalDate startOfWeek(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int offset = dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue();
        return date.minusDays(offset);
    }

    private LocalDate firstDayOfQuarter(LocalDate date) {
        int quarterIndex = (date.getMonthValue() - 1) / 3;
        int firstMonth = quarterIndex * 3 + 1;
        return LocalDate.of(date.getYear(), firstMonth, 1);
    }

    private static class TrendTotals {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;

        void add(TrendPointDto point) {
            income = income.add(point.getIncome());
            expense = expense.add(point.getExpense());
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Start date cannot be after end date");
        }
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new BusinessException("Year must be between 2000 and 2100");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
    }

    private void validateType(int type) {
        if (type != TYPE_INCOME && type != TYPE_EXPENSE) {
            throw new BusinessException("Type must be 1 (income) or 2 (expense)");
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
