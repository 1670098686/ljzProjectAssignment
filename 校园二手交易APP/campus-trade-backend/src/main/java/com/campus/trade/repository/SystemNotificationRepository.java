package com.campus.trade.repository;

import com.campus.trade.model.entity.SystemNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {

    Page<SystemNotification> findByUserId(Long userId, Pageable pageable);
}
