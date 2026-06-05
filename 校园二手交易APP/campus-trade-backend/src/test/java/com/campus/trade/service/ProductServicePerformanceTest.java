package com.campus.trade.service;

import com.campus.trade.dto.product.ProductRequest;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.campus.trade.model.entity.Category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 商品服务性能测试
 * 验证系统在高并发下的表现
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServicePerformanceTest {

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
        
        // 设置ProductReviewService的默认mock行为（attachRatingSummary方法返回类型是void）
        doNothing().when(productReviewService).attachRatingSummary(any(com.campus.trade.dto.product.ProductResponse.class));
        doNothing().when(productReviewService).attachRatingSummary(any(Collection.class));
        
        // 设置CategoryService的默认mock行为
        Category mockCategory = new Category();
        mockCategory.setId(1L);
        mockCategory.setCode("BOOKS");
        when(categoryService.getEnabledEntity(anyLong())).thenReturn(mockCategory);
        
        // 设置UserRepository的默认mock行为
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        
        // 设置ProductRepository的默认mock行为
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(System.currentTimeMillis());
            return saved;
        });
    }

    /**
     * 测试高并发创建商品的性能
     * 
     * @throws InterruptedException 如果线程中断
     */
    @Test
    void createProduct_shouldHandleHighConcurrency() throws InterruptedException {
        // 并发线程数
        int threadCount = 50;
        // 每个线程创建的商品数量
        int productsPerThread = 10;
        // 总商品数量
        int totalProducts = threadCount * productsPerThread;
        
        // 创建计数器
        CountDownLatch latch = new CountDownLatch(threadCount);
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 提交任务
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < productsPerThread; j++) {
                        ProductRequest request = new ProductRequest();
                        request.setTitle("Test Product " + j);
                        request.setDescription("Test Description " + j);
                        request.setPrice(new BigDecimal("100.00"));
                        request.setCategory(ProductCategory.BOOKS);
                        
                        productService.createProduct("seller", request);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成
        latch.await();
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        
        // 关闭线程池
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        
        // 验证总商品数量
        verify(productRepository, times(totalProducts)).save(any(Product.class));
        
        // 输出性能指标
        long duration = endTime - startTime;
        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("总商品数量: " + totalProducts);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均响应时间: " + (duration / (double) totalProducts) + " ms");
        System.out.println("吞吐量: " + (totalProducts / (duration / 1000.0)) + " 件/秒");
        System.out.println("==================\n");
        
        // 验证响应时间是否在可接受范围内（例如，平均响应时间小于50ms）
        assertTrue((duration / (double) totalProducts) < 50, "平均响应时间超过50ms");
    }

    /**
     * 测试高并发获取商品的性能
     * 
     * @throws InterruptedException 如果线程中断
     */
    @Test
    void getProduct_shouldHandleHighConcurrency() throws InterruptedException {
        // 并发线程数
        int threadCount = 100;
        // 每个线程获取商品的次数
        int requestsPerThread = 50;
        // 总请求数量
        int totalRequests = threadCount * requestsPerThread;
        
        // 准备测试数据
        List<Product> products = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            Product p = new Product();
            p.setId(i);
            p.setTitle("Product " + i);
            p.setDescription("Description " + i);
            p.setPrice(new BigDecimal("100.00"));
            p.setCategory(ProductCategory.BOOKS);
            p.setSeller(seller);
            p.setStatus(ProductStatus.ON_SALE);
            products.add(p);
        }
        
        // 设置ProductRepository的mock行为
        when(productRepository.findById(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            return products.stream()
                    .filter(p -> p.getId() == id)
                    .findFirst();
        });
        
        // 创建计数器
        CountDownLatch latch = new CountDownLatch(threadCount);
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 提交任务
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        // 随机获取一个商品
                        long productId = (long) (Math.random() * 100) + 1;
                        productService.getProduct(productId, false);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成
        latch.await();
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        
        // 关闭线程池
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        
        // 输出性能指标
        long duration = endTime - startTime;
        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("总请求数量: " + totalRequests);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均响应时间: " + (duration / (double) totalRequests) + " ms");
        System.out.println("吞吐量: " + (totalRequests / (duration / 1000.0)) + " 请求/秒");
        System.out.println("==================\n");
        
        // 验证响应时间是否在可接受范围内（例如，平均响应时间小于10ms）
        assertTrue((duration / (double) totalRequests) < 10, "平均响应时间超过10ms");
    }

    /**
     * 测试高并发更新商品状态的性能
     * 
     * @throws InterruptedException 如果线程中断
     */
    @Test
    void updateStatus_shouldHandleHighConcurrency() throws InterruptedException {
        // 并发线程数
        int threadCount = 20;
        // 每个线程更新商品状态的次数
        int updatesPerThread = 10;
        // 总更新次数
        int totalUpdates = threadCount * updatesPerThread;
        
        // 准备测试数据
        Product testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Test Product");
        testProduct.setSeller(seller);
        testProduct.setStatus(ProductStatus.ON_SALE);
        
        // 设置ProductRepository的mock行为
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        
        // 创建计数器
        CountDownLatch latch = new CountDownLatch(threadCount);
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 提交任务
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < updatesPerThread; j++) {
                        // 交替更新商品状态
                        ProductStatus status = (threadIndex + j) % 2 == 0 ? ProductStatus.ON_SALE : ProductStatus.OFF_SALE;
                        productService.updateStatus("seller", 1L, status);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成
        latch.await();
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        
        // 关闭线程池
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        
        // 输出性能指标
        long duration = endTime - startTime;
        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("总更新次数: " + totalUpdates);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均响应时间: " + (duration / (double) totalUpdates) + " ms");
        System.out.println("吞吐量: " + (totalUpdates / (duration / 1000.0)) + " 更新/秒");
        System.out.println("==================\n");
        
        // 验证响应时间是否在可接受范围内（例如，平均响应时间小于20ms）
        assertTrue((duration / (double) totalUpdates) < 20, "平均响应时间超过20ms");
    }
}
