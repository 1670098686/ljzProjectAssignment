package com.campus.trade.websocket;

import com.campus.trade.security.AdminUserDetailsService;
import com.campus.trade.security.CustomUserDetailsService;
import com.campus.trade.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AdminUserDetailsService adminUserDetailsService;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.withUsername("alice")
                .password("pwd")
                .roles("USER")
                .build();
    }

    @Test
    void preSend_setsPrincipalOnValidToken() {
        when(tokenProvider.getUsername("token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(tokenProvider.validateToken("token", userDetails)).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        StompHeaderAccessor updated = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

        assertNotNull(updated);
        assertNotNull(updated.getUser());
        assertEquals("alice", updated.getUser().getName());
        verify(tokenProvider).getUsername("token");
        verify(tokenProvider).validateToken("token", userDetails);
    }

    @Test
    void preSend_rejectsWhenMissingToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, mock(MessageChannel.class)));
        verify(tokenProvider, never()).getUsername(any());
    }
}
