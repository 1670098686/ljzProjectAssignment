package com.finance.database.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据库路由切面
 * 自动根据SQL语句类型路由到主库或从库
 */
@Aspect
@Component
@Order(1) // 确保在其他事务切面之前执行
public class DataSourceRoutingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceRoutingAspect.class);
    
    private static final String[] READ_ONLY_KEYWORDS = {
        "SELECT", "SHOW", "DESCRIBE", "EXPLAIN", "DESC"
    };
    
    private static final String[] WRITE_KEYWORDS = {
        "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", 
        "TRUNCATE", "REPLACE", "MERGE", "CALL"
    };
    
    @Around("execution(* com.finance.repository..*(..))")
    public Object aroundRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取方法名和参数
        String methodName = method.getName();
        Object[] args = joinPoint.getArgs();
        
        // 判断是否需要强制路由到主库
        if (isForcePrimary(method)) {
            logger.debug("强制路由到主库: {}", methodName);
            return executeWithPrimary(joinPoint);
        }
        
        // 判断是否需要强制路由到从库
        if (isForceReplica(method)) {
            logger.debug("强制路由到从库: {}", methodName);
            return executeWithReplica(joinPoint);
        }
        
        // 根据方法名自动判断
        DatabaseType databaseType = determineDatabaseTypeByMethodName(methodName);
        
        if (databaseType == DatabaseType.REPLICA) {
            logger.debug("自动路由到从库: {}", methodName);
            return executeWithReplica(joinPoint);
        } else {
            logger.debug("自动路由到主库: {}", methodName);
            return executeWithPrimary(joinPoint);
        }
    }
    
    /**
     * 强制路由到主库
     */
    private Object executeWithPrimary(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DatabaseContextHolder.setDatabaseType(DatabaseType.PRIMARY);
            return joinPoint.proceed();
        } finally {
            DatabaseContextHolder.clearDatabaseType();
        }
    }
    
    /**
     * 强制路由到从库
     */
    private Object executeWithReplica(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DatabaseContextHolder.setDatabaseType(DatabaseType.REPLICA);
            return joinPoint.proceed();
        } finally {
            DatabaseContextHolder.clearDatabaseType();
        }
    }
    
    /**
     * 根据方法名判断数据库类型
     */
    private DatabaseType determineDatabaseTypeByMethodName(String methodName) {
        // 查询方法路由到从库
        if (methodName.startsWith("find") || 
            methodName.startsWith("get") || 
            methodName.startsWith("query") ||
            methodName.startsWith("search") ||
            methodName.startsWith("list") ||
            methodName.startsWith("count") ||
            methodName.startsWith("exists") ||
            methodName.startsWith("load") ||
            methodName.contains("By") ||
            methodName.contains("Summary") ||
            methodName.contains("Statistics")) {
            return DatabaseType.REPLICA;
        }
        
        // 修改方法路由到主库
        if (methodName.startsWith("save") || 
            methodName.startsWith("create") || 
            methodName.startsWith("update") ||
            methodName.startsWith("delete") ||
            methodName.startsWith("remove") ||
            methodName.startsWith("batch") ||
            methodName.contains("Count") ||
            methodName.contains("Increment") ||
            methodName.contains("Decrement")) {
            return DatabaseType.PRIMARY;
        }
        
        // 默认为主库
        return DatabaseType.PRIMARY;
    }
    
    /**
     * 强制主库注解
     */
    private boolean isForcePrimary(Method method) {
        return method.isAnnotationPresent(ForcePrimaryDataSource.class);
    }
    
    /**
     * 检查方法是否标记为强制从库
     */
    private boolean isForceReplica(Method method) {
        return method.isAnnotationPresent(ForceReplicaDataSource.class);
    }
}