package com.microservices.log430.orderservice.adapters.external.marketdata;

public class MarketDataUpdateDTO {
    private Double lastPrice;
    private Object orderBook; // Utilise Object pour compatibilité, à ajuster selon le mapping

    public MarketDataUpdateDTO() {}
    public MarketDataUpdateDTO(Double lastPrice, Object orderBook) {
        this.lastPrice = lastPrice;
        this.orderBook = orderBook;
    }
    public Double getLastPrice() { return lastPrice; }
    public void setLastPrice(Double lastPrice) { this.lastPrice = lastPrice; }
    public Object getOrderBook() { return orderBook; }
    public void setOrderBook(Object orderBook) { this.orderBook = orderBook; }
}

