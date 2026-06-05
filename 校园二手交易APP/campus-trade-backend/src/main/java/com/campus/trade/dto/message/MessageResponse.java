package com.campus.trade.dto.message;

import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.enums.MessageType;
import com.campus.trade.model.enums.RelatedType;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private UserSummary sender;
    private UserSummary receiver;
    private String content;
    private MessageType messageType;
    private RelatedType relatedType;
    private Long relatedId;
    private String attachmentUrl;
    private boolean read;
    private LocalDateTime readTime;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserSummary getSender() {
        return sender;
    }

    public void setSender(UserSummary sender) {
        this.sender = sender;
    }

    public UserSummary getReceiver() {
        return receiver;
    }

    public void setReceiver(UserSummary receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public RelatedType getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(RelatedType relatedType) {
        this.relatedType = relatedType;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
