package com.microservices.log430.notificationservice.adapters.persistence.map;

import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import com.microservices.log430.notificationservice.adapters.persistence.entities.NotificationLogEntity;

public class NotificationLogMapper {
    public static NotificationLog toDomain(NotificationLogEntity entity) {
        if (entity == null) return null;
        return new NotificationLog(
            entity.getUserId(),
            entity.getMessage(),
            entity.getTimestamp(),
            entity.getChannel()
        );
    }

    public static NotificationLogEntity toEntity(NotificationLog domain) {
        if (domain == null) return null;
        NotificationLogEntity entity = new NotificationLogEntity();
        entity.setUserId(domain.getUserId());
        entity.setMessage(domain.getMessage());
        entity.setTimestamp(domain.getTimestamp());
        entity.setChannel(domain.getChannel());
        return entity;
    }
}

