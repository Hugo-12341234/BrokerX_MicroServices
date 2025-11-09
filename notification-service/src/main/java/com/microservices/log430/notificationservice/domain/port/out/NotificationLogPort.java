package com.microservices.log430.notificationservice.domain.port.out;

import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;

public interface NotificationLogPort {
    NotificationLog save(NotificationLog notificationLog);
}