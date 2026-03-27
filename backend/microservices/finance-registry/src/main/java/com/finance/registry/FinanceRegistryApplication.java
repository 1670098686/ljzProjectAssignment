package com.finance.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 财务系统服务注册中心
 * 
 * 基于Eureka的服务注册与发现中心，负责：
 * - 管理所有微服务的注册信息
 * - 提供服务发现功能
 * - 服务健康检查和状态监控
 * - 负载均衡和服务治理
 * 
 * 技术特性：
 * - 独立的Spring Boot应用
 * - 集成Eureka Server
 * - 安全管理（Basic Auth）
 * - 监控端点支持
 * - 环境隔离配置
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@SpringBootApplication
@EnableEurekaServer
public class FinanceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceRegistryApplication.class, args);
        
        System.out.println("""
            
            =========================================
                 财务系统服务注册中心启动成功！
            =========================================
            
            🏢 服务名称: Finance Registry Service
            🌐 管理页面: http://localhost:8761/
            🔐 用户名: admin
            🔑 密码: finance123
            
            📋 核心功能:
            ✅ 服务注册与发现
            ✅ 健康状态监控
            ✅ 服务治理中心
            ✅ 负载均衡支持
            ✅ 微服务网关路由
            
            💡 使用说明:
            1. 所有微服务启动时会自动注册到注册中心
            2. 可通过管理页面查看所有注册的服务
            3. 其他服务可通过Eureka Client发现和调用
            
            =========================================
            """);
    }
}