package com.campus.trade.repository;

import com.campus.trade.model.entity.OperationLogEntry;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLogEntry, Long>, JpaSpecificationExecutor<OperationLogEntry> {
}
