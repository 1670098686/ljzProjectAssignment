package com.mall.order.repository;

import com.mall.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByIdDesc(Long userId);
    List<Order> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);
    Order findByOrderNo(String orderNo);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
