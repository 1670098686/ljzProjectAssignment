package com.campus.trade.service;

import com.campus.trade.dto.admin.SystemConfigCreateRequest;
import com.campus.trade.dto.admin.SystemConfigResponse;
import com.campus.trade.dto.admin.SystemConfigUpdateRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.SystemConfig;
import com.campus.trade.model.enums.SystemConfigScope;
import com.campus.trade.model.enums.SystemConfigStatus;
import com.campus.trade.repository.SystemConfigRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class SystemConfigService {

    private final SystemConfigRepository repository;

    public SystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    public List<SystemConfigResponse> list(SystemConfigScope scope, SystemConfigStatus status) {
        List<SystemConfig> configs;
        if (scope != null && status != null) {
            configs = repository.findByScopeAndStatus(scope, status);
        } else {
            configs = repository.findAll(Sort.by(Sort.Direction.ASC, "scope").and(Sort.by("key")));
        }
        return configs.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SystemConfigResponse create(SystemConfigCreateRequest request) {
        String normalizedKey = normalizeKey(request.getKey());
        if (repository.findByKey(normalizedKey).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "配置项已存在: " + normalizedKey);
        }

        SystemConfig cfg = new SystemConfig();
        cfg.setKey(normalizedKey);
        cfg.setValue(Objects.toString(request.getValue(), ""));
        cfg.setDescription(trimToNull(request.getDescription()));
        if (request.getScope() != null) cfg.setScope(request.getScope());
        if (request.getStatus() != null) cfg.setStatus(request.getStatus());

        return toResponse(repository.save(cfg));
    }

    @Transactional
    public SystemConfigResponse update(Long id, SystemConfigUpdateRequest request) {
        SystemConfig cfg = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "配置不存在"));

        cfg.setValue(Objects.toString(request.getValue(), ""));
        cfg.setDescription(trimToNull(request.getDescription()));
        if (request.getScope() != null) cfg.setScope(request.getScope());
        if (request.getStatus() != null) cfg.setStatus(request.getStatus());

        return toResponse(repository.save(cfg));
    }

    private SystemConfigResponse toResponse(SystemConfig cfg) {
        SystemConfigResponse r = new SystemConfigResponse();
        r.setId(cfg.getId());
        r.setKey(cfg.getKey());
        r.setValue(cfg.getValue());
        r.setDescription(cfg.getDescription());
        r.setScope(cfg.getScope());
        r.setStatus(cfg.getStatus());
        r.setUpdateTime(cfg.getUpdateTime());
        return r;
    }

    private String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return "";
        }
        return key.trim();
    }

    private String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }
}
