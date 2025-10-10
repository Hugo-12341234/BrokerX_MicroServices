package com.microservices.log430.apigateway.errors;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

@Component
public class JsonErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        var err = super.getErrorAttributes(request, options.including(ErrorAttributeOptions.Include.MESSAGE));
        return Map.of(
                "timestamp", err.getOrDefault("timestamp", ""),
                "path", err.getOrDefault("path", ""),
                "status", err.getOrDefault("status", 500),
                "error", err.getOrDefault("error", "Internal Server Error"),
                "message", err.getOrDefault("message", "Unexpected error"),
                "requestId", request.exchange().getRequest().getHeaders().getFirst("X-Request-Id") != null ? request.exchange().getRequest().getHeaders().getFirst("X-Request-Id") : ""
        );
    }
}

