package com.microservices.log430.matchingservice.adapters.messaging.listeners;

import com.microservices.log430.matchingservice.adapters.messaging.events.OrderPlacedEvent;
import com.microservices.log430.matchingservice.domain.port.in.MatchingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener pour les événements ORDER_PLACED
 */
@Component
public class OrderPlacedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderPlacedEventListener.class);
    private final MatchingPort matchingPort;

    @Autowired
    public OrderPlacedEventListener(MatchingPort matchingPort) {
        this.matchingPort = matchingPort;
    }

    /**
     * Écoute les événements ORDER_PLACED et délègue au service
     */
    @RabbitListener(queues = "${messaging.queue.order-placed}")
    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent orderPlacedEvent) {
        logger.info("Réception d'un événement ORDER_PLACED: clientOrderId={}, userId={}, symbol={}",
                   orderPlacedEvent.getClientOrderId(), orderPlacedEvent.getUserId(), orderPlacedEvent.getSymbol());
        try {
            matchingPort.processOrderPlacedEvent(orderPlacedEvent);
        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'événement ORDER_PLACED: {}", e.getMessage(), e);
        }
    }
}
