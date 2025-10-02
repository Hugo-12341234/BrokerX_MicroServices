package com.microservices.log430.authservice.adapters.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mfa_challenges")
public class MfaChallengeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public MfaChallengeEntity() {}

    public MfaChallengeEntity(Long userId, String code, LocalDateTime createdAt, LocalDateTime expiresAt,
                              boolean used, String ipAddress, int failedAttempts, LocalDateTime lockedUntil) {
        this.userId = userId;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
        this.ipAddress = ipAddress;
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
}
