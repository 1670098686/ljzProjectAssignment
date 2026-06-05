package com.finance.event;

/**
 * Abstraction that hides the underlying event bus implementation.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
