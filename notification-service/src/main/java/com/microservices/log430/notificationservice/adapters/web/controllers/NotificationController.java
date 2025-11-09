package com.microservices.log430.notificationservice.adapters.web.controllers;

import com.microservices.log430.notificationservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.notificationservice.domain.port.in.NotificationPort;
import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import com.microservices.log430.notificationservice.adapters.web.dto.NotificationLogDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationPort notificationPort;

    @Autowired
    public NotificationController(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @PostMapping("")
    public ResponseEntity<?> notify(@RequestBody NotificationLogDTO notificationLogDTO) {
        logger.info("Réception d'une notification à envoyer : userId={}, channel={}, message={}, email={}", notificationLogDTO.getUserId(), notificationLogDTO.getChannel(), notificationLogDTO.getMessage(), notificationLogDTO.getEmail());
        try {
            NotificationLog saved = notificationPort.sendNotification(notificationLogDTO);
            logger.info("Notification envoyée et journalisée : id={}, userId={}, channel={}", saved.getId(), saved.getUserId(), saved.getChannel());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi ou de la journalisation de la notification : {}", e.getMessage(), e);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                "/api/v1/notifications",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erreur lors de l'envoi ou de la journalisation de la notification",
                ""
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}