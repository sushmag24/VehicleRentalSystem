package com.vehiclerental.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {
        // Configuration fields if needed
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1. Bypass validation for login and register
            if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
                return chain.filter(exchange);
            }

            // 2. Extract Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization Header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 3. Validate Token & extract claims
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);
                String email = claims.getSubject();

                // 4. Role Authorization Check for Admin APIs
                if (path.contains("/api/admin/") && !"ADMIN".equalsIgnoreCase(role)) {
                    return onError(exchange, "Access denied. Admin role required.", HttpStatus.FORBIDDEN);
                }

                // 5. Mutate Request to add User Context Headers
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", String.valueOf(userId))
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                return onError(exchange, "Invalid/Expired Token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String jsonError = String.format("{\"error\": \"%s\"}", err);
        byte[] bytes = jsonError.getBytes();
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
