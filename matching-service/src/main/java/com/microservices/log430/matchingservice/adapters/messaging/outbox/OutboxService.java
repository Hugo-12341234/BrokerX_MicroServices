package com.microservices.log430.matchingservice.adapters.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservices.log430.matchingservice.adapters.persistence.entities.OutboxEventEntity;
import com.microservices.log430.matchingservice.adapters.persistence.repositories.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service responsable de la gestion des événements dans l'outbox pour matching-service
 */
@Service
public class OutboxService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Sauvegarde un événement dans l'outbox de manière transactionnelle
     */
    @Transactional
    public void saveEvent(String eventType, Long aggregateId, Object event) {
        try {
            UUID eventId = UUID.randomUUID();
            String payload = objectMapper.writeValueAsString(event);

            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                eventId,
                eventType,
                aggregateId.toString(),
                payload
            );

            outboxEventRepository.save(outboxEvent);

            logger.info("Événement sauvegardé dans l'outbox: eventId={}, type={}, aggregateId={}",
                       eventId, eventType, aggregateId);

        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de la sérialisation de l'événement: eventType={}, aggregateId={}",
                        eventType, aggregateId, e);
            throw new RuntimeException("Erreur lors de la sérialisation de l'événement", e);
        }
    }

    /**
     * Marque un événement comme traité
     */
    @Transactional
    public void markEventAsProcessed(Long eventId) {
        OutboxEventEntity event = outboxEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Événement non trouvé: " + eventId));

        event.markAsProcessed();
        outboxEventRepository.save(event);

        logger.info("Événement marqué comme traité: eventId={}, type={}",
                   event.getEventId(), event.getEventType());
    }

    /**
     * Marque un événement comme échoué
     */
    @Transactional
    public void markEventAsFailed(Long eventId, String errorMessage) {
        OutboxEventEntity event = outboxEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Événement non trouvé: " + eventId));

        event.markAsFailed(errorMessage);
        outboxEventRepository.save(event);

        logger.warn("Événement marqué comme échoué: eventId={}, type={}, retryCount={}, error={}",
                   event.getEventId(), event.getEventType(), event.getRetryCount(), errorMessage);
    }

    /**
     * Nettoie les événements traités anciens (plus de 7 jours)
     */
    @Transactional
    public void cleanupProcessedEvents() {
        Instant cutoff = Instant.now().minusSeconds(7 * 24 * 3600); // 7 jours
        var processedEvents = outboxEventRepository.findProcessedEventsBefore(cutoff);

        if (!processedEvents.isEmpty()) {
            outboxEventRepository.deleteAll(processedEvents);
            logger.info("Nettoyage de {} événements traités anciens", processedEvents.size());
        }
    }
}
