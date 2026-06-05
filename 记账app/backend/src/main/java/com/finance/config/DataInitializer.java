package com.finance.config;

import com.finance.entity.Category;
import com.finance.entity.SavingGoal;
import com.finance.entity.TransactionRecord;
import com.finance.entity.UserProfile;
import com.finance.repository.CategoryRepository;
import com.finance.repository.SavingGoalRepository;
import com.finance.repository.TransactionRecordRepository;
import com.finance.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 测试数据初始化配置
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("dev")
    public CommandLineRunner initData(
            CategoryRepository categoryRepository,
            TransactionRecordRepository transactionRepository,
            UserProfileRepository userProfileRepository,
            SavingGoalRepository savingGoalRepository) {
        
        return args -> {
            Long userId = 1L;
            
            logger.info("🚀 开始初始化测试数据...");

            // 初始化用户配置
            if (userProfileRepository.findById(userId).isEmpty()) {
                UserProfile userProfile = new UserProfile();
                userProfile.setUserId(userId);
                userProfile.setNickname("测试用户");
                userProfile.setRemindTime("21:00");
                userProfileRepository.save(userProfile);
                logger.info("✅ 创建测试用户配置");
            }

            // 初始化分类数据
            if (categoryRepository.count() == 0) {
                List<Category> categories = List.of(
                    // 收入分类
                    createCategory("工资", "work", 1, userId),
                    createCategory("奖金", "bonus", 1, userId),
                    createCategory("投资收益", "investment", 1, userId),
                    createCategory("其他收入", "other_income", 1, userId),
                    // 支出分类
                    createCategory("餐饮", "restaurant", 2, userId),
                    createCategory("交通", "transport", 2, userId),
                    createCategory("购物", "shopping", 2, userId),
                    createCategory("娱乐", "entertainment", 2, userId),
                    createCategory("医疗", "medical", 2, userId),
                    createCategory("教育", "education", 2, userId),
                    createCategory("住房", "housing", 2, userId),
                    createCategory("其他支出", "other_expense", 2, userId)
                );
                
                categoryRepository.saveAll(categories);
                logger.info("✅ 创建了 {} 个分类", categories.size());
            }

            // 初始化交易记录（如果还没有的话）
            if (transactionRepository.count() == 0) {
                // 获取所有分类
                List<Category> categories = categoryRepository.findAll();
                
                List<TransactionRecord> transactions = List.of(
                    // 示例收入记录
                    createTransaction(1, categories.get(0), new BigDecimal("8000.00"), 
                        LocalDate.now().minusDays(30), "月工资", userId),
                    createTransaction(1, categories.get(1), new BigDecimal("1500.00"), 
                        LocalDate.now().minusDays(25), "项目奖金", userId),
                    createTransaction(1, categories.get(2), new BigDecimal("500.00"), 
                        LocalDate.now().minusDays(20), "股票分红", userId),
                    
                    // 示例支出记录
                    createTransaction(2, categories.get(4), new BigDecimal("180.50"), 
                        LocalDate.now().minusDays(28), "午餐", userId),
                    createTransaction(2, categories.get(5), new BigDecimal("25.00"), 
                        LocalDate.now().minusDays(27), "地铁卡充值", userId),
                    createTransaction(2, categories.get(6), new BigDecimal("299.99"), 
                        LocalDate.now().minusDays(26), "购买耳机", userId),
                    createTransaction(2, categories.get(7), new BigDecimal("120.00"), 
                        LocalDate.now().minusDays(25), "电影票", userId),
                    createTransaction(2, categories.get(8), new BigDecimal("88.00"), 
                        LocalDate.now().minusDays(24), "买药", userId),
                    createTransaction(2, categories.get(4), new BigDecimal("85.00"), 
                        LocalDate.now().minusDays(23), "晚餐", userId),
                    createTransaction(2, categories.get(5), new BigDecimal("12.00"), 
                        LocalDate.now().minusDays(22), "打车费", userId)
                );
                
                transactionRepository.saveAll(transactions);
                logger.info("✅ 创建了 {} 条交易记录", transactions.size());
            }

            // 初始化储蓄目标
            if (savingGoalRepository.count() == 0) {
                List<SavingGoal> goals = List.of(
                    createSavingGoal("购买笔记本电脑", new BigDecimal("10000.00"), 
                        new BigDecimal("3000.00"), LocalDate.now().plusMonths(6), "想要购买一台MacBook Pro用于工作和学习", userId),
                    createSavingGoal("年度旅游基金", new BigDecimal("8000.00"), 
                        new BigDecimal("1500.00"), LocalDate.now().plusMonths(12), "计划年底去日本旅游", userId)
                );
                
                savingGoalRepository.saveAll(goals);
                logger.info("✅ 创建了 {} 个储蓄目标", goals.size());
            }

            logger.info("🎉 测试数据初始化完成！");
        };
    }
    
    /**
     * 创建分类对象
     */
    private Category createCategory(String name, String icon, int type, Long userId) {
        Category category = new Category();
        category.setName(name);
        category.setIcon(icon);
        category.setType(type);
        category.setUserId(userId);
        return category;
    }
    
    /**
     * 创建交易记录对象
     */
    private TransactionRecord createTransaction(int type, Category category, BigDecimal amount, 
                                               LocalDate transactionDate, String remark, Long userId) {
        TransactionRecord transaction = new TransactionRecord();
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate);
        transaction.setRemark(remark);
        transaction.setUserId(userId);
        return transaction;
    }
    
    /**
     * 创建储蓄目标对象
     */
    private SavingGoal createSavingGoal(String name, BigDecimal targetAmount, BigDecimal currentAmount,
                                       LocalDate deadline, String description, Long userId) {
        SavingGoal goal = new SavingGoal();
        goal.setName(name);
        goal.setTargetAmount(targetAmount);
        goal.setCurrentAmount(currentAmount);
        goal.setDeadline(deadline);
        goal.setDescription(description);
        goal.setUserId(userId);
        return goal;
    }
}