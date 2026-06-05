package com.campus.trade.util;

import com.campus.trade.dto.cart.CartItemResponse;
import com.campus.trade.model.entity.CartItem;

public final class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }
        CartItemResponse response = new CartItemResponse();
        response.setId(cartItem.getId());
        response.setQuantity(cartItem.getQuantity());
        response.setProduct(ProductMapper.toResponse(cartItem.getProduct()));
        response.setCreateTime(cartItem.getCreateTime());
        response.setUpdateTime(cartItem.getUpdateTime());
        return response;
    }
}
