package com.finance.event.listener;

import com.finance.entity.UserProfile;
import com.finance.event.BudgetAlertEvent;
import com.finance.event.BudgetOverspendEvent;
import com.finance.repository.UserProfileRepository;
import com.finance.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 预算事件监听器 - 处理预算相关的预警和超支事件
 */
@Component
public class BudgetEventListener {

    private static final Logger log = LoggerFactory.getLogger(BudgetEventListener.class);

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * 监听预算预警事件
     */
    @EventListener
    public void handleBudgetAlertEvent(BudgetAlertEvent event) {
        try {
            log.debug("收到预算预警事件: 用户ID={}, 预算ID={}", event.getUserId(), event.getAlert().getBudgetId());

            // 查找用户档案
            Optional<UserProfile> userOptional = userProfileRepository.findById(event.getUserId());
            if (userOptional.isPresent()) {
                UserProfile user = userOptional.get();
                
                // 发送预算预警通知
                sendBudgetNotification(event, user);
                
                // 记录预警历史
                recordAlertHistory(event, user);
            } else {
                log.warn("未找到用户档案: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理预算预警事件失败: {}", event.getUserId(), e);
        }
    }

    /**
     * 监听预算超支事件
     */
    @EventListener
    public void handleBudgetOverspendEvent(BudgetOverspendEvent event) {
        try {
            log.debug("收到预算超支事件: 用户ID={}, 预算ID={}", event.getUserId(), event.getBudgetId());

            // 查找用户档案
            Optional<UserProfile> userOptional = userProfileRepository.findById(event.getUserId());
            if (userOptional.isPresent()) {
                UserProfile user = userOptional.get();
                
                // 发送超支通知
                sendOverspendNotification(event, user);
                
                // 检查通知频率限制
                handleNotificationFrequencyLimit(event, user);
            } else {
                log.warn("未找到用户档案: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理预算超支事件失败: {}", event.getUserId(), e);
        }
    }

    /**
     * 发送预算预警通知
     */
    private void sendBudgetNotification(BudgetAlertEvent event, UserProfile user) {
        try {
            String categoryName = event.getAlert().getCategoryName();
            BigDecimal spentAmount = event.getAlert().getSpentAmount();
            BigDecimal usagePercentage = event.getAlert().getUsagePercentage();
            
            String message = String.format(
                "预算预警提醒：您的%s分类本月已使用%.2f元，占预算的%.1f%%",
                categoryName,
                spentAmount.doubleValue(),
                usagePercentage.doubleValue() * 100
            );

            // 根据用户通知偏好发送通知
            if (Boolean.TRUE.equals(user.getPushEnabled())) {
                // 发送实际通知到消息队列或推送服务
                sendActualNotification(user.getUserId(), "预算预警", message, "BUDGET_ALERT");
                log.info("预算预警：已向用户{}发送推送通知：{}", user.getUserId(), message);
            } else {
                log.debug("用户{}已禁用推送通知，跳过发送", user.getUserId());
            }

        } catch (Exception e) {
            log.error("发送预算预警通知失败: {}", event.getUserId(), e);
        }
    }
    
    /**
     * 发送实际通知到消息队列或推送服务
     */
    private void sendActualNotification(Long userId, String title, String content, String notificationType) {
        try {
            // 这里可以集成实际的通知推送服务
            // 例如：发送到消息队列、调用推送API等
            
            // 模拟发送通知到消息队列
            log.info("发送通知到消息队列 - 用户ID: {}, 标题: {}, 内容: {}, 类型: {}", 
                    userId, title, content, notificationType);
                    
            // 实际实现时，可以调用以下服务：
            // 1. 消息队列服务（RabbitMQ/Kafka）
            // 2. 推送服务（Firebase/极光推送）
            // 3. 短信/邮件服务
            
        } catch (Exception e) {
            log.error("发送实际通知失败: {}", userId, e);
        }
    }

    /**
     * 发送超支通知
     */
    private void sendOverspendNotification(BudgetOverspendEvent event, UserProfile user) {
        try {
            BigDecimal spentAmount = event.getSpentAmount();
            BigDecimal budgetAmount = event.getBudgetAmount();
            BigDecimal overspentAmount = spentAmount.subtract(budgetAmount);
            
            String message = String.format(
                "紧急通知：您的预算已超支！本月支出%.2f元，超预算%.2f元",
                spentAmount.doubleValue(),
                overspentAmount.doubleValue()
            );

            // 超支情况发送紧急通知
            if (Boolean.TRUE.equals(user.getPushEnabled())) {
                // 发送实际紧急通知
                sendActualNotification(user.getUserId(), "预算超支紧急提醒", message, "BUDGET_OVERSPEND");
                log.warn("预算超支：已向用户{}发送紧急推送通知：{}", user.getUserId(), message);
            } else {
                log.debug("用户{}已禁用推送通知，跳过发送超支提醒", user.getUserId());
            }

        } catch (Exception e) {
            log.error("发送预算超支通知失败: {}", event.getUserId(), e);
        }
    }

    /**
     * 记录预警历史
     */
    private void recordAlertHistory(BudgetAlertEvent event, UserProfile user) {
        // 这里可以添加记录预警历史的逻辑
        log.debug("记录预算预警历史: 用户ID={}, 预算ID={}", user.getUserId(), event.getAlert().getBudgetId());
    }

    /**
     * 处理通知频率限制
     */
    private void handleNotificationFrequencyLimit(BudgetOverspendEvent event, UserProfile user) {
        try {
            // 如果用户频繁超支，可能需要调整通知频率
            if (user.getReminderFrequencyDays() != null) {
                log.debug("检查用户通知频率限制: {}", event.getUserId());
                // 根据超支频率调整通知策略
            }
        } catch (Exception e) {
            log.error("处理通知频率限制失败: {}", event.getUserId(), e);
        }
    }
}