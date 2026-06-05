package com.finance.messaging.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finance.event.SavingGoalAchievedEvent;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationMessage {

    private final Long userId;
    private final String title;
    private final String content;
    private final LocalDateTime occurredAt;
    private final Map<String, Object> metadata;

    private NotificationMessage(Long userId,
                                String title,
                                String content,
                                LocalDateTime occurredAt,
                                Map<String, Object> metadata) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
    }

    public static NotificationMessage from(SavingGoalAchievedEvent event) {
        return new NotificationMessage(
                event.getUserId(),
                "储蓄目标达成",
                "恭喜你达成储蓄目标，金额 " + event.getTargetAmount(),
                event.getAchievedAt(),
                Map.of(
                        "goalId", event.getGoalId(),
                        "targetAmount", event.getTargetAmount()
                )
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "NotificationMessage{" +
                "userId=" + userId +
                ", title='" + title + '\'' +
                ", occurredAt=" + occurredAt +
                ", metadata=" + metadata +
                '}';
    }
}
