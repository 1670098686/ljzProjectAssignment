package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.cart.AddCartItemRequest;
import com.campus.trade.dto.cart.CartItemResponse;
import com.campus.trade.dto.cart.CartSummaryResponse;
import com.campus.trade.dto.cart.UpdateCartItemRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.model.entity.CartItem;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.CartItemRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private User buyer;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);
        buyer.setUsername("buyer");
        buyer.setEmail("buyer@test.com");

        seller = new User();
        seller.setId(2L);
        seller.setUsername("seller");
        seller.setEmail("seller@test.com");

        product = new Product();
        product.setId(5L);
        product.setTitle("Desk Lamp");
        product.setPrice(new BigDecimal("36.00"));
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);
    }

    @Test
    void addItem_accumulatesQuantityWhenItemExists() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        CartItem existing = new CartItem();
        existing.setId(100L);
        existing.setUser(buyer);
        existing.setProduct(product);
        existing.setQuantity(1);
        existing.setCreateTime(LocalDateTime.now());
        existing.setUpdateTime(LocalDateTime.now());
        when(cartItemRepository.findByUserIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(2);

        CartItemResponse response = cartService.addItem("buyer", request);

        assertEquals(3, response.getQuantity());
        assertNotNull(response.getProduct());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addItem_createsNewItemWhenMissing() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductId(1L, 5L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(222L);
            return saved;
        });

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(1);

        CartItemResponse response = cartService.addItem("buyer", request);

        assertEquals(1, response.getQuantity());
        assertEquals(222L, response.getId());
    }

    @Test
    void addItem_rejectsSellerSelfCart() {
        product.setSeller(buyer);
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);

        assertThrows(BusinessException.class, () -> cartService.addItem("buyer", request));
    }

    @Test
    void listItems_returnsPaginatedResponses() {
        CartItem cartItem = new CartItem();
        cartItem.setId(101L);
        cartItem.setUser(buyer);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setCreateTime(LocalDateTime.now());
        cartItem.setUpdateTime(LocalDateTime.now());
        Page<CartItem> page = new PageImpl<>(List.of(cartItem));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PaginatedResponse<CartItemResponse> response = cartService.listItems("buyer", 1, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getMeta().getPage());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cartItemRepository).findByUserId(eq(1L), pageableCaptor.capture());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void updateQuantity_persistsChange() {
        CartItem cartItem = new CartItem();
        cartItem.setId(101L);
        cartItem.setUser(buyer);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByIdAndUserId(101L, 1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(cartItem)).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(3);

        CartItemResponse response = cartService.updateQuantity("buyer", 101L, request);

        assertEquals(3, response.getQuantity());
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    void removeItem_deletesRecord() {
        CartItem cartItem = new CartItem();
        cartItem.setId(101L);
        cartItem.setUser(buyer);
        cartItem.setProduct(product);
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByIdAndUserId(101L, 1L)).thenReturn(Optional.of(cartItem));

        cartService.removeItem("buyer", 101L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void removeItemByProduct_deletesMatchingEntry() {
        CartItem cartItem = new CartItem();
        cartItem.setId(500L);
        cartItem.setUser(buyer);
        cartItem.setProduct(product);
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUserIdAndProductId(1L, 5L)).thenReturn(Optional.of(cartItem));

        cartService.removeItemByProduct("buyer", 5L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void clearCart_invokesBulkDelete() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));

        cartService.clearCart("buyer");

        verify(cartItemRepository).deleteAllByUserId(1L);
    }

    @Test
    void countItems_returnsRepositoryValue() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(cartItemRepository.countByUserId(1L)).thenReturn(4L);

        long count = cartService.countItems("buyer");

        assertEquals(4L, count);
    }

    @Test
    void getSummary_calculatesTotals() {
        CartItem first = new CartItem();
        first.setUser(buyer);
        first.setProduct(product);
        first.setQuantity(2);
        CartItem second = new CartItem();
        Product anotherProduct = new Product();
        anotherProduct.setPrice(new BigDecimal("5.00"));
        second.setProduct(anotherProduct);
        second.setQuantity(1);
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findAllByUserId(1L)).thenReturn(List.of(first, second));

        CartSummaryResponse summary = cartService.getSummary("buyer");

        assertEquals(3, summary.getTotalQuantity());
        assertEquals(2, summary.getUniqueProducts());
        assertEquals(new BigDecimal("77.00"), summary.getTotalAmount());
    }
}
