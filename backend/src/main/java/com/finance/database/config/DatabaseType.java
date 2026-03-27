package com.finance.database.config;

/**
 * 数据库类型枚举
 */
public enum DatabaseType {
    PRIMARY,   // 主库（写库）
    REPLICA    // 从库（读库）
}