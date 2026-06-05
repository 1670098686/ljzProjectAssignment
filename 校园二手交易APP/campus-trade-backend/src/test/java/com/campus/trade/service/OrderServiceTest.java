package com.campus.trade.service;

import com.campus.trade.dto.order.CreateOrderRequest;
import com.campus.trade.dto.order.OrderActionRequest;
import com.campus.trade.dto.order.OrderResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.CartItemRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private User buyer;
    private User seller;
    private Product product;

    @BeforeEach
    void initFixtures() {
        buyer = buildUser(1L, "buyer1");
        seller = buildUser(2L, "seller1");
        product = new Product();
        product.setId(5L);
        product.setPrice(new BigDecimal("88.00"));
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);
    }

    @Test
    void createOrder_persistsBuyerAndShippingAddress() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(5L);
        request.setShippingAddress("Dorm 2");
        request.setNote("keep safe");

        OrderResponse response = orderService.createOrder("buyer1", request);

        assertNotNull(response);
        assertEquals("Dorm 2", response.getShippingAddress());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.getStatus());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order persisted = captor.getValue();
        assertEquals(buyer, persisted.getBuyer());
        assertEquals(product, persisted.getProduct());
        assertEquals(seller, persisted.getSeller());
        assertEquals("Dorm 2", persisted.getShippingAddress());
        assertEquals(OrderStatus.PENDING_PAYMENT, persisted.getStatus());
        assertEquals(ProductStatus.OFF_SALE, product.getStatus(), "Product should be locked after order creation");
        verify(cartItemRepository).deleteByUserIdAndProductId(buyer.getId(), product.getId());
    }

    @Test
    void confirmOrder_updatesStatusAndPaymentTime() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.confirmOrder("seller1", 10L);

        assertEquals(OrderStatus.PENDING_SHIPMENT, order.getStatus());
        assertNotNull(order.getPaymentTime());
    }

    @Test
    void confirmOrder_rejectsNonSeller() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.confirmOrder("buyer1", 11L));
    }

    @Test
    void cancelOrder_allowsBuyerAndRestoresProduct() {
        Order order = buildOrder(OrderStatus.PENDING_SHIPMENT);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));

        OrderActionRequest request = new OrderActionRequest();
        request.setReason("Changed mind");

        orderService.cancelOrder("buyer1", 12L, request);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Changed mind", order.getBuyerNote());
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
    }

    @Test
    void cancelOrder_blocksSellerAttempt() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(13L)).thenReturn(Optional.of(order));

        OrderActionRequest request = new OrderActionRequest();

        assertThrows(BusinessException.class, () -> orderService.cancelOrder("seller1", 13L, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Order buildOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(99L);
        order.setOrderNo("O123");
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setStatus(status);
        order.setPrice(new BigDecimal("88.00"));
        order.setPaymentTime(null);
        order.setShippingAddress("Dorm 1");
        return order;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("pwd");
        user.setSchool("Campus");
        return user;
    }
}
