package com.finance.service;

import com.finance.dto.BudgetAlertConfigRequest;
import com.finance.dto.BudgetAlertConfigResponse;
import com.finance.dto.BudgetAlertDto;
import com.finance.dto.BudgetAlertHistoryRequest;
import com.finance.dto.BudgetAlertHistoryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetAlertService {

    /**
     * 获取预算预警信息
     * 
     * @param year 年份
     * @param month 月份
     * @param warningThreshold 预警阈值
     * @param criticalThreshold 临界预警阈值
     * @param pushEnabled 是否推送通知
     * @return 预算预警列表
     */
    List<BudgetAlertDto> getBudgetAlerts(Integer year,
                                         Integer month,
                                         BigDecimal warningThreshold,
                                         BigDecimal criticalThreshold,
                                         boolean pushEnabled);

    /**
     * 配置预警规则
     * 
     * @param request 预警规则配置请求
     * @return 预警规则配置响应
     */
    BudgetAlertConfigResponse configureAlertRule(BudgetAlertConfigRequest request);

    /**
     * 获取预警规则配置
     * 
     * @return 预警规则配置
     */
    BudgetAlertConfigResponse getAlertConfig();

    /**
     * 获取预警历史记录
     * 
     * @param request 预警历史查询请求
     * @return 预警历史记录列表（分页）
     */
    BudgetAlertHistoryResponse.BudgetAlertHistoryListResponse getAlertHistory(BudgetAlertHistoryRequest request);
}
