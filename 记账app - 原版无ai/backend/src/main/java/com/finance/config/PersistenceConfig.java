package com.finance.config;

import com.finance.converter.AmountEncryptConverter;
import com.finance.converter.MapConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/**
 * JPA 持久化配置类
 * 配置加密转换器，确保敏感数据字段在数据库操作时自动加密/解密
 */
@Configuration
public class PersistenceConfig {

    /**
     * 配置 JPA 转换器
     * JPA AttributeConverter 会被自动检测，无需手动注册
     */
    @Bean
    public PersistenceUnitPostProcessor persistenceUnitPostProcessor() {
        return new PersistenceUnitPostProcessor() {
            @Override
            public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
                // JPA AttributeConverter 会被自动检测，无需手动注册
            }
        };
    }
}