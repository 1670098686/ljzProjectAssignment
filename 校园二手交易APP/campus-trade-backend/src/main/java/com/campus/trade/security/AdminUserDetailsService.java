package com.campus.trade.security;

import com.campus.trade.model.entity.Admin;
import com.campus.trade.model.enums.AdminStatus;
import com.campus.trade.repository.AdminRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public AdminUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(emailOrUsername)
                .orElseGet(() -> adminRepository.findByUsername(emailOrUsername)
                        .orElseThrow(() -> new UsernameNotFoundException("管理员不存在")));
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new UsernameNotFoundException("管理员已禁用");
        }
        return AdminUserDetails.fromAdmin(admin);
    }
}
