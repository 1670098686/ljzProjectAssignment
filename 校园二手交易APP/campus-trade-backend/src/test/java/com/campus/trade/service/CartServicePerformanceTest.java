package com.campus.trade.service;

import com.campus.trade.dto.cart.AddCartItemRequest;
import com.campus.trade.dto.cart.CartItemResponse;
import com.campus.trade.dto.cart.UpdateCartItemRequest;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 购物车服务性能测试
 * 验证系统在高并发下的购物车操作表现
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServicePerformanceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

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

        // 正常商品
        product = new Product();
        product.setId(5L);
        product.setTitle("Desk Lamp");
        product.setPrice(new BigDecimal("36.00"));
        product.setSeller(seller);
        product.setStatus(ProductStatus.ON_SALE);

        // 设置UserRepository的mock行为
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        
        // 设置ProductRepository的mock行为
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        
        // 设置CartItemRepository的mock行为
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(System.currentTimeMillis());
            return saved;
        });
        
        // 设置获取购物车商品的mock行为
        List<CartItem> cartItems = new ArrayList<>();
        when(cartItemRepository.findAllByUserId(anyLong())).thenReturn(cartItems);
        
        // 设置通过用户ID和商品ID查找购物车项的mock行为
        when(cartItemRepository.findByUserIdAndProductId(anyLong(), anyLong())).thenReturn(Optional.empty());
    }

    /**
     * 测试高并发添加商品到购物车的性能
     * 
     * @throws InterruptedException 如果线程中断
     */
    @Test
    void addItem_shouldHandleHighConcurrency() throws InterruptedException {
        // 并发线程数
        int threadCount = 50;
        // 每个线程添加商品的次数
        int addsPerThread = 10;
        // 总添加次数
        int totalAdds = threadCount * addsPerThread;
        
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
                    for (int j = 0; j < addsPerThread; j++) {
                        AddCartItemRequest request = new AddCartItemRequest();
                        request.setProductId(5L);
                        request.setQuantity(1);
                        
                        cartService.addItem("buyer", request);
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
        
        // 验证总添加次数
        verify(cartItemRepository, times(totalAdds)).save(any(CartItem.class));
        
        // 输出性能指标
        long duration = endTime - startTime;
        System.out.println("\n=== 购物车添加商品性能测试结果 ===");
        System.out.println("总添加次数: " + totalAdds);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均响应时间: " + (duration / (double) totalAdds) + " ms");
        System.out.println("吞吐量: " + (totalAdds / (duration / 1000.0)) + " 件/秒");
        System.out.println("==================\n");
        
        // 验证响应时间是否在可接受范围内（例如，平均响应时间小于50ms）
        assertTrue((duration / (double) totalAdds) < 50, "平均响应时间超过50ms");
    }

    /**
     * 测试高并发更新购物车商品数量的性能
     * 
     * @throws InterruptedException 如果线程中断
     */
    @Test
    void updateQuantity_shouldHandleHighConcurrency() throws InterruptedException {
        // 并发线程数
        int threadCount = 20;
        // 每个线程更新商品的次数
        int updatesPerThread = 5;
        // 总更新次数
        int totalUpdates = threadCount * updatesPerThread;
        
        // 准备购物车商品数据
        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setUser(buyer);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        
        // 设置CartItemRepository的mock行为
        when(cartItemRepository.findByIdAndUserId(1L, buyer.getId())).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        
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
                        UpdateCartItemRequest request = new UpdateCartItemRequest();
                        // 交替更新商品数量
                        request.setQuantity((threadIndex + j) % 2 == 0 ? 2 : 3);
                        
                        cartService.updateQuantity("buyer", 1L, request);
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
        
        // 验证总更新次数
        verify(cartItemRepository, times(totalUpdates)).save(any(CartItem.class));
        
        // 输出性能指标
        long duration = endTime - startTime;
        System.out.println("\n=== 购物车更新商品性能测试结果 ===");
        System.out.println("总更新次数: " + totalUpdates);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均响应时间: " + (duration / (double) totalUpdates) + " ms");
        System.out.println("吞吐量: " + (totalUpdates / (duration / 1000.0)) + " 更新/秒");
        System.out.println("==================\n");
        
        // 验证响应时间是否在可接受范围内（例如，平均响应时间小于20ms）
        assertTrue((duration / (double) totalUpdates) < 20, "平均响应时间超过20ms");
    }

    /**
     * 测试高并发获取购物车商品的性能
     * 
     * @throws InterruptedException 如果线程中断
     */
    @Test
    void listItems_shouldHandleHighConcurrency() throws InterruptedException {
        // 并发线程数
        int threadCount = 100;
        // 每个线程获取购物车的次数
        int requestsPerThread = 20;
        // 总请求数量
        int totalRequests = threadCount * requestsPerThread;
        
        // 准备购物车商品数据
        List<CartItem> cartItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CartItem cartItem = new CartItem();
            cartItem.setId((long) i + 1);
            cartItem.setUser(buyer);
            cartItem.setProduct(product);
            cartItem.setQuantity(i + 1);
            cartItems.add(cartItem);
        }
        
        // 设置CartItemRepository的mock行为
        when(cartItemRepository.findAllByUserId(anyLong())).thenReturn(cartItems);
        
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
                        // 使用listItems方法，需要提供page和size参数
                        var response = cartService.listItems("buyer", 1, 10);
                        // 验证返回结果
                        assertNotNull(response);
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
        System.out.println("\n=== 购物车获取商品性能测试结果 ===");
        System.out.println("总请求数量: " + totalRequests);
        System.out.println("并发线程数: " + threadCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均响应时间: " + (duration / (double) totalRequests) + " ms");
        System.out.println("吞吐量: " + (totalRequests / (duration / 1000.0)) + " 请求/秒");
        System.out.println("==================\n");
        
        // 验证响应时间是否在可接受范围内（例如，平均响应时间小于10ms）
        assertTrue((duration / (double) totalRequests) < 10, "平均响应时间超过10ms");
    }
}
