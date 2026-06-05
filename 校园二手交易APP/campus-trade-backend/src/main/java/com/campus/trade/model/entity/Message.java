package com.campus.trade.model.entity;

import com.campus.trade.model.enums.MessageType;
import com.campus.trade.model.enums.RelatedType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 20)
    private RelatedType relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "attachment_url", length = 200)
    private String attachmentUrl;

    @Column(name = "is_read", nullable = false, columnDefinition = "bit(1)")
    private boolean read = false;

    @Column(name = "read_time")
    private LocalDateTime readTime;
}
