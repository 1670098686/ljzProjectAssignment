package com.campus.trade.repository;

import com.campus.trade.model.entity.Report;
import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import com.campus.trade.repository.projection.ReportStatusCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    long countByTargetTypeAndTargetIdAndReporterId(ReportTargetType targetType, Long targetId, Long reporterId);

    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    long countByStatus(ReportStatus status);

    long countByAutoFlaggedTrue();

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByReporterId(Long reporterId);

    long countByReporterIdAndStatus(Long reporterId, ReportStatus status);

    @Override
    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAll(Specification<Report> spec, Pageable pageable);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "reporter")
    Optional<Report> findByIdAndReporterId(Long id, Long reporterId);

    @EntityGraph(attributePaths = "reporter")
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    @Query("select r.status as status, count(r) as total from Report r group by r.status")
    java.util.List<ReportStatusCountView> aggregateStatus();

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            java.util.Collection<ReportStatus> statuses);
}
