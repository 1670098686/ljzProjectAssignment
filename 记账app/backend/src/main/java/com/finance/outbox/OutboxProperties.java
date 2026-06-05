package com.finance.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private int batchSize = 50;
    private int maxAttempts = 5;
    private Duration retryDelay = Duration.ofSeconds(30);
    private Duration dispatchInterval = Duration.ofSeconds(2);

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Duration getDispatchInterval() {
        return dispatchInterval;
    }

    public void setDispatchInterval(Duration dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
    }
}
