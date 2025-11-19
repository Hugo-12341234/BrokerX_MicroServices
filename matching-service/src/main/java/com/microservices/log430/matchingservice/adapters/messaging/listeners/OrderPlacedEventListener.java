package com.microservices.log430.matchingservice.adapters.messaging.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservices.log430.matchingservice.adapters.messaging.events.OrderPlacedEvent;
import com.microservices.log430.matchingservice.adapters.messaging.events.OrderMatchedEvent;
import com.microservices.log430.matchingservice.adapters.messaging.events.OrderRejectedEvent;
import com.microservices.log430.matchingservice.adapters.messaging.events.NotificationEvent;
import com.microservices.log430.matchingservice.adapters.messaging.outbox.OutboxService;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;
import com.microservices.log430.matchingservice.domain.service.MatchingService;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;
import com.microservices.log430.matchingservice.adapters.web.dto.OrderBookDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Listener pour les événements ORDER_PLACED
 */
@Component
public class OrderPlacedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderPlacedEventListener.class);

    private final MatchingService matchingService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderPlacedEventListener(MatchingService matchingService, OutboxService outboxService) {
        this.matchingService = matchingService;
        this.outboxService = outboxService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Écoute les événements ORDER_PLACED et effectue le matching
     */
    @RabbitListener(queues = "${messaging.queue.order-placed}")
    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent orderPlacedEvent) {
        logger.info("Réception d'un événement ORDER_PLACED: clientOrderId={}, userId={}, symbol={}",
                   orderPlacedEvent.getClientOrderId(), orderPlacedEvent.getUserId(), orderPlacedEvent.getSymbol());

        try {
            // Conversion de l'événement en OrderBook
            OrderBook orderBook = convertOrderPlacedEventToOrderBook(orderPlacedEvent);

            logger.info("Début du matching pour ordre: clientOrderId={}, userId={}, symbol={}, side={}",
                       orderBook.getClientOrderId(), orderBook.getUserId(), orderBook.getSymbol(), orderBook.getSide());

            // Exécuter le matching
            MatchingResult result = matchingService.matchOrder(orderBook);

            if (result != null && result.updatedOrder != null) {
                // Publier les événements selon le résultat
                publishMatchingEvents(result, orderPlacedEvent);

            } else {
                logger.error("Résultat de matching null pour ordre: {}", orderBook.getClientOrderId());
                publishOrderRejected(orderPlacedEvent, "Erreur interne lors du matching");
            }

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'ordre placé: {}", e.getMessage(), e);
            publishOrderRejected(orderPlacedEvent, "Erreur lors du matching: " + e.getMessage());
        }
    }

    /**
     * Convertit un OrderPlacedEvent en OrderBook
     */
    private OrderBook convertOrderPlacedEventToOrderBook(OrderPlacedEvent event) {
        OrderBook orderBook = new OrderBook();
        orderBook.setId(event.getId());
        orderBook.setClientOrderId(event.getClientOrderId());
        orderBook.setUserId(event.getUserId());
        orderBook.setSymbol(event.getSymbol());
        orderBook.setSide(event.getSide());
        orderBook.setType(event.getType());
        orderBook.setQuantity(event.getQuantity());
        orderBook.setPrice(event.getPrice());
        orderBook.setDuration(event.getDuration());
        orderBook.setTimestamp(event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        orderBook.setStatus(event.getStatus());
        orderBook.setRejectReason(event.getRejectReason());
        orderBook.setVersion(event.getVersion());
        orderBook.setQuantityRemaining(event.getQuantity());

        return orderBook;
    }

    /**
     * Publie les événements selon le résultat du matching
     */
    private void publishMatchingEvents(MatchingResult result, OrderPlacedEvent originalOrder) {
        try {
            if (result.executions != null && !result.executions.isEmpty()) {
                // Il y a eu des exécutions -> ORDER_MATCHED
                publishOrderMatched(result, originalOrder);
                publishNotificationEvents(result);

            } else if ("REJETE".equals(result.updatedOrder.getStatus()) ||
                      (result.updatedOrder.getRejectReason() != null && !result.updatedOrder.getRejectReason().isEmpty())) {
                // Ordre rejeté
                publishOrderRejected(originalOrder, result.updatedOrder.getRejectReason());

            } else {
                // Ordre accepté mais pas d'exécution immédiate (DAY order par exemple)
                publishOrderMatched(result, originalOrder);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la publication des événements de matching: {}", e.getMessage(), e);
        }
    }

    /**
     * Publie l'événement ORDER_MATCHED
     */
    private void publishOrderMatched(MatchingResult result, OrderPlacedEvent originalOrder) {
        List<OrderMatchedEvent.ExecutionDetails> executionDetails = null;

        if (result.executions != null) {
            executionDetails = result.executions.stream()
                .map(exec -> new OrderMatchedEvent.ExecutionDetails(
                    exec.getId(),
                    exec.getBuyerUserId(),
                    exec.getSellerUserId(),
                    exec.getSymbol(),
                    exec.getFillQuantity(),
                    exec.getFillPrice(),
                    exec.getExecutionTime(),
                    exec.getOrderId()
                ))
                .collect(Collectors.toList());
        }

        List<String> modifiedCandidateIds = null;
        if (result.modifiedCandidates != null) {
            modifiedCandidateIds = result.modifiedCandidates.stream()
                .map(candidate -> candidate.getClientOrderId())
                .collect(Collectors.toList());
        }

        OrderMatchedEvent event = new OrderMatchedEvent(
            originalOrder.getId(),
            originalOrder.getClientOrderId(),
            originalOrder.getUserId(),
            originalOrder.getSymbol(),
            originalOrder.getSide(),
            originalOrder.getType(),
            originalOrder.getQuantity(),
            originalOrder.getPrice(),
            originalOrder.getDuration(),
            originalOrder.getTimestamp(),
            result.updatedOrder.getStatus(),
            result.updatedOrder.getRejectReason(),
            originalOrder.getVersion(),
            executionDetails,
            modifiedCandidateIds
        );

        outboxService.saveEvent("ORDER_MATCHED", originalOrder.getId(), event);
        logger.info("Événement ORDER_MATCHED publié pour ordre: {}", originalOrder.getClientOrderId());
    }

    /**
     * Publie l'événement ORDER_REJECTED
     */
    private void publishOrderRejected(OrderPlacedEvent originalOrder, String rejectReason) {
        OrderRejectedEvent event = new OrderRejectedEvent(
            originalOrder.getId(),
            originalOrder.getClientOrderId(),
            originalOrder.getUserId(),
            originalOrder.getSymbol(),
            originalOrder.getSide(),
            originalOrder.getType(),
            originalOrder.getQuantity(),
            originalOrder.getPrice(),
            originalOrder.getDuration(),
            originalOrder.getTimestamp(),
            "REJETE",
            rejectReason,
            originalOrder.getVersion()
        );

        outboxService.saveEvent("ORDER_REJECTED", originalOrder.getId(), event);
        logger.info("Événement ORDER_REJECTED publié pour ordre: {} - raison: {}",
                   originalOrder.getClientOrderId(), rejectReason);
    }

    /**
     * Publie les événements de notification
     */
    private void publishNotificationEvents(MatchingResult result) {
        if (result.executions != null) {
            for (ExecutionReport exec : result.executions) {
                // Notification pour l'acheteur
                if (exec.getBuyerUserId() != null && exec.getBuyerUserId() != 9999L) {
                    String buyerMsg = String.format("Exécution d'ordre ACHAT : %d %s @ %.2f. OrderId: %s. ExecutionReportId: %s. Date: %s.",
                        exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());

                    NotificationEvent buyerNotification = new NotificationEvent(
                        exec.getBuyerUserId(), buyerMsg, Instant.now(), "WEBSOCKET", null
                    );

                    outboxService.saveEvent("NOTIFICATION_SEND", exec.getBuyerUserId(), buyerNotification);
                }

                // Notification pour le vendeur
                if (exec.getSellerUserId() != null && exec.getSellerUserId() != 9999L) {
                    String sellerMsg = String.format("Exécution d'ordre VENTE : %d %s @ %.2f. OrderId: %s. ExecutionReportId: %s. Date: %s.",
                        exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());

                    NotificationEvent sellerNotification = new NotificationEvent(
                        exec.getSellerUserId(), sellerMsg, Instant.now(), "WEBSOCKET", null
                    );

                    outboxService.saveEvent("NOTIFICATION_SEND", exec.getSellerUserId(), sellerNotification);
                }
            }
        }
    }
}

