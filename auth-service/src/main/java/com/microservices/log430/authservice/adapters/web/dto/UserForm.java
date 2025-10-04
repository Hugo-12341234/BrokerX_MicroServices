package com.microservices.log430.authservice.adapters.web.dto;

import java.time.LocalDate;

public class UserForm {
    private String email;
    private String password;
    private String name;
    private String adresse;
    private LocalDate dateDeNaissance;

    // getters/setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public LocalDate getDateDeNaissance() { return dateDeNaissance; }
    public void setDateDeNaissance(LocalDate dateDeNaissance) { this.dateDeNaissance = dateDeNaissance;}
}
