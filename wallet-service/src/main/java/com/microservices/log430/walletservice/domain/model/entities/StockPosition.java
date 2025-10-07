package com.microservices.log430.walletservice.domain.model.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class StockPosition {
    private UUID id;
    private Wallet wallet;
    private String symbol;
    private Integer quantity;
    private BigDecimal averagePrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StockPosition() {}

    public StockPosition(UUID id, Wallet wallet, String symbol, Integer quantity, BigDecimal averagePrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.wallet = wallet;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

