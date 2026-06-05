package com.finance.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 服务注册中心安全配置
 * 
 * 配置Eureka Dashboard的安全访问控制
 * 
 * @author 财务系统开发团队
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 用户详情服务（内存用户）
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("finance123"))
                .roles("ADMIN")
                .build();
        
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（Eureka需要）
            .csrf(csrf -> csrf.disable())
            
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许Eureka Dashboard访问
                .requestMatchers("/eureka/**").authenticated()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/").authenticated()
                .anyRequest().authenticated()
            )
            
            // HTTP Basic认证
            .httpBasic(basic -> {})
            
            // 禁用表单登录
            .formLogin(form -> form.disable());
        
        return http.build();
    }
}