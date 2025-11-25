package com.microservices.log430.orderservice.adapters.messaging.listeners;

import com.microservices.log430.orderservice.adapters.messaging.events.OrderMatchedEvent;
import com.microservices.log430.orderservice.adapters.messaging.events.OrderRejectedEvent;
import com.microservices.log430.orderservice.domain.port.in.OrderPlacementPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener pour les événements ORDER_MATCHED et ORDER_REJECTED
 * Délègue toute la logique métier au service via le port OrderPlacementPort
 */
@Component
public class MatchingEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MatchingEventListener.class);
    private final OrderPlacementPort orderPlacementPort;

    @Autowired
    public MatchingEventListener(OrderPlacementPort orderPlacementPort) {
        this.orderPlacementPort = orderPlacementPort;
    }

    /**
     * Écoute les événements ORDER_MATCHED et délègue au service
     */
    @RabbitListener(queues = "${messaging.queue.order-matched}")
    @Transactional
    public void handleOrderMatched(OrderMatchedEvent event) {
        logger.info("Réception d'un événement ORDER_MATCHED: orderId={}, clientOrderId={}, status={}",
                   event.getOrderId(), event.getClientOrderId(), event.getStatus());
        try {
            orderPlacementPort.processOrderMatched(event);
        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'événement ORDER_MATCHED: orderId={}, error={}",
                        event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Écoute les événements ORDER_REJECTED et délègue au service
     */
    @RabbitListener(queues = "${messaging.queue.order-rejected}")
    @Transactional
    public void handleOrderRejected(OrderRejectedEvent event) {
        logger.info("Réception d'un événement ORDER_REJECTED: orderId={}, clientOrderId={}, raison={}",
                   event.getOrderId(), event.getClientOrderId(), event.getRejectReason());
        try {
            orderPlacementPort.processOrderRejected(event);
        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'événement ORDER_REJECTED: orderId={}, error={}",
                        event.getOrderId(), e.getMessage(), e);
        }
    }
}
