package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.order.BatchOrderCreateResponse;
import com.campus.trade.dto.order.CartCheckoutRequest;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.order.CreateOrderRequest;
import com.campus.trade.dto.order.OrderActionRequest;
import com.campus.trade.dto.order.OrderResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.CartItem;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.PaymentStatus;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.repository.CartItemRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.util.OrderMapper;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    @Transactional
    public OrderResponse createOrder(String username, CreateOrderRequest request) {
        User buyer = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "商品不可下单");
        }
        if (product.getSeller().getId().equals(buyer.getId())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "不能购买自己的商品");
        }
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(product.getSeller());
        order.setPrice(product.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.NOT_INITIATED);
        order.setRefundStatus(RefundStatus.NONE);
        order.setBuyerNote(request.getNote());
        order.setShippingAddress(request.getShippingAddress());
        orderRepository.save(order);
        product.setStatus(ProductStatus.OFF_SALE);
        productRepository.save(product);
        cartItemRepository.deleteByUserIdAndProductId(buyer.getId(), product.getId());
        emailNotificationService.notifySellerNewOrder(order);
        notifyOrderEvent(order.getSeller(), "有新的订单待确认",
            String.format("买家 %s 已对商品《%s》提交订单，订单号 %s。",
                resolveDisplayName(buyer), resolveProductTitle(product), order.getOrderNo()));
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public BatchOrderCreateResponse checkoutFromCart(String username, CartCheckoutRequest request) {
        User buyer = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        BatchOrderCreateResponse response = new BatchOrderCreateResponse();
        List<OrderResponse> createdOrders = new ArrayList<>();
        List<BatchOrderCreateResponse.FailedItem> failed = new ArrayList<>();

        for (Long cartItemId : request.getCartItemIds()) {
            try {
                CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, buyer.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "购物车项不存在"));

                Integer quantity = cartItem.getQuantity();
                if (quantity != null && quantity > 1) {
                    throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "当前商品仅支持单件下单");
                }

                Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

                if (product.getStatus() != ProductStatus.ON_SALE) {
                    throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "商品不可下单");
                }
                if (product.getSeller().getId().equals(buyer.getId())) {
                    throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "不能购买自己的商品");
                }

                Order order = new Order();
                order.setOrderNo(generateOrderNo());
                order.setProduct(product);
                order.setBuyer(buyer);
                order.setSeller(product.getSeller());
                order.setPrice(product.getPrice());
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                order.setPaymentStatus(PaymentStatus.NOT_INITIATED);
                order.setRefundStatus(RefundStatus.NONE);
                order.setBuyerNote(request.getNote());
                order.setShippingAddress(request.getShippingAddress());
                orderRepository.save(order);

                product.setStatus(ProductStatus.OFF_SALE);
                productRepository.save(product);
                cartItemRepository.deleteById(cartItem.getId());

                emailNotificationService.notifySellerNewOrder(order);
                notifyOrderEvent(order.getSeller(), "有新的订单待确认",
                    String.format("买家 %s 已对商品《%s》提交订单，订单号 %s。",
                        resolveDisplayName(buyer), resolveProductTitle(product), order.getOrderNo()));

                createdOrders.add(OrderMapper.toResponse(order));
            } catch (BusinessException ex) {
                failed.add(new BatchOrderCreateResponse.FailedItem(cartItemId, ex.getMessage()));
            } catch (Exception ex) {
                failed.add(new BatchOrderCreateResponse.FailedItem(cartItemId, "下单失败"));
            }
        }

        response.setOrders(createdOrders);
        response.setFailed(failed);
        return response;
    }

    public PaginatedResponse<OrderResponse> listOrders(String username, boolean sold, OrderStatus status, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> pageResult;
        if (sold) {
            pageResult = status == null ? orderRepository.findBySellerId(user.getId(), pageable)
                    : orderRepository.findBySellerIdAndStatus(user.getId(), status, pageable);
        } else {
            pageResult = status == null ? orderRepository.findByBuyerId(user.getId(), pageable)
                    : orderRepository.findByBuyerIdAndStatus(user.getId(), status, pageable);
        }
        Page<OrderResponse> responsePage = pageResult.map(OrderMapper::toResponse);
        return PaginatedResponse.of(responsePage.getContent(), page, size, responsePage.getTotalElements());
    }

    public OrderResponse getOrderDetail(String username, Long orderId) {
        Order order = getOrderForUser(username, orderId);
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public void confirmOrder(String username, Long orderId) {
        Order order = getOrderForUser(username, orderId);
        if (!order.getSeller().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权确认订单");
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PENDING_SHIPMENT);
            if (order.getPaymentTime() == null) {
                order.setPaymentTime(LocalDateTime.now());
            }
            emailNotificationService.notifyBuyerOrderAccepted(order);
            notifyOrderEvent(order.getBuyer(), "卖家已确认订单",
                String.format("卖家 %s 已确认订单 %s，请协商线下交付。",
                    resolveDisplayName(order.getSeller()), order.getOrderNo()));
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING_SHIPMENT) {
            order.setStatus(OrderStatus.PENDING_RECEIPT);
            order.setDeliveryTime(LocalDateTime.now());
            notifyOrderEvent(order.getBuyer(), "卖家已发货",
                String.format("卖家 %s 标记订单 %s 已发货，请留意取货信息。",
                    resolveDisplayName(order.getSeller()), order.getOrderNo()));
            return;
        }
        throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不可确认");
    }

    @Transactional
    public void rejectOrder(String username, Long orderId, OrderActionRequest request) {
        Order order = getOrderForUser(username, orderId);
        if (!order.getSeller().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权拒绝订单");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不可拒绝");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setSellerNote(request.getReason());
        order.getProduct().setStatus(ProductStatus.ON_SALE);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        emailNotificationService.notifyBuyerOrderRejected(order, request.getReason());
        notifyOrderEvent(order.getBuyer(), "订单已被拒绝",
            String.format("卖家 %s 拒绝了订单 %s。原因：%s",
                resolveDisplayName(order.getSeller()), order.getOrderNo(),
                StringUtils.hasText(request.getReason()) ? request.getReason() : "未提供"));
    }

    @Transactional
    public void cancelOrder(String username, Long orderId, OrderActionRequest request) {
        Order order = getOrderForUser(username, orderId);
        if (!order.getBuyer().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权取消订单");
        }
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单已完结");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不可取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setBuyerNote(request.getReason());
        order.getProduct().setStatus(ProductStatus.ON_SALE);
        if (order.getPaymentStatus() != PaymentStatus.SUCCEEDED) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }
        emailNotificationService.notifySellerOrderCancelledByBuyer(order, request.getReason());
        notifyOrderEvent(order.getSeller(), "买家已取消订单",
            String.format("买家 %s 取消了订单 %s。备注：%s",
                resolveDisplayName(order.getBuyer()), order.getOrderNo(),
                StringUtils.hasText(request.getReason()) ? request.getReason() : "无"));
    }

    @Transactional
    public void completeOrder(String username, Long orderId, OrderActionRequest request) {
        Order order = getOrderForUser(username, orderId);
        if (!order.getBuyer().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权确认收货");
        }
        if (order.getStatus() != OrderStatus.PENDING_SHIPMENT && order.getStatus() != OrderStatus.PENDING_RECEIPT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不可完成");
        }
        if (request.getRating() == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "请提供评分");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setReceiveTime(LocalDateTime.now());
        order.setBuyerRating(request.getRating());
        order.setBuyerComment(request.getComment());
        order.getProduct().setStatus(ProductStatus.SOLD);
        emailNotificationService.notifyOrderCompleted(order);
        notifyOrderEvent(order.getSeller(), "订单已完成",
            String.format("买家 %s 已确认收货，订单 %s 已完成。",
                resolveDisplayName(order.getBuyer()), order.getOrderNo()));
    }

    @Transactional
    public void sellerReviewBuyer(String username, Long orderId, OrderActionRequest request) {
        Order order = getOrderForUser(username, orderId);
        if (!order.getSeller().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权评价买家");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单未完成，无法评价");
        }
        if (order.getSellerRating() != null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "已评价，无需重复提交");
        }
        if (request.getRating() == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "请提供评分");
        }
        order.setSellerRating(request.getRating());
        order.setSellerComment(request.getComment());

        notifyOrderEvent(order.getBuyer(), "卖家已评价",
            String.format("卖家 %s 已对订单 %s 进行评价。",
                resolveDisplayName(order.getSeller()), order.getOrderNo()));
    }

    /**
     * 定时处理超时未支付的订单
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    @Transactional
    public void processTimeoutOrders() {
        LocalDateTime now = LocalDateTime.now();
        // 查找超过15分钟仍处于待支付状态的订单
        List<Order> timeoutOrders = orderRepository.findAllByStatusAndCreateTimeBefore(
            OrderStatus.PENDING_PAYMENT, now.minusMinutes(15));
        
        for (Order order : timeoutOrders) {
            try {
                // 取消订单
                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.EXPIRED);
                order.setSellerNote("订单超时未支付，自动取消");
                // 恢复商品状态
                Product product = order.getProduct();
                if (product != null) {
                    product.setStatus(ProductStatus.ON_SALE);
                    productRepository.save(product);
                }
                orderRepository.save(order);
                
                // 发送通知
                notifyOrderEvent(order.getBuyer(), "订单已取消",
                    String.format("订单 %s 因超时未支付已自动取消。", order.getOrderNo()));
                notifyOrderEvent(order.getSeller(), "订单已取消",
                    String.format("订单 %s 因超时未支付已自动取消。", order.getOrderNo()));
            } catch (Exception e) {
                log.error("处理超时订单失败，订单号：{}", order.getOrderNo(), e);
            }
        }
    }

    public PaginatedResponse<OrderResponse> adminListOrders(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Order> orders = (status == null) ? orderRepository.findAll(pageable) : orderRepository.findByStatus(status, pageable);
        List<OrderResponse> responses = orders.getContent().stream().map(OrderMapper::toResponse).toList();
        return PaginatedResponse.of(responses, page, size, orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderDetailForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public BatchOperationResult adminBatchUpdateStatus(List<Long> orderIds, OrderStatus status) {
        if (orderIds == null || orderIds.isEmpty()) {
            return BatchOperationResult.builder().successCount(0).failedCount(0).totalCount(0).message("无订单可处理").build();
        }

        long success = 0;
        long failed = 0;
        for (Long id : orderIds) {
            try {
                Order order = orderRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
                
                // 更新订单状态
                order.setStatus(status);
                
                // 根据状态自动更新相关字段
                switch (status) {
                    case COMPLETED -> {
                        if (order.getReceiveTime() == null) {
                            order.setReceiveTime(LocalDateTime.now());
                        }
                    }
                    case CANCELLED -> {
                        if (order.getPaymentStatus() == PaymentStatus.SUCCEEDED) {
                            order.setPaymentStatus(PaymentStatus.REFUNDED);
                        }
                    }
                    case PENDING_SHIPMENT -> {
                        if (order.getPaymentStatus() == PaymentStatus.NOT_INITIATED) {
                            order.setPaymentStatus(PaymentStatus.SUCCEEDED);
                        }
                    }
                    case PENDING_RECEIPT -> {
                        if (order.getDeliveryTime() == null) {
                            order.setDeliveryTime(LocalDateTime.now());
                        }
                    }
                    default -> {
                        // 其他状态不处理
                    }
                }
                
                orderRepository.save(order);
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }

        return BatchOperationResult.builder()
                .successCount(success)
                .failedCount(failed)
                .totalCount(orderIds.size())
                .message(failed > 0 ? "部分订单处理失败" : "批量处理成功")
                .build();
    }
    private Order getOrderForUser(String username, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getBuyer().getUsername().equals(username) && !order.getSeller().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权访问订单");
        }
        return order;
    }

    private String generateOrderNo() {
        return "O" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void notifyOrderEvent(User recipient, String title, String message) {
        if (recipient == null) {
            return;
        }
        String content = message;
        notificationService.notifyUser(recipient.getId(), title, content);
    }

    private String resolveDisplayName(User user) {
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

    private String resolveProductTitle(Product product) {
        return product != null && StringUtils.hasText(product.getTitle()) ? product.getTitle() : "商品";
    }

    @Transactional
    public void deleteOrder(String username, Long orderId) {
        Order order = getOrderForUser(username, orderId);
        orderRepository.delete(order);
    }
}
