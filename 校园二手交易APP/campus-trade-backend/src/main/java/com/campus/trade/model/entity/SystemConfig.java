package com.campus.trade.model.entity;

import com.campus.trade.model.enums.SystemConfigScope;
import com.campus.trade.model.enums.SystemConfigStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_configs")
public class SystemConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 100, unique = true)
    private String key;

    @Column(name = "config_value", nullable = false, length = 500)
    private String value;

    @Column(length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SystemConfigScope scope = SystemConfigScope.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SystemConfigStatus status = SystemConfigStatus.ACTIVE;
}
