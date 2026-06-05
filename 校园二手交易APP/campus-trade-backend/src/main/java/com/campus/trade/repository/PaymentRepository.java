package com.campus.trade.repository;

import com.campus.trade.model.entity.Payment;
import com.campus.trade.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findTopByOrderIdOrderByCreateTimeDesc(Long orderId);

    List<Payment> findByOrderIdOrderByCreateTimeDesc(Long orderId);

    List<Payment> findByOrderIdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    Optional<Payment> findByReferenceNo(String referenceNo);

    Optional<Payment> findByIdAndBuyerId(Long paymentId, Long buyerId);
}
