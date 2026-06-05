package com.campus.trade.repository.projection;

import com.campus.trade.model.enums.OrderStatus;

public interface OrderStatusCountView {

    OrderStatus getStatus();

    long getTotal();
}
