package com.microservices.log430.matchingservice.adapters.persistence.repositories;

import com.microservices.log430.matchingservice.adapters.persistence.entities.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Trouve tous les événements non traités qui sont prêts à être traités
     * (soit jamais tentés, soit prêts pour un retry)
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processedAt IS NULL AND " +
           "(e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) AND e.retryCount < e.maxRetries " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findUnprocessedEventsReadyForProcessing(Instant now);

    /**
     * Trouve un événement par son eventId
     */
    OutboxEventEntity findByEventId(UUID eventId);

    /**
     * Trouve tous les événements traités avant une certaine date (pour nettoyage)
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processedAt IS NOT NULL AND e.processedAt < :before")
    List<OutboxEventEntity> findProcessedEventsBefore(Instant before);

    /**
     * Trouve tous les événements échoués définitivement (max retries atteint)
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processedAt IS NULL AND e.retryCount >= e.maxRetries")
    List<OutboxEventEntity> findFailedEvents();
}
