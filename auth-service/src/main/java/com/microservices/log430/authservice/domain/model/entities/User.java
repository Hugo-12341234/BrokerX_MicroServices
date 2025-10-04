package com.microservices.log430.authservice.domain.model.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private String name;
    private String adresse;
    private LocalDate dateDeNaissance;
    private Status status;
    private BigDecimal balance;

    public enum Status { PENDING, ACTIVE, REJECTED, SUSPENDED }

    public User() {
        this.balance = BigDecimal.ZERO;
    }

    public User(Long id, String email, String passwordHash, String name, String adresse, LocalDate dateDeNaissance, Status status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.adresse = adresse;
        this.dateDeNaissance = dateDeNaissance;
        this.status = status;
        this.balance = BigDecimal.ZERO;
    }

    public User(Long id, String email, String passwordHash, String name, String adresse, LocalDate dateDeNaissance, Status status, BigDecimal balance) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.adresse = adresse;
        this.dateDeNaissance = dateDeNaissance;
        this.status = status;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public void activate() { this.status = Status.ACTIVE; }
    public void reject() { this.status = Status.REJECTED; }

    public void creditBalance(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getAdresse() { return adresse; }
    public LocalDate getDateDeNaissance() { return dateDeNaissance; }
    public Status getStatus() { return status; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setDateDeNaissance(LocalDate dateDeNaissance) { this.dateDeNaissance = dateDeNaissance; }
    public void setStatus(Status status) { this.status = status; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance != null ? balance : BigDecimal.ZERO; }
}

