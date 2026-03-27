package com.finance.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private String id;

    @Column(name = "exchange_name", nullable = false, length = 100)
    private String exchange;

    @Column(name = "routing_key", nullable = false, length = 150)
    private String routingKey;

    @Column(name = "payload_type", nullable = false, length = 255)
    private String payloadType;

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "LONGTEXT")
    private String lastError;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected OutboxEvent() {
        // JPA only
    }

    private OutboxEvent(String exchange,
                        String routingKey,
                        String payloadType,
                        String payload) {
        this.id = UUID.randomUUID().toString();
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payloadType = payloadType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.availableAt = LocalDateTime.now();
        this.attempts = 0;
    }

    public static OutboxEvent pending(String exchange,
                                      String routingKey,
                                      String payloadType,
                                      String payload) {
        return new OutboxEvent(exchange, routingKey, payloadType, payload);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = OutboxEventStatus.COMPLETED;
        this.lastError = null;
        this.availableAt = LocalDateTime.now();
    }

    public void markForRetry(String errorMessage, LocalDateTime nextAttemptAt, int maxAttempts) {
        this.attempts += 1;
        this.lastError = errorMessage;
        if (this.attempts >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
            this.availableAt = nextAttemptAt;
        } else {
            this.status = OutboxEventStatus.RETRY;
            this.availableAt = nextAttemptAt;
        }
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxEventStatus.FAILED;
        this.lastError = errorMessage;
        this.availableAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
