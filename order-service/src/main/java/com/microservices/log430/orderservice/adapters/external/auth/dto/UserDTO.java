package com.microservices.log430.orderservice.adapters.external.auth.dto;

import java.time.LocalDate;

public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String adresse;
    private LocalDate dateDeNaissance;
    private String status;

    public UserDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public LocalDate getDateDeNaissance() { return dateDeNaissance; }
    public void setDateDeNaissance(LocalDate dateDeNaissance) { this.dateDeNaissance = dateDeNaissance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

