package com.finance.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository,
                         ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String exchange,
                        String routingKey,
                        Object payload) {
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.pending(exchange, routingKey, payload.getClass().getName(), serializedPayload);
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for routing {}", routingKey, e);
            throw new IllegalStateException("Cannot serialize outbox payload", e);
        }
    }
}
