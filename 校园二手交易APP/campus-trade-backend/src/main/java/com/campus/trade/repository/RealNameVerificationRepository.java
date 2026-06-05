package com.campus.trade.repository;

import com.campus.trade.model.entity.RealNameVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealNameVerificationRepository extends JpaRepository<RealNameVerification, Long> {

    Optional<RealNameVerification> findByUserId(Long userId);
}
