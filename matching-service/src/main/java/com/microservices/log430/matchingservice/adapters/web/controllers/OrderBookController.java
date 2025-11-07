package com.microservices.log430.matchingservice.adapters.web.controllers;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.port.in.MatchingPort;
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
    private final MatchingPort matchingPort;

    public OrderBookController(MatchingPort matchingPort) {this.matchingPort = matchingPort;}

    @PostMapping
    public ResponseEntity<?> matchOrder(@RequestBody OrderDTO orderDto, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        try {
            logger.info("Réception d'un nouvel ordre à apparier : clientOrderId={}, symbol={}, side={}, quantity={}, price={}, version={}",
                    orderDto.clientOrderId, orderDto.symbol, orderDto.side, orderDto.quantity, orderDto.price, orderDto.version);
            OrderBook orderBook = OrderToOrderBookMapper.toOrderBook(orderDto);
            logger.info("OrderBook : clientOrderId={}, symbol={}, side={}, quantity={}, price={}, version={}",
                    orderBook.getClientOrderId(), orderBook.getSymbol(), orderBook.getSide(), orderBook.getQuantity(), orderBook.getPrice(), orderBook.getVersion());
            MatchingResult result = matchingPort.matchOrder(orderBook);
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

    @PutMapping("/{clientOrderId}")
    public ResponseEntity<?> modifyOrder(@PathVariable String clientOrderId, @RequestBody OrderDTO orderDto, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        try {
            OrderBook orderBook = OrderToOrderBookMapper.toOrderBook(orderDto);
            OrderBook updatedOrder = matchingPort.modifyOrder(clientOrderId, orderBook);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(Instant.now(), path, 404, "Not Found", ex.getMessage(), requestId != null ? requestId : ""));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(Instant.now(), path, 400, "Bad Request", ex.getMessage(), requestId != null ? requestId : ""));
        } catch (Exception e) {
            logger.error("Erreur lors de la modification de l'ordre {}: {}", clientOrderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(Instant.now(), path, 500, "Internal Server Error", "Erreur lors de la modification de l'ordre : " + e.getMessage(), requestId != null ? requestId : ""));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        try {
            OrderBook cancelledOrder = matchingPort.cancelOrder(orderId);
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
