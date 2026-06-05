package com.campus.trade.util;

import com.campus.trade.dto.message.MessageResponse;
import com.campus.trade.model.entity.Message;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setSender(UserMapper.toMaskedSummary(message.getSender()));
        response.setReceiver(UserMapper.toMaskedSummary(message.getReceiver()));
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setRelatedType(message.getRelatedType());
        response.setRelatedId(message.getRelatedId());
        response.setAttachmentUrl(message.getAttachmentUrl());
        response.setRead(message.isRead());
        response.setReadTime(message.getReadTime());
        response.setCreateTime(message.getCreateTime());
        return response;
    }
}
