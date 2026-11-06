package com.finance.database;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库分表策略实现
 * 支持按时间维度进行水平分表，优化大数据量查询性能
 */
public class ShardingStrategy {
    
    private static final String TABLE_PREFIX = "transactions_";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM");
    
    /**
     * 根据交易日期计算分表名称
     * @param transactionDate 交易日期
     * @return 分表名称
     */
    public static String getTableName(LocalDateTime transactionDate) {
        String monthKey = transactionDate.format(FORMATTER);
        return TABLE_PREFIX + monthKey;
    }
    
    /**
     * 根据用户ID和日期范围获取需要查询的分表列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分表名称列表
     */
    public static List<String> getTableNamesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<String> tableNames = new ArrayList<>();
        LocalDateTime current = startDate.withDayOfMonth(1);
        LocalDateTime end = endDate.withDayOfMonth(1);
        
        while (!current.isAfter(end)) {
            tableNames.add(getTableName(current));
            current = current.plusMonths(1);
        }
        
        return tableNames;
    }
    
    /**
     * 生成创建分表的SQL语句
     * @param tableName 表名
     * @return 创建表的SQL语句
     */
    public static String generateCreateTableSQL(String tableName) {
        return String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                type INT NOT NULL,
                category_id BIGINT NOT NULL,
                amount DECIMAL(10,2) NOT NULL,
                transaction_date DATE NOT NULL,
                remark VARCHAR(100),
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_user_date_%s (user_id, transaction_date),
                INDEX idx_user_type_category_%s (user_id, type, category_id),
                INDEX idx_user_recent_%s (user_id, transaction_date DESC),
                CONSTRAINT fk_%s_category FOREIGN KEY (category_id) REFERENCES categories (id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """, tableName, tableName.substring(tableName.length()-7), 
               tableName.substring(tableName.length()-7),
               tableName.substring(tableName.length()-7),
               tableName.substring(tableName.length()-7));
    }
    
    /**
     * 生成归档旧数据的SQL语句
     * @param tableName 目标表名
     * @param sourceTable 源表名
     * @param cutoffDate 截止日期
     * @return 归档SQL语句
     */
    public static String generateArchiveSQL(String tableName, String sourceTable, LocalDateTime cutoffDate) {
        return String.format("""
            INSERT INTO %s 
            SELECT * FROM %s 
            WHERE transaction_date < '%s'
            """, tableName, sourceTable, cutoffDate.toLocalDate());
    }
}