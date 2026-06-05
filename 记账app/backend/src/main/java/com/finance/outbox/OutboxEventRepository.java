package com.finance.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findTop100ByStatusInAndAvailableAtBeforeOrderByCreatedAtAsc(Collection<OutboxEventStatus> statuses,
                                                                                   LocalDateTime availableBefore);
}
