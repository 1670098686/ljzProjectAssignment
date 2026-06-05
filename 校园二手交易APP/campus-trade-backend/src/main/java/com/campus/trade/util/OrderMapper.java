package com.campus.trade.util;

import com.campus.trade.dto.order.OrderResponse;
import com.campus.trade.model.entity.Order;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setProduct(ProductMapper.toResponse(order.getProduct()));
        response.setBuyer(UserMapper.toMaskedSummary(order.getBuyer()));
        response.setSeller(UserMapper.toMaskedSummary(order.getSeller()));
        response.setPrice(order.getPrice());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentReference(order.getPaymentReference());
        response.setPaymentTime(order.getPaymentTime());
        response.setPaymentExpireTime(order.getPaymentExpireTime());
        response.setPaymentMetadata(order.getPaymentMetadata());
        response.setDeliveryTime(order.getDeliveryTime());
        response.setReceiveTime(order.getReceiveTime());
        response.setShippingAddress(order.getShippingAddress());
        response.setBuyerNote(order.getBuyerNote());
        response.setSellerNote(order.getSellerNote());
        response.setBuyerRating(order.getBuyerRating());
        response.setBuyerComment(order.getBuyerComment());
        response.setSellerRating(order.getSellerRating());
        response.setSellerComment(order.getSellerComment());
        response.setRefundStatus(order.getRefundStatus());
        response.setRefundTime(order.getRefundTime());
        return response;
    }
}
