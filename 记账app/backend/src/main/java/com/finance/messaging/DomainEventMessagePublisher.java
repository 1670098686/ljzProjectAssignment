package com.finance.messaging;

import com.finance.event.BudgetAlertEvent;
import com.finance.event.BudgetOverspendEvent;
import com.finance.event.SavingGoalAchievedEvent;
import com.finance.event.TransactionCreatedEvent;
import com.finance.messaging.payload.BudgetEventMessage;
import com.finance.messaging.payload.NotificationMessage;
import com.finance.messaging.payload.StatisticsRecomputeMessage;
import com.finance.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to domain events and repackages them into RabbitMQ messages. This
 * keeps the domain layer unaware of messaging concerns while giving us an
 * asynchronous pipeline for heavy workflows.
 */
@Component
public class DomainEventMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventMessagePublisher.class);

    private final OutboxService outboxService;

    public DomainEventMessagePublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBudgetAlert(BudgetAlertEvent event) {
        send(MessagingConstants.ROUTING_BUDGET_ALERT, BudgetEventMessage.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBudgetOverspend(BudgetOverspendEvent event) {
        send(MessagingConstants.ROUTING_BUDGET_ALERT, BudgetEventMessage.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        send(MessagingConstants.ROUTING_STATISTICS, StatisticsRecomputeMessage.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSavingGoalAchieved(SavingGoalAchievedEvent event) {
        send(MessagingConstants.ROUTING_NOTIFICATION, NotificationMessage.from(event));
    }

    private void send(String routingKey, Object payload) {
        outboxService.enqueue(MessagingConstants.EVENT_EXCHANGE, routingKey, payload);
        log.debug("Enqueued {} for routing {}", payload.getClass().getSimpleName(), routingKey);
    }
}
