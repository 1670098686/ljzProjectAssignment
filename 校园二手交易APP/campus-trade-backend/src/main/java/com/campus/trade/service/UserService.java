package com.campus.trade.service;

import com.campus.trade.dto.user.AccountStatusResponse;
import com.campus.trade.dto.user.ChangePasswordRequest;
import com.campus.trade.dto.user.UpdateProfileRequest;
import com.campus.trade.dto.user.UserProfileResponse;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.util.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserSummary getCurrentUserSummary(String username) {
        return UserMapper.toSummary(findByUsername(username));
    }

    public UserProfileResponse getProfile(String username) {
        User user = findByUsername(username);
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        response.setSchool(user.getSchool());
        response.setContactInfo(user.getContactInfo());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setEmailVerified(user.isEmailVerified());
        response.setDeleteRequested(user.isDeleteRequested());
        response.setDeleteReason(user.getDeleteReason());
        response.setDeleteScheduleTime(user.getDeleteScheduleTime());
        return response;
    }

    @Transactional
    public void updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);
        
        // 更新用户名（如果提供了新用户名）
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            // 限制用户名不能以admin开头
            if (request.getUsername().toLowerCase().startsWith("admin")) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名不能以admin开头");
            }
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        
        user.setPhone(request.getPhone());
        user.setSchool(request.getSchool());
        user.setAvatar(request.getAvatar());
        user.setContactInfo(request.getContactInfo());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public AccountStatusResponse requestDelete(String username, String reason) {
        User user = findByUsername(username);
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "邮箱未验证，无法发起注销");
        }
        if (user.isDeleteRequested()) {
            throw new BusinessException(ErrorCode.DELETE_REQUEST_PENDING, "已有注销申请在处理中");
        }
        user.setDeleteRequested(true);
        user.setDeleteReason(reason);
        user.setDeleteScheduleTime(LocalDateTime.now().plusDays(7));
        return toAccountStatus(user);
    }

    @Transactional
    public AccountStatusResponse cancelDelete(String username) {
        User user = findByUsername(username);
        if (!user.isDeleteRequested()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前没有注销申请");
        }
        user.setDeleteRequested(false);
        user.setDeleteReason(null);
        user.setDeleteScheduleTime(null);
        return toAccountStatus(user);
    }

    public AccountStatusResponse getAccountStatus(String username) {
        User user = findByUsername(username);
        return toAccountStatus(user);
    }

    private AccountStatusResponse toAccountStatus(User user) {
        AccountStatusResponse response = new AccountStatusResponse();
        response.setDeleteRequested(user.isDeleteRequested());
        response.setDeleteReason(user.getDeleteReason());
        response.setDeleteScheduleTime(user.getDeleteScheduleTime());
        response.setStatus(user.getStatus());
        return response;
    }

    private User findByUsername(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }
}
