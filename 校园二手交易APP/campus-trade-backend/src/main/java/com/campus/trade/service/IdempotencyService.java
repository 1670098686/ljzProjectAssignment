package com.campus.trade.service;

import com.campus.trade.config.IdempotencyProperties;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.IdempotencyRecord;
import com.campus.trade.model.enums.IdempotencyStatus;
import com.campus.trade.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class IdempotencyService {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final IdempotencyRecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties properties;
    private final TransactionTemplate idempotencyTxTemplate;

    public IdempotencyService(IdempotencyRecordRepository recordRepository,
                              ObjectMapper objectMapper,
                              IdempotencyProperties properties,
                              PlatformTransactionManager transactionManager) {
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.idempotencyTxTemplate = new TransactionTemplate(transactionManager);
        this.idempotencyTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T execute(String idempotencyKey,
                         String owner,
                         String scope,
                         Object requestBody,
                         Supplier<T> action,
                         Class<T> responseType) {
        if (!properties.isEnabled()) {
            return action.get();
        }
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED, "缺少 Idempotency-Key 请求头");
        }
        if (!StringUtils.hasText(owner)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "无法识别当前用户用于幂等校验");
        }
        String normalizedScope = StringUtils.hasText(scope) ? scope : "DEFAULT";
        String requestHash = buildRequestHash(requestBody);

        Optional<IdempotencyRecord> existingOpt = recordRepository.findByOwnerAndIdempotencyKey(owner, idempotencyKey);
        if (existingOpt.isPresent()) {
            return resolveExistingRecord(existingOpt.get(), requestHash, responseType);
        }

        IdempotencyRecord pendingRecord = createPendingRecord(owner, normalizedScope, idempotencyKey, requestHash);
        try {
            T result = action.get();
            String responseSnapshot = serializeResponse(result, responseType);
            markAsCompleted(pendingRecord.getId(), responseSnapshot);
            return result;
        } catch (BusinessException ex) {
            markAsFailed(pendingRecord.getId(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markAsFailed(pendingRecord.getId(), ex.getMessage());
            throw ex;
        }
    }

    private <T> T resolveExistingRecord(IdempotencyRecord record, String requestHash, Class<T> responseType) {
        if (record.getRequestHash() != null && requestHash != null && !record.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, "当前 Idempotency-Key 已被其他请求使用");
        }
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return deserializeResponse(record.getResponseBody(), responseType);
        }
        if (record.getStatus() == IdempotencyStatus.FAILED) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REPLAYED, "此前请求失败，请更换 Idempotency-Key 重新提交");
        }
        throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS, "请求仍在处理中，请稍候");
    }

    private IdempotencyRecord createPendingRecord(String owner,
                                                  String scope,
                                                  String key,
                                                  String requestHash) {
        try {
            IdempotencyRecord record = idempotencyTxTemplate.execute(status -> {
                IdempotencyRecord entity = new IdempotencyRecord();
                entity.setOwner(owner);
                entity.setScope(scope);
                entity.setIdempotencyKey(key);
                entity.setRequestHash(requestHash);
                entity.setStatus(IdempotencyStatus.PENDING);
                entity.setExpireAt(LocalDateTime.now().plusMinutes(Math.max(1, properties.getTtlMinutes())));
                return recordRepository.save(entity);
            });
            if (record == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建幂等记录失败");
            }
            return record;
        } catch (DataIntegrityViolationException ex) {
            return recordRepository.findByOwnerAndIdempotencyKey(owner, key)
                    .orElseThrow(() -> ex);
        }
    }

    private void markAsCompleted(Long recordId, String responseSnapshot) {
        idempotencyTxTemplate.executeWithoutResult(status -> {
            IdempotencyRecord record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            record.setErrorMessage(null);
            record.setResponseBody(responseSnapshot);
            recordRepository.save(record);
        });
    }

    private void markAsFailed(Long recordId, String errorMessage) {
        idempotencyTxTemplate.executeWithoutResult(status -> {
            IdempotencyRecord record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalStateException("Idempotency record not found"));
            record.setStatus(IdempotencyStatus.FAILED);
            record.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "UNKNOWN_ERROR");
            recordRepository.save(record);
        });
    }

    private String buildRequestHash(Object requestBody) {
        if (requestBody == null) {
            return null;
        }
        try {
            byte[] payload = objectMapper.writeValueAsBytes(requestBody);
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "请求摘要失败");
        }
    }

    private <T> String serializeResponse(T response, Class<T> responseType) {
        if (responseType == Void.class || response == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "响应序列化失败");
        }
    }

    private <T> T deserializeResponse(String payload, Class<T> responseType) {
        if (responseType == Void.class || payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, responseType);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "幂等响应解析失败");
        }
    }
}
