package com.microservices.log430.matchingservice.adapters.web.dto;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import java.util.List;

public class OrderBookDTO {
    private String symbol;
    private List<OrderBook> orders;

    public OrderBookDTO(String symbol, List<OrderBook> orders) {
        this.symbol = symbol;
        this.orders = orders;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public List<OrderBook> getOrders() { return orders; }
    public void setOrders(List<OrderBook> orders) { this.orders = orders; }
}