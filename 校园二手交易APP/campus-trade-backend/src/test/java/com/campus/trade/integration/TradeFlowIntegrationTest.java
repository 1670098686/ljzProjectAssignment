package com.campus.trade.integration;

import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.config.TestCacheConfig;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.SystemNotificationRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.VerificationTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestCacheConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SystemNotificationRepository systemNotificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        systemNotificationRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authEndpoints_shouldRegisterAndLoginUser() throws Exception {
        Map<String, Object> registerPayload = new HashMap<>();
        registerPayload.put("username", "flow_user");
        registerPayload.put("password", "Password123");
        registerPayload.put("email", "flow_user@example.com");
        registerPayload.put("role", "STUDENT");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("flow_user"));

        assertTrue(userRepository.existsByUsername("flow_user"));

        User registered = userRepository.findByUsername("flow_user").orElseThrow();
        registered.setEmailVerified(true);
        userRepository.save(registered);

        Map<String, String> loginPayload = new HashMap<>();
        loginPayload.put("email", "flow_user@example.com");
        loginPayload.put("password", "Password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("flow_user"));
    }

    @Test
    @WithMockUser(username = "seller_flow", roles = "STUDENT")
    void productEndpoints_shouldCreateAndListItems() throws Exception {
        createUser("seller_flow");

        ProductRequest request = new ProductRequest();
        request.setTitle("Integration Test Product");
        request.setDescription("Sample description");
        request.setPrice(new BigDecimal("88.50"));
        request.setCategory(ProductCategory.BOOKS);

        mockMvc.perform(post("/api/v1/products")
                        .header("Idempotency-Key", "test-product-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Integration Test Product"));

        assertEquals(1L, productRepository.count());

        Product created = productRepository.findAll().get(0);
        created.setAuditStatus(AuditStatus.APPROVED);
        productRepository.save(created);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Integration Test Product"));
    }

    @Test
    @WithMockUser(username = "buyer_flow", roles = "STUDENT")
    void orderEndpoint_shouldCreateOrderAndLockProduct() throws Exception {
        User seller = createUser("seller_flow");
        createUser("buyer_flow");
        Product product = createProduct(seller);

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", product.getId());
        payload.put("shippingAddress", "Dorm 8-101");
        payload.put("note", "Handle carefully");

        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", "test-order-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        Product locked = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.OFF_SALE, locked.getStatus());
        assertEquals(1L, orderRepository.count());
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setEmail(username + "@example.com");
        user.setRole(UserRole.STUDENT);
        user.setStatus(AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Product createProduct(User seller) {
        Product product = new Product();
        product.setTitle("Textbook");
        product.setDescription("Factory sealed");
        product.setPrice(new BigDecimal("66.00"));
        product.setCategory(ProductCategory.BOOKS);
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);
        product.setAuditStatus(AuditStatus.APPROVED);
        product.setViewCount(0);
        product.setLikeCount(0);
        return productRepository.save(product);
    }
}
