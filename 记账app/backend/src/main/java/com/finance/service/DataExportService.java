package com.finance.service;

import com.finance.dto.UserExportRequest;
import com.finance.dto.UserExportResponse;

/**
 * 数据导出服务接口
 * 负责处理用户数据的导出功能
 */
public interface DataExportService {
    
    /**
     * 执行数据导出
     * @param request 导出请求参数
     * @return 导出响应结果
     */
    UserExportResponse executeDataExport(UserExportRequest request);
    
    /**
     * 导出交易数据
     * @param request 导出请求参数
     * @return 导出文件路径
     */
    String exportTransactionData(UserExportRequest request);
    
    /**
     * 导出预算数据
     * @param request 导出请求参数
     * @return 导出文件路径
     */
    String exportBudgetData(UserExportRequest request);
    
    /**
     * 导出储蓄目标数据
     * @param request 导出请求参数
     * @return 导出文件路径
     */
    String exportSavingGoalData(UserExportRequest request);
    
    /**
     * 导出统计报表数据
     * @param request 导出请求参数
     * @return 导出文件路径
     */
    String exportStatisticsData(UserExportRequest request);
}