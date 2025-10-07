package com.microservices.log430.walletservice.adapters.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_rule")
public class StockRuleEntity {
    @Id
    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal tickSize;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal minBand;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal maxBand;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public StockRuleEntity() {}

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
