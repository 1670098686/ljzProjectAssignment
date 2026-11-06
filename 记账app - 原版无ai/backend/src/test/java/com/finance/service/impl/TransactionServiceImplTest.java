package com.finance.service.impl;

import com.finance.context.UserContext;
import com.finance.dto.CreateTransactionRequest;
import com.finance.dto.TransactionDto;
import com.finance.dto.TransactionQuery;
import com.finance.entity.Category;
import com.finance.entity.TransactionRecord;
import com.finance.event.DomainEventPublisher;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.CategoryRepository;
import com.finance.repository.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionServiceImpl的单元测试
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserContext userContext;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransactionRecord mockTransactionRecord;
    private Category mockCategory;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        mockCategory = new Category();
        mockCategory.setId(1L);
        mockCategory.setName("餐饮");
        mockCategory.setUserId(userId);

        mockTransactionRecord = new TransactionRecord();
        mockTransactionRecord.setId(1L);
        mockTransactionRecord.setUserId(userId);
        mockTransactionRecord.setType(2); // 支出
        mockTransactionRecord.setCategory(mockCategory);
        mockTransactionRecord.setAmount(BigDecimal.valueOf(100.0));
        mockTransactionRecord.setTransactionDate(LocalDate.now());
        mockTransactionRecord.setRemark("午餐");
        mockTransactionRecord.setCreateTime(LocalDateTime.now());
        mockTransactionRecord.setUpdateTime(LocalDateTime.now());

        // 设置UserContext的默认行为
        when(userContext.getCurrentUserId()).thenReturn(userId);
    }

    /**
     * 测试查询交易记录
     */
    @Test
    void testQueryTransactions() {
        // 设置mock行为，使用any匹配器，避免参数不匹配问题
        doReturn(Collections.singletonList(mockTransactionRecord))
                .when(transactionRecordRepository)
                .findByFilter(anyLong(), any(), any(), any(), any());

        // 调用被测方法
        List<TransactionDto> result = transactionService.queryTransactions(null);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockTransactionRecord.getId(), result.get(0).getId());
        assertEquals(mockTransactionRecord.getCategory().getName(), result.get(0).getCategoryName());

        // 验证mock调用
        verify(transactionRecordRepository, times(1))
                .findByFilter(anyLong(), any(), any(), any(), any());
    }

    /**
     * 测试获取单个交易记录
     */
    @Test
    void testGetTransaction() {
        // 设置mock行为
        when(transactionRecordRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(mockTransactionRecord));

        // 调用被测方法
        TransactionDto result = transactionService.getTransaction(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(mockTransactionRecord.getId(), result.getId());
        assertEquals(mockTransactionRecord.getAmount(), result.getAmount());

        // 验证mock调用
        verify(transactionRecordRepository, times(1))
                .findByIdAndUserId(1L, userId);
    }

    /**
     * 测试获取不存在的交易记录
     */
    @Test
    void testGetTransactionNotFound() {
        // 设置mock行为
        when(transactionRecordRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        // 调用被测方法，验证抛出异常
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.getTransaction(999L);
        });

        // 验证mock调用
        verify(transactionRecordRepository, times(1))
                .findByIdAndUserId(999L, userId);
    }

    /**
     * 测试创建交易记录
     */
    @Test
    void testCreateTransaction() {
        // 准备测试数据
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(2);
        request.setCategoryId(1L);
        request.setAmount(BigDecimal.valueOf(200.0));
        request.setTransactionDate(LocalDate.now());
        request.setRemark("晚餐");

        // 设置mock行为
        when(categoryRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(mockCategory));
        when(transactionRecordRepository.save(any(TransactionRecord.class)))
                .thenReturn(mockTransactionRecord);

        // 调用被测方法
        TransactionDto result = transactionService.createTransaction(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(mockTransactionRecord.getId(), result.getId());

        // 验证mock调用
        verify(categoryRepository, times(1))
                .findByIdAndUserId(1L, userId);
        verify(transactionRecordRepository, times(1))
                .save(any(TransactionRecord.class));
        verify(eventPublisher, times(1))
                .publish(any());
    }

    /**
     * 测试删除交易记录
     */
    @Test
    void testDeleteTransaction() {
        // 设置mock行为
        when(transactionRecordRepository.existsByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(true);
        doNothing().when(transactionRecordRepository).deleteById(anyLong());

        // 调用被测方法
        transactionService.deleteTransaction(1L);

        // 验证mock调用
        verify(transactionRecordRepository, times(1))
                .existsByIdAndUserId(1L, userId);
        verify(transactionRecordRepository, times(1))
                .deleteById(1L);
    }

    /**
     * 测试删除不存在的交易记录
     */
    @Test
    void testDeleteTransactionNotFound() {
        // 设置mock行为
        when(transactionRecordRepository.existsByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(false);

        // 调用被测方法，验证抛出异常
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.deleteTransaction(999L);
        });

        // 验证mock调用
        verify(transactionRecordRepository, times(1))
                .existsByIdAndUserId(999L, userId);
        verify(transactionRecordRepository, never())
                .deleteById(anyLong());
    }
}
