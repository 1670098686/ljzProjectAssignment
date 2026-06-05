package com.campus.trade.service.impl;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.report.ReportAdminQuery;
import com.campus.trade.dto.report.ReportAuditRequest;
import com.campus.trade.dto.report.ReportCreateRequest;
import com.campus.trade.dto.report.ReportDetailResponse;
import com.campus.trade.dto.report.ReportListRequest;
import com.campus.trade.dto.report.ReportResponse;
import com.campus.trade.dto.report.ReportStatsResponse;
import com.campus.trade.dto.report.ReportStatusSummary;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Message;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.Report;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.ReportTargetType;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.ReportRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.projection.ReportStatusCountView;
import com.campus.trade.service.NotificationService;
import com.campus.trade.service.ReportService;
import com.campus.trade.util.ReportMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of("欺诈", "诈骗", "涉黄", "违法", "辱骂", "骚扰", "spam");
    private static final Collection<ReportStatus> ACTIVE_STATUSES = List.of(ReportStatus.PENDING, ReportStatus.IN_PROGRESS);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             ProductRepository productRepository,
                             OrderRepository orderRepository,
                             MessageRepository messageRepository,
                             NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void createReport(String username, ReportCreateRequest request) {
        User reporter = findUserByUsername(username);
        validateTargetRequirement(request);
        preventDuplicateReport(reporter.getId(), request);

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setTargetSnapshot(buildTargetSnapshot(request.getTargetType(), request.getTargetId(), reporter));
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setEvidenceUrls(new ArrayList<>(request.getEvidenceUrls() == null ? List.of() : request.getEvidenceUrls()));
        report.setContactInfo(request.getContactInfo());

        applyAutoModeration(report);
        reportRepository.save(report);

        notificationService.notifyUser(reporter, "举报已接收", buildUserAcknowledgement(report));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReportResponse> listMyReports(String username, ReportListRequest request) {
        User reporter = findUserByUsername(username);
        ReportListRequest effective = sanitizeRequest(request);
        Specification<Report> spec = buildUserSpecification(reporter.getId(), effective);
        Pageable pageable = PageRequest.of(effective.getPage() - 1, effective.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Report> page = reportRepository.findAll(spec, pageable);
        List<ReportResponse> items = page.getContent().stream()
                .map(ReportMapper::toResponse)
                .collect(Collectors.toList());
        return PaginatedResponse.of(items, effective.getPage(), effective.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getMyReport(String username, Long id) {
        User reporter = findUserByUsername(username);
        Report report = reportRepository.findByIdAndReporterId(id, reporter.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "举报不存在或已处理"));
        return ReportMapper.toDetail(report);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReportResponse> listReports(ReportAdminQuery query) {
        ReportAdminQuery effective = sanitizeAdminQuery(query);
        Specification<Report> spec = buildAdminSpecification(effective);
        Sort sort = Sort.by(Sort.Order.desc("autoFlagged"), Sort.Order.desc("createTime"));
        Pageable pageable = PageRequest.of(effective.getPage() - 1, effective.getSize(), sort);
        Page<Report> page = reportRepository.findAll(spec, pageable);
        List<ReportResponse> items = page.getContent().stream()
                .map(ReportMapper::toResponse)
                .collect(Collectors.toList());
        return PaginatedResponse.of(items, effective.getPage(), effective.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "举报不存在"));
        return ReportMapper.toDetail(report);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportStatsResponse stats() {
        ReportStatsResponse stats = new ReportStatsResponse();
        stats.setTotalReports(reportRepository.count());
        stats.setPendingReports(reportRepository.countByStatus(ReportStatus.PENDING));
        stats.setInProgressReports(reportRepository.countByStatus(ReportStatus.IN_PROGRESS));
        stats.setResolvedReports(reportRepository.countByStatus(ReportStatus.RESOLVED));
        stats.setRejectedReports(reportRepository.countByStatus(ReportStatus.REJECTED));
        stats.setAutoFlaggedReports(reportRepository.countByAutoFlaggedTrue());
        stats.setTodayReports(reportRepository.countByCreateTimeBetween(LocalDate.now().atStartOfDay(), LocalDateTime.now()));
        List<ReportStatusCountView> aggregates = reportRepository.aggregateStatus();
        List<ReportStatusSummary> summaries = aggregates == null ? List.of() : aggregates.stream()
            .map(view -> new ReportStatusSummary(view.getStatus(), view.getTotal()))
            .collect(Collectors.toList());
        stats.setDistribution(summaries);
        return stats;
    }

    @Override
    public void auditReport(Long id, String adminUsername, ReportAuditRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "举报不存在"));
        applyAudit(report, adminUsername, request);
    }

    @Override
    public BatchOperationResult batchAudit(List<Long> ids, String adminUsername, ReportAuditRequest request) {
        if (CollectionUtils.isEmpty(ids)) {
            return BatchOperationResult.builder()
                    .totalCount(0)
                    .successCount(0)
                    .failedCount(0)
                    .message("未指定举报记录")
                    .build();
        }
        long success = 0;
        long failed = 0;
        for (Long id : ids) {
            try {
                Report report = reportRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "举报不存在"));
                applyAudit(report, adminUsername, request);
                success++;
            } catch (BusinessException ex) {
                failed++;
            }
        }
        return BatchOperationResult.builder()
                .totalCount(ids.size())
                .successCount(success)
                .failedCount(failed)
                .message(failed > 0 ? "部分举报处理失败" : "全部举报处理完成")
                .build();
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateTargetRequirement(ReportCreateRequest request) {
        if (request.getTargetType() != ReportTargetType.OTHER && request.getTargetId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "举报对象不能为空");
        }
    }

    private void preventDuplicateReport(Long reporterId, ReportCreateRequest request) {
        if (request.getTargetId() == null) {
            return;
        }
        boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                reporterId,
                request.getTargetType(),
                request.getTargetId(),
                ACTIVE_STATUSES);
        if (exists) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请勿重复举报同一内容，正在处理中");
        }
    }

    private String buildTargetSnapshot(ReportTargetType targetType, Long targetId, User reporter) {
        if (targetType == ReportTargetType.OTHER || targetId == null) {
            return "其他内容";
        }
        return switch (targetType) {
            case PRODUCT -> productRepository.findById(targetId)
                    .map(product -> "商品：" + product.getTitle())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            case ORDER -> orderRepository.findById(targetId)
                    .map(order -> "订单：" + order.getOrderNo())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
            case USER -> userRepository.findById(targetId)
                    .map(user -> "用户：" + user.getUsername())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            case MESSAGE -> buildMessageSnapshot(targetId, reporter);
            default -> "其他内容";
        };
    }

    private String buildMessageSnapshot(Long messageId, User reporter) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "消息不存在"));
        if (!message.getSender().getId().equals(reporter.getId()) && !message.getReceiver().getId().equals(reporter.getId())) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "无权举报该消息");
        }
        return "消息：" + abbreviate(message.getContent(), 50);
    }

    private void applyAutoModeration(Report report) {
        boolean keywordHit = containsHighRisk(report.getReason()) || containsHighRisk(report.getDescription());
        boolean repeated = report.getTargetId() != null &&
            reportRepository.countByTargetTypeAndTargetId(report.getTargetType(), report.getTargetId()) >= 2;
        if (keywordHit || repeated) {
            report.setAutoFlagged(true);
            report.setAutoReason(keywordHit ? "命中高危关键词" : "举报次数异常增高");
            report.setStatus(ReportStatus.IN_PROGRESS);
        } else {
            report.setAutoFlagged(false);
            report.setAutoReason(null);
            report.setStatus(ReportStatus.PENDING);
        }
    }

    private boolean containsHighRisk(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.CHINA);
        return HIGH_RISK_KEYWORDS.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase(Locale.CHINA)));
    }

    private ReportListRequest sanitizeRequest(ReportListRequest request) {
        ReportListRequest effective = request == null ? new ReportListRequest() : request;
        effective.setPage(Math.max(1, effective.getPage()));
        effective.setSize(Math.max(1, Math.min(50, effective.getSize())));
        return effective;
    }

    private ReportAdminQuery sanitizeAdminQuery(ReportAdminQuery query) {
        ReportAdminQuery effective = query == null ? new ReportAdminQuery() : query;
        effective.setPage(Math.max(1, effective.getPage()));
        effective.setSize(Math.max(1, Math.min(100, effective.getSize())));
        return effective;
    }

    private Specification<Report> buildUserSpecification(Long reporterId, ReportListRequest request) {
        Specification<Report> spec = Specification.where((root, cq, cb) -> cb.equal(root.get("reporter").get("id"), reporterId));
        if (request.getStatus() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), request.getStatus()));
        }
        if (request.getTargetType() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("targetType"), request.getTargetType()));
        }
        return spec;
    }

    private Specification<Report> buildAdminSpecification(ReportAdminQuery query) {
        Specification<Report> spec = Specification.where(null);
        if (query.getStatus() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), query.getStatus()));
        }
        if (query.getTargetType() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("targetType"), query.getTargetType()));
        }
        if (Boolean.TRUE.equals(query.getAutoFlaggedOnly())) {
            spec = spec.and((root, cq, cb) -> cb.isTrue(root.get("autoFlagged")));
        }
        if (query.getStartTime() != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("createTime"), query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("createTime"), query.getEndTime()));
        }
        if (StringUtils.hasText(query.getReporterKeyword())) {
            String like = "%" + query.getReporterKeyword().trim().toLowerCase(Locale.CHINA) + "%";
            spec = spec.and((root, cq, cb) -> {
                var reporterJoin = root.join("reporter", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(reporterJoin.get("username")), like),
                        cb.like(cb.lower(reporterJoin.get("email")), like),
                        cb.like(cb.lower(reporterJoin.get("phone")), like)
                );
            });
        }
        return spec;
    }

    private void applyAudit(Report report, String adminUsername, ReportAuditRequest request) {
        if (!(request.getStatus() == ReportStatus.RESOLVED || request.getStatus() == ReportStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅允许将举报置为已处理或已驳回");
        }
        // 允许二次修改已处理的举报
        report.setStatus(request.getStatus());
        report.setResolution(request.getResolution());
        report.setHandledBy(adminUsername);
        report.setHandledTime(LocalDateTime.now());
        notifyReporter(report);
    }

    private void notifyReporter(Report report) {
        String statusText = report.getStatus() == ReportStatus.RESOLVED ? "已处理" : "已驳回";
        String title = "举报处理结果";
        String content = String.format("您对%s的举报%s，处理结果：%s", defaultSnapshot(report), statusText, report.getResolution());
        notificationService.notifyUser(report.getReporter(), title, content);
    }

    private String buildUserAcknowledgement(Report report) {
        return String.format("我们已收到您关于%s的举报，将尽快审核。", defaultSnapshot(report));
    }

    private String defaultSnapshot(Report report) {
        return StringUtils.hasText(report.getTargetSnapshot()) ? report.getTargetSnapshot() : "相关内容";
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }
}
