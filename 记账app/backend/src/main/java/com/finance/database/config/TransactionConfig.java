package com.finance.database.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 事务管理配置
 * 确保读写分离与事务管理的兼容性
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * 配置事务管理器
     * @param dataSource 路由数据源
     * @return 事务管理器
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        
        // 配置事务管理器属性
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        transactionManager.setNestedTransactionAllowed(true);
        
        return transactionManager;
    }
}