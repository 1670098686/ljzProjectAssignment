package com.finance.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * API网关熔断回退控制器
 * 
 * 当下游服务不可用时，提供友好的错误响应
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 主服务熔断回退处理
     */
    @GetMapping("/main-service")
    public ResponseEntity<Map<String, Object>> fallbackMainService() {
        Map<String, Object> response = createFallbackResponse("主服务", 
            "用户管理服务暂时不可用，请稍后重试");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 统计服务熔断回退处理
     */
    @GetMapping("/statistics-service")
    public ResponseEntity<Map<String, Object>> fallbackStatisticsService() {
        Map<String, Object> response = createFallbackResponse("统计服务", 
            "数据分析服务暂时不可用，请稍后重试");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 预警服务熔断回退处理
     */
    @GetMapping("/alert-service")
    public ResponseEntity<Map<String, Object>> fallbackAlertService() {
        Map<String, Object> response = createFallbackResponse("预警服务", 
            "预警通知服务暂时不可用，紧急请直接联系客服");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 通用熔断回退处理
     */
    @GetMapping("/{service}")
    public ResponseEntity<Map<String, Object>> fallbackService(@PathVariable String service) {
        String serviceName = service.replace("-service", "服务");
        Map<String, Object> response = createFallbackResponse(serviceName, 
            serviceName + "暂时不可用，请稍后重试");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 创建熔断回退响应
     */
    private Map<String, Object> createFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("service", serviceName);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("retryable", true);
        response.put("errorCode", "SERVICE_TEMP_UNAVAILABLE");
        response.put("help", "如果问题持续存在，请联系技术支持团队");
        return response;
    }
}