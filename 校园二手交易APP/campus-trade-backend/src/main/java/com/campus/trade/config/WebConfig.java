package com.campus.trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-root}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理
        registry.addResourceHandler("/upload/**")
                // 支持文件系统路径
                .addResourceLocations("file:" + uploadRoot + "/")
                // 支持类路径
                .addResourceLocations("classpath:/static/upload/")
                .setCachePeriod(3600);
    }
}
