package com.microservices.log430.notificationservice.adapters.persistence.portImpl;

import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import com.microservices.log430.notificationservice.domain.port.out.NotificationLogPort;
import com.microservices.log430.notificationservice.adapters.persistence.repository.NotificationLogRepository;
import com.microservices.log430.notificationservice.adapters.persistence.map.NotificationLogMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationLogAdapter implements NotificationLogPort {
    private final NotificationLogRepository notificationLogRepository;

    public NotificationLogAdapter(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public NotificationLog save(NotificationLog notificationLog) {
        var entity = NotificationLogMapper.toEntity(notificationLog);
        var saved = notificationLogRepository.save(entity);
        return NotificationLogMapper.toDomain(saved);
    }
}

