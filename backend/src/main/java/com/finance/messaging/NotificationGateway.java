package com.finance.messaging;

import com.finance.messaging.payload.BudgetEventMessage;
import com.finance.messaging.payload.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder gateway for downstream notification channels. For now it logs
 * the intent so we have observability while wiring the asynchronous
 * pipeline. Later it can forward to SMS, push, or email providers.
 */
@Component
public class NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGateway.class);

    public void dispatchBudgetEvent(BudgetEventMessage message) {
        if (message.getEventType() == BudgetEventMessage.EventType.ALERT && message.getAlert() != null) {
            log.info("[BudgetAlert] user={} category={} level={} message={}",
                    message.getUserId(),
                    message.getCategoryId(),
                    message.getAlert().getAlertLevel(),
                    message.getAlert().getMessage());
        } else {
            log.warn("[BudgetOverspend] user={} budget={} spent={}/{} for period {}-{}",
                    message.getUserId(),
                    message.getBudgetId(),
                    message.getSpentAmount(),
                    message.getBudgetAmount(),
                    message.getYear(),
                    message.getMonth());
        }
    }

    public void dispatchUserNotification(NotificationMessage message) {
        log.info("[UserNotification] user={} title={} content={} metadata={}",
                message.getUserId(),
                message.getTitle(),
                message.getContent(),
                message.getMetadata());
    }
}
