package com.campus.trade.config;

import com.campus.trade.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    FileStorageProperties.class,
    FileSecurityProperties.class,
    IdempotencyProperties.class,
    WebSocketProperties.class,
    MailProperties.class
})
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
