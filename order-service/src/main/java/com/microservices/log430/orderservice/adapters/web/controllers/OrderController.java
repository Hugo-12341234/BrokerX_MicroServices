package com.microservices.log430.orderservice.adapters.web.controllers;

import com.microservices.log430.orderservice.domain.port.in.OrderPlacementPort;
import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
import com.microservices.log430.orderservice.adapters.web.dto.OrderResponse;
import com.microservices.log430.orderservice.adapters.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderPlacementPort orderPlacementPort;

    @Autowired
    public OrderController(OrderPlacementPort orderPlacementPort) {
        this.orderPlacementPort = orderPlacementPort;
    }

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest orderRequest, HttpServletRequest httpRequest) {
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        logger.info("Réception d'une requête de placement d'ordre. Path: {}, RequestId: {}, X-User-Id: {}", path, requestId, userIdHeader);
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            logger.warn("Header X-User-Id manquant. Path: {}, RequestId: {}", path, requestId);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-User-Id manquant",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException e) {
            logger.error("Header X-User-Id invalide: {}. Path: {}, RequestId: {}", userIdHeader, path, requestId);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-User-Id invalide",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        try {
            logger.info("Placement de l'ordre pour l'utilisateur {}", userId);
            OrderPlacementPort.OrderPlacementResult result = orderPlacementPort.placeOrder(new OrderPlacementPort.OrderPlacementRequest(orderRequest, userId));
            OrderResponse orderResponse = result.toOrderResponse();
            if ("ACCEPTE".equals(result.getStatus())) {
                logger.info("Ordre accepté pour l'utilisateur {}. OrderId: {}", userId, orderResponse.getId());
                return ResponseEntity.ok(orderResponse);
            } else {
                logger.warn("Ordre rejeté pour l'utilisateur {}. Raison: {}", userId, result.getMessage());
                ErrorResponse err = new ErrorResponse(
                    Instant.now(),
                    path,
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    result.getMessage() != null ? result.getMessage() : "Ordre rejeté",
                    requestId != null ? requestId : ""
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
        } catch (Exception e) {
            logger.error("Erreur lors du placement de l'ordre pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erreur lors du placement de l'ordre : " + e.getMessage(),
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
