package com.microservices.log430.notificationservice.domain.service;

import com.microservices.log430.notificationservice.domain.port.in.NotificationPort;
import com.microservices.log430.notificationservice.domain.port.out.NotificationLogPort;
import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class NotificationService implements NotificationPort {
    private final NotificationLogPort notificationLogPort;

    public NotificationService(NotificationLogPort notificationLogPort) {
        this.notificationLogPort = notificationLogPort;
    }

    @Override
    public NotificationLog sendNotification(NotificationLog notificationLog) {
        // Ajoute l'horodatage ici si non fourni
        if (notificationLog.getTimestamp() == null) {
            notificationLog.setTimestamp(Instant.now());
        }
        // Ici tu pourrais ajouter la logique d'envoi push/email/etc.
        // ...
        // Journalisation
        return notificationLogPort.save(notificationLog);
    }
}
