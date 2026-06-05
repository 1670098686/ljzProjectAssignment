package com.campus.trade.service;

import com.campus.trade.config.MailProperties;
import com.campus.trade.dto.auth.JwtLoginResponse;
import com.campus.trade.dto.auth.LoginRequest;
import com.campus.trade.dto.auth.RegisterRequest;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.model.entity.VerificationToken;
import com.campus.trade.model.enums.VerificationTokenType;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.security.CustomUserDetails;
import com.campus.trade.security.JwtProperties;
import com.campus.trade.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private MailProperties mailProperties;

    @Mock
    private AccountDeletionService accountDeletionService;
    @Mock
    private AdminService adminService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("Secret123");
        request.setEmail("alice@example.com");
        request.setPhone("13800138000");
        request.setRole(UserRole.STUDENT);
        return request;
    }

    @Test
    void register_shouldPersistUserWithEncodedPassword() {
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        VerificationToken token = new VerificationToken();
        token.setToken("123456");
        when(verificationTokenService.createToken(any(User.class), anyString(), eq(VerificationTokenType.EMAIL_VERIFICATION), any(Duration.class)))
            .thenReturn(token);
        when(mailProperties.isEnabled()).thenReturn(true);

        UserSummary summary = authService.register(request);

        assertEquals("alice", summary.getUsername());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertEquals("encoded", persisted.getPassword());
        assertEquals(UserRole.STUDENT, persisted.getRole());
        verify(emailService).sendVerificationCode(eq(persisted), eq("123456"), eq(VerificationTokenType.EMAIL_VERIFICATION));
    }

    @Test
    void register_shouldRejectDuplicatedUsername() {
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnTokenAndSummary() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("alice@example.com");
        request.setPassword("Secret123");

        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "alice@example.com",
                "encoded",
                true,
                Collections.emptyList()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        User stored = new User();
        stored.setId(1L);
        stored.setUsername("alice");
        stored.setPassword("encoded");
        stored.setEmail("alice@example.com");
        stored.setRole(UserRole.STUDENT);
        stored.setStatus(AccountStatus.ACTIVE);
        stored.setEmailVerified(true);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateToken(principal)).thenReturn("jwt-token");
        JwtProperties properties = new JwtProperties();
        properties.setExpirationSeconds(3600);
        when(jwtTokenProvider.getProperties()).thenReturn(properties);

        JwtLoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("alice", response.getUser().getUsername());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_shouldThrowBusinessExceptionWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("alice@example.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        // 配置adminService.login()方法抛出BusinessException
        when(adminService.login(any())).thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS, "管理员登录失败"));

        assertThrows(BusinessException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).generateToken(any());
    }
}
