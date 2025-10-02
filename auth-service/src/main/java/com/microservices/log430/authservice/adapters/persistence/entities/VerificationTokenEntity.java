package com.microservices.log430.authservice.adapters.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_token")
public class VerificationTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tokenHash;
    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    private LocalDateTime expiryDate;
    public VerificationTokenEntity() {}
    public VerificationTokenEntity(Long id, String tokenHash, UserEntity user, LocalDateTime expiryDate) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiryDate = expiryDate;
    }
    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}

