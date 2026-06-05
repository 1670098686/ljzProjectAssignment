package com.finance.cache;

/**
 * Central place for cache name constants to avoid typos and keep
 * configuration in sync with service usage.
 */
public final class CacheNames {

    private CacheNames() {
    }

    // 统计数据缓存
    public static final String STATISTICS_SUMMARY = "statistics:summary";
    public static final String STATISTICS_CATEGORY = "statistics:category";
    public static final String STATISTICS_TREND = "statistics:trend";
    public static final String USER_STATISTICS = "statistics:user";

    // 预算管理缓存
    public static final String BUDGET_MONTHLY = "budgets:monthly";
    public static final String BUDGET_ALERT = "budgets:alert";

    // 用户配置缓存
    public static final String USER_PROFILE = "user:profile";
    public static final String USER_PREFERENCES = "user:preferences";

    // 分类管理缓存
    public static final String CATEGORY_LIST = "categories:list";
    public static final String CATEGORY_USER = "categories:user";

    // 交易记录缓存
    public static final String TRANSACTION_LIST = "transactions:list";
    public static final String TRANSACTION_DETAIL = "transactions:detail";

    // 储蓄目标缓存
    public static final String SAVING_GOALS = "saving:goals";
    public static final String GOAL_PROGRESS = "saving:progress";

    // 缓存穿透防护缓存
    public static final String NULL_CACHE = "cache:null:protection";
    public static final String BLOOM_FILTER = "bloom:filter:cache";

    // 缓存监控缓存
    public static final String CACHE_METRICS = "metrics:cache";
    public static final String CACHE_STATS = "stats:cache";
}
