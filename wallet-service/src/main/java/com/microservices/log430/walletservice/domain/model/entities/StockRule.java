package com.microservices.log430.walletservice.domain.model.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockRule {
    private String symbol;
    private BigDecimal tickSize;
    private BigDecimal minBand;
    private BigDecimal maxBand;
    private LocalDateTime updatedAt;

    public StockRule() {}

    public StockRule(String symbol, BigDecimal tickSize, BigDecimal minBand, BigDecimal maxBand, LocalDateTime updatedAt) {
        this.symbol = symbol;
        this.tickSize = tickSize;
        this.minBand = minBand;
        this.maxBand = maxBand;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getTickSize() { return tickSize; }
    public void setTickSize(BigDecimal tickSize) { this.tickSize = tickSize; }
    public BigDecimal getMinBand() { return minBand; }
    public void setMinBand(BigDecimal minBand) { this.minBand = minBand; }
    public BigDecimal getMaxBand() { return maxBand; }
    public void setMaxBand(BigDecimal maxBand) { this.maxBand = maxBand; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

