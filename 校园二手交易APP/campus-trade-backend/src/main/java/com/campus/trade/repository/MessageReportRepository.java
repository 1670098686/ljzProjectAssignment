package com.campus.trade.repository;

import com.campus.trade.model.entity.MessageReport;
import com.campus.trade.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReportRepository extends JpaRepository<MessageReport, Long> {

    Page<MessageReport> findByStatus(ReportStatus status, Pageable pageable);
}
