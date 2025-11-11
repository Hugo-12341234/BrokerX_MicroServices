package com.microservices.log430.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayCorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // N'applique le CORS que sur les routes API, pas sur /ws
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/auth/**", config);
        source.registerCorsConfiguration("/order/**", config);
        source.registerCorsConfiguration("/wallet/**", config);
        source.registerCorsConfiguration("/matching/**", config);
        source.registerCorsConfiguration("/notification/**", config);
        // PAS de CORS sur /ws
        return new CorsWebFilter(source);
    }
}
