package com.campus.trade.repository;

import com.campus.trade.model.entity.User;
import com.campus.trade.model.entity.VerificationToken;
import com.campus.trade.model.enums.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findTopByUserAndTokenAndTypeOrderByCreateTimeDesc(User user,
                                                                                  String token,
                                                                                  VerificationTokenType type);

    Optional<VerificationToken> findTopByUserAndTypeOrderByCreateTimeDesc(User user,
                                                                          VerificationTokenType type);

    Optional<VerificationToken> findTopByEmailAndTokenAndTypeOrderByCreateTimeDesc(String email,
                                                                                  String token,
                                                                                  VerificationTokenType type);

    Optional<VerificationToken> findTopByEmailAndTypeOrderByCreateTimeDesc(String email,
                                                                          VerificationTokenType type);

    Optional<VerificationToken> findTopByEmailAndTypeAndUsedTrueOrderByUsedAtDesc(String email,
                                                                                  VerificationTokenType type);

    long countByUserAndTypeAndCreateTimeAfter(User user,
                                              VerificationTokenType type,
                                              LocalDateTime time);

    long countByEmailAndTypeAndCreateTimeAfter(String email,
                                              VerificationTokenType type,
                                              LocalDateTime time);

    List<VerificationToken> findByUserAndTypeAndUsedFalse(User user, VerificationTokenType type);

    List<VerificationToken> findByEmailAndTypeAndUsedFalse(String email, VerificationTokenType type);

    long deleteByUsedIsTrueAndExpiresAtBefore(LocalDateTime before);
}
