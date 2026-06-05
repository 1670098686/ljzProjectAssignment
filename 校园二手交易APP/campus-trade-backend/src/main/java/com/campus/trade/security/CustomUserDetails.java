package com.campus.trade.security;

import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id,
                             String username,
                             String password,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static CustomUserDetails fromUser(User user) {
        Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
        String role = "ROLE_" + user.getRole().name();
        authorities.add(new SimpleGrantedAuthority(role));
        
        // 为ADMIN角色添加所有必要的管理员权限
        if (user.getRole().name().equals("ADMIN")) {
            authorities.add(new SimpleGrantedAuthority("ADMIN_ADMIN_MANAGEMENT"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_PRODUCT_REVIEW"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_USER_MANAGEMENT"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_MESSAGE_REVIEW"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_NOTIFICATION_MANAGEMENT"));
        }
        
        // 开发环境暂时移除邮箱验证要求
        boolean enabled = user.getStatus() == AccountStatus.ACTIVE;
        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword(), enabled, authorities);
    }

    public static CustomUserDetails fromUser(User user, String email) {
        Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
        String role = "ROLE_" + user.getRole().name();
        authorities.add(new SimpleGrantedAuthority(role));
        
        // 为ADMIN角色添加所有必要的管理员权限
        if (user.getRole().name().equals("ADMIN")) {
            authorities.add(new SimpleGrantedAuthority("ADMIN_ADMIN_MANAGEMENT"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_PRODUCT_REVIEW"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_USER_MANAGEMENT"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_MESSAGE_REVIEW"));
            authorities.add(new SimpleGrantedAuthority("ADMIN_NOTIFICATION_MANAGEMENT"));
        }
        
        // 开发环境暂时移除邮箱验证要求
        boolean enabled = user.getStatus() == AccountStatus.ACTIVE;
        return new CustomUserDetails(user.getId(), email, user.getPassword(), enabled, authorities);
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
