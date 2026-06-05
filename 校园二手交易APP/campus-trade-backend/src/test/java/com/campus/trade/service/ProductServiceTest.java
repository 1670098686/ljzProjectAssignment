package com.campus.trade.service;

import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.entity.Category;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.UserRole;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

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
        seller.setId(10L);
        seller.setUsername("seller");
        seller.setEmail("seller@example.com");
        seller.setRole(UserRole.STUDENT);

        product = new Product();
        product.setId(100L);
        product.setSeller(seller);
        product.setTitle("Java Handbook");
        product.setDescription("Almost new");
        product.setPrice(new BigDecimal("35.50"));
        product.setCategory(ProductCategory.BOOKS);
        product.setStatus(ProductStatus.ON_SALE);
        product.setAuditStatus(AuditStatus.PENDING);
        product.setViewCount(0);
        product.setLikeCount(0);
    }

    @Test
    void createProduct_shouldSetDefaultsAndPersist() {
        ProductRequest request = new ProductRequest();
        request.setTitle("Java Handbook");
        request.setDescription("Almost new");
        request.setPrice(new BigDecimal("35.50"));
        request.setCategory(ProductCategory.BOOKS);
        request.setTags(java.util.List.of("教材", "Java"));

        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        ProductResponse response = productService.createProduct("seller", request);

        assertEquals("Java Handbook", response.getTitle());
        assertEquals(ProductStatus.ON_SALE, response.getStatus());
        assertEquals(AuditStatus.PENDING, response.getAuditStatus());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product persisted = productCaptor.getValue();
        assertEquals(seller, persisted.getSeller());
        assertEquals(java.util.List.of("教材", "Java"), persisted.getTags());
    }

    @Test
    void createProduct_shouldUseCategoryIdWhenProvided() {
        ProductRequest request = new ProductRequest();
        request.setTitle("Desk");
        request.setPrice(new BigDecimal("120.00"));
        request.setCategoryId(1L);

        Category category = new Category();
        category.setId(1L);
        category.setCode("BOOKS");
        category.setName("图书");
        category.setEnabled(true);

        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(categoryService.getEnabledEntity(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct("seller", request);

        assertEquals(ProductCategory.BOOKS, response.getCategory());
    }

    @Test
    void createProduct_shouldFailWhenSellerMissing() {
        ProductRequest request = new ProductRequest();
        request.setTitle("Desk");
        request.setPrice(new BigDecimal("120.00"));
        request.setCategory(ProductCategory.BOOKS);

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> productService.createProduct("unknown", request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProduct_shouldIncreaseViewCountWhenRequested() {
        product.setViewCount(3);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.getProduct(100L, true);

        assertEquals(4, product.getViewCount());
    }

    @Test
    void updateStatus_shouldRejectWhenUserNotOwner() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> productService.updateStatus("intruder", 100L, ProductStatus.SOLD));
    }
}
