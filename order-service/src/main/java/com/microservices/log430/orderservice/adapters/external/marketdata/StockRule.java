package com.microservices.log430.orderservice.adapters.external.marketdata;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockRule implements Serializable {
    private String symbol;
    private BigDecimal tickSize;
    private BigDecimal minBand;
    private BigDecimal maxBand;
    private BigDecimal price;
    private LocalDateTime updatedAt;

    public StockRule() {}

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getTickSize() { return tickSize; }
    public void setTickSize(BigDecimal tickSize) { this.tickSize = tickSize; }
    public BigDecimal getMinBand() { return minBand; }
    public void setMinBand(BigDecimal minBand) { this.minBand = minBand; }
    public BigDecimal getMaxBand() { return maxBand; }
    public void setMaxBand(BigDecimal maxBand) { this.maxBand = maxBand; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
