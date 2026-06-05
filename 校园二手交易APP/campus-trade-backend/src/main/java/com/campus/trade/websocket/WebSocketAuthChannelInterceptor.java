package com.campus.trade.websocket;

import com.campus.trade.security.AdminUserDetailsService;
import com.campus.trade.security.CustomUserDetailsService;
import com.campus.trade.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AdminUserDetailsService adminUserDetailsService;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider tokenProvider,
                                           CustomUserDetailsService userDetailsService,
                                           AdminUserDetailsService adminUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.adminUserDetailsService = adminUserDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor).orElseThrow(() -> new MessagingException("Missing Authorization header"));
        String username = tokenProvider.getUsername(token);
        UserDetails userDetails = resolveUserDetails(username);
        if (!tokenProvider.validateToken(token, userDetails)) {
            throw new MessagingException("Invalid token");
        }
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authenticationToken);
    }

    private Optional<String> resolveToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(tokenHeader)) {
            return Optional.of(tokenHeader);
        }
        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (StringUtils.hasText(accessToken)) {
            return Optional.of(accessToken);
        }
        return Optional.empty();
    }

    private UserDetails resolveUserDetails(String username) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ignored) {
            return adminUserDetailsService.loadUserByUsername(username);
        }
    }
}
