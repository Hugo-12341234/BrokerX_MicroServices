package com.microservices.log430.matchingservice.domain.port.in;

import com.microservices.log430.matchingservice.adapters.web.dto.LastPriceDTO;
import com.microservices.log430.matchingservice.adapters.web.dto.OrderBookDTO;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;

public interface MatchingPort {
    MatchingResult matchOrder(OrderBook orderBook);
    OrderBook modifyOrder(String clientOrderId, OrderBook orderDto);
    OrderBook cancelOrder(Long orderId);
    LastPriceDTO getLastPriceBySymbol(String symbol);
    OrderBookDTO getOrderBookBySymbol(String symbol);
}