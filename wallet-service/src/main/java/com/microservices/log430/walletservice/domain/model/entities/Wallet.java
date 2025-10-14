package com.microservices.log430.walletservice.domain.model.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Wallet {
    private UUID id;
    private Long userId;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StockPosition> stockPositions;

    public Wallet() {}

    public Wallet(UUID id, Long userId, BigDecimal balance, LocalDateTime createdAt, LocalDateTime updatedAt, List<StockPosition> stockPositions) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stockPositions = stockPositions;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<StockPosition> getStockPositions() { return stockPositions; }
    public void setStockPositions(List<StockPosition> stockPositions) { this.stockPositions = stockPositions; }

    public void updateStockQuantity(String symbol, int quantityChange) {
        if (stockPositions == null) return;
        StockPosition position = null;
        for (StockPosition sp : stockPositions) {
            if (sp.getSymbol().equals(symbol)) {
                position = sp;
                break;
            }
        }
        if (position != null) {
            int newQty = position.getQuantity() + quantityChange;
            if (newQty <= 0) {
                stockPositions.remove(position);
            } else {
                position.setQuantity(newQty);
                position.setUpdatedAt(java.time.LocalDateTime.now());
            }
        } else if (quantityChange > 0) {
            StockPosition newPosition = new StockPosition();
            newPosition.setSymbol(symbol);
            newPosition.setQuantity(quantityChange);
            newPosition.setAveragePrice(BigDecimal.ZERO); // à adapter si besoin
            newPosition.setCreatedAt(java.time.LocalDateTime.now());
            newPosition.setUpdatedAt(java.time.LocalDateTime.now());
            newPosition.setWallet(this);
            stockPositions.add(newPosition);
        }
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
