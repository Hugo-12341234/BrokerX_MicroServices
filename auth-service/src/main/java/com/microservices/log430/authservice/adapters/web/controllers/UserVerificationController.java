package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.domain.port.in.RegistrationPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
public class UserVerificationController {
    private static final Logger logger = LoggerFactory.getLogger(UserVerificationController.class);
    private final RegistrationPort registrationPort;

    public UserVerificationController(RegistrationPort registrationPort) {
        this.registrationPort = registrationPort;
    }

    @GetMapping("/api/v1/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        logger.info("Vérification du compte utilisateur avec le token : {}", token);
        boolean success = registrationPort.verifyUser(token);
        logger.info("Résultat de la vérification pour le token {}: {}", token, success ? "succès" : "échec");
        if (success) {
            logger.info("Activation réussie pour le token : {}", token);
            return ResponseEntity.ok(Map.of("message", "Votre compte a été activé avec succès !"));
        } else {
            logger.warn("Échec de l'activation pour le token : {} (lien invalide ou expiré)", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Le lien d'activation est invalide ou expiré."));
        }
    }
}
