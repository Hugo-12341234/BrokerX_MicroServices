package com.microservices.log430.walletservice.domain.model.entities;

import java.time.LocalDateTime;

public class WalletAudit {
    private Long id;
    private Long userId;
    private String details;
    private LocalDateTime createdAt;

    public WalletAudit() {}

    public WalletAudit(Long userId, String details, LocalDateTime createdAt) {
        this.userId = userId;
        this.details = details;
        this.createdAt = createdAt;
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
