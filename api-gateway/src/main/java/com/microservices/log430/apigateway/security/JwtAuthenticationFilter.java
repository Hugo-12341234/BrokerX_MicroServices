package com.microservices.log430.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        logger.info("Requête entrante sur le chemin : {}", path);
        if (isPublicRoute(path)) {
            logger.debug("Route publique détectée : {}. Authentification ignorée.", path);
            return chain.filter(exchange);
        }
        String token = extractToken(exchange);
        logger.debug("Token extrait : {}", token != null ? "présent" : "absent");
        if (token != null && jwtTokenUtil.validateToken(token)) {
            Long userId = jwtTokenUtil.getUserId(token);
            logger.info("Token valide. userId extrait : {}", userId);
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .build())
                .build();
            return chain.filter(mutatedExchange);
        } else {
            logger.warn("Token absent ou invalide. Accès refusé à la route : {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth") || path.startsWith("/users") || path.startsWith("/register") || path.startsWith("/verify");
    }

    private String extractToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            logger.debug("Token trouvé dans l'en-tête Authorization.");
            return bearer.substring(7);
        }
        if (exchange.getRequest().getCookies().containsKey("jwt")) {
            logger.debug("Token trouvé dans le cookie 'jwt'.");
            return exchange.getRequest().getCookies().getFirst("jwt").getValue();
        }
        logger.debug("Aucun token trouvé dans la requête.");
        return null;
    }
}
