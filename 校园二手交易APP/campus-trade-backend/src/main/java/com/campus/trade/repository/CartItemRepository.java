package com.campus.trade.repository;

import com.campus.trade.model.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Page<CartItem> findByUserId(Long userId, Pageable pageable);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteAllByUserId(Long userId);

    long countByUserId(Long userId);

    List<CartItem> findAllByUserId(Long userId);
}
