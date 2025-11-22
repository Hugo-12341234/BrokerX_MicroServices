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
            // Publier l'événement ORDER_MATCHED ou ORDER_REJECTED selon le cas
            if ("REJETE".equals(result.updatedOrder.getStatus()) ||
                (result.updatedOrder.getRejectReason() != null && !result.updatedOrder.getRejectReason().isEmpty())) {
                // Ordre rejeté
                publishOrderRejected(originalOrder, result.updatedOrder.getRejectReason());
            } else {
                // Ordre accepté (avec ou sans exécutions)
                publishOrderMatched(result, originalOrder);
            }

            // Publier les notifications spécifiques selon le résultat
            publishDetailedNotifications(result, originalOrder);

        } catch (Exception e) {
            logger.error("Erreur lors de la publication des événements de matching: {}", e.getMessage(), e);
            // Publier une notification d'erreur
            publishErrorNotification(originalOrder, "Erreur lors du traitement: " + e.getMessage());
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
     * Publie les notifications détaillées selon le résultat du matching
     */
    private void publishDetailedNotifications(MatchingResult result, OrderPlacedEvent originalOrder) {
        try {
            // Déterminer le type d'ordre et son statut final
            boolean isDAY = "DAY".equalsIgnoreCase(originalOrder.getDuration());
            boolean isIOC = "IOC".equalsIgnoreCase(originalOrder.getDuration());
            boolean isFOK = "FOK".equalsIgnoreCase(originalOrder.getDuration());

            String finalStatus = result.updatedOrder.getStatus();
            boolean hasExecutions = result.executions != null && !result.executions.isEmpty();

            // Publier notification de statut d'ordre
            publishOrderStatusNotification(result, originalOrder, isDAY, isIOC, isFOK, hasExecutions);

            // Publier notifications d'exécution individuelles si il y en a
            if (hasExecutions) {
                publishExecutionNotifications(result.executions);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la publication des notifications détaillées: {}", e.getMessage(), e);
        }
    }

    /**
     * Publie la notification de statut d'ordre
     */
    private void publishOrderStatusNotification(MatchingResult result, OrderPlacedEvent originalOrder,
                                              boolean isDAY, boolean isIOC, boolean isFOK, boolean hasExecutions) {
        String notificationMessage;
        String status;
        String finalStatus = result.updatedOrder.getStatus();

        // Déterminer le message et le statut selon le type d'ordre et le résultat
        if ("Cancelled".equals(finalStatus) && isFOK) {
            notificationMessage = String.format("Ordre FOK ANNULÉ : %s %d %s @ %.2f$. Liquidité insuffisante. OrderId : %s",
                originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
            status = "FOK_CANCELLED";

        } else if ("Cancelled".equals(finalStatus) && isIOC) {
            notificationMessage = String.format("Ordre IOC traité : %s %d %s @ %.2f$. Partie non exécutée annulée. OrderId : %s",
                originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
            status = "IOC_PARTIAL_CANCELLED";

        } else if ("PartiallyFilled".equals(finalStatus) && isIOC) {
            notificationMessage = String.format("Ordre IOC partiellement exécuté : %s %s @ %.2f$. Reste annulé. OrderId : %s",
                originalOrder.getSide(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
            status = "IOC_PARTIAL_FILLED";

        } else if ("Working".equals(finalStatus) && isDAY) {
            if (!hasExecutions) {
                notificationMessage = String.format("Ordre DAY PLACÉ : %s %d %s @ %.2f$. En attente dans le carnet. OrderId : %s",
                    originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                    originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
                status = "DAY_PLACED_NO_MATCH";
            } else {
                notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s @ %.2f$. Reste en attente. OrderId : %s",
                    originalOrder.getSide(), originalOrder.getSymbol(),
                    originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
                status = "DAY_PARTIAL_FILLED";
            }

        } else if ("PartiallyFilled".equals(finalStatus) && isDAY) {
            notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s @ %.2f$. Reste en attente. OrderId : %s",
                originalOrder.getSide(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
            status = "DAY_PARTIAL_FILLED";

        } else if ("Filled".equals(finalStatus)) {
            notificationMessage = String.format("Ordre COMPLÈTEMENT EXÉCUTÉ : %s %d %s @ %.2f$. OrderId : %s",
                originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
            status = "FILLED";

        } else if ("Cancelled".equals(finalStatus)) {
            notificationMessage = String.format("Ordre ANNULÉ : %s %d %s @ %.2f$. OrderId : %s",
                originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, originalOrder.getId());
            status = "CANCELLED";

        } else {
            notificationMessage = String.format("Ordre traité : %s %d %s @ %.2f$. Statut : %s. OrderId : %s",
                originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                originalOrder.getPrice() != null ? originalOrder.getPrice() : 0.0, finalStatus, originalOrder.getId());
            status = "PROCESSED";
        }

        // Publier la notification de statut
        NotificationEvent orderStatusEvent = new NotificationEvent(
            originalOrder.getUserId(),
            notificationMessage,
            Instant.now(),
            "WEBSOCKET",
            null,
            status
        );

        outboxService.saveEvent("NOTIFICATION_SEND", originalOrder.getId(), orderStatusEvent);
        logger.info("Notification de statut publiée pour orderId={}, userId={}, status={}",
                   originalOrder.getId(), originalOrder.getUserId(), status);
    }

    /**
     * Publie les notifications d'exécution individuelles
     */
    private void publishExecutionNotifications(List<ExecutionReport> executions) {
        for (ExecutionReport exec : executions) {
            // Notification pour l'acheteur
            if (exec.getBuyerUserId() != null && exec.getBuyerUserId() != 9999L) {
                String buyerMsg = String.format("Exécution d'ordre ACHAT : %d %s @ %.2f$. OrderId: %s. ExecutionReportId: %s. Date: %s",
                    exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());

                NotificationEvent buyerNotification = new NotificationEvent(
                    exec.getBuyerUserId(), buyerMsg, Instant.now(), "WEBSOCKET", null, "EXECUTED"
                );

                outboxService.saveEvent("NOTIFICATION_SEND", exec.getBuyerUserId(), buyerNotification);
                logger.info("Notification d'exécution publiée pour buyer userId={}, executionId={}",
                           exec.getBuyerUserId(), exec.getId());
            }

            // Notification pour le vendeur
            if (exec.getSellerUserId() != null && exec.getSellerUserId() != 9999L) {
                String sellerMsg = String.format("Exécution d'ordre VENTE : %d %s @ %.2f$. OrderId: %s. ExecutionReportId: %s. Date: %s",
                    exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());

                NotificationEvent sellerNotification = new NotificationEvent(
                    exec.getSellerUserId(), sellerMsg, Instant.now(), "WEBSOCKET", null, "EXECUTED"
                );

                outboxService.saveEvent("NOTIFICATION_SEND", exec.getSellerUserId(), sellerNotification);
                logger.info("Notification d'exécution publiée pour seller userId={}, executionId={}",
                           exec.getSellerUserId(), exec.getId());
            }
        }
    }

    /**
     * Publie une notification d'erreur
     */
    private void publishErrorNotification(OrderPlacedEvent originalOrder, String errorMessage) {
        try {
            String message = String.format("Erreur lors du traitement de l'ordre : %s %d %s. OrderId : %s. Erreur : %s",
                originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                originalOrder.getId(), errorMessage);

            NotificationEvent errorEvent = new NotificationEvent(
                originalOrder.getUserId(),
                message,
                Instant.now(),
                "WEBSOCKET",
                null,
                "ERROR"
            );

            outboxService.saveEvent("NOTIFICATION_SEND", originalOrder.getId(), errorEvent);
            logger.info("Notification d'erreur publiée pour orderId={}, userId={}",
                       originalOrder.getId(), originalOrder.getUserId());
        } catch (Exception e) {
            logger.error("Impossible de publier la notification d'erreur : {}", e.getMessage(), e);
        }
    }
}

