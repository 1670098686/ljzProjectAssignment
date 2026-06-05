package com.campus.trade.dto.message;

import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.enums.RelatedType;

public class ConversationSummaryResponse {

    private UserSummary partner;
    private MessageResponse lastMessage;
    private long unreadCount;
    private RelatedType relatedType;
    private Long relatedId;

    public UserSummary getPartner() {
        return partner;
    }

    public void setPartner(UserSummary partner) {
        this.partner = partner;
    }

    public MessageResponse getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageResponse lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
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
}
