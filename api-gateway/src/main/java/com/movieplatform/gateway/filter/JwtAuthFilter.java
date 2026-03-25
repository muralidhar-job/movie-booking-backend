package com.movieplatform.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * API Gateway Pattern — Global JWT Auth Filter.
 * Validates JWT once here; downstream services receive X-User-Id and X-User-Role headers.
 * Design Pattern: Chain of Responsibility (filter chain).
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Paths that bypass JWT validation
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/movies",
        "/api/v1/offers",
        "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Correlation ID — injected for distributed tracing
        String correlationId = UUID.randomUUID().toString();
        log.info("Gateway request: {} {} correlationId={}", request.getMethod(), path, correlationId);

        // Skip auth for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange.mutate()
                .request(r -> r.header("X-Correlation-Id", correlationId))
                .build());
        }

        // Extract and validate JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = parseToken(token);

            String userId = claims.getSubject();
            String role   = claims.get("role", String.class);

            log.info("Authenticated userId={} role={} path={}", userId, role, path);

            // Mutate request: forward user context to downstream services
            ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id",       userId)
                .header("X-User-Role",     role)
                .header("X-Correlation-Id", correlationId)
                .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() { return -1; } // Run before all other filters
}
