package com.microservices.log430.marketdataservice.adapters.external.matching;

import java.util.List;

public class OrderBookDTO {
    private String symbol;
    private List<Object> orders; // Utilise Object pour compatibilité, à ajuster selon le mapping

    public OrderBookDTO(String symbol, List<Object> orders) {
        this.symbol = symbol;
        this.orders = orders;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public List<Object> getOrders() { return orders; }
    public void setOrders(List<Object> orders) { this.orders = orders; }
}