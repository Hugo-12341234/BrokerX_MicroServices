package com.microservices.log430.orderservice.adapters.messaging.listeners;

import com.microservices.log430.orderservice.adapters.messaging.events.OrderMatchedEvent;
import com.microservices.log430.orderservice.adapters.messaging.events.OrderRejectedEvent;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.out.OrderPort;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletUpdateRequest;
import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataClient;
import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Listener pour les événements ORDER_MATCHED et ORDER_REJECTED
 * Reproduit exactement le comportement de l'ancien code synchrone
 */
@Component
public class MatchingEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MatchingEventListener.class);

    private final OrderPort orderPort;
    private final WalletClient walletClient;
    private final MarketDataClient marketDataClient;

    @Autowired
    public MatchingEventListener(OrderPort orderPort, WalletClient walletClient, MarketDataClient marketDataClient) {
        this.orderPort = orderPort;
        this.walletClient = walletClient;
        this.marketDataClient = marketDataClient;
    }

    /**
     * Écoute les événements ORDER_MATCHED et traite les exécutions
     */
    @RabbitListener(queues = "${messaging.queue.order-matched}")
    @Transactional
    public void handleOrderMatched(OrderMatchedEvent event) {
        logger.info("Réception d'un événement ORDER_MATCHED: orderId={}, clientOrderId={}, status={}",
                   event.getOrderId(), event.getClientOrderId(), event.getStatus());

        try {
            // Mise à jour du statut de l'ordre principal
            updateOrderStatus(event.getOrderId(), event.getStatus(), event.getRejectReason());

            // Synchronisation des ordres candidats modifiés (reproduit l'ancien code)
            if (event.getModifiedCandidateIds() != null && !event.getModifiedCandidateIds().isEmpty()) {
                synchronizeModifiedCandidates(event.getModifiedCandidateIds());
            }

            // Mise à jour des portefeuilles pour chaque exécution (reproduit l'ancien code)
            if (event.getExecutions() != null && !event.getExecutions().isEmpty()) {
                List<String> deals = processExecutions(event.getExecutions());
                logger.info("Exécutions traitées pour ordre {}: {}", event.getClientOrderId(), deals);

                // Notification market-data avec le dernier prix
                notifyMarketData(event.getSymbol(), event.getExecutions());
            }

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'événement ORDER_MATCHED: orderId={}, error={}",
                        event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Écoute les événements ORDER_REJECTED
     */
    @RabbitListener(queues = "${messaging.queue.order-rejected}")
    @Transactional
    public void handleOrderRejected(OrderRejectedEvent event) {
        logger.info("Réception d'un événement ORDER_REJECTED: orderId={}, clientOrderId={}, raison={}",
                   event.getOrderId(), event.getClientOrderId(), event.getRejectReason());

        try {
            // Mise à jour du statut de l'ordre
            updateOrderStatus(event.getOrderId(), event.getStatus(), event.getRejectReason());

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'événement ORDER_REJECTED: orderId={}, error={}",
                        event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Met à jour le statut d'un ordre (reproduit l'ancien code)
     */
    private void updateOrderStatus(Long orderId, String status, String rejectReason) {
        Optional<Order> orderOpt = orderPort.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            Order.OrderStatus oldStatus = order.getStatus();
            order.setStatus(Order.OrderStatus.fromString(status));
            order.setRejectReason(rejectReason);
            orderPort.save(order);

            logger.info("Statut ordre mis à jour: orderId={}, clientOrderId={}, {} -> {}",
                       orderId, order.getClientOrderId(), oldStatus, order.getStatus());
        } else {
            logger.warn("Ordre non trouvé pour mise à jour: orderId={}", orderId);
        }
    }

    /**
     * Synchronise les ordres candidats modifiés (reproduit l'ancien code)
     */
    private void synchronizeModifiedCandidates(List<String> modifiedCandidateIds) {
        logger.info("Synchronisation de {} ordres candidats modifiés", modifiedCandidateIds.size());

        for (String clientOrderId : modifiedCandidateIds) {
            try {
                // Trouver l'ordre correspondant dans order-service par clientOrderId
                Optional<Order> candidateOrder = orderPort.findByClientOrderId(clientOrderId);
                if (candidateOrder.isPresent()) {
                    Order candidateOrderEntity = candidateOrder.get();
                    Order.OrderStatus oldStatus = candidateOrderEntity.getStatus();

                    // Note: Le statut exact des candidats devrait être fourni par l'événement
                    // Pour l'instant, on assume qu'ils sont partiellement remplis ou remplis
                    candidateOrderEntity.setStatus(Order.OrderStatus.PARTIALLYFILLED);
                    orderPort.save(candidateOrderEntity);

                    logger.info("Ordre candidat synchronisé: clientOrderId={}, userId={}, {} -> {}",
                               candidateOrderEntity.getClientOrderId(), candidateOrderEntity.getUserId(),
                               oldStatus, candidateOrderEntity.getStatus());
                } else {
                    logger.warn("Ordre candidat modifié non trouvé dans order-service: clientOrderId={}",
                               clientOrderId);
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la synchronisation de l'ordre candidat clientOrderId={}: {}",
                           clientOrderId, e.getMessage(), e);
            }
        }
    }

    /**
     * Traite les exécutions et met à jour les portefeuilles (reproduit l'ancien code)
     */
    private List<String> processExecutions(List<OrderMatchedEvent.ExecutionDetails> executions) {
        List<String> deals = new ArrayList<>();

        for (OrderMatchedEvent.ExecutionDetails exec : executions) {
            try {
                // Mise à jour du portefeuille de l'acheteur (reproduit l'ancien code)
                if (exec.getBuyerUserId() != null && exec.getBuyerUserId() != 9999L) {
                    WalletUpdateRequest buyerUpdate = new WalletUpdateRequest(
                        exec.getBuyerUserId(),
                        exec.getSymbol(),
                        exec.getFillQuantity(),
                        -exec.getFillPrice() * exec.getFillQuantity()
                    );
                    logger.info("Mise à jour du portefeuille de l'acheteur");
                    walletClient.updateWallet(buyerUpdate);
                    logger.info("Portefeuille mis à jour pour l'acheteur userId={}: +{} {} et -{} en cash",
                        exec.getBuyerUserId(), exec.getFillQuantity(), exec.getSymbol(),
                        exec.getFillPrice() * exec.getFillQuantity());
                } else {
                    logger.info("Skip update portefeuille pour l'acheteur userId=9999 (seed)");
                }

                // Mise à jour du portefeuille du vendeur (reproduit l'ancien code)
                if (exec.getSellerUserId() != null && exec.getSellerUserId() != 9999L) {
                    WalletUpdateRequest sellerUpdate = new WalletUpdateRequest(
                        exec.getSellerUserId(),
                        exec.getSymbol(),
                        -exec.getFillQuantity(),
                        exec.getFillPrice() * exec.getFillQuantity()
                    );
                    logger.info("Mise à jour du portefeuille du vendeur");
                    walletClient.updateWallet(sellerUpdate);
                    logger.info("Portefeuille mis à jour pour le vendeur userId={}: -{} {} et +{} en cash",
                        exec.getSellerUserId(), exec.getFillQuantity(), exec.getSymbol(),
                        exec.getFillPrice() * exec.getFillQuantity());
                } else {
                    logger.info("Skip update portefeuille pour le vendeur userId=9999 (seed)");
                }

                // Enregistrer le deal pour les logs
                deals.add(String.format("Deal: %d %s @ %.2f entre acheteur %d et vendeur %d",
                    exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(),
                    exec.getBuyerUserId(), exec.getSellerUserId()));

            } catch (Exception ex) {
                logger.error("Erreur lors de la mise à jour du portefeuille pour le deal: {}", ex.getMessage(), ex);
            }
        }

        return deals;
    }

    /**
     * Notifie le market-data service avec les nouvelles informations (reproduit l'ancien code)
     */
    private void notifyMarketData(String symbol, List<OrderMatchedEvent.ExecutionDetails> executions) {
        try {
            Double lastPrice = null;
            if (executions != null && !executions.isEmpty()) {
                // Prendre le prix de la dernière exécution
                lastPrice = executions.get(executions.size() - 1).getFillPrice();
            }

            MarketDataUpdateDTO dto = new MarketDataUpdateDTO(lastPrice, null);
            marketDataClient.streamMarketData(symbol, dto);
            logger.info("Market data notification envoyée pour symbol {}", symbol);

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification market data pour symbol {}", symbol, e);
        }
    }
}
