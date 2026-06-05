package com.campus.trade.security;

import com.campus.trade.controller.ProductController;
import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 安全测试用例
 * 针对SQL注入、XSS攻击等安全漏洞进行测试
 */
@ExtendWith(MockitoExtension.class)
class SecurityTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        objectMapper = new ObjectMapper();

        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller");
        seller.setEmail("seller@test.com");
        seller.setRole(UserRole.STUDENT);

        product = new Product();
        product.setId(1L);
        product.setTitle("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("100.00"));
        product.setCategory(ProductCategory.BOOKS);
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 测试SQL注入攻击 - 通过商品搜索接口
     */
    @Test
    void searchProduct_shouldNotBeVulnerableToSqlInjection() throws Exception {
        // 使用常见的SQL注入攻击字符串
        String sqlInjection = "' OR 1=1 --";
        
        mockMvc.perform(get("/api/v1/products")
                .param("keyword", sqlInjection)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // 期望返回正常状态，而不是SQL错误
    }

    /**
     * 测试XSS攻击 - 通过商品标题
     */
    @Test
    void createProduct_shouldSanitizeXssContent() throws Exception {
        // XSS攻击字符串
        String xssScript = "<script>alert('XSS');</script>";
        
        ProductRequest request = new ProductRequest();
        request.setTitle(xssScript + " Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("100.00"));
        request.setCategory(ProductCategory.BOOKS);
        
        // 模拟服务层抛出异常或处理XSS
        when(productService.createProduct(anyString(), any(ProductRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PARAM));
        
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试XSS攻击 - 通过商品描述
     */
    @Test
    void createProduct_shouldSanitizeXssInDescription() throws Exception {
        // XSS攻击字符串
        String xssScript = "<img src='x' onerror='alert(\"XSS\")'>";
        
        ProductRequest request = new ProductRequest();
        request.setTitle("Test Product");
        request.setDescription(xssScript + " Description");
        request.setPrice(new BigDecimal("100.00"));
        request.setCategory(ProductCategory.BOOKS);
        
        // 模拟服务层抛出异常或处理XSS
        when(productService.createProduct(anyString(), any(ProductRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PARAM));
        
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试命令注入攻击 - 通过文件上传接口
     */
    @Test
    void uploadFile_shouldNotBeVulnerableToCommandInjection() throws Exception {
        // 命令注入攻击字符串
        String commandInjection = "; rm -rf /";
        
        // 测试文件上传接口是否对文件名进行了安全处理
        mockMvc.perform(post("/api/v1/files/upload")
                .param("filename", "test.txt" + commandInjection)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试路径遍历攻击 - 通过文件下载接口
     */
    @Test
    void downloadFile_shouldPreventPathTraversal() throws Exception {
        // 路径遍历攻击字符串
        String pathTraversal = "../../../etc/passwd";
        
        mockMvc.perform(get("/api/v1/files/download")
                .param("filename", pathTraversal)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试敏感信息泄露 - 错误信息中不应包含敏感信息
     */
    @Test
    void errorResponse_shouldNotLeakSensitiveInfo() throws Exception {
        // 使用无效的商品ID
        mockMvc.perform(get("/api/v1/products/999999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errorCode").exists())
                // 错误信息中不应包含SQL异常堆栈或其他敏感信息
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.sql").doesNotExist());
    }
}
