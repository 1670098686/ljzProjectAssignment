package com.finance.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * API网关限流键解析器配置
 * 
 * 提供多种限流策略：基于用户、基于IP、基于路径等
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@Configuration
public class RateLimitKeyResolverConfig {

    /**
     * 基于用户ID的限流键解析器
     * 从JWT token中提取用户ID作为限流标识
     * 
     * @return 限流键解析器
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return new UserKeyResolver();
    }

    /**
     * 基于客户端IP的限流键解析器
     * 
     * @return IP限流键解析器
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    /**
     * 基于请求路径的限流键解析器
     * 
     * @return 路径限流键解析器
     */
    @Bean("pathKeyResolver")
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }

    /**
     * 复合限流键解析器
     * 结合用户ID和IP的限流策略
     * 
     * @return 复合限流键解析器
     */
    @Bean("compositeKeyResolver")
    public KeyResolver compositeKeyResolver() {
        return new CompositeKeyResolver();
    }

    /**
     * 用户ID限流键解析器实现
     */
    public static class UserKeyResolver implements KeyResolver {
        
        @Override
        public Mono<String> resolve(ServerWebExchange exchange) {
            String userId = getUserIdFromToken(exchange);
            return Mono.just(userId != null ? userId : "anonymous");
        }

        /**
         * 从请求头或JWT token中提取用户ID
         * 实际项目中应集成Spring Security OAuth2
         */
        private String getUserIdFromToken(ServerWebExchange exchange) {
            // 从Authorization头提取用户ID（简化示例）
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 在实际项目中，这里应该解析JWT token
                // 这里仅作为示例，返回token的前8位作为用户ID
                return authHeader.substring(7, Math.min(authHeader.length(), 16));
            }
            
            // 从请求头获取用户ID（备用方案）
            String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userIdHeader != null) {
                return userIdHeader;
            }
            
            return null;
        }
    }

    /**
     * 复合限流键解析器实现
     * 结合用户ID、IP和路径的限流策略
     */
    public static class CompositeKeyResolver implements KeyResolver {
        
        @Override
        public Mono<String> resolve(ServerWebExchange exchange) {
            String userId = getUserIdFromToken(exchange);
            String clientIp = getClientIp(exchange);
            String path = exchange.getRequest().getPath().value();
            
            String key = String.format("%s:%s:%s", 
                userId != null ? userId : "anonymous", 
                clientIp, 
                extractEndpoint(path));
            
            return Mono.just(key);
        }

        private String getUserIdFromToken(ServerWebExchange exchange) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7, Math.min(authHeader.length(), 16));
            }
            return null;
        }

        private String getClientIp(ServerWebExchange exchange) {
            try {
                return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            } catch (Exception e) {
                return "unknown";
            }
        }

        /**
         * 提取endpoint路径（去除路径参数）
         */
        private String extractEndpoint(String path) {
            if (path == null || path.isEmpty()) {
                return "root";
            }
            
            // 移除查询参数
            int queryIndex = path.indexOf('?');
            if (queryIndex != -1) {
                path = path.substring(0, queryIndex);
            }
            
            // 提取主要路径
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return parts[2]; // /api/v1/users -> users
            }
            
            return path.replace("/", "_");
        }
    }
}