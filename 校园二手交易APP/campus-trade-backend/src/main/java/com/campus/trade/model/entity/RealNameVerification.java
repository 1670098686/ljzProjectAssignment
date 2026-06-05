package com.campus.trade.model.entity;

import com.campus.trade.model.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "real_name_verifications")
public class RealNameVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "id_number_hash", nullable = false, length = 64)
    private String idNumberHash;

    @Column(name = "id_number_last4", nullable = false, length = 4)
    private String idNumberLast4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;
}
