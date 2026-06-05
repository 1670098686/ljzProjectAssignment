package com.campus.trade.repository;

import com.campus.trade.model.entity.IdempotencyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByOwnerAndIdempotencyKey(String owner, String idempotencyKey);
}
