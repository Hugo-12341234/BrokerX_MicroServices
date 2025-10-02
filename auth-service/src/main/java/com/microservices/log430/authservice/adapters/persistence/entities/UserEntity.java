package com.microservices.log430.authservice.adapters.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String passwordHash;
    private String name;
    private String adresse;
    private LocalDate dateDeNaissance;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(precision = 15, scale = 2)
    private BigDecimal balance;

    public enum Status { PENDING, ACTIVE, REJECTED, SUSPENDED }

    public UserEntity() {
        this.balance = BigDecimal.ZERO;
    }

    public UserEntity(Long id, String email, String passwordHash, String name, String adresse, LocalDate dateDeNaissance, Status status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.adresse = adresse;
        this.dateDeNaissance = dateDeNaissance;
        this.status = status;
        this.balance = BigDecimal.ZERO;
    }
    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public LocalDate getDateDeNaissance() { return dateDeNaissance; }
    public void setDateDeNaissance(LocalDate dateDeNaissance) { this.dateDeNaissance = dateDeNaissance; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}

