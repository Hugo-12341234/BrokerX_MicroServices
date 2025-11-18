package com.microservices.log430.matchingservice.domain.port.out;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.adapters.web.dto.LastPriceDTO;
import com.microservices.log430.matchingservice.adapters.web.dto.OrderBookDTO;
import java.util.List;
import java.util.Optional;

public interface OrderBookPort {
    OrderBook save(OrderBook orderBook);
    Optional<OrderBook> findById(Long id);
    Optional<OrderBook> findByClientOrderId(String clientOrderId);
    List<OrderBook> findAllBySymbol(String symbol);
    List<OrderBook> findAll();
    void deleteById(Long id);
    OrderBookDTO getOrderBookSnapshot(String symbol);
    LastPriceDTO getLastExecutionPrice(String symbol);
}
