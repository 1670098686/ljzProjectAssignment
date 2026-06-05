package com.campus.trade.model.entity;

import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(length = 100)
    private String school;

    @Column(length = 200)
    private String avatar;

    @Column(name = "contact_info", length = 100)
    private String contactInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "delete_requested", nullable = false)
    private boolean deleteRequested = false;

    @Column(name = "delete_reason", length = 200)
    private String deleteReason;

    @Column(name = "delete_schedule_time")
    private LocalDateTime deleteScheduleTime;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "disabled_reason", length = 200)
    private String disabledReason;
    
    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;
    
    @Column(name = "disabled_by")
    private Long disabledBy;
} 
