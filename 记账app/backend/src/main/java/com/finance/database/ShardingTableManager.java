package com.finance.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分表管理服务
 * 负责自动创建分表、维护分表索引和管理分表生命周期
 */
@Service
public class ShardingTableManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ShardingTableManager.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 初始化时自动创建当月和下月的分表
     */
    @PostConstruct
    public void initShardingTables() {
        logger.info("初始化分表管理...");
        
        // 创建当月分表
        String currentTable = ShardingStrategy.getTableName(LocalDateTime.now());
        createShardingTable(currentTable);
        
        // 创建下月分表
        String nextTable = ShardingStrategy.getTableName(LocalDateTime.now().plusMonths(1));
        createShardingTable(nextTable);
        
        logger.info("分表初始化完成");
    }
    
    /**
     * 每日凌晨自动检查并创建下月的分表
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void autoCreateNextMonthTable() {
        logger.info("检查并创建下月分表...");
        
        String nextMonthTable = ShardingStrategy.getTableName(LocalDateTime.now().plusMonths(1));
        createShardingTable(nextMonthTable);
        
        logger.info("下月分表创建完成: {}", nextMonthTable);
    }
    
    /**
     * 创建分表
     * @param tableName 表名
     */
    public void createShardingTable(String tableName) {
        try {
            String createSQL = ShardingStrategy.generateCreateTableSQL(tableName);
            jdbcTemplate.execute(createSQL);
            logger.info("分表创建成功: {}", tableName);
        } catch (Exception e) {
            logger.error("分表创建失败: {}, 错误: {}", tableName, e.getMessage());
            throw new RuntimeException("分表创建失败: " + tableName, e);
        }
    }
    
    /**
     * 根据日期范围获取需要查询的分表列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分表名称列表
     */
    public List<String> getTablesForDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return ShardingStrategy.getTableNamesByDateRange(startDate, endDate);
    }
    
    /**
     * 执行跨分表查询
     * @param baseSQL 基础SQL模板，使用 {table} 作为表名占位符
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param params SQL参数
     * @return 查询结果
     */
    public <T> List<T> executeCrossTableQuery(String baseSQL, LocalDateTime startDate, 
                                            LocalDateTime endDate, org.springframework.jdbc.core.RowMapper<T> rowMapper, Object... params) {
        List<String> tables = getTablesForDateRange(startDate, endDate);
        List<T> allResults = new java.util.ArrayList<>();
        
        for (String table : tables) {
            try {
                String sql = baseSQL.replace("{table}", table);
                List<T> results = jdbcTemplate.query(sql, rowMapper, params);
                allResults.addAll(results);
            } catch (Exception e) {
                logger.warn("分表 {} 查询失败，跳过此表: {}", table, e.getMessage());
                // 继续查询其他分表，不因单个分表失败而中断
            }
        }
        
        return allResults;
    }
    
    /**
     * 执行跨分表更新操作
     * @param baseSQL 基础SQL模板
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param params SQL参数
     * @return 更新影响行数
     */
    public int executeCrossTableUpdate(String baseSQL, LocalDateTime startDate, 
                                     LocalDateTime endDate, Object... params) {
        List<String> tables = getTablesForDateRange(startDate, endDate);
        int totalAffectedRows = 0;
        
        for (String table : tables) {
            try {
                String sql = baseSQL.replace("{table}", table);
                int affectedRows = jdbcTemplate.update(sql, params);
                totalAffectedRows += affectedRows;
            } catch (Exception e) {
                logger.warn("分表 {} 更新失败: {}", table, e.getMessage());
                // 继续处理其他分表
            }
        }
        
        return totalAffectedRows;
    }
    
    /**
     * 归档历史数据（定期维护）
     * @param cutoffDate 归档截止日期，超过此日期的数据将被归档
     */
    public void archiveOldData(LocalDateTime cutoffDate) {
        logger.info("开始归档历史数据，截止日期: {}", cutoffDate);
        
        String archiveTable = "transactions_archive_" + cutoffDate.getYear() + 
                            String.format("%02d", cutoffDate.getMonthValue());
        
        // 创建归档表
        createShardingTable(archiveTable);
        
        // 归档数据
        int cutoffYear = cutoffDate.getYear();
        for (int year = 2020; year <= cutoffYear; year++) {
            int maxMonth = year == cutoffYear ? cutoffDate.getMonthValue() - 1 : 12;
            if (maxMonth <= 0) {
                continue;
            }
            for (int month = 1; month <= maxMonth; month++) {
                LocalDateTime tableDate = LocalDateTime.of(year, month, 1, 0, 0);
                if (!tableDate.isBefore(cutoffDate)) {
                    continue;
                }
                String tableName = ShardingStrategy.getTableName(tableDate);
                archiveTableData(tableName, archiveTable, cutoffDate);
            }
        }
        
        logger.info("历史数据归档完成");
    }

    /**
     * 清理指定日期之前的归档表
     * @param cleanupBefore 清理阈值
     */
    public void cleanupArchivedTables(LocalDateTime cleanupBefore) {
        logger.info("开始清理归档表，阈值: {}", cleanupBefore);
        String sql = """
            SELECT TABLE_NAME FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME LIKE 'transactions_archive_%'
            """;

        List<String> archiveTables = jdbcTemplate.queryForList(sql, String.class);
        if (archiveTables.isEmpty()) {
            logger.info("未找到需要清理的归档表");
            return;
        }

        String threshold = String.format("%04d%02d", cleanupBefore.getYear(), cleanupBefore.getMonthValue());
        for (String tableName : archiveTables) {
            String suffix = extractArchiveSuffix(tableName);
            if (suffix == null) {
                continue;
            }
            if (suffix.compareTo(threshold) < 0) {
                dropArchiveTable(tableName);
            }
        }
    }
    
    /**
     * 归档单个表的数据
     * @param sourceTable 源表
     * @param archiveTable 归档表
     * @param cutoffDate 截止日期
     */
    private void archiveTableData(String sourceTable, String archiveTable, LocalDateTime cutoffDate) {
        try {
            String archiveSQL = ShardingStrategy.generateArchiveSQL(archiveTable, sourceTable, cutoffDate);
            jdbcTemplate.execute(archiveSQL);
            logger.info("表 {} 数据归档完成", sourceTable);
        } catch (Exception e) {
            logger.warn("表 {} 归档失败: {}", sourceTable, e.getMessage());
        }
    }

    private String extractArchiveSuffix(String tableName) {
        String prefix = "transactions_archive_";
        if (tableName == null || !tableName.startsWith(prefix) || tableName.length() <= prefix.length()) {
            return null;
        }
        return tableName.substring(prefix.length());
    }

    private void dropArchiveTable(String tableName) {
        try {
            String safeName = tableName.replace("`", "");
            jdbcTemplate.execute("DROP TABLE IF EXISTS `" + safeName + "`");
            logger.info("归档表 {} 已清理", tableName);
        } catch (Exception e) {
            logger.warn("清理归档表 {} 失败: {}", tableName, e.getMessage());
        }
    }
    
    /**
     * 获取分表统计信息
     * @return 分表统计信息
     */
    public ShardingStats getShardingStats() {
        String sql = """
            SELECT 
                TABLE_NAME as table_name,
                TABLE_ROWS as row_count,
                ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) as size_mb
            FROM information_schema.TABLES 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME LIKE 'transactions\\_%' 
            AND TABLE_NAME != 'transactions'
            ORDER BY TABLE_NAME DESC
            """;
            
        List<TableStats> tableStats = jdbcTemplate.query(sql, (rs, rowNum) -> {
            TableStats stats = new TableStats();
            stats.setTableName(rs.getString("table_name"));
            stats.setRowCount(rs.getLong("row_count"));
            stats.setSizeMB(rs.getDouble("size_mb"));
            return stats;
        });
        
        return new ShardingStats(tableStats);
    }
    
    /**
     * 分表统计信息
     */
    public static class ShardingStats {
        private List<TableStats> tableStats;
        private long totalRows;
        private double totalSizeMB;
        
        public ShardingStats(List<TableStats> tableStats) {
            this.tableStats = tableStats;
            this.totalRows = tableStats.stream().mapToLong(TableStats::getRowCount).sum();
            this.totalSizeMB = tableStats.stream().mapToDouble(TableStats::getSizeMB).sum();
        }
        
        // Getters and Setters
        public List<TableStats> getTableStats() { return tableStats; }
        public void setTableStats(List<TableStats> tableStats) { this.tableStats = tableStats; }
        public long getTotalRows() { return totalRows; }
        public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
        public double getTotalSizeMB() { return totalSizeMB; }
        public void setTotalSizeMB(double totalSizeMB) { this.totalSizeMB = totalSizeMB; }
    }
    
    /**
     * 单表统计信息
     */
    public static class TableStats {
        private String tableName;
        private long rowCount;
        private double sizeMB;
        
        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public long getRowCount() { return rowCount; }
        public void setRowCount(long rowCount) { this.rowCount = rowCount; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
    }
}