package com.mall.product.repository;

import com.mall.product.entity.StockOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockOperationLogRepository extends JpaRepository<StockOperationLog, Long> {

    Optional<StockOperationLog> findByOrderIdAndProductIdAndActionType(String orderId, Long productId, String actionType);
}
