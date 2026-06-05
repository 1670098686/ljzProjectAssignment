package com.mall.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:3000",
        "http://localhost",
        "http://127.0.0.1:3000",
        "http://127.0.0.1"
    );

    private static final String DEFAULT_ALLOWED_ORIGIN = "http://localhost:3000";

    private static final long MAX_AGE = 3600L;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin == null) {
            return chain.filter(exchange);
        }

        String allowedOrigin = getAllowedOrigin(origin);

        HttpHeaders requestHeaders = request.getHeaders();
        if (CorsUtils.isCorsRequest(request)) {
            String requestMethod = requestHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            List<String> requestHeadersList = requestHeaders.get(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                requestMethod != null ? requestMethod : "GET,POST,PUT,DELETE,OPTIONS");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(MAX_AGE));

            if (requestHeadersList != null && !requestHeadersList.isEmpty()) {
                response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    String.join(", ", requestHeadersList));
            }

            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                "Content-Disposition");

            if (request.getMethod() == HttpMethod.OPTIONS) {
                return response.setComplete();
            }
        }

        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            "Content-Disposition");

        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-Forwarded-Host", getRemoteHost(request))
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private String getAllowedOrigin(String origin) {
        if (ALLOWED_ORIGINS.contains(origin)) {
            return origin;
        }
        for (String allowed : ALLOWED_ORIGINS) {
            if (origin.startsWith(allowed) || allowed.startsWith(origin)) {
                return allowed;
            }
        }
        return DEFAULT_ALLOWED_ORIGIN;
    }

    private String getRemoteHost(ServerHttpRequest request) {
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "localhost";
    }
}