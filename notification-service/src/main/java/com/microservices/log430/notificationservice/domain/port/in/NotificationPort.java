package com.microservices.log430.notificationservice.domain.port.in;

import com.microservices.log430.notificationservice.adapters.web.dto.NotificationLogDTO;
import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;

public interface NotificationPort {
    /**
     * Envoie la notification (push/email/etc.) et la journalise dans la base.
     * @param notificationLogDTO notification à envoyer et journaliser
     * @return notification journalisée
     */
    NotificationLog sendNotification(NotificationLogDTO notificationLogDTO);
}
