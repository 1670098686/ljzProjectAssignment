package com.campus.trade;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestConfig {
    // 移除passwordEncoder bean定义，使用主配置中的bean
}