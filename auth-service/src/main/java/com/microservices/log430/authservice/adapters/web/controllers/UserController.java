package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.adapters.web.dto.UserForm;
import com.microservices.log430.authservice.domain.port.in.RegistrationPort;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final RegistrationPort registrationPort;

    public UserController(RegistrationPort registrationPort) {
        this.registrationPort = registrationPort;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserForm form) {
        logger.info("Tentative d'inscription pour l'email : {}", form.getEmail());
        if ((form.getEmail() == null || form.getEmail().isBlank())) {
            logger.warn("Email manquant lors de l'inscription");
            return ResponseEntity.badRequest().body(Map.of("message", "Veuillez renseigner un email."));
        }
        // Validation format email
        if (form.getEmail() != null && !form.getEmail().isBlank() &&
                !form.getEmail().matches("^.+@.+\\..+$")) {
            logger.warn("Format d'email invalide : {}", form.getEmail());
            return ResponseEntity.badRequest().body(Map.of("message", "Format d'email invalide."));
        }
        // Validation date de naissance
        if (form.getDateDeNaissance() == null) {
            logger.warn("Date de naissance manquante lors de l'inscription");
            return ResponseEntity.badRequest().body(Map.of("message", "Veuillez renseigner la date de naissance."));
        }

        try {
            registrationPort.register(
                    form.getEmail(),
                    form.getPassword(),
                    form.getName(),
                    form.getAdresse(),
                    form.getDateDeNaissance()
            );
            logger.info("Inscription réussie pour l'email : {}", form.getEmail());
            return ResponseEntity.ok(Map.of("message", "Inscription réussie !"));
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de l'inscription : {}", e.getMessage());
            if ("Email déjà utilisé".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }
}