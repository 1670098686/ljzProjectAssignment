package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.admin.AdminLoginRequest;
import com.campus.trade.dto.admin.AdminLoginResponse;
import com.campus.trade.dto.admin.AdminRegisterRequest;
import com.campus.trade.dto.admin.BatchUserFinalizeDeletionRequest;
import com.campus.trade.dto.admin.UpdateUserStatusRequest;
import com.campus.trade.dto.product.BatchProductReviewRequest;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.product.ProductReviewRequest;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Admin;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AdminPermission;
import com.campus.trade.model.enums.AdminStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.repository.AdminRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.security.AdminUserDetails;
import com.campus.trade.security.JwtTokenProvider;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.util.AdminPermissionUtils;
import com.campus.trade.util.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.EnumSet;
import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final AccountDeletionService accountDeletionService;

    public AdminService(AdminRepository adminRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        ProductService productService,
                        UserRepository userRepository,
                        AccountDeletionService accountDeletionService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.productService = productService;
        this.userRepository = userRepository;
        this.accountDeletionService = accountDeletionService;
    }

    @Transactional
    public Admin register(AdminRegisterRequest request) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "管理员用户名已存在");
        }
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "管理员邮箱已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setEmail(request.getEmail());
        admin.setRole(request.getRole());
        EnumSet<AdminPermission> permissions = AdminPermissionUtils.normalizePermissions(
            request.getRole(),
            request.getPermissions()
        );
        admin.setPermissions(AdminPermissionUtils.serializePermissions(permissions));
        adminRepository.save(admin);
        return admin;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        // 首先尝试通过用户名查找管理员
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseGet(() -> 
                    // 如果找不到，尝试通过邮箱查找管理员
                    adminRepository.findByEmail(request.getUsername())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "管理员不存在"))
                );
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ADMIN_PERMISSION_DENIED, "管理员已禁用");
        }
        // 不记录密码信息，保护用户隐私
        log.info("Admin found: username={}, email={}", admin.getUsername(), admin.getEmail());
        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), admin.getPassword());
        log.info("Password match: {}", passwordMatch);
        if (!passwordMatch) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        EnumSet<AdminPermission> permissions = AdminPermissionUtils.resolveStoredPermissions(admin);
        AdminUserDetails userDetails = AdminUserDetails.fromAdmin(admin);
        String token = jwtTokenProvider.generateToken(userDetails);
        return new AdminLoginResponse(
            token,
            jwtTokenProvider.getProperties().getExpirationSeconds(),
            admin.getRole(),
            AdminPermissionUtils.toNameList(permissions)
        );
    }

    public PaginatedResponse<ProductResponse> listPendingProducts(int page, int size) {
        return productService.listPendingProducts(page, size);
    }

    public ProductResponse reviewProduct(Long productId, ProductReviewRequest request) {
        return productService.reviewProduct(productId, request.getApproved(), request.getReason());
    }

    public PaginatedResponse<UserSummary> listUsers(AccountStatus status, UserRole role, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Specification<User> spec = Specification.<User>where((root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role))
            .and((root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status));
        Page<User> result = userRepository.findAll(spec, pageable);
        return PaginatedResponse.of(result.map(UserMapper::toSummary).getContent(), page, size, result.getTotalElements());
    }

    @Transactional
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "状态不能为空");
        }
        user.setStatus(request.getStatus());
        
        if (request.getStatus() == AccountStatus.DISABLED) {
            // 记录封禁信息
            user.setDisabledReason(request.getReason());
            user.setDisabledAt(LocalDateTime.now());
            user.setDisabledBy(SecurityUtils.getCurrentAdminId());
        } else {
            // 解封时清空封禁信息
            user.setDisabledReason(null);
            user.setDisabledAt(null);
            user.setDisabledBy(null);
        }
    }

    @Transactional
    public void finalizeUserDeletion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        accountDeletionService.finalizeDeletionNow(user);
    }

    @Transactional
    public void cancelUserDeletion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setDeleteRequested(false);
        user.setDeleteReason(null);
        user.setDeleteScheduleTime(null);
    }

    @Transactional
    public BatchOperationResult batchReviewProducts(BatchProductReviewRequest request) {
        List<Long> ids = request.getProductIds();
        long success = 0;
        long failed = 0;

        for (Long id : ids) {
            if (id == null) {
                failed++;
                continue;
            }
            try {
                productService.reviewProduct(id, Boolean.TRUE.equals(request.getApproved()), request.getReason());
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }

        return BatchOperationResult.builder()
                .successCount(success)
                .failedCount(failed)
                .totalCount(ids == null ? 0 : ids.size())
                .message("批量审核完成")
                .build();
    }

    @Transactional
    public BatchOperationResult batchFinalizeUserDeletion(BatchUserFinalizeDeletionRequest request) {
        List<Long> ids = request.getUserIds();
        long success = 0;
        long failed = 0;

        for (Long id : ids) {
            if (id == null) {
                failed++;
                continue;
            }
            try {
                finalizeUserDeletion(id);
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }

        return BatchOperationResult.builder()
                .successCount(success)
                .failedCount(failed)
                .totalCount(ids == null ? 0 : ids.size())
                .message("批量注销完成")
                .build();
    }
}
