package com.microservices.log430.apigateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }
        String token = extractToken(exchange);
        if (token != null && jwtTokenUtil.validateToken(token)) {
            // Extraire le userId du token
            Long userId = jwtTokenUtil.getUserId(token);
            // Ajouter le header X-User-Id
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .build())
                .build();
            return chain.filter(mutatedExchange);
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // Priorité haute
    }

    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth") || path.startsWith("/users") || path.startsWith("/register") || path.startsWith("/verify");
    }

    private String extractToken(ServerWebExchange exchange) {
        // Cherche dans l'en-tête Authorization
        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // Cherche dans les cookies
        if (exchange.getRequest().getCookies().containsKey("jwt")) {
            return exchange.getRequest().getCookies().getFirst("jwt").getValue();
        }
        return null;
    }
}
