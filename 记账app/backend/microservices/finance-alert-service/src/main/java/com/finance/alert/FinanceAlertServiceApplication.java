package com.finance.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 财务预警服务启动类
 * 
 * 功能职责：
 * - 预算预警监控与提醒
 * - 储蓄目标进度监控
 * - 预警规则配置管理
 * - 预警历史记录查询
 * - 多种方式通知推送（邮件、推送、应用内通知）
 * 
 * 技术特性：
 * - 独立数据库存储
 * - 异步处理预警任务
 * - Redis缓存优化
 * - 消息队列解耦
 * - 服务发现集成
 * - 熔断器保护
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableScheduling
public class FinanceAlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceAlertServiceApplication.class, args);
        System.out.println("""
            
            ==========================================
                   财务预警服务启动成功！
            ==========================================
            
            服务功能：
            ✓ 预算预警监控与提醒
            ✓ 储蓄目标进度监控  
            ✓ 预警规则配置管理
            ✓ 预警历史记录查询
            ✓ 多种通知方式推送
            
            技术特性：
            ✓ 独立数据库存储
            ✓ 异步处理优化
            ✓ Redis缓存支持
            ✓ 消息队列解耦
            ✓ 服务发现集成
            ✓ 熔断器保护
            
            访问地址：
            - 服务地址: http://localhost:8084/alert-service
            - 健康检查: http://localhost:8084/alert-service/actuator/health
            - API文档: http://localhost:8084/alert-service/swagger-ui.html
            
            ==========================================
            """);
    }
}