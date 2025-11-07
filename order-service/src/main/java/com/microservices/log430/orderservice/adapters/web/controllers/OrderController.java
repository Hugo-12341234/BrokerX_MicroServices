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
        String clientOrderId = httpRequest.getHeader("Idempotency-Key");
        logger.info("Réception d'une requête de placement d'ordre. Path: {}, RequestId: {}, X-User-Id: {}, clientOrderId: {}", path, requestId, userIdHeader, clientOrderId);
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
            OrderPlacementPort.OrderPlacementResult result = orderPlacementPort.placeOrder(new OrderPlacementPort.OrderPlacementRequest(orderRequest, userId), clientOrderId);
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

    @PutMapping("/{orderId}")
    public ResponseEntity<?> modifyOrder(@PathVariable Long orderId, @RequestBody OrderRequest orderRequest, HttpServletRequest httpRequest) {
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        logger.info("Réception d'une requête de modification d'ordre. Path: {}, RequestId: {}, X-User-Id: {}", path, requestId, userIdHeader);
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            ErrorResponse err = new ErrorResponse(Instant.now(), path, HttpStatus.BAD_REQUEST.value(), "Bad Request", "Header X-User-Id manquant", requestId != null ? requestId : "");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        Long userId;
        try { userId = Long.valueOf(userIdHeader); } catch (NumberFormatException e) {
            ErrorResponse err = new ErrorResponse(Instant.now(), path, HttpStatus.BAD_REQUEST.value(), "Bad Request", "Header X-User-Id invalide", requestId != null ? requestId : "");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        try {
            OrderResponse updatedOrder = orderPlacementPort.modifyOrder(orderId, orderRequest, userId);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(Instant.now(), path, 404, "Not Found", ex.getMessage(), requestId != null ? requestId : ""));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(Instant.now(), path, 400, "Bad Request", ex.getMessage(), requestId != null ? requestId : ""));
        } catch (Exception e) {
            logger.error("Erreur lors de la modification de l'ordre {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(Instant.now(), path, 500, "Internal Server Error", "Erreur lors de la modification de l'ordre : " + e.getMessage(), requestId != null ? requestId : ""));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        logger.info("Réception d'une requête d'annulation d'ordre. Path: {}, RequestId: {}, X-User-Id: {}", path, requestId, userIdHeader);
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            ErrorResponse err = new ErrorResponse(Instant.now(), path, HttpStatus.BAD_REQUEST.value(), "Bad Request", "Header X-User-Id manquant", requestId != null ? requestId : "");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        Long userId;
        try { userId = Long.valueOf(userIdHeader); } catch (NumberFormatException e) {
            ErrorResponse err = new ErrorResponse(Instant.now(), path, HttpStatus.BAD_REQUEST.value(), "Bad Request", "Header X-User-Id invalide", requestId != null ? requestId : "");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        try {
            OrderResponse cancelledOrder = orderPlacementPort.cancelOrder(orderId);
            return ResponseEntity.ok(cancelledOrder);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(Instant.now(), path, 404, "Not Found", ex.getMessage(), requestId != null ? requestId : ""));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(Instant.now(), path, 400, "Bad Request", ex.getMessage(), requestId != null ? requestId : ""));
        } catch (Exception e) {
            logger.error("Erreur lors de l'annulation de l'ordre {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(Instant.now(), path, 500, "Internal Server Error", "Erreur lors de l'annulation de l'ordre : " + e.getMessage(), requestId != null ? requestId : ""));
        }
    }
}
