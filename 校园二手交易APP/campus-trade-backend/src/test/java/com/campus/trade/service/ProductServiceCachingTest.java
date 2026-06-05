package com.campus.trade.service;

import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceCachingTest {

    @InjectMocks
    private ProductService productService;

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

    @Test
    void getProduct_shouldReturnSameResponse_whenIncreaseViewFalse() {
        Product product = buildProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse first = productService.getProduct(1L, false);
        ProductResponse second = productService.getProduct(1L, false);

        assertThat(first.getId()).isEqualTo(second.getId());
        verify(productRepository, times(2)).findById(1L);
    }

    @Test
    void listProducts_shouldCallRepositoryMultipleTimes() {
        Product product = buildProduct();
        when(productRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        productService.listProducts(null, null, null, 1, 10, sort);
        productService.listProducts(null, null, null, 1, 10, sort);

        verify(productRepository, times(2)).findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class));
    }

    private Product buildProduct() {
        User seller = new User();
        seller.setId(99L);
        seller.setUsername("seller_user");
        seller.setEmail("seller@example.com");
        seller.setPassword("encoded");
        seller.setRole(UserRole.STUDENT);

        Product product = new Product();
        product.setId(1L);
        product.setTitle("Book");
        product.setDescription("Nice");
        product.setPrice(BigDecimal.TEN);
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);
        product.setAuditStatus(AuditStatus.APPROVED);
        return product;
    }
}
