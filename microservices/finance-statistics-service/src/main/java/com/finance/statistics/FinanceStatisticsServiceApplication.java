package com.finance.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 财务统计微服务启动类
 * 
 * 该微服务负责：
 * - 数据统计和分析
 * - 报表生成
 * - 趋势分析
 * - 图表数据接口
 * 
 * 功能特性：
 * - 独立数据库存储统计结果
 * - 异步数据同步处理
 * - Redis缓存加速
 * - 熔断器保护
 * - 服务发现集成
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableScheduling
public class FinanceStatisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceStatisticsServiceApplication.class, args);
    }
}