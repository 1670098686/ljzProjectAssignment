package com.campus.trade.service;

import com.campus.trade.dto.cart.AddCartItemRequest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 购物车服务边界条件测试
 * 针对各种边界值和极端情况添加测试用例
 */
@ExtendWith(MockitoExtension.class)
class CartServiceBoundaryTest {

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
    private Product soldProduct;
    private Product outOfStockProduct;

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

        // 正常商品
        product = new Product();
        product.setId(5L);
        product.setTitle("Desk Lamp");
        product.setPrice(new BigDecimal("36.00"));
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);

        // 已售商品
        soldProduct = new Product();
        soldProduct.setId(6L);
        soldProduct.setTitle("Sold Product");
        soldProduct.setPrice(new BigDecimal("100.00"));
        soldProduct.setSeller(seller);
        soldProduct.setStatus(ProductStatus.SOLD);
    }

    /**
     * 测试添加商品时数量为0的边界情况
     */
    @Test
    void addItem_shouldThrowExceptionWhenQuantityIsZero() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(0);

        assertThrows(BusinessException.class, () -> cartService.addItem("buyer", request));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    /**
     * 测试添加商品时数量为负数的边界情况
     */
    @Test
    void addItem_shouldThrowExceptionWhenQuantityIsNegative() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(-1);

        assertThrows(BusinessException.class, () -> cartService.addItem("buyer", request));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    /**
     * 测试添加已售商品的边界情况
     */
    @Test
    void addItem_shouldThrowExceptionWhenProductIsSoldOut() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(6L)).thenReturn(Optional.of(soldProduct));

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(6L);
        request.setQuantity(1);

        assertThrows(BusinessException.class, () -> cartService.addItem("buyer", request));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    /**
     * 测试添加大量商品到购物车的边界情况
     */
    @Test
    void addItem_shouldHandleLargeQuantity() {
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
        request.setQuantity(1000); // 大量商品

        cartService.addItem("buyer", request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(1000, captor.getValue().getQuantity());
    }

    /**
     * 测试添加商品到购物车时数量累加超过边界值的情况
     */
    @Test
    void addItem_shouldAccumulateLargeQuantities() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        
        // 已存在商品，数量为999
        CartItem existing = new CartItem();
        existing.setId(100L);
        existing.setUser(buyer);
        existing.setProduct(product);
        existing.setQuantity(999);
        existing.setCreateTime(LocalDateTime.now());
        existing.setUpdateTime(LocalDateTime.now());
        
        when(cartItemRepository.findByUserIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(100); // 再次添加100个

        cartService.addItem("buyer", request);

        assertEquals(1099, existing.getQuantity());
        verify(cartItemRepository).save(existing);
    }

    /**
     * 测试商品不存在时的边界情况
     */
    @Test
    void addItem_shouldThrowExceptionWhenProductNotFound() {
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(999L)).thenReturn(Optional.empty()); // 不存在的商品ID

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(999L);
        request.setQuantity(1);

        assertThrows(BusinessException.class, () -> cartService.addItem("buyer", request));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }
}
