package com.microservices.log430.authservice.domain.port.in;

import com.microservices.log430.authservice.domain.model.entities.User;
import java.time.LocalDate;

public interface RegistrationPort {
    User register(String email, String rawPassword, String name, String adresse, LocalDate dateDeNaissance);
    boolean verifyUser(String token);
    User getUserInfo(Long userId);
}
