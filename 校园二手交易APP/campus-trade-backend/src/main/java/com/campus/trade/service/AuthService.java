package com.campus.trade.service;

import com.campus.trade.config.MailProperties;
import com.campus.trade.dto.admin.AdminLoginRequest;
import com.campus.trade.dto.admin.AdminLoginResponse;
import com.campus.trade.dto.auth.JwtLoginResponse;
import com.campus.trade.dto.auth.LoginRequest;
import com.campus.trade.dto.auth.PasswordResetCodeRequest;
import com.campus.trade.dto.auth.RegisterRequest;
import com.campus.trade.dto.auth.ResendVerificationEmailRequest;
import com.campus.trade.dto.auth.ResetPasswordRequest;
import com.campus.trade.dto.auth.VerifyEmailRequest;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.entity.VerificationToken;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.model.enums.VerificationTokenType;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.repository.AdminRepository;
import com.campus.trade.security.CustomUserDetails;
import com.campus.trade.security.JwtTokenProvider;
import com.campus.trade.util.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofMinutes(30);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final AccountDeletionService accountDeletionService;
    private final AdminService adminService;

    public AuthService(UserRepository userRepository,
                       AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       VerificationTokenService verificationTokenService,
                       EmailService emailService,
                       MailProperties mailProperties,
                       AccountDeletionService accountDeletionService,
                       AdminService adminService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
        this.mailProperties = (mailProperties != null ? mailProperties : new MailProperties());
        this.accountDeletionService = accountDeletionService;
        this.adminService = adminService;
    }

    @Transactional
    public UserSummary register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }
        // 限制用户名不能以admin开头
        if (request.getUsername().toLowerCase().startsWith("admin")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名不能以admin开头");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());

        // 课题/离线模式：允许先验证邮箱、后注册
        // 只要在 verification_tokens 里有“已验证且未过期”的记录，就视为已验证邮箱
        user.setEmailVerified(verificationTokenService.hasValidVerifiedEmail(request.getEmail()));
        userRepository.save(user);
        
        String email = user.getEmail();

        // 未验证邮箱才生成验证码；验证码只入库，不要求真实发送
        if (!user.isEmailVerified()) {
            VerificationToken token = verificationTokenService.createToken(
                user, email, VerificationTokenType.EMAIL_VERIFICATION, EMAIL_VERIFICATION_TTL);
            log.info("[LocalVerificationCode] type={}, email={}, code={}", VerificationTokenType.EMAIL_VERIFICATION, email, token.getToken());
            // 仅当启用真实邮件时才发送，避免离线模式下渲染模板等额外开销
            if (mailProperties.isEnabled()) {
                runAfterCommitOrNow(() -> emailService.sendVerificationCode(user, token.getToken(), VerificationTokenType.EMAIL_VERIFICATION));
            }
        }
        
        return UserMapper.toSummary(user);
    }

    @Transactional
    public JwtLoginResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail();
        String password = request.getPassword();
        
        log.info("[登录调试] 开始用户登录流程 - 用户名/邮箱: {}, 密码长度: {}", usernameOrEmail, password != null ? password.length() : 0);
        
        try {
            // 使用用户名或邮箱进行认证
            log.info("[登录调试] 开始Spring Security认证 - 用户名/邮箱: {}", usernameOrEmail);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, password)
            );
            log.info("[登录调试] Spring Security认证成功 - Principal: {}", authentication.getPrincipal());
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            log.info("[登录调试] 获取到用户详情 - 用户名: {}", userDetails.getUsername());
            
            // 先尝试通过用户名查找，再尝试通过邮箱查找
            log.info("[登录调试] 开始查询用户数据库 - 用户名: {}", userDetails.getUsername());
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseGet(() -> {
                        log.info("[登录调试] 用户名查找失败，尝试通过邮箱查找: {}", usernameOrEmail);
                        return userRepository.findByEmail(usernameOrEmail)
                                .orElseThrow(() -> {
                                    log.error("[登录调试] 用户查询失败 - 用户名: {}, 邮箱: {}", userDetails.getUsername(), usernameOrEmail);
                                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                                });
                    });
            
            log.info("[登录调试] 找到用户 - ID: {}, 用户名: {}, 邮箱验证状态: {}, 账户状态: {}", 
                    user.getId(), user.getUsername(), user.isEmailVerified(), user.getStatus());
            
            if (!user.isEmailVerified()) {
                log.warn("[登录调试] 用户邮箱未验证 - 用户: {}", user.getUsername());
                throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "请先完成邮箱验证");
            }
            if (user.getStatus() == AccountStatus.DISABLED) {
                log.warn("[登录调试] 账户已停用 - 用户: {}", user.getUsername());
                throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已停用，请联系管理员");
            }
            if (user.isDeleteRequested()) {
                if (user.getDeleteScheduleTime() != null && user.getDeleteScheduleTime().isBefore(LocalDateTime.now())) {
                    accountDeletionService.finalizeDeletionNow(user);
                    throw new BusinessException(ErrorCode.ACCOUNT_DELETED, "账号已完成注销");
                }
                throw new BusinessException(ErrorCode.DELETE_REQUEST_PENDING, "账号注销倒计时中，暂无法登录");
            }
            
            user.setLastLogin(LocalDateTime.now());
            String token = jwtTokenProvider.generateToken(userDetails);
            UserSummary summary = UserMapper.toSummary(user);
            
            log.info("[登录调试] 用户登录成功 - 用户名: {}, 角色: {}, Token长度: {}", 
                    user.getUsername(), user.getRole(), token.length());
            
            return new JwtLoginResponse(token, jwtTokenProvider.getProperties().getExpirationSeconds(), summary);
            
        } catch (BusinessException ex) {
            log.warn("[登录调试] 用户登录失败 - 错误码: {}, 错误信息: {}, 开始尝试管理员登录", 
                    ex.getErrorCode(), ex.getMessage());
            
            // 如果是用户不存在或密码错误，尝试使用管理员登录
            if (ex.getErrorCode() == ErrorCode.INVALID_CREDENTIALS || ex.getErrorCode() == ErrorCode.USER_NOT_FOUND) {
                try {
                    log.info("[登录调试] 开始尝试管理员登录 - 用户名: {}, 密码长度: {}", usernameOrEmail, password.length());
                    
                    // 尝试使用管理员登录
                    AdminLoginRequest adminLoginRequest = new AdminLoginRequest();
                    adminLoginRequest.setUsername(usernameOrEmail);
                    adminLoginRequest.setPassword(password);
                    
                    log.info("[登录调试] 调用AdminService.login() - AdminLoginRequest: 用户名={}, 密码长度={}", 
                            adminLoginRequest.getUsername(), adminLoginRequest.getPassword().length());
                    
                    AdminLoginResponse adminResponse = adminService.login(adminLoginRequest);
                    
                    log.info("[登录调试] 管理员登录成功 - Token长度: {}, 过期时间: {}", 
                            adminResponse.getAccessToken().length(), adminResponse.getExpiresIn());
                    
                    // 转换管理员信息为UserSummary格式
                    UserSummary adminSummary = new UserSummary();
                    adminSummary.setUsername(adminLoginRequest.getUsername());
                    adminSummary.setEmail(adminLoginRequest.getUsername()); // 使用用户名作为邮箱
                    adminSummary.setRole(UserRole.ADMIN); // 管理员角色，使用正确的ADMIN角色
                    adminSummary.setEmailVerified(true);
                    
                    log.info("[登录调试] 管理员登录完成 - 返回UserSummary: 用户名={}, 角色={}, 邮箱已验证={}", 
                            adminSummary.getUsername(), adminSummary.getRole(), adminSummary.isEmailVerified());
                    
                    return new JwtLoginResponse(adminResponse.getAccessToken(), adminResponse.getExpiresIn(), adminSummary);
                    
                } catch (BusinessException adminEx) {
                    log.error("[登录调试] 管理员登录失败 - 错误码: {}, 错误信息: {}", 
                            adminEx.getErrorCode(), adminEx.getMessage());
                    // 管理员登录也失败，抛出原始异常
                    throw ex;
                }
            }
            log.error("[登录调试] 非用户不存在或密码错误异常 - 错误码: {}, 错误信息: {}", ex.getErrorCode(), ex.getMessage());
            throw ex;
            
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("[登录调试] Spring Security认证异常 - 异常类型: {}, 错误信息: {}, 开始尝试管理员登录", 
                    e.getClass().getSimpleName(), e.getMessage());
            
            // 尝试使用管理员登录
            try {
                log.info("[登录调试] 开始尝试管理员登录(AuthenticationException) - 用户名: {}, 密码长度: {}", usernameOrEmail, password.length());
                
                AdminLoginRequest adminLoginRequest = new AdminLoginRequest();
                adminLoginRequest.setUsername(usernameOrEmail);
                adminLoginRequest.setPassword(password);
                
                log.info("[登录调试] 调用AdminService.login() - AdminLoginRequest: 用户名={}, 密码长度={}", 
                        adminLoginRequest.getUsername(), adminLoginRequest.getPassword().length());
                
                AdminLoginResponse adminResponse = adminService.login(adminLoginRequest);
                
                log.info("[登录调试] 管理员登录成功 - Token长度: {}, 过期时间: {}", 
                        adminResponse.getAccessToken().length(), adminResponse.getExpiresIn());
                
                // 转换管理员信息为UserSummary格式
                UserSummary adminSummary = new UserSummary();
                adminSummary.setUsername(adminLoginRequest.getUsername());
                adminSummary.setEmail(adminLoginRequest.getUsername()); // 使用用户名作为邮箱
                adminSummary.setRole(UserRole.ADMIN); // 管理员角色，使用正确的ADMIN角色
                adminSummary.setEmailVerified(true);
                
                log.info("[登录调试] 管理员登录完成 - 返回UserSummary: 用户名={}, 角色={}, 邮箱已验证={}", 
                        adminSummary.getUsername(), adminSummary.getRole(), adminSummary.isEmailVerified());
                
                return new JwtLoginResponse(adminResponse.getAccessToken(), adminResponse.getExpiresIn(), adminSummary);
                
            } catch (BusinessException adminEx) {
                log.error("[登录调试] 管理员登录失败(AuthenticationException) - 错误码: {}, 错误信息: {}", 
                        adminEx.getErrorCode(), adminEx.getMessage());
                // 管理员登录也失败，根据异常类型返回不同的错误信息
                if (e instanceof org.springframework.security.authentication.DisabledException) {
                    throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已停用，请联系管理员");
                } else if (e instanceof org.springframework.security.core.userdetails.UsernameNotFoundException) {
                    throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
                } else {
                    throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "邮箱或密码错误");
                }
            }
        }
    }

    @Transactional
    public String resendVerificationEmail(ResendVerificationEmailRequest request) {
        String email = request.getEmail();
        // 查找用户，但不抛出异常
        User user = userRepository.findByEmail(email)
                .orElse(null);
        // 如果用户存在且邮箱已验证，则抛出异常
        if (user != null && user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱已完成验证");
        }
        // 先生成验证码并保存到数据库
        VerificationToken token = verificationTokenService.createToken(
                user, email, VerificationTokenType.EMAIL_VERIFICATION, EMAIL_VERIFICATION_TTL);

        // 课题/离线模式：不实际发送，直接在日志提示（同时验证码已写入数据库）
        log.info("[LocalVerificationCode] type={}, email={}, code={}", VerificationTokenType.EMAIL_VERIFICATION, email, token.getToken());

        // 只有当启用真实邮件，且用户存在且邮箱不为空时，才发送邮件；并确保提交后发送
        if (mailProperties.isEnabled()) {
            runAfterCommitOrNow(() -> {
                if (user != null && StringUtils.hasText(user.getEmail())) {
                    emailService.sendVerificationCode(user, token.getToken(), VerificationTokenType.EMAIL_VERIFICATION);
                }
            });
        }

        return token.getToken();
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        VerificationToken token = verificationTokenService.validateOrThrow(
                user, email, request.getVerificationCode(), VerificationTokenType.EMAIL_VERIFICATION);
        verificationTokenService.markUsed(token);

        // 用户存在则直接标记邮箱已验证；用户不存在代表“先验证邮箱、后注册”的流程
        if (user != null) {
            user.setEmailVerified(true);
            log.info("User {} completed email verification", user.getId());
        } else {
            log.info("Email {} verified before registration", email);
        }
    }

    @Transactional
    public String requestPasswordReset(PasswordResetCodeRequest request) {
        String email = request.getEmail();
        // 查找用户，但不抛出异常
        User user = userRepository.findByEmail(email)
                .orElse(null);
        // 无论用户是否存在，都生成验证码并保存到数据库
        VerificationToken token = verificationTokenService.createToken(
                user, email, VerificationTokenType.PASSWORD_RESET, PASSWORD_RESET_TTL);

        // 课题/离线模式：不实际发送，直接在日志提示（同时验证码已写入数据库）
        log.info("[LocalVerificationCode] type={}, email={}, code={}", VerificationTokenType.PASSWORD_RESET, email, token.getToken());

        // 只有当启用真实邮件，且用户存在且邮箱已验证时，才发送邮件
        if (mailProperties.isEnabled() && user != null && user.isEmailVerified()) {
            runAfterCommitOrNow(() -> emailService.sendVerificationCode(user, token.getToken(), VerificationTokenType.PASSWORD_RESET));
        }

        return token.getToken();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElse(null);
        try {
            VerificationToken token = verificationTokenService.validateOrThrow(
                    user, email, request.getVerificationCode(), VerificationTokenType.PASSWORD_RESET);
            // 只有当验证码验证成功且用户真实存在时，才重置密码
            if (user != null) {
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                verificationTokenService.markUsed(token);
            } else {
                // 如果用户不存在，抛出验证码无效的错误
                throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "验证码不存在");
            }
        } catch (BusinessException e) {
            throw e;
        }
    }

    private void runAfterCommitOrNow(Runnable action) {
        if (action == null) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception ex) {
                        log.warn("Post-commit action failed", ex);
                    }
                }
            });
            return;
        }

        try {
            action.run();
        } catch (Exception ex) {
            log.warn("Immediate action failed", ex);
        }
    }

    // sendEmailVerification方法已被整合到resendVerificationEmail方法中，不再需要独立的sendEmailVerification方法
}
