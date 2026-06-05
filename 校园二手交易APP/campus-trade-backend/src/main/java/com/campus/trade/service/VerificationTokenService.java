package com.campus.trade.service;

import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.entity.VerificationToken;
import com.campus.trade.model.enums.VerificationTokenType;
import com.campus.trade.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VerificationTokenService {

    private static final Logger log = LoggerFactory.getLogger(VerificationTokenService.class);

    private final VerificationTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationTokenService(VerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public VerificationToken createToken(User user, String email, VerificationTokenType type, Duration ttl) {
        enforceRateLimit(user, email, type);
        invalidateActiveTokens(user, email, type);
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setEmail(email);
        token.setToken(generateNumericCode());
        token.setType(type);
        token.setExpiresAt(LocalDateTime.now().plus(ttl));
        if (user != null) {
            log.debug("Generated {} token for user {}", type, user.getId());
        } else {
            log.debug("Generated {} token for email {}", type, email);
        }
        return tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public VerificationToken validateOrThrow(User user, String email, String providedCode, VerificationTokenType type) {
        VerificationToken token;
        if (user != null) {
            token = tokenRepository
                    .findTopByUserAndTokenAndTypeOrderByCreateTimeDesc(user, providedCode, type)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "验证码不存在"));
        } else {
            token = tokenRepository
                    .findTopByEmailAndTokenAndTypeOrderByCreateTimeDesc(email, providedCode, type)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "验证码不存在"));
        }
        if (token.isUsed()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "验证码已被使用");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED, "验证码已过期");
        }
        return token;
    }

    @Transactional
    public void markUsed(VerificationToken token) {
        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        // 显式保存修改，确保数据持久化
        tokenRepository.save(token);
    }

    @Transactional
    public long purgeExpiredUsedTokens() {
        return tokenRepository.deleteByUsedIsTrueAndExpiresAtBefore(LocalDateTime.now().minusDays(30));
    }

    @Transactional(readOnly = true)
    public boolean hasValidVerifiedEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return tokenRepository.findTopByEmailAndTypeAndUsedTrueOrderByUsedAtDesc(email, VerificationTokenType.EMAIL_VERIFICATION)
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    private void enforceRateLimit(User user, String email, VerificationTokenType type) {
        LocalDateTime now = LocalDateTime.now();

        if (user != null) {
            tokenRepository.findTopByUserAndTypeOrderByCreateTimeDesc(user, type)
                    .ifPresent(latest -> {
                        if (latest.getCreateTime() != null && latest.getCreateTime().isAfter(now.minusSeconds(60))) {
                            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作过于频繁，请稍后再试");
                        }
                    });

            long countLastHour = tokenRepository.countByUserAndTypeAndCreateTimeAfter(user, type, now.minusHours(1));
            if (countLastHour >= 20) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请求次数过多，请稍后再试");
            }
        } else {
            tokenRepository.findTopByEmailAndTypeOrderByCreateTimeDesc(email, type)
                    .ifPresent(latest -> {
                        if (latest.getCreateTime() != null && latest.getCreateTime().isAfter(now.minusSeconds(60))) {
                            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作过于频繁，请稍后再试");
                        }
                    });

            long countLastHour = tokenRepository.countByEmailAndTypeAndCreateTimeAfter(email, type, now.minusHours(1));
            if (countLastHour >= 20) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请求次数过多，请稍后再试");
            }
        }
    }

    @Transactional
    private void invalidateActiveTokens(User user, String email, VerificationTokenType type) {
        List<VerificationToken> activeTokens;
        if (user != null) {
            activeTokens = tokenRepository.findByUserAndTypeAndUsedFalse(user, type);
        } else {
            activeTokens = tokenRepository.findByEmailAndTypeAndUsedFalse(email, type);
        }
        LocalDateTime now = LocalDateTime.now();
        for (VerificationToken token : activeTokens) {
            token.setUsed(true);
            token.setUsedAt(now);
        }
        // 显式保存所有修改的令牌，确保数据持久化
        if (!activeTokens.isEmpty()) {
            tokenRepository.saveAll(activeTokens);
        }
    }

    private String generateNumericCode() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
