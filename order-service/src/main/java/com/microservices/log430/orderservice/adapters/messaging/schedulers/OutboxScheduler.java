package com.microservices.log430.orderservice.adapters.messaging.schedulers;

import com.microservices.log430.orderservice.adapters.messaging.publishers.EventPublisher;
import com.microservices.log430.orderservice.adapters.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler responsable du traitement périodique de l'outbox
 */
@Component
public class OutboxScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);

    private final EventPublisher eventPublisher;
    private final OutboxService outboxService;

    @Autowired
    public OutboxScheduler(EventPublisher eventPublisher, OutboxService outboxService) {
        this.eventPublisher = eventPublisher;
        this.outboxService = outboxService;
    }

    /**
     * Traite les événements de l'outbox toutes les 10 secondes
     */
    @Scheduled(fixedDelay = 1000) // 1 seconde - plus rapide pour dev
    public void processOutboxEvents() {
        try {
            logger.debug("Début du traitement schedulé de l'outbox");
            eventPublisher.processOutboxEvents();

        } catch (Exception e) {
            logger.error("Erreur lors du traitement schedulé de l'outbox", e);
        }
    }

    /**
     * Nettoie les événements traités anciens une fois par heure
     */
    @Scheduled(fixedDelay = 3600000) // 1 heure
    public void cleanupProcessedEvents() {
        try {
            logger.debug("Début du nettoyage des événements traités");
            outboxService.cleanupProcessedEvents();

        } catch (Exception e) {
            logger.error("Erreur lors du nettoyage des événements traités", e);
        }
    }
}
