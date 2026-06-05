package com.campus.trade.service;

import com.campus.trade.dto.user.RealNameStatusResponse;
import com.campus.trade.dto.user.RealNameSubmitRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.RealNameVerification;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.VerificationStatus;
import com.campus.trade.repository.RealNameVerificationRepository;
import com.campus.trade.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RealNameVerificationService {

    private final UserRepository userRepository;
    private final RealNameVerificationRepository repository;

    public RealNameVerificationService(UserRepository userRepository, RealNameVerificationRepository repository) {
        this.userRepository = userRepository;
        this.repository = repository;
    }

    @Transactional
    public RealNameStatusResponse getStatus(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return repository.findByUserId(user.getId())
                .map(RealNameVerificationService::toResponse)
                .orElseGet(() -> {
                    RealNameStatusResponse res = new RealNameStatusResponse();
                    res.setStatus(VerificationStatus.NONE);
                    return res;
                });
    }

    @Transactional
    public RealNameStatusResponse submit(String username, RealNameSubmitRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String idNumber = request.getIdNumber() == null ? "" : request.getIdNumber().trim();
        if (idNumber.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "身份证号不能为空");
        }
        String last4 = idNumber.length() >= 4 ? idNumber.substring(idNumber.length() - 4) : idNumber;

        RealNameVerification verification = repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    RealNameVerification v = new RealNameVerification();
                    v.setUser(user);
                    return v;
                });

        verification.setRealName(request.getRealName());
        verification.setIdNumberHash(sha256Hex(idNumber));
        verification.setIdNumberLast4(String.format("%4s", last4).replace(' ', '0'));
        verification.setStatus(VerificationStatus.PENDING);
        verification.setSubmittedAt(LocalDateTime.now());
        verification.setReviewedAt(null);
        verification.setRejectReason(null);

        repository.save(verification);
        return toResponse(verification);
    }

    private static RealNameStatusResponse toResponse(RealNameVerification v) {
        RealNameStatusResponse res = new RealNameStatusResponse();
        res.setStatus(v.getStatus());
        res.setRealName(v.getRealName());
        res.setIdNumberLast4(v.getIdNumberLast4());
        res.setSubmittedAt(v.getSubmittedAt());
        res.setRejectReason(v.getRejectReason());
        return res;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法处理证件信息");
        }
    }
}
