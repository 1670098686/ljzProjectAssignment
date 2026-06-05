package com.campus.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /**
     * 是否启用真实邮件发送，默认为 false 表示仅日志模拟。
     */
    private boolean enabled = false;

    /**
     * 发件人邮箱地址。
     */
    private String from;

    /**
     * 邮件底部签名。
     */
    private String signature = "【校园二手交易】";

    /**
     * 支持邮箱，用于邮件正文展示。
     */
    private String supportEmail = "support@campus-trade.com";

    /**
     * 前端应用基础地址，用于拼接详情链接。
     */
    private String appBaseUrl = "https://app.campus-trade.com";

    /**
     * 邮件发送失败重试次数。
     */
    private int maxAttempts = 3;

    /**
     * 重试之间的等待毫秒数。
     */
    private long retryBackoffMillis = 1500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getAppBaseUrl() {
        return appBaseUrl;
    }

    public void setAppBaseUrl(String appBaseUrl) {
        this.appBaseUrl = appBaseUrl;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    public void setRetryBackoffMillis(long retryBackoffMillis) {
        this.retryBackoffMillis = retryBackoffMillis;
    }
}
