package com.microservices.log430.orderservice.adapters.messaging.publishers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservices.log430.orderservice.adapters.persistence.entities.OutboxEventEntity;
import com.microservices.log430.orderservice.adapters.persistence.repository.OutboxEventRepository;
import com.microservices.log430.orderservice.adapters.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service responsable de publier les événements depuis l'outbox vers RabbitMQ
 */
@Service
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange orderExchange;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Value("${messaging.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    @Autowired
    public EventPublisher(RabbitTemplate rabbitTemplate,
                         TopicExchange orderExchange,
                         OutboxEventRepository outboxEventRepository,
                         OutboxService outboxService) {
        this.rabbitTemplate = rabbitTemplate;
        this.orderExchange = orderExchange;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxService = outboxService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Traite tous les événements en attente dans l'outbox
     */
    public void processOutboxEvents() {
        logger.debug("Début du traitement des événements outbox");

        List<OutboxEventEntity> events = outboxEventRepository
            .findUnprocessedEventsReadyForProcessing(Instant.now());

        if (events.isEmpty()) {
            logger.debug("Aucun événement à traiter dans l'outbox");
            return;
        }

        logger.info("Traitement de {} événements depuis l'outbox", events.size());

        for (OutboxEventEntity event : events) {
            try {
                publishEvent(event);
                outboxService.markEventAsProcessed(event.getId());

            } catch (Exception e) {
                logger.error("Erreur lors de la publication de l'événement: eventId={}, type={}",
                           event.getEventId(), event.getEventType(), e);
                outboxService.markEventAsFailed(event.getId(), e.getMessage());
            }
        }
    }

    /**
     * Publie un événement spécifique vers RabbitMQ
     */
    private void publishEvent(OutboxEventEntity event) {
        String routingKey = getRoutingKeyForEventType(event.getEventType());

        logger.info("Publication de l'événement: eventId={}, type={}, routingKey={}",
                   event.getEventId(), event.getEventType(), routingKey);

        try {
            // Le payload est déjà sérialisé en JSON dans l'outbox
            Object eventData = objectMapper.readValue(event.getPayload(), Object.class);

            rabbitTemplate.convertAndSend(
                orderExchange.getName(),
                routingKey,
                eventData
            );

            logger.info("Événement publié avec succès: eventId={}", event.getEventId());

        } catch (Exception e) {
            logger.error("Erreur lors de la publication vers RabbitMQ: eventId={}", event.getEventId(), e);
            throw new RuntimeException("Erreur de publication RabbitMQ", e);
        }
    }

    /**
     * Détermine la routing key basée sur le type d'événement
     */
    private String getRoutingKeyForEventType(String eventType) {
        return switch (eventType) {
            case "ORDER_PLACED" -> orderPlacedRoutingKey;
            default -> throw new IllegalArgumentException("Type d'événement non supporté: " + eventType);
        };
    }
}
