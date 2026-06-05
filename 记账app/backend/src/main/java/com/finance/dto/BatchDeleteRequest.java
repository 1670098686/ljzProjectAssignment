package com.finance.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量删除请求DTO
 */
public class BatchDeleteRequest {

    @NotEmpty(message = "交易记录ID列表不能为空")
    private List<@NotNull(message = "交易记录ID不能为null") Long> transactionIds;

    /**
     * 是否确认删除（防止误操作）
     */
    @NotNull(message = "删除确认状态不能为null")
    private Boolean confirmDelete;

    public BatchDeleteRequest() {
    }

    public List<Long> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(List<Long> transactionIds) {
        this.transactionIds = transactionIds;
    }

    public Boolean getConfirmDelete() {
        return confirmDelete;
    }

    public void setConfirmDelete(Boolean confirmDelete) {
        this.confirmDelete = confirmDelete;
    }
}