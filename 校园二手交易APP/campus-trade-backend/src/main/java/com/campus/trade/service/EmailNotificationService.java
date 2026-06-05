package com.campus.trade.service;

import com.campus.trade.config.MailProperties;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.EmailTemplateType;
import com.campus.trade.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailNotificationService {

    private final EmailService emailService;
    private final MailProperties mailProperties;

    public EmailNotificationService(EmailService emailService, MailProperties mailProperties) {
        this.emailService = emailService;
        this.mailProperties = mailProperties;
    }

    public void notifySellerNewOrder(Order order) {
        if (order == null) {
            return;
        }
        String buyerName = resolveName(order.getBuyer());
        sendOrderUpdateEmail(order.getSeller(), "有新的订单待确认", "买家 " + buyerName + " 已提交订单，请及时确认。", order);
    }

    public void notifyBuyerOrderAccepted(Order order) {
        if (order == null) {
            return;
        }
        String sellerName = resolveName(order.getSeller());
        sendOrderUpdateEmail(order.getBuyer(), "卖家已确认订单", sellerName + " 将尽快与您联系并安排交付。", order);
    }

    public void notifyBuyerOrderRejected(Order order, String reason) {
        if (order == null) {
            return;
        }
        String detail = StringUtils.hasText(reason) ? "拒绝原因：" + reason : "卖家未提供具体原因。";
        sendOrderUpdateEmail(order.getBuyer(), "卖家拒绝了订单", detail, order);
    }

    public void notifySellerOrderCancelledByBuyer(Order order, String reason) {
        if (order == null) {
            return;
        }
        String buyerName = resolveName(order.getBuyer());
        String detail = (StringUtils.hasText(reason)
                ? "买家备注：" + reason
                : "买家未填写说明。") + " 发起人：" + buyerName;
        sendOrderUpdateEmail(order.getSeller(), "买家取消了订单", detail, order);
    }

    public void notifyOrderCompleted(Order order) {
        if (order == null) {
            return;
        }
        sendOrderUpdateEmail(order.getSeller(), "买家已确认收货", "系统已为您结算该笔订单。", order);
    }

    public void notifyPaymentSucceeded(Order order) {
        if (order == null) {
            return;
        }
        Map<String, Object> buyerVariables = baseOrderVariables(order, order.getBuyer());
        emailService.sendTemplateEmail(order.getBuyer().getEmail(), EmailTemplateType.PAYMENT_SUCCESS_BUYER, buyerVariables);

        Map<String, Object> sellerVariables = baseOrderVariables(order, order.getSeller());
        emailService.sendTemplateEmail(order.getSeller().getEmail(), EmailTemplateType.PAYMENT_SUCCESS_SELLER, sellerVariables);
    }

    private void sendOrderUpdateEmail(User recipient, String title, String message, Order order) {
        if (recipient == null || order == null || !StringUtils.hasText(recipient.getEmail())) {
            return;
        }
        Map<String, Object> variables = baseOrderVariables(order, recipient);
        variables.put("title", title);
        variables.put("message", message);
        emailService.sendTemplateEmail(recipient.getEmail(), EmailTemplateType.ORDER_STATUS_UPDATE, variables);
    }

    private Map<String, Object> baseOrderVariables(Order order, User recipient) {
        Map<String, Object> variables = new HashMap<>();
        if (recipient != null) {
            variables.put("recipientName", resolveName(recipient));
        }
        variables.put("orderNo", order.getOrderNo());
        variables.put("orderUrl", buildOrderUrl(order.getId()));
        variables.put("statusLabel", translateStatus(order.getStatus()));
        Product product = order.getProduct();
        if (product != null) {
            variables.put("productTitle", product.getTitle());
        }
        BigDecimal price = order.getPrice();
        if (price != null) {
            variables.put("price", price.stripTrailingZeros().toPlainString());
        }
        return variables;
    }

    private String translateStatus(OrderStatus status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case PENDING_PAYMENT -> "待支付";
            case PENDING_SHIPMENT -> "待发货";
            case PENDING_RECEIPT -> "待收货";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }

    private String buildOrderUrl(Long orderId) {
        String baseUrl = mailProperties.getAppBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return String.format("/orders/%s", orderId);
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "orders/" + orderId;
        }
        return baseUrl + "/orders/" + orderId;
    }

    private String resolveName(User user) {
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return StringUtils.hasText(user.getEmail()) ? user.getEmail() : "用户";
    }
}
