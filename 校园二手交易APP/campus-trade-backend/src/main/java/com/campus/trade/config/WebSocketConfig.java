package com.campus.trade.config;

import com.campus.trade.websocket.WebSocketAuthChannelInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final WebSocketProperties properties;

    public WebSocketConfig(WebSocketAuthChannelInterceptor authChannelInterceptor,
                           WebSocketProperties properties) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String endpoint = properties.getEndpoint();
        String[] allowedOrigins = toArray(properties.getAllowedOriginPatterns());
        HandshakeHandler handshakeHandler = defaultHandshakeHandler();
        HttpSessionHandshakeInterceptor handshakeInterceptor = new HttpSessionHandshakeInterceptor();
        handshakeInterceptor.setCreateSession(true);

        registry.addEndpoint(endpoint)
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
        registry.addEndpoint(endpoint)
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        String[] brokerPrefixes = toArray(properties.getSimpleBrokerPrefixes());
        long[] heartbeat = {
                properties.getHeartbeat().getServer().toMillis(),
                properties.getHeartbeat().getClient().toMillis()
        };
        registry.enableSimpleBroker(brokerPrefixes)
                .setHeartbeatValue(heartbeat)
                .setTaskScheduler(stompHeartbeatScheduler());
        registry.setUserDestinationPrefix(properties.getUserDestinationPrefix());
        registry.setApplicationDestinationPrefixes(
                toArray(properties.getApplicationDestinationPrefixes()));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
        configureTaskExecutor(registration, properties.getInbound());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        configureTaskExecutor(registration, properties.getOutbound());
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(properties.getTransport().getMessageSizeLimit());
        registry.setSendBufferSizeLimit(properties.getTransport().getSendBufferSizeLimit());
        registry.setSendTimeLimit(properties.getTransport().getSendTimeLimit());
    }

    @Bean(name = "stompHeartbeatScheduler")
    public TaskScheduler stompHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    private void configureTaskExecutor(ChannelRegistration registration, WebSocketProperties.ChannelPool pool) {
        registration.taskExecutor()
                .corePoolSize(pool.getCorePoolSize())
                .maxPoolSize(pool.getMaxPoolSize())
                .queueCapacity(pool.getQueueCapacity());
    }

    private HandshakeHandler defaultHandshakeHandler() {
        return new DefaultHandshakeHandler();
    }

    private String[] toArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new String[0];
        }
        return values.toArray(new String[0]);
    }
}
