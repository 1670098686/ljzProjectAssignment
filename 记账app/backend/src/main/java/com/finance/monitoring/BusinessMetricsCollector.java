package com.finance.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标收集器
 * 用于收集和监控个人收支记账APP的业务指标
 */
@Component
public class BusinessMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // 交易相关指标
    private final Counter transactionTotalCounter;
    private final Counter transactionSuccessCounter;
    private final Counter transactionFailureCounter;
    private final Timer transactionProcessingTimer;
    
    // 分类使用指标
    private final Counter categoryUsageCounter;
    private final ConcurrentHashMap<String, AtomicLong> categoryUsageMap = new ConcurrentHashMap<>();
    
    // 预算相关指标
    private final Counter budgetExceededCounter;
    private final ConcurrentHashMap<String, AtomicLong> budgetSpentMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> budgetTargetMap = new ConcurrentHashMap<>();
    
    // 用户活动指标
    private final Counter userLoginCounter;
    private final Counter userLogoutCounter;
    private final ConcurrentHashMap<String, AtomicLong> activeUsersMap = new ConcurrentHashMap<>();
    
    // API调用指标
    private final Counter apiRequestCounter;
    private final Timer apiResponseTimer;
    
    // 储蓄目标指标
    private final Counter savingGoalCompletedCounter;
    private final ConcurrentHashMap<String, AtomicLong> savingGoalCurrentMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> savingGoalTargetMap = new ConcurrentHashMap<>();

    public BusinessMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化交易指标
        this.transactionTotalCounter = Counter.builder("transaction_total")
            .description("Total number of transactions")
            .register(meterRegistry);
            
        this.transactionSuccessCounter = Counter.builder("transaction_success_total")
            .description("Total number of successful transactions")
            .register(meterRegistry);
            
        this.transactionFailureCounter = Counter.builder("transaction_failure_total")
            .description("Total number of failed transactions")
            .register(meterRegistry);
            
        this.transactionProcessingTimer = Timer.builder("transaction_processing_duration")
            .description("Transaction processing time")
            .register(meterRegistry);
            
        // 初始化分类使用指标
        this.categoryUsageCounter = Counter.builder("category_usage_total")
            .description("Total category usage count")
            .register(meterRegistry);
            
        // 初始化预算相关指标
        this.budgetExceededCounter = Counter.builder("budget_exceeded_total")
            .description("Total number of budget exceeded events")
            .register(meterRegistry);
            
        // 初始化用户活动指标
        this.userLoginCounter = Counter.builder("user_login_total")
            .description("Total user login count")
            .register(meterRegistry);
            
        this.userLogoutCounter = Counter.builder("user_logout_total")
            .description("Total user logout count")
            .register(meterRegistry);
            
        // 初始化API调用指标
        this.apiRequestCounter = Counter.builder("api_request_total")
            .description("Total API request count")
            .register(meterRegistry);
            
        this.apiResponseTimer = Timer.builder("api_response_duration")
            .description("API response time")
            .register(meterRegistry);
            
        // 初始化储蓄目标指标
        this.savingGoalCompletedCounter = Counter.builder("saving_goal_completed_total")
            .description("Total number of completed saving goals")
            .register(meterRegistry);
    }

    /**
     * 记录交易处理
     */
    public void recordTransaction(boolean success, String category, long processingTimeMs) {
        transactionTotalCounter.increment();
        
        if (success) {
            transactionSuccessCounter.increment();
        } else {
            transactionFailureCounter.increment();
        }
        
        if (processingTimeMs > 0) {
            transactionProcessingTimer.record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        // 记录分类使用
        if (category != null) {
            recordCategoryUsage(category);
        }
    }

    /**
     * 记录分类使用
     */
    public void recordCategoryUsage(String category) {
        categoryUsageCounter.increment();
        
        if (category != null) {
            categoryUsageMap.computeIfAbsent(category, k -> new AtomicLong(0)).incrementAndGet();
            
            // 创建或更新分类使用的Gauge
            Gauge.builder("category_usage_count", 
                categoryUsageMap.get(category), 
                AtomicLong::get)
                .description("Category usage count")
                .tag("category", category)
                .register(meterRegistry);
        }
    }

    /**
     * 记录预算超支
     */
    public void recordBudgetExceeded(String category) {
        budgetExceededCounter.increment();
    }

    /**
     * 记录预算支出和目标
     */
    public void recordBudgetSpent(String category, double spentAmount) {
        budgetSpentMap.computeIfAbsent(category, k -> new AtomicLong(0))
            .set((long) (spentAmount * 100)); // 转换为分
            
        updateBudgetRatio(category);
    }

    public void recordBudgetTarget(String category, double targetAmount) {
        budgetTargetMap.computeIfAbsent(category, k -> new AtomicLong(0))
            .set((long) (targetAmount * 100)); // 转换为分
            
        updateBudgetRatio(category);
    }

    /**
     * 更新预算执行比例
     */
    private void updateBudgetRatio(String category) {
        AtomicLong spent = budgetSpentMap.get(category);
        AtomicLong target = budgetTargetMap.get(category);
        
        if (spent != null && target != null && target.get() > 0) {
            double ratio = (double) spent.get() / target.get();
            
            Gauge.builder("budget_spent_ratio", 
                () -> ratio)
                .description("Budget spent ratio")
                .tag("category", category)
                .register(meterRegistry);
        }
    }

    /**
     * 记录用户登录
     */
    public void recordUserLogin(String userId) {
        userLoginCounter.increment();
        
        if (userId != null) {
            activeUsersMap.computeIfAbsent(userId, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * 记录用户登出
     */
    public void recordUserLogout(String userId) {
        userLogoutCounter.increment();
        
        if (userId != null) {
            activeUsersMap.computeIfAbsent(userId, k -> new AtomicLong(0)).decrementAndGet();
        }
    }

    /**
     * 记录API请求
     */
    public void recordApiRequest(String uri, String method, int statusCode, long responseTimeMs) {
        apiRequestCounter.increment();
        
        if (responseTimeMs > 0) {
            apiResponseTimer.record(responseTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 记录储蓄目标完成
     */
    public void recordSavingGoalCompleted(String goalName) {
        savingGoalCompletedCounter.increment();
    }

    /**
     * 记录储蓄目标进度
     */
    public void recordSavingGoalProgress(String goalName, double currentAmount, double targetAmount) {
        savingGoalCurrentMap.computeIfAbsent(goalName, k -> new AtomicLong(0))
            .set((long) (currentAmount * 100));
            
        savingGoalTargetMap.computeIfAbsent(goalName, k -> new AtomicLong(0))
            .set((long) (targetAmount * 100));
            
        // 创建储蓄目标进度Gauge
        double progress = targetAmount > 0 ? currentAmount / targetAmount : 0;
        
        Gauge.builder("saving_goal_progress", 
            () -> progress)
            .description("Saving goal progress")
            .tag("goal_name", goalName)
            .register(meterRegistry);
    }
}