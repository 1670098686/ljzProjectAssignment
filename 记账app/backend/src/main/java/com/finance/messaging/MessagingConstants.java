package com.finance.messaging;

/**
 * Central place for exchange, queue and routing key names so producers and
 * consumers stay consistent.
 */
public final class MessagingConstants {

    private MessagingConstants() {
    }

    public static final String EVENT_EXCHANGE = "finance.events";
    public static final String DLX_EXCHANGE = "finance.events.dlx";

    public static final String QUEUE_BUDGET_ALERT = "finance.queue.budget-alert";
    public static final String QUEUE_NOTIFICATION = "finance.queue.notification";
    public static final String QUEUE_STATISTICS = "finance.queue.statistics";

    public static final String QUEUE_BUDGET_ALERT_DLQ = QUEUE_BUDGET_ALERT + ".dlq";
    public static final String QUEUE_NOTIFICATION_DLQ = QUEUE_NOTIFICATION + ".dlq";
    public static final String QUEUE_STATISTICS_DLQ = QUEUE_STATISTICS + ".dlq";

    public static final String ROUTING_BUDGET_ALERT = "budget.alert.created";
    public static final String ROUTING_NOTIFICATION = "notification.dispatch";
    public static final String ROUTING_STATISTICS = "statistics.recompute";

    public static final String ROUTING_BUDGET_ALERT_DLQ = ROUTING_BUDGET_ALERT + ".dlq";
    public static final String ROUTING_NOTIFICATION_DLQ = ROUTING_NOTIFICATION + ".dlq";
    public static final String ROUTING_STATISTICS_DLQ = ROUTING_STATISTICS + ".dlq";
}
