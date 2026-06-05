package com.campus.trade.repository;

import com.campus.trade.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    Page<Message> findBySenderIdOrReceiverId(Long senderId, Long receiverId, Pageable pageable);

    List<Message> findBySenderIdAndReceiverIdAndReadFalse(Long senderId, Long receiverId);

    List<Message> findBySenderIdOrReceiverIdOrderByCreateTimeDesc(Long senderId, Long receiverId);

    long countByReadFalse();

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);
}
