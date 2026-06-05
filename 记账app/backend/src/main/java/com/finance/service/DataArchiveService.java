package com.finance.service;

import com.finance.database.ShardingTableManager;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 调度归档与清理任务，防止交易分表无限增长。
 */
@Service
public class DataArchiveService {

    private static final Logger log = LoggerFactory.getLogger(DataArchiveService.class);

    private final ShardingTableManager shardingTableManager;

    public DataArchiveService(ShardingTableManager shardingTableManager) {
        this.shardingTableManager = shardingTableManager;
    }

    /**
     * 每日凌晨 2 点归档 6 个月前的交易数据。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveHistoricalData() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        log.info("触发历史数据归档任务，截止时间: {}", cutoff);
        shardingTableManager.archiveOldData(cutoff);
    }

    /**
     * 每日凌晨 3 点清理超过 2 年的归档表，防止磁盘占用过大。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupArchivedTables() {
        LocalDateTime cleanupBefore = LocalDateTime.now().minusYears(2);
        log.info("触发归档表清理任务，阈值: {}", cleanupBefore);
        shardingTableManager.cleanupArchivedTables(cleanupBefore);
    }
}
