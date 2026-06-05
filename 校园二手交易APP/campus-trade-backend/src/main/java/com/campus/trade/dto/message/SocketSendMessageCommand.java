package com.campus.trade.dto.message;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.campus.trade.model.enums.MessageType;

/**
 * Command payload for sending a message via WebSocket.
 */
public class SocketSendMessageCommand {

    @NotNull
    private Long toUserId;

    private Long productId;

    private Long orderId;

    private MessageType messageType;

    @NotNull
    private String content;

    private String attachmentUrl;

    @Size(max = 64)
    private String clientMessageId;

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public SendMessageRequest toSendMessageRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setToUserId(this.toUserId);
        request.setProductId(this.productId);
        request.setOrderId(this.orderId);
        request.setMessageType(this.messageType);
        request.setContent(this.content);
        request.setAttachmentUrl(this.attachmentUrl);
        return request;
    }
}
