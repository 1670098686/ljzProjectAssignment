package com.mall.stock.repository;

import com.mall.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Stock findByProductId(Long productId);

    @Modifying
    @Query("UPDATE Stock s SET s.availableQty = s.availableQty - :quantity, s.lockedQty = s.lockedQty + :quantity, s.version = s.version + 1 WHERE s.productId = :productId AND s.availableQty >= :quantity AND s.version = :version")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    @Modifying
    @Query("UPDATE Stock s SET s.lockedQty = s.lockedQty - :quantity, s.availableQty = s.availableQty + :quantity, s.version = s.version + 1 WHERE s.productId = :productId AND s.lockedQty >= :quantity AND s.version = :version")
    int rollbackStock(@Param("productId") Long productId, @Param("quantity") Integer quantity, @Param("version") Integer version);
}