package com.finance.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Bridges the internal {@link DomainEventPublisher} abstraction to Spring's
 * {@link ApplicationEventPublisher} so our domain layers remain decoupled
 * from the framework.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    public SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }
        delegate.publishEvent(event);
    }
}
