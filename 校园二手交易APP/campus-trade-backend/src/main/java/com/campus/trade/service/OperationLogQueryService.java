package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.admin.OperationLogResponse;
import com.campus.trade.model.entity.OperationLogEntry;
import com.campus.trade.model.enums.OperationResult;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.repository.OperationLogRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogQueryService {

    private final OperationLogRepository repository;

    public OperationLogQueryService(OperationLogRepository repository) {
        this.repository = repository;
    }

    public PaginatedResponse<OperationLogResponse> list(
            String operator,
            String action,
            OperationType type,
            OperationResult result,
            int page,
            int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Specification<OperationLogEntry> spec = Specification.where(null);
        if (StringUtils.hasText(operator)) {
            String keyword = operator.trim().toLowerCase();
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("operator")), "%" + keyword + "%"));
        }
        if (StringUtils.hasText(action)) {
            String keyword = action.trim().toLowerCase();
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("action")), "%" + keyword + "%"));
        }
        if (type != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), type));
        }
        if (result != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("result"), result));
        }

        PageRequest pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OperationLogEntry> pageData = repository.findAll(spec, pageable);
        List<OperationLogResponse> items = pageData.getContent().stream().map(OperationLogResponse::from).toList();
        return PaginatedResponse.of(items, safePage, safeSize, pageData.getTotalElements());
    }
}
