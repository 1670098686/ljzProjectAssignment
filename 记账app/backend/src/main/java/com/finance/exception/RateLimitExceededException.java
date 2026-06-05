package com.finance.exception;

/**
 * API限流超限异常
 * 
 * 当API请求频率超过限制时抛出此异常
 * 
 * @author Finance Application Team
 * @since 2025-01
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final String clientIp;
    private final String requestPath;
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String message) {
        super(message);
        this.clientIp = null;
        this.requestPath = null;
        this.retryAfterSeconds = 60;
    }
    
    public RateLimitExceededException(String message, String clientIp, String requestPath, long retryAfterSeconds) {
        super(message);
        this.clientIp = clientIp;
        this.requestPath = requestPath;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public String getRequestPath() {
        return requestPath;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}