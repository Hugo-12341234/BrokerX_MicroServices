package com.microservices.log430.authservice.domain.model.entities;

import java.time.LocalDateTime;

public class VerificationToken {
    private Long id;
    private String tokenHash;
    private User user;
    private LocalDateTime expiryDate;
    public VerificationToken() {}
    public VerificationToken(Long id, String tokenHash, User user, LocalDateTime expiryDate) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiryDate = expiryDate;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}
