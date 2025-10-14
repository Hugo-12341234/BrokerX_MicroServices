package com.microservices.log430.matchingservice.adapters.web.controllers;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.service.MatchingService;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;
import com.microservices.log430.matchingservice.adapters.web.dto.OrderDTO;
import com.microservices.log430.matchingservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.matchingservice.adapters.persistence.map.OrderToOrderBookMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/orderbook")
public class OrderBookController {
    private static final Logger logger = LoggerFactory.getLogger(OrderBookController.class);
    private final MatchingService matchingService;

    public OrderBookController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping
    public ResponseEntity<?> matchOrder(@RequestBody OrderDTO orderDto, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        try {
            logger.info("Réception d'un nouvel ordre à apparier : clientOrderId={}, symbol={}, side={}, quantity={}, price={}",
                    orderDto.clientOrderId, orderDto.symbol, orderDto.side, orderDto.quantity, orderDto.price);
            OrderBook orderBook = OrderToOrderBookMapper.toOrderBook(orderDto);
            MatchingResult result = matchingService.matchOrder(orderBook);
            logger.info("Matching terminé pour clientOrderId={}, statut={}, executions={}",
                    result.updatedOrder.getClientOrderId(), result.updatedOrder.getStatus(), result.executions.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Erreur lors du matching de l'ordre clientOrderId={}: {}", orderDto.clientOrderId, e.getMessage(), e);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erreur lors du matching de l'ordre : " + e.getMessage(),
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
