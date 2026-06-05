package com.finance.service.impl;

import com.finance.context.UserContext;
import com.finance.event.DomainEventPublisher;
import com.finance.dto.BudgetAlertConfigRequest;
import com.finance.dto.BudgetAlertConfigResponse;
import com.finance.dto.BudgetAlertDto;
import com.finance.dto.BudgetAlertHistoryRequest;
import com.finance.dto.BudgetAlertHistoryResponse;
import com.finance.entity.Budget;
import com.finance.entity.BudgetAlertConfig;
import com.finance.entity.BudgetAlertHistory;
import com.finance.event.BudgetAlertEvent;
import com.finance.event.BudgetOverspendEvent;
import com.finance.exception.BusinessException;
import com.finance.repository.BudgetAlertConfigRepository;
import com.finance.repository.BudgetAlertHistoryRepository;
import com.finance.repository.BudgetRepository;
import com.finance.repository.TransactionRecordRepository;
import com.finance.service.BudgetAlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BudgetAlertServiceImpl implements BudgetAlertService {

    private static final BigDecimal DEFAULT_WARNING = new BigDecimal("0.8");
    private static final BigDecimal DEFAULT_CRITICAL = new BigDecimal("0.9");

    private final BudgetRepository budgetRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final BudgetAlertConfigRepository budgetAlertConfigRepository;
    private final BudgetAlertHistoryRepository budgetAlertHistoryRepository;
    private final UserContext userContext;
    private final DomainEventPublisher eventPublisher;

    public BudgetAlertServiceImpl(BudgetRepository budgetRepository,
                                  TransactionRecordRepository transactionRecordRepository,
                                  BudgetAlertConfigRepository budgetAlertConfigRepository,
                                  BudgetAlertHistoryRepository budgetAlertHistoryRepository,
                                  UserContext userContext,
                                  DomainEventPublisher eventPublisher) {
        this.budgetRepository = budgetRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.budgetAlertConfigRepository = budgetAlertConfigRepository;
        this.budgetAlertHistoryRepository = budgetAlertHistoryRepository;
        this.userContext = userContext;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<BudgetAlertDto> getBudgetAlerts(Integer year,
                                                Integer month,
                                                BigDecimal warningThreshold,
                                                BigDecimal criticalThreshold,
                                                boolean pushEnabled) {
        YearMonth target = resolveYearMonth(year, month);
        AlertRule rule = AlertRule.of(warningThreshold, criticalThreshold, pushEnabled);
        Long userId = userContext.getCurrentUserId();

        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonth(userId, target.getYear(), target.getMonthValue());
        if (budgets.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> expenseMap = transactionRecordRepository
                .sumMonthlyExpenseByCategory(userId, target.getYear(), target.getMonthValue())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (BigDecimal) row[1]));

        List<BudgetAlertDto> alerts = new ArrayList<>();
        for (Budget budget : budgets) {
            BudgetAlertDto alert = buildAlert(budget, expenseMap, rule, target, userId);
            if (alert != null) {
                alerts.add(alert);
            }
        }
        return alerts;
    }

    private BudgetAlertDto buildAlert(Budget budget,
                                      Map<Long, BigDecimal> expenseMap,
                                      AlertRule rule,
                                      YearMonth target,
                                      Long userId) {
        BigDecimal spent = expenseMap.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
        if (spent.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal usage = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP);
        AlertLevel level = determineLevel(usage, rule);
        if (level == AlertLevel.NORMAL) {
            return null;
        }

        BigDecimal remaining = budget.getAmount().subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        BudgetAlertDto alert = BudgetAlertDto.builder()
                .budgetId(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .year(target.getYear())
                .month(target.getMonthValue())
                .budgetAmount(budget.getAmount())
                .spentAmount(spent)
                .remainingAmount(remaining)
                .usageRate(usage)
                .alertLevel(level.name())
                .triggeredThreshold(rule.thresholdFor(level))
                .message(renderMessage(level, usage))
                .notificationSent(false)
                .alertTime(LocalDateTime.now())
                .build();

        if (rule.pushEnabled) {
            eventPublisher.publish(new BudgetAlertEvent(userId, alert));
            alert.setNotificationSent(true);
        }

        if (spent.compareTo(budget.getAmount()) > 0) {
            eventPublisher.publish(new BudgetOverspendEvent(
                    budget.getUserId(),
                    budget.getId(),
                    budget.getCategory().getId(),
                    budget.getAmount(),
                    spent,
                    target.getYear(),
                    target.getMonthValue()));
        }

        return alert;
    }

    private AlertLevel determineLevel(BigDecimal usage, AlertRule rule) {
        if (usage.compareTo(rule.criticalThreshold) >= 0) {
            return AlertLevel.CRITICAL;
        }
        if (usage.compareTo(rule.warningThreshold) >= 0) {
            return AlertLevel.WARNING;
        }
        return AlertLevel.NORMAL;
    }

    private String renderMessage(AlertLevel level, BigDecimal usage) {
        String percent = usage.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%";
        if (level == AlertLevel.CRITICAL) {
            return "预算已触发红色告警，使用率达到" + percent;
        }
        return "预算已触发黄色告警，使用率达到" + percent;
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year == null && month == null) {
            return YearMonth.now();
        }
        if (year == null || month == null) {
            throw new BusinessException("year and month must both be provided");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new BusinessException("year must be between 2000 and 2100");
        }
        return YearMonth.of(year, month);
    }

    private enum AlertLevel {
        NORMAL,
        WARNING,
        CRITICAL
    }

    private record AlertRule(BigDecimal warningThreshold,
                             BigDecimal criticalThreshold,
                             boolean pushEnabled) {

        private static AlertRule of(BigDecimal warning,
                                    BigDecimal critical,
                                    boolean pushEnabled) {
            BigDecimal warningValue = warning == null ? DEFAULT_WARNING : warning;
            BigDecimal criticalValue = critical == null ? DEFAULT_CRITICAL : critical;

            validateThreshold("warningThreshold", warningValue);
            validateThreshold("criticalThreshold", criticalValue);

            if (warningValue.compareTo(criticalValue) >= 0) {
                throw new BusinessException("criticalThreshold must be greater than warningThreshold");
            }
            return new AlertRule(warningValue, criticalValue, pushEnabled);
        }

        private static void validateThreshold(String name, BigDecimal value) {
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) >= 1) {
                throw new BusinessException(name + " must be between 0 and 1");
            }
        }

        private BigDecimal thresholdFor(AlertLevel level) {
            return switch (level) {
                case WARNING -> warningThreshold;
                case CRITICAL -> criticalThreshold;
                default -> BigDecimal.ZERO;
            };
        }
    }

    @Override
    @Transactional
    public BudgetAlertConfigResponse configureAlertRule(BudgetAlertConfigRequest request) {
        Long userId = userContext.getCurrentUserId();
        
        // 验证请求参数
        validateConfigRequest(request);
        
        // 查找现有配置
        BudgetAlertConfig existingConfig = budgetAlertConfigRepository.findByUserId(userId)
                .orElseGet(() -> {
                    BudgetAlertConfig newConfig = new BudgetAlertConfig();
                    newConfig.setUserId(userId);
                    return newConfig;
                });
        
        // 更新配置
        existingConfig.setWarningThreshold(request.getWarningThreshold());
        existingConfig.setCriticalThreshold(request.getCriticalThreshold());
        existingConfig.setPushEnabled(request.getPushEnabled());
        existingConfig.setEnableAllCategories(request.getEnableAllCategories());
        existingConfig.setCategoryIds(request.getCategoryIds());
        
        // 保存配置
        BudgetAlertConfig savedConfig = budgetAlertConfigRepository.save(existingConfig);
        
        // 转换为响应
        return BudgetAlertConfigResponse.builder()
                .configId(savedConfig.getId())
                .warningThreshold(savedConfig.getWarningThreshold())
                .criticalThreshold(savedConfig.getCriticalThreshold())
                .pushEnabled(savedConfig.getPushEnabled())
                .enableAllCategories(savedConfig.getEnableAllCategories())
                .categoryIds(savedConfig.getCategoryIds())
                .createdTime(savedConfig.getCreatedTime())
                .updatedTime(savedConfig.getUpdatedTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetAlertConfigResponse getAlertConfig() {
        Long userId = userContext.getCurrentUserId();
        
        BudgetAlertConfig config = budgetAlertConfigRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultConfig(userId));
        
        return BudgetAlertConfigResponse.builder()
                .configId(config.getId())
                .warningThreshold(config.getWarningThreshold())
                .criticalThreshold(config.getCriticalThreshold())
                .pushEnabled(config.getPushEnabled())
                .enableAllCategories(config.getEnableAllCategories())
                .categoryIds(config.getCategoryIds())
                .createdTime(config.getCreatedTime())
                .updatedTime(config.getUpdatedTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetAlertHistoryResponse.BudgetAlertHistoryListResponse getAlertHistory(BudgetAlertHistoryRequest request) {
        Long userId = userContext.getCurrentUserId();
        
        // 设置默认分页参数
        int page = Math.max(request.getPage() - 1, 0); // 转换为0基索引
        int size = request.getSize();
        Pageable pageable = PageRequest.of(page, size);
        
        // 计算查询条件
        Integer queryYear = resolveYear(request);
        Integer queryMonth = resolveMonth(request);
        List<String> alertLevels = request.getAlertLevels();
        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();
        
        // 查询历史记录
        Page<BudgetAlertHistory> historyPage = budgetAlertHistoryRepository.findByUserIdAndFilters(
                userId, queryYear, queryMonth, alertLevels, request.getCategoryId(), startDate, endDate, pageable);
        
        // 转换为响应对象
        List<BudgetAlertHistoryResponse> responses = historyPage.getContent().stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
        
        // 构建分页信息
        BudgetAlertHistoryResponse.PageInfo pageInfo = BudgetAlertHistoryResponse.PageInfo.builder()
                .currentPage(request.getPage())
                .pageSize(size)
                .totalRecords(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .hasNext(historyPage.hasNext())
                .hasPrevious(historyPage.hasPrevious())
                .build();
        
        return BudgetAlertHistoryResponse.BudgetAlertHistoryListResponse.builder()
                .records(responses)
                .pageInfo(pageInfo)
                .build();
    }

    private void validateConfigRequest(BudgetAlertConfigRequest request) {
        if (request.getWarningThreshold().compareTo(request.getCriticalThreshold()) >= 0) {
            throw new BusinessException("criticalThreshold必须大于warningThreshold");
        }
        
        if (!request.getEnableAllCategories() && 
            (request.getCategoryIds() == null || request.getCategoryIds().isEmpty())) {
            throw new BusinessException("当enableAllCategories为false时，必须指定categoryIds");
        }
    }

    private BudgetAlertConfig createDefaultConfig(Long userId) {
        BudgetAlertConfig config = new BudgetAlertConfig();
        config.setUserId(userId);
        config.setWarningThreshold(DEFAULT_WARNING);
        config.setCriticalThreshold(DEFAULT_CRITICAL);
        config.setPushEnabled(false);
        config.setEnableAllCategories(true);
        config.setCategoryIds(List.of());
        return config;
    }

    private Integer resolveYear(BudgetAlertHistoryRequest request) {
        if (request.getYear() != null) {
            return request.getYear();
        }
        if (request.getQuarter() != null) {
            return request.getYear() != null ? request.getYear() : LocalDateTime.now().getYear();
        }
        return null;
    }

    private Integer resolveMonth(BudgetAlertHistoryRequest request) {
        if (request.getMonth() != null) {
            return request.getMonth();
        }
        if (request.getQuarter() != null) {
            int quarter = request.getQuarter();
            if (quarter < 1 || quarter > 4) {
                throw new BusinessException("quarter必须在1-4之间");
            }
            return quarter * 3; // 季度转换为中间月份
        }
        return null;
    }

    private BudgetAlertHistoryResponse convertToHistoryResponse(BudgetAlertHistory history) {
        return BudgetAlertHistoryResponse.builder()
                .alertId(history.getId())
                .budgetId(history.getBudgetId())
                .categoryId(history.getCategoryId())
                .categoryName(history.getCategoryName())
                .year(history.getYear())
                .month(history.getMonth())
                .budgetAmount(history.getBudgetAmount())
                .spentAmount(history.getSpentAmount())
                .usageRate(history.getUsageRate())
                .alertLevel(history.getAlertLevel())
                .triggeredThreshold(history.getTriggeredThreshold())
                .message(history.getMessage())
                .notificationSent(history.getNotificationSent())
                .alertTime(history.getAlertTime())
                .resolvedTime(history.getResolvedTime())
                .build();
    }
}
