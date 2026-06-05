package com.campus.trade.model.entity;

import com.campus.trade.model.enums.AdminRole;
import com.campus.trade.model.enums.AdminStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "admins")
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role = AdminRole.ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus status = AdminStatus.ACTIVE;

    @Lob
    @Column(name = "permissions")
    private String permissions;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_ip", length = 50)
    private String loginIp;
}
