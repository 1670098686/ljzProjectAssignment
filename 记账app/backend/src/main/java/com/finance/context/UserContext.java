package com.finance.context;

import org.springframework.stereotype.Component;

/**
 * 用户上下文，用于存储当前用户信息，实现多租户支持
 */
@Component
public class UserContext {
    
    // 使用ThreadLocal存储当前用户ID，支持多线程并发
    private final ThreadLocal<Long> userId = new ThreadLocal<>();
    
    // 默认用户ID，用于未认证状态
    private static final long DEFAULT_USER_ID = 1L;
    
    /**
     * 获取当前用户ID
     * 如果未设置，返回默认用户ID
     */
    public Long getCurrentUserId() {
        Long currentId = userId.get();
        return currentId != null ? currentId : DEFAULT_USER_ID;
    }
    
    /**
     * 设置当前用户ID
     */
    public void setCurrentUserId(Long id) {
        userId.set(id);
    }
    
    /**
     * 清除当前用户ID，用于请求结束时清理
     */
    public void clear() {
        userId.remove();
    }
}
