package com.mall.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class HeaderForwardFilter implements GlobalFilter, Ordered {

    private static final String[] FORWARD_HEADERS = {
        "X-User-Id",
        "Authorization",
        "X-Token",
        "X-Request-Id"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        ServerHttpRequest.Builder builder = request.mutate();

        for (String headerName : FORWARD_HEADERS) {
            String headerValue = headers.getFirst(headerName);
            if (headerValue != null) {
                builder.header(headerName, headerValue);
            }
        }

        ServerHttpRequest modifiedRequest = builder.build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -90;
    }
}