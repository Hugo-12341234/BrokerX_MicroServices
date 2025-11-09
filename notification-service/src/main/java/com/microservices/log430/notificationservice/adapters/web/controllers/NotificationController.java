package com.microservices.log430.notificationservice.adapters.web.controllers;

import com.microservices.log430.notificationservice.domain.port.in.NotificationPort;
import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationPort notificationPort;

    @Autowired
    public NotificationController(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @PostMapping("")
    public ResponseEntity<?> notify(@RequestBody NotificationLog notificationLog) {
        NotificationLog saved = notificationPort.sendNotification(notificationLog);
        return ResponseEntity.ok(saved);
    }
}