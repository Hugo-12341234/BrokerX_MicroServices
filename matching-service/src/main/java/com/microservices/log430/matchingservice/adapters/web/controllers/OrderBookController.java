package com.microservices.log430.matchingservice.adapters.web.controllers;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.service.MatchingService;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;
import com.microservices.log430.matchingservice.adapters.web.dto.OrderDTO;
import com.microservices.log430.matchingservice.adapters.persistence.map.OrderToOrderBookMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/orderbook")
public class OrderBookController {
    private static final Logger logger = LoggerFactory.getLogger(OrderBookController.class);
    private final MatchingService matchingService;

    public OrderBookController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping
    public ResponseEntity<MatchingResult> matchOrder(@RequestBody OrderDTO orderDto) {
        logger.info("Réception d'un nouvel ordre à apparier : clientOrderId={}, symbol={}, side={}, quantity={}, price={}",
                orderDto.clientOrderId, orderDto.symbol, orderDto.side, orderDto.quantity, orderDto.price);
        OrderBook orderBook = OrderToOrderBookMapper.toOrderBook(orderDto);
        MatchingResult result = matchingService.matchOrder(orderBook);
        logger.info("Matching terminé pour clientOrderId={}, statut={}, executions={}",
                result.updatedOrder.getClientOrderId(), result.updatedOrder.getStatus(), result.executions.size());
        return ResponseEntity.ok(result);
    }
}
