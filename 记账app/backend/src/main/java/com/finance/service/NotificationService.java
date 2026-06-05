package com.finance.service;

import org.springframework.stereotype.Service;

/**
 * 通知服务类
 * 负责处理系统通知相关操作
 */
@Service
public class NotificationService {
    
    /**
     * 发送通知
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     */
    public void sendNotification(Long userId, String title, String content) {
        // 简化实现，实际项目中需要使用通知服务（如邮件、短信、推送等）
        System.out.println("发送通知给用户 " + userId + ": " + title + " - " + content);
    }
    
    /**
     * 发送预算超支通知
     * @param userId 用户ID
     * @param budgetName 预算名称
     * @param overspentAmount 超支金额
     */
    public void sendBudgetOverspentNotification(Long userId, String budgetName, double overspentAmount) {
        String title = "预算超支提醒";
        String content = "您的预算 \"" + budgetName + "\" 已超支 " + overspentAmount + " 元，请及时调整！";
        sendNotification(userId, title, content);
    }
    
    /**
     * 发送储蓄目标完成通知
     * @param userId 用户ID
     * @param goalName 储蓄目标名称
     * @param achievedAmount 已达成金额
     */
    public void sendSavingGoalAchievedNotification(Long userId, String goalName, double achievedAmount) {
        String title = "储蓄目标达成";
        String content = "恭喜您！储蓄目标 \"" + goalName + "\" 已达成，当前金额：" + achievedAmount + " 元！";
        sendNotification(userId, title, content);
    }
    
    /**
     * 发送交易提醒通知
     * @param userId 用户ID
     * @param transactionType 交易类型
     * @param amount 交易金额
     * @param category 交易分类
     */
    public void sendTransactionReminder(Long userId, String transactionType, double amount, String category) {
        String title = "交易提醒";
        String content = "您有一笔新的 " + transactionType + " 交易：" + amount + " 元，分类：" + category;
        sendNotification(userId, title, content);
    }
}