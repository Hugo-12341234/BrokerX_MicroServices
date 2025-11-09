package com.microservices.log430.notificationservice.adapters.persistence.repository;

import com.microservices.log430.notificationservice.adapters.persistence.entities.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {
}

