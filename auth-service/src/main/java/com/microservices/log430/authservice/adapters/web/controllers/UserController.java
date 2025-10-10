package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.authservice.adapters.web.dto.UserForm;
import com.microservices.log430.authservice.domain.port.in.RegistrationPort;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final RegistrationPort registrationPort;

    public UserController(RegistrationPort registrationPort) {
        this.registrationPort = registrationPort;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserForm form, HttpServletRequest httpRequest) {
        logger.info("Tentative d'inscription pour l'email : {}", form.getEmail());
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        if ((form.getEmail() == null || form.getEmail().isBlank())) {
            logger.warn("Email manquant lors de l'inscription");
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Veuillez renseigner un email.",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.badRequest().body(err);
        }
        if (form.getEmail() != null && !form.getEmail().isBlank() &&
                !form.getEmail().matches("^.+@.+\\..+$")) {
            logger.warn("Format d'email invalide : {}", form.getEmail());
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Format d'email invalide.",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.badRequest().body(err);
        }
        if (form.getDateDeNaissance() == null) {
            logger.warn("Date de naissance manquante lors de l'inscription");
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Veuillez renseigner la date de naissance.",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.badRequest().body(err);
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
            int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            String error = "Internal Server Error";
            if ("Email déjà utilisé".equals(e.getMessage())) {
                status = HttpStatus.CONFLICT.value();
                error = "Conflict";
            }
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                path,
                status,
                error,
                e.getMessage(),
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(status).body(err);
        }
    }
}