package com.microservices.log430.matchingservice.domain.port.in;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;

public interface MatchingPort {
    MatchingResult matchOrder(OrderBook orderBook);
    OrderBook modifyOrder(String clientOrderId, OrderBook orderDto);
    OrderBook cancelOrder(Long orderId);
}