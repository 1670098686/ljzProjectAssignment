package com.campus.trade.security;

import com.campus.trade.model.entity.User;
import com.campus.trade.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 先尝试通过用户名查找
        User user = userRepository.findByUsername(usernameOrEmail)
                // 如果没找到，尝试通过邮箱查找
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("用户不存在")));
        return CustomUserDetails.fromUser(user);
    }
}