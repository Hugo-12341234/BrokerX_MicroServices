package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataClient;
import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataUpdateDTO;
import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
import com.microservices.log430.orderservice.adapters.web.dto.OrderResponse;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.in.OrderPlacementPort;
import com.microservices.log430.orderservice.domain.port.in.PreTradeValidationPort;
import com.microservices.log430.orderservice.domain.port.out.OrderPort;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletResponse;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletUpdateRequest;
import com.microservices.log430.orderservice.adapters.external.matching.MatchingClient;
import com.microservices.log430.orderservice.adapters.external.matching.dto.OrderDTO;
import com.microservices.log430.orderservice.adapters.messaging.outbox.OutboxService;
import com.microservices.log430.orderservice.adapters.messaging.events.OrderPlacedEvent;
import com.microservices.log430.orderservice.adapters.messaging.events.NotificationEvent;
import com.microservices.log430.orderservice.adapters.messaging.events.OrderMatchedEvent;
import com.microservices.log430.orderservice.adapters.messaging.events.OrderRejectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService implements OrderPlacementPort {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderPort orderPort;
    private final PreTradeValidationPort preTradeValidationPort;
    private final WalletClient walletClient;
    private final MatchingClient matchingClient;
    private final MarketDataClient marketDataClient;
    private final OutboxService outboxService;

    @Autowired
    public OrderService(OrderPort orderPort, PreTradeValidationPort preTradeValidationPort, WalletClient walletClient,
                        MatchingClient matchingClient, MarketDataClient marketDataClient, OutboxService outboxService) {
        this.orderPort = orderPort;
        this.preTradeValidationPort = preTradeValidationPort;
        this.walletClient = walletClient;
        this.matchingClient = matchingClient;
        this.marketDataClient = marketDataClient;
        this.outboxService = outboxService;
    }
    
    @Override
    public OrderPlacementResult placeOrder(OrderPlacementRequest request, String clientOrderId) {
        logger.info("Placement d'ordre demandé. userId={}, clientOrderId={}, symbol={}, side={}, type={}, quantité={}, prix={}, durée={}",
            request.getUserId(), clientOrderId,
            request.getOrderRequest().getSymbol(),
            request.getOrderRequest().getSide(),
            request.getOrderRequest().getType(),
            request.getOrderRequest().getQuantity(),
            request.getOrderRequest().getPrice(),
            request.getOrderRequest().getDuration());
        Optional<Order> existingOrder = orderPort.findByClientOrderId(clientOrderId);
        if (existingOrder.isPresent()) {
            Order existing = existingOrder.get();
            logger.info("Idempotence : ordre déjà existant. clientOrderId={}, status={}, orderId={}", clientOrderId, existing.getStatus(), existing.getId());
            return OrderPlacementResult.of(
                existing.getStatus() == Order.OrderStatus.ACCEPTE || existing.getStatus() == Order.OrderStatus.WORKING,
                existing.getId(),
                existing.getStatus().name(),
                (existing.getStatus() == Order.OrderStatus.ACCEPTE || existing.getStatus() == Order.OrderStatus.WORKING) ?
                    "Ordre placé avec succès" : existing.getRejectReason()
            );
        }
        WalletResponse walletResponse;
        try {
            logger.info("Appel au wallet-service pour userId={}", request.getUserId());
            walletResponse = walletClient.getWallet(request.getUserId());
        } catch (Exception ex) {
            logger.error("Erreur lors de la récupération du portefeuille pour userId={}: {}", request.getUserId(), ex.getMessage(), ex);
            return OrderPlacementResult.failure(null, "Portefeuille introuvable ou inaccessible");
        }
        if (walletResponse == null || !walletResponse.isSuccess() || walletResponse.getWallet() == null) {
            logger.warn("Portefeuille introuvable ou réponse invalide pour userId={}", request.getUserId());
            return OrderPlacementResult.failure(null, "Portefeuille introuvable");
        }
        Wallet wallet = walletResponse.getWallet();
        OrderRequest orderRequest = request.getOrderRequest();
        logger.info("Validation pré-trade pour userId={}, symbol={}, side={}, type={}, quantité={}, prix={}",
            request.getUserId(), orderRequest.getSymbol(), orderRequest.getSide(), orderRequest.getType(), orderRequest.getQuantity(), orderRequest.getPrice());
        PreTradeValidationPort.ValidationRequest validationRequest = new PreTradeValidationPort.ValidationRequest(
            orderRequest.getSymbol(),
            Order.Side.valueOf(orderRequest.getSide()),
            Order.OrderType.valueOf(orderRequest.getType()),
            orderRequest.getQuantity(),
            orderRequest.getPrice(),
            wallet
        );
        PreTradeValidationPort.ValidationResult validation = preTradeValidationPort.validateOrder(validationRequest);
        Order order = new Order();
        order.setClientOrderId(clientOrderId);
        order.setUserId(request.getUserId());
        order.setSymbol(orderRequest.getSymbol());
        order.setSide(Order.Side.valueOf(orderRequest.getSide()));
        order.setType(Order.OrderType.valueOf(orderRequest.getType()));
        order.setQuantity(orderRequest.getQuantity());
        order.setPrice(orderRequest.getPrice());
        order.setDuration(Order.DurationType.valueOf(orderRequest.getDuration()));
        order.setTimestamp(Instant.now());
        if (!validation.isValid()) {
            logger.warn("Ordre rejeté pour userId={}, clientOrderId={}, raison={}", request.getUserId(), clientOrderId, validation.getRejectReason());
            order.setStatus(Order.OrderStatus.REJETE);
            order.setRejectReason(validation.getRejectReason());
            Order savedOrder = orderPort.save(order);

            // Publication d'un événement de notification pour ordre rejeté lors de la validation pré-trade
            try {
                String rejectionMessage = String.format("Ordre REJETÉ : %s %d %s @ %.2f$. Raison : %s. OrderId : %s",
                    orderRequest.getSide(), orderRequest.getQuantity(), orderRequest.getSymbol(),
                    orderRequest.getPrice() != null ? orderRequest.getPrice() : 0.0,
                    validation.getRejectReason(), savedOrder.getId());

                NotificationEvent notificationEvent = new NotificationEvent(
                    request.getUserId(),
                    rejectionMessage,
                    Instant.now(),
                    "WEBSOCKET",
                    null, // email sera récupéré par le notification-service
                    "REJECTED"
                );

                outboxService.saveEvent("NOTIFICATION_SEND", savedOrder.getId(), notificationEvent);
                logger.info("Événement de notification pour ordre rejeté sauvegardé dans l'outbox pour orderId={}, clientOrderId={}",
                           savedOrder.getId(), savedOrder.getClientOrderId());

            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de l'événement de notification pour ordre rejeté : {}", e.getMessage(), e);
            }

            return OrderPlacementResult.failure(savedOrder.getId(), validation.getRejectReason());
        } else {
            logger.info("Ordre accepté pour userId={}, clientOrderId={}, orderId={}", request.getUserId(), clientOrderId, order.getId());
            order.setStatus(Order.OrderStatus.WORKING); // Working (en attente de matching asynchrone)
            order.setRejectReason(null);
            Order savedOrder = orderPort.save(order);

            // Publication d'un événement OrderPlaced dans l'outbox (architecture événementielle)
            try {
                OrderPlacedEvent event = new OrderPlacedEvent(
                    savedOrder.getId(),
                    savedOrder.getClientOrderId(),
                    savedOrder.getUserId(),
                    savedOrder.getSymbol(),
                    savedOrder.getSide().name(),
                    savedOrder.getType().name(),
                    savedOrder.getQuantity(),
                    savedOrder.getPrice(),
                    savedOrder.getDuration().name(),
                    savedOrder.getTimestamp(),
                    savedOrder.getStatus().name(),
                    savedOrder.getRejectReason(),
                    savedOrder.getVersion()
                );

                outboxService.saveEvent("ORDER_PLACED", savedOrder.getId(), event);
                logger.info("Événement OrderPlaced sauvegardé dans l'outbox pour orderId={}, clientOrderId={}",
                           savedOrder.getId(), savedOrder.getClientOrderId());

            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de l'événement OrderPlaced : {}", e.getMessage(), e);
                // L'ordre est déjà sauvé, mais l'événement n'a pas pu être publié
                return OrderPlacementResult.success(savedOrder.getId(), "Ordre placé, mais événement non publié : " + e.getMessage());
            }

            // L'ordre est maintenant accepté et l'événement est dans l'outbox
            // Le matching sera traité de façon asynchrone par le matching-service
            return OrderPlacementResult.success(savedOrder.getId(), "Ordre placé avec succès. Le matching sera traité sous peu.");
        }
    }

    @Override
    public OrderResponse modifyOrder(Long orderId, OrderRequest orderRequest, Long userId) {
        Optional<Order> orderOpt = orderPort.findById(orderId);
        if (orderOpt.isEmpty()) throw new IllegalArgumentException("Ordre non trouvé");
        Order order = orderOpt.get();
        // Vérification de l'expiration pour les ordres DAY
        if (isOrderExpired(order)) {
            throw new IllegalStateException("Impossible de modifier un ordre expiré (DAY)");
        }
        // Vérification du verrouillage optimiste
        Long versionBefore = order.getVersion();
        if (Order.OrderStatus.FILLED.equals(order.getStatus()) || Order.OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new IllegalStateException("Impossible de modifier un ordre rempli ou annulé");
        }
        // Contrôles pré-trade
        PreTradeValidationPort.ValidationRequest validationRequest = new PreTradeValidationPort.ValidationRequest(
            orderRequest.getSymbol(),
            Order.Side.valueOf(orderRequest.getSide()),
            Order.OrderType.valueOf(orderRequest.getType()),
            orderRequest.getQuantity(),
            orderRequest.getPrice(),
            walletClient.getWallet(userId).getWallet()
        );
        PreTradeValidationPort.ValidationResult validation = preTradeValidationPort.validateOrder(validationRequest);
        if (!validation.isValid()) {
            order.setStatus(Order.OrderStatus.REJETE);
            order.setRejectReason(validation.getRejectReason());
            orderPort.save(order);
            throw new IllegalStateException("Modification rejetée : " + validation.getRejectReason());
        }
        // Appliquer les modifications
        order.setQuantity(orderRequest.getQuantity());
        order.setPrice(orderRequest.getPrice());
        order.setType(Order.OrderType.valueOf(orderRequest.getType()));
        order.setDuration(Order.DurationType.valueOf(orderRequest.getDuration()));
        order.setTimestamp(Instant.now());
        Order updatedOrder = orderPort.save(order);
        // Vérification du verrouillage optimiste
        if (!versionBefore.equals(updatedOrder.getVersion() - 1)) {
            throw new RuntimeException("Conflit de version : l'ordre a été modifié par une autre transaction");
        }
        logger.info("Order id : {}, version after update : {}, clientOrderId : {}", updatedOrder.getId(), updatedOrder.getVersion(), updatedOrder.getClientOrderId());
        // Synchronisation avec le matching-service
        OrderDTO orderDTO = new OrderDTO(
            updatedOrder.getId(),
            updatedOrder.getClientOrderId(),
            updatedOrder.getUserId(),
            updatedOrder.getSymbol(),
            updatedOrder.getSide().name(),
            updatedOrder.getType().name(),
            updatedOrder.getQuantity(),
            updatedOrder.getPrice(),
            updatedOrder.getDuration().name(),
            updatedOrder.getTimestamp(),
            updatedOrder.getStatus().name(),
            updatedOrder.getRejectReason(),
            updatedOrder.getVersion()
        );
        try {
            matchingClient.modifyOrder(updatedOrder.getClientOrderId(), orderDTO);
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation avec le matching-service : {}", e.getMessage(), e);
        }
        return OrderResponse.fromOrder(updatedOrder);
    }

    public OrderResponse cancelOrder(Long orderId) {
        Optional<Order> orderOpt = orderPort.findById(orderId);
        if (orderOpt.isEmpty()) throw new IllegalArgumentException("Ordre non trouvé");
        Order order = orderOpt.get();
        // Vérification de l'expiration pour les ordres DAY
        if (isOrderExpired(order)) {
            throw new IllegalStateException("Impossible d'annuler un ordre expiré (DAY)");
        }
        Long versionBefore = order.getVersion();
        if (Order.OrderStatus.FILLED.equals(order.getStatus()) || Order.OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new IllegalStateException("Impossible d'annuler un ordre rempli ou déjà annulé");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setTimestamp(Instant.now());
        Order updatedOrder = orderPort.save(order);
        if (!versionBefore.equals(updatedOrder.getVersion() - 1)) {
            throw new RuntimeException("Conflit de version : l'ordre a été modifié par une autre transaction");
        }
        // Synchronisation avec le matching-service
        OrderDTO orderDTO = new OrderDTO(
            updatedOrder.getId(),
            updatedOrder.getClientOrderId(),
            updatedOrder.getUserId(),
            updatedOrder.getSymbol(),
            updatedOrder.getSide().name(),
            updatedOrder.getType().name(),
            updatedOrder.getQuantity(),
            updatedOrder.getPrice(),
            updatedOrder.getDuration().name(),
            updatedOrder.getTimestamp(),
            updatedOrder.getStatus().name(),
            updatedOrder.getRejectReason(),
            updatedOrder.getVersion()
        );
        try {
            matchingClient.cancelOrder(updatedOrder.getId(), orderDTO);
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation avec le matching-service : {}", e.getMessage(), e);
        }
        return OrderResponse.fromOrder(updatedOrder);
    }

    @Override
    public List<Order> findOrdersByUserId(Long userId) {
        return orderPort.findByUserId(userId);
    }

    @Override
    public void processOrderMatched(OrderMatchedEvent event) {
        logger.info("Traitement ORDER_MATCHED: orderId={}, clientOrderId={}, status={}",
                event.getOrderId(), event.getClientOrderId(), event.getStatus());
        // Mise à jour du statut de l'ordre principal
        updateOrderStatus(event.getOrderId(), event.getStatus(), event.getRejectReason());
        // Synchronisation des ordres candidats modifiés
        if (event.getModifiedCandidateIds() != null && !event.getModifiedCandidateIds().isEmpty()) {
            synchronizeModifiedCandidates(event.getModifiedCandidateIds());
        }
        // Mise à jour des portefeuilles pour chaque exécution
        if (event.getExecutions() != null && !event.getExecutions().isEmpty()) {
            List<String> deals = processExecutions(event.getExecutions());
            logger.info("Exécutions traitées pour ordre {}: {}", event.getClientOrderId(), deals);
            // Notification market-data avec le dernier prix
            notifyMarketData(event.getSymbol(), event.getExecutions());
        }
    }

    @Override
    public void processOrderRejected(OrderRejectedEvent event) {
        logger.info("Traitement ORDER_REJECTED: orderId={}, clientOrderId={}, raison={}",
                event.getOrderId(), event.getClientOrderId(), event.getRejectReason());
        updateOrderStatus(event.getOrderId(), event.getStatus(), event.getRejectReason());
    }

    // --- Logique extraite du listener ---
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

    private void synchronizeModifiedCandidates(List<String> modifiedCandidateIds) {
        logger.info("Synchronisation de {} ordres candidats modifiés", modifiedCandidateIds.size());
        for (String clientOrderId : modifiedCandidateIds) {
            try {
                Optional<Order> candidateOrder = orderPort.findByClientOrderId(clientOrderId);
                if (candidateOrder.isPresent()) {
                    Order candidateOrderEntity = candidateOrder.get();
                    Order.OrderStatus oldStatus = candidateOrderEntity.getStatus();
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

    private List<String> processExecutions(List<OrderMatchedEvent.ExecutionDetails> executions) {
        List<String> deals = new ArrayList<>();
        for (OrderMatchedEvent.ExecutionDetails exec : executions) {
            try {
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
                deals.add(String.format("Deal: %d %s @ %.2f entre acheteur %d et vendeur %d",
                        exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(),
                        exec.getBuyerUserId(), exec.getSellerUserId()));
            } catch (Exception ex) {
                logger.error("Erreur lors de la mise à jour du portefeuille pour le deal: {}", ex.getMessage(), ex);
            }
        }
        return deals;
    }

    private void notifyMarketData(String symbol, List<OrderMatchedEvent.ExecutionDetails> executions) {
        try {
            Double lastPrice = null;
            if (executions != null && !executions.isEmpty()) {
                lastPrice = executions.get(executions.size() - 1).getFillPrice();
            }
            MarketDataUpdateDTO dto = new MarketDataUpdateDTO(lastPrice, null);
            marketDataClient.streamMarketData(symbol, dto);
            logger.info("Market data notification envoyée pour symbol {}", symbol);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification market data pour symbol {}", symbol, e);
        }
    }

    private boolean isOrderExpired(Order order) {
        if (order.getDuration() == Order.DurationType.DAY) {
            Instant expiration = order.getTimestamp().plusSeconds(24 * 3600);
            return Instant.now().isAfter(expiration);
        }
        return false;
    }
}
