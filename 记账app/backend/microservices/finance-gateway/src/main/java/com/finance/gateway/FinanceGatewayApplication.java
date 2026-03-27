package com.finance.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API网关启动类
 * 
 * 功能职责：
 * - 微服务路由转发
 * - 请求负载均衡
 * - API限流和熔断
 * - 认证和授权
 * - 请求日志和监控
 * - 响应缓存
 * 
 * 技术特性：
 * - Spring Cloud Gateway
 * - 动态路由配置
 * - 服务发现集成
 * - 熔断器保护
 * - 链路追踪支持
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class FinanceGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceGatewayApplication.class, args);
        System.out.println("""
            
            ==========================================
                   API网关服务启动成功！
            ==========================================
            
            服务功能：
            ✓ 微服务路由转发
            ✓ 请求负载均衡
            ✓ API限流和熔断
            ✓ 认证和授权
            ✓ 请求日志和监控
            
            技术特性：
            ✓ Spring Cloud Gateway
            ✓ 动态路由配置
            ✓ 服务发现集成
            ✓ 熔断器保护
            ✓ 链路追踪支持
            
            访问地址：
            - 网关地址: http://localhost:8080
            - 健康检查: http://localhost:8080/actuator/health
            - 路由状态: http://localhost:8080/actuator/gateway/routes
            
            ==========================================
            """);
    }
}