package com.campus.trade.repository;

import com.campus.trade.model.entity.SystemConfig;
import com.campus.trade.model.enums.SystemConfigScope;
import com.campus.trade.model.enums.SystemConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long>, JpaSpecificationExecutor<SystemConfig> {
    Optional<SystemConfig> findByKey(String key);

    List<SystemConfig> findByScopeAndStatus(SystemConfigScope scope, SystemConfigStatus status);
}
