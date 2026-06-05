package com.finance.service;

import com.finance.dto.BatchDeleteRequest;
import com.finance.dto.CreateTransactionRequest;
import com.finance.dto.ExportRequest;
import com.finance.dto.TransactionDto;
import com.finance.dto.TransactionQuery;
import com.finance.dto.UpdateTransactionRequest;

import java.util.List;

public interface TransactionService {

    List<TransactionDto> queryTransactions(TransactionQuery query);

    TransactionDto getTransaction(Long id);

    TransactionDto createTransaction(CreateTransactionRequest request);

    TransactionDto updateTransaction(Long id, UpdateTransactionRequest request);

    void deleteTransaction(Long id);

    /**
     * 批量删除交易记录
     * 
     * @param request 批量删除请求
     * @return 删除的记录数量
     */
    int batchDeleteTransactions(BatchDeleteRequest request);

    /**
     * 导出交易记录
     * 
     * @param request 导出请求，包含过滤条件和格式设置
     * @return 导出的文件数据（字节数组）
     */
    byte[] exportTransactions(ExportRequest request);
}
