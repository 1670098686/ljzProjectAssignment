package com.finance.aspect;

import com.finance.annotation.OperationLog;
import com.finance.context.UserContext;
import com.finance.service.OperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志AOP切面
 * 自动拦截标记了@OperationLog注解的方法并记录操作日志
 */
@Aspect
@Component
@Order(100) // 确保在其他切面之后执行
public class OperationLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    private final UserContext userContext;

    @Autowired
    public OperationLogAspect(OperationLogService operationLogService, ObjectMapper objectMapper, UserContext userContext) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
        this.userContext = userContext;
    }

    /**
     * 环绕通知：拦截标记了@OperationLog注解的方法
     */
    @Around("@annotation(operationLog)")
    public Object aroundOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLog) 
            throws Throwable {
        
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        String operationType = operationLog.value();
        String description = operationLog.description();
        String businessType = operationLog.businessType();
        
        // 获取用户ID（需要根据实际的用户认证机制获取）
        Long userId = getCurrentUserId();
        
        // 获取请求信息
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        
        // 获取业务对象ID（从方法参数中提取）
        Long businessId = extractBusinessId(joinPoint, businessType);
        
        Object result = null;
        String errorMessage = null;
        String operationParams = null;
        String operationResult = null;
        
        try {
            // 记录方法参数（如果需要）
            if (operationLog.recordParams()) {
                operationParams = serializeObject(joinPoint.getArgs());
            }
            
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录返回值（如果需要）
            if (operationLog.recordResult()) {
                operationResult = serializeObject(result);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录成功日志
            if (businessId != null && !businessType.isEmpty()) {
                operationLogService.logBusinessOperation(
                    operationType, description, userId, businessType, businessId);
            } else {
                operationLogService.logSuccess(
                    operationType, description, userId, operationResult, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            errorMessage = e.getMessage();
            
            // 记录失败日志
            if (businessId != null && !businessType.isEmpty()) {
                // 记录带业务对象的失败日志
                operationLogService.logBusinessOperation(operationType, description, userId, businessType, businessId);
            } else {
                operationLogService.logFailure(
                    operationType, description, userId, errorMessage, executionTime);
            }
            
            logger.error("操作日志记录失败：{}, 方法：{}, 错误：{}", 
                        operationType, signature.getName(), e.getMessage());
            
            // 重新抛出异常
            throw e;
        }
    }

    /**
     * 获取当前用户ID
     * 使用UserContext服务获取当前认证用户的ID
     */
    private Long getCurrentUserId() {
        try {
            return userContext.getCurrentUserId();
        } catch (Exception e) {
            logger.warn("获取当前用户ID失败，使用默认用户ID: {}", e.getMessage());
            return 1L; // 失败时返回默认用户ID
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            logger.warn("获取客户端IP地址失败：", e);
            return null;
        }
    }

    /**
     * 获取用户代理
     */
    private String getUserAgent() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            logger.warn("获取用户代理失败：", e);
            return null;
        }
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从方法参数中提取业务对象ID
     */
    private Long extractBusinessId(ProceedingJoinPoint joinPoint, String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return null;
        }
        
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        
        // 根据不同的业务类型和操作类型来提取ID
        switch (businessType.toUpperCase()) {
            case "TRANSACTION":
                if (methodName.contains("delete") || methodName.contains("update")) {
                    // 删除或更新操作，第一个参数通常是ID
                    if (args.length > 0 && args[0] instanceof Long) {
                        return (Long) args[0];
                    }
                }
                break;
                
            case "BUDGET":
                if (args.length > 0 && args[0] instanceof Long) {
                    return (Long) args[0];
                }
                break;
                
            case "CATEGORY":
                if (args.length > 0 && args[0] instanceof Long) {
                    return (Long) args[0];
                }
                break;
                
            case "SAVING_GOAL":
                if (args.length > 0 && args[0] instanceof Long) {
                    return (Long) args[0];
                }
                break;
                
            case "SAVING_RECORD":
                if (args.length > 0 && args[0] instanceof Long) {
                    return (Long) args[0];
                }
                break;
                
            default:
                break;
        }
        
        return null;
    }

    /**
     * 序列化对象为JSON字符串
     */
    private String serializeObject(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("序列化对象失败：", e);
            return obj.toString();
        }
    }
}