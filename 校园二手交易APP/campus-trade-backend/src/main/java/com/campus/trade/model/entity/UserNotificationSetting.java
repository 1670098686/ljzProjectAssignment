package com.campus.trade.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "notify_chat", nullable = false, columnDefinition = "bit(1)")
    private boolean notifyChat = true;

    @Column(name = "notify_orders", nullable = false, columnDefinition = "bit(1)")
    private boolean notifyOrders = true;

    @Column(name = "notify_system", nullable = false, columnDefinition = "bit(1)")
    private boolean notifySystem = true;

    @Column(name = "notify_marketing", nullable = false, columnDefinition = "bit(1)")
    private boolean notifyMarketing = false;
}
