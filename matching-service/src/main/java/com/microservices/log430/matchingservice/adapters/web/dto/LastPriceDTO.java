package com.microservices.log430.matchingservice.adapters.web.dto;

public class LastPriceDTO {
    private String symbol;
    private Double lastPrice;
    private String timestamp;

    // Constructeur par défaut
    public LastPriceDTO() {}

    public LastPriceDTO(String symbol, Double lastPrice, String timestamp) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Double getLastPrice() { return lastPrice; }
    public void setLastPrice(Double lastPrice) { this.lastPrice = lastPrice; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}