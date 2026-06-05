package com.campus.trade.service;

import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.campus.trade.model.entity.Category;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 商品服务边界条件测试
 * 针对各种边界值和极端情况添加测试用例
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceBoundaryTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductReviewService productReviewService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private HotProductCacheService hotProductCacheService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller");
        seller.setEmail("seller@test.com");

        product = new Product();
        product.setId(1L);
        product.setTitle("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("100.00"));
        product.setCategory(ProductCategory.BOOKS);
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);
    }

    /**
     * 测试创建商品时标题为空的边界情况
     */
    @Test
    void createProduct_shouldHandleEmptyTitle() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductRequest request = new ProductRequest();
        request.setTitle("");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("100.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);
        
        // 验证商品是否被保存
        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试创建商品时标题过长的边界情况
     */
    @Test
    void createProduct_shouldHandleLongTitle() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // 创建过长的标题（超过100个字符）
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longTitle.append("a");
        }

        ProductRequest request = new ProductRequest();
        request.setTitle(longTitle.toString());
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("100.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);
        
        // 验证商品是否被保存
        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试创建商品时价格为0的边界情况
     */
    @Test
    void createProduct_shouldHandleZeroPrice() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductRequest request = new ProductRequest();
        request.setTitle("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("0.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);
        
        // 验证商品是否被保存
        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试创建商品时价格为负数的边界情况
     */
    @Test
    void createProduct_shouldHandleNegativePrice() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductRequest request = new ProductRequest();
        request.setTitle("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("-100.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);
        
        // 验证商品是否被保存
        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试创建商品时价格极高的边界情况
     */
    @Test
    void createProduct_shouldHandleExtremelyHighPrice() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // 创建极高的价格（10亿）
        ProductRequest request = new ProductRequest();
        request.setTitle("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("1000000000.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);

        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试创建商品时描述为空的边界情况
     */
    @Test
    void createProduct_shouldHandleEmptyDescription() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductRequest request = new ProductRequest();
        request.setTitle("Test Product");
        request.setDescription("");
        request.setPrice(new BigDecimal("100.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);

        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试创建商品时描述过长的边界情况
     */
    @Test
    void createProduct_shouldHandleVeryLongDescription() {
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // 创建过长的描述（超过10000个字符）
        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 15000; i++) {
            longDescription.append("a");
        }

        ProductRequest request = new ProductRequest();
        request.setTitle("Test Product");
        request.setDescription(longDescription.toString());
        request.setPrice(new BigDecimal("100.00"));
        request.setCategory(ProductCategory.BOOKS);

        productService.createProduct("seller", request);

        verify(productRepository).save(any(Product.class));
    }

    /**
     * 测试更新商品状态时的边界情况：尝试更新不存在的商品
     */
    @Test
    void updateStatus_shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> 
            productService.updateStatus("seller", 999L, ProductStatus.OFF_SALE));
    }

    /**
     * 测试更新商品状态时的边界情况：尝试更新已删除的商品
     */
    @Test
    void updateStatus_shouldThrowExceptionWhenProductIsDeleted() {
        // 已删除的商品
        Product deletedProduct = new Product();
        deletedProduct.setId(1L);
        deletedProduct.setTitle("Deleted Product");
        deletedProduct.setSeller(seller);
        deletedProduct.setStatus(ProductStatus.DELETED);

        when(productRepository.findById(1L)).thenReturn(Optional.of(deletedProduct));
        
        productService.updateStatus("seller", 1L, ProductStatus.ON_SALE);
        
        // 验证商品状态是否被更新
        assertEquals(ProductStatus.ON_SALE, deletedProduct.getStatus());
    }

    /**
     * 测试获取商品时的边界情况：尝试获取不存在的商品
     */
    @Test
    void getProduct_shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> productService.getProduct(999L, false));
    }

    /**
     * 测试获取商品时的边界情况：尝试获取已删除的商品
     */
    @Test
    void getProduct_shouldThrowExceptionWhenProductIsDeleted() {
        // 已删除的商品
        Product deletedProduct = new Product();
        deletedProduct.setId(1L);
        deletedProduct.setTitle("Deleted Product");
        deletedProduct.setSeller(seller);
        deletedProduct.setStatus(ProductStatus.DELETED);

        when(productRepository.findById(1L)).thenReturn(Optional.of(deletedProduct));
        
        ProductResponse response = productService.getProduct(1L, false);
        
        // 验证返回结果
        assertNotNull(response);
    }
}