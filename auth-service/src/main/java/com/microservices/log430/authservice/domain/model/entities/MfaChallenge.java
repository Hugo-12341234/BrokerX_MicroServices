package com.microservices.log430.authservice.domain.model.entities;

import java.time.LocalDateTime;

public class MfaChallenge {
    private Long id;
    private Long userId;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean used;
    private String ipAddress;
    private int failedAttempts = 0;
    private LocalDateTime lockedUntil;

    public MfaChallenge() {}

    public MfaChallenge(Long userId, String code, LocalDateTime createdAt, LocalDateTime expiresAt, String ipAddress, int failedAttempts, LocalDateTime lockedUntil) {
        this.userId = userId;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.used = false;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
    }
}
