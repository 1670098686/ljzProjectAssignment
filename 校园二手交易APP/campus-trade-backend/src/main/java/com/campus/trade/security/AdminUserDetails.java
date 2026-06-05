package com.campus.trade.security;

import com.campus.trade.model.entity.Admin;
import com.campus.trade.model.enums.AdminPermission;
import com.campus.trade.model.enums.AdminStatus;
import com.campus.trade.util.AdminPermissionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

public class AdminUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    private AdminUserDetails(Long id,
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

    public static AdminUserDetails fromAdmin(Admin admin) {
        EnumSet<AdminPermission> permissions = AdminPermissionUtils.resolveStoredPermissions(admin);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()));
        permissions.stream()
                .map(AdminPermission::getAuthority)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        boolean enabled = admin.getStatus() == AdminStatus.ACTIVE;
        return new AdminUserDetails(admin.getId(), admin.getUsername(), admin.getPassword(), enabled, authorities);
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
