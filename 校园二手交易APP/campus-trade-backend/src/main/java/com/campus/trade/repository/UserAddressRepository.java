package com.campus.trade.repository;

import com.campus.trade.model.entity.UserAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIsDefaultDescCreateTimeDesc(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    List<UserAddress> findByUserId(Long userId);
}
