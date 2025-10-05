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

    public enum Status { PENDING, ACTIVE, REJECTED, SUSPENDED }

    public User(Long id, String email, String passwordHash, String name, String adresse, LocalDate dateDeNaissance, Status status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.adresse = adresse;
        this.dateDeNaissance = dateDeNaissance;
        this.status = status;
    }

    public User(Long id, String email, String passwordHash, String name, String adresse, LocalDate dateDeNaissance, Status status, BigDecimal balance) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.adresse = adresse;
        this.dateDeNaissance = dateDeNaissance;
        this.status = status;
    }

    public void activate() { this.status = Status.ACTIVE; }
    public void reject() { this.status = Status.REJECTED; }

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
}

