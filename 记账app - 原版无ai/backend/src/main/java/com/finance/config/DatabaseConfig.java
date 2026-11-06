package com.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 数据库配置类
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    /**
     * 开发环境数据库初始化
     */
    @Bean
    @Profile("dev")
    public DataSource dataSourceInitializer(DataSource dataSource) {
        // 创建连接测试
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 测试连接
            statement.execute("SELECT 1");
            System.out.println("✅ 数据库连接测试成功");
            
            // 检查表是否存在
            try {
                statement.execute("SELECT COUNT(*) FROM category LIMIT 1");
                System.out.println("✅ 分类表已存在，数据初始化可能已完成");
            } catch (Exception e) {
                System.out.println("⚠️  分类表不存在，请运行数据库初始化脚本");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dataSource;
    }

    /**
     * Spring Data JPA配置
     */
    @Bean
    public org.springframework.orm.jpa.JpaVendorAdapter jpaVendorAdapter() {
        org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter adapter = 
            new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter();
        adapter.setShowSql(true);
        adapter.setGenerateDdl(false);
        adapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        return adapter;
    }
}