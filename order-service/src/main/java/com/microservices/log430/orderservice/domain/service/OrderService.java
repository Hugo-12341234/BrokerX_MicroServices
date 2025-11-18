package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataClient;
import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataUpdateDTO;
import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
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
import com.microservices.log430.orderservice.adapters.external.matching.dto.MatchingResult;
import com.microservices.log430.orderservice.adapters.external.matching.dto.ExecutionReportDTO;
import com.microservices.log430.orderservice.adapters.external.matching.dto.OrderBookDTO;
import com.microservices.log430.orderservice.adapters.web.dto.OrderResponse;
import com.microservices.log430.orderservice.adapters.external.notification.NotificationClient;
import com.microservices.log430.orderservice.adapters.external.notification.dto.NotificationLogDTO;
import com.microservices.log430.orderservice.adapters.external.auth.UserInfoClient;
import com.microservices.log430.orderservice.adapters.external.auth.dto.UserDTO;
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
    private final NotificationClient notificationClient;
    private final UserInfoClient userInfoClient;
    private final MarketDataClient marketDataClient;

    @Autowired
    public OrderService(OrderPort orderPort, PreTradeValidationPort preTradeValidationPort, WalletClient walletClient,
                        MatchingClient matchingClient, NotificationClient notificationClient, UserInfoClient userInfoClient,
                        MarketDataClient marketDataClient) {
        this.orderPort = orderPort;
        this.preTradeValidationPort = preTradeValidationPort;
        this.walletClient = walletClient;
        this.matchingClient = matchingClient;
        this.notificationClient = notificationClient;
        this.userInfoClient = userInfoClient;
        this.marketDataClient = marketDataClient;
    }

    private void notifyMarketData(String symbol, Double lastPrice, Object orderBook) {
        MarketDataUpdateDTO dto = new MarketDataUpdateDTO(lastPrice, orderBook);
        try {
            marketDataClient.streamMarketData(symbol, dto);
            logger.info("Market data notification envoyée pour symbol {}", symbol);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification market data pour symbol {}", symbol, e);
        }
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
                existing.getStatus() == Order.OrderStatus.ACCEPTE,
                existing.getId(),
                existing.getStatus().name(),
                existing.getStatus() == Order.OrderStatus.ACCEPTE ?
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
            return OrderPlacementResult.failure(savedOrder.getId(), validation.getRejectReason());
        } else {
            logger.info("Ordre accepté pour userId={}, clientOrderId={}, orderId={}", request.getUserId(), clientOrderId, order.getId());
            order.setStatus(Order.OrderStatus.WORKING);
            order.setRejectReason(null);
            Order savedOrder = orderPort.save(order);

            // Appel au matching-service
            OrderDTO orderDTO = new OrderDTO(
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
            logger.info("orderDTO version : {}", orderDTO.version);
            MatchingResult matchingResult = null;
            try {
                matchingResult = matchingClient.matchOrder(orderDTO);
            } catch (Exception e) {
                logger.error("Erreur lors de l'appel au matching-service : {}", e.getMessage(), e);
                return OrderPlacementResult.success(savedOrder.getId(), "Ordre placé, mais matching non effectué : " + e.getMessage());
            }

            // Notification market-data avec le dernier prix et l'ordre book mis à jour
            notifyMarketData(orderRequest.getSymbol(), matchingResult != null && matchingResult.executions != null && !matchingResult.executions.isEmpty() ?
                matchingResult.executions.get(matchingResult.executions.size() - 1).getFillPrice() : null,
                matchingResult != null ? matchingResult.updatedOrder : null);

            // Mise à jour du statut du Order selon le résultat du matching-service
            if (matchingResult != null && matchingResult.updatedOrder != null) {
                OrderBookDTO ob = matchingResult.updatedOrder;
                savedOrder.setStatus(Order.OrderStatus.fromString(ob.getStatus()));
                savedOrder.setRejectReason(ob.getRejectReason());
                // Si applicable, mettre à jour la quantité restante ou autres champs
                // savedOrder.setQuantityRemaining(ob.getQuantityRemaining()); // si champ présent dans Order
                orderPort.save(savedOrder);
            }

            // Synchronisation des ordres candidats modifiés
            if (matchingResult != null && matchingResult.modifiedCandidates != null && !matchingResult.modifiedCandidates.isEmpty()) {
                logger.info("Synchronisation de {} ordres candidats modifiés", matchingResult.modifiedCandidates.size());
                for (OrderBookDTO modifiedCandidate : matchingResult.modifiedCandidates) {
                    try {
                        // Trouver l'ordre correspondant dans order-service par clientOrderId
                        Optional<Order> candidateOrder = orderPort.findByClientOrderId(modifiedCandidate.getClientOrderId());
                        if (candidateOrder.isPresent()) {
                            Order candidateOrderEntity = candidateOrder.get();
                            Order.OrderStatus oldStatus = candidateOrderEntity.getStatus();
                            candidateOrderEntity.setStatus(Order.OrderStatus.fromString(modifiedCandidate.getStatus()));
                            orderPort.save(candidateOrderEntity);
                            logger.info("Ordre candidat synchronisé : clientOrderId={}, userId={}, {} -> {}",
                                       candidateOrderEntity.getClientOrderId(), candidateOrderEntity.getUserId(), oldStatus, candidateOrderEntity.getStatus());
                        } else {
                            logger.warn("Ordre candidat modifié non trouvé dans order-service : clientOrderId={}",
                                       modifiedCandidate.getClientOrderId());
                        }
                    } catch (Exception e) {
                        logger.error("Erreur lors de la synchronisation de l'ordre candidat clientOrderId={} : {}",
                                   modifiedCandidate.getClientOrderId(), e.getMessage(), e);
                    }
                }
            }

            // Mise à jour des portefeuilles pour chaque deal
            List<String> deals = new ArrayList<>();
            if (matchingResult != null && matchingResult.executions != null) {
                for (ExecutionReportDTO exec : matchingResult.executions) {
                    try {
                        // Skip update si userId == 9999 (seed)
                        if (exec.getBuyerUserId() != null && exec.getBuyerUserId() == 9999L) {
                            logger.info("Skip update portefeuille pour l'acheteur userId=9999 (seed)");
                        } else {
                            WalletUpdateRequest buyerUpdate = new WalletUpdateRequest(
                                exec.getBuyerUserId(),
                                exec.getSymbol(),
                                exec.getFillQuantity(),
                                -exec.getFillPrice() * exec.getFillQuantity()
                            );
                            logger.info("Nous allons mettre à jour le portefeuille de l'acheteur");
                            walletClient.updateWallet(buyerUpdate);
                            logger.info("Portefeuille mis à jour pour l'acheteur userId={} : +{} {} et -{} en cash",
                                exec.getBuyerUserId(), exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice() * exec.getFillQuantity());
                        }
                        // Skip update si userId == 9999 (seed)
                        if (exec.getSellerUserId() != null && exec.getSellerUserId() == 9999L) {
                            logger.info("Skip update portefeuille pour le vendeur userId=9999 (seed)");
                        } else {
                            WalletUpdateRequest sellerUpdate = new WalletUpdateRequest(
                                exec.getSellerUserId(),
                                exec.getSymbol(),
                                -exec.getFillQuantity(),
                                exec.getFillPrice() * exec.getFillQuantity()
                            );
                            logger.info("Nous allons mettre à jour le portefeuille du vendeur");
                            walletClient.updateWallet(sellerUpdate);
                            logger.info("Portefeuille mis à jour pour le vendeur userId={} : -{} {} et +{} en cash",
                                exec.getSellerUserId(), exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice() * exec.getFillQuantity());
                        }
                        // Notification buyer
                        String buyerEmail = null;
                        if (exec.getBuyerUserId() != null && exec.getBuyerUserId() != 9999L) {
                            try {
                                UserDTO buyerInfo = userInfoClient.getUserInfo(exec.getBuyerUserId());
                                buyerEmail = buyerInfo != null ? buyerInfo.getEmail() : null;
                            } catch (Exception e) {
                                logger.warn("Impossible d'obtenir l'email de l'acheteur userId={}", exec.getBuyerUserId());
                            }
                            String buyerMsg = String.format("Exécution d'ordre ACHAT : %d %s @ %.2f. OrderId: %s. ExecutionReportId: %s. Date: %s.",
                                exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());
                            NotificationLogDTO buyerNotif = new NotificationLogDTO(
                                exec.getBuyerUserId(), buyerMsg, Instant.now(), "WEBSOCKET", buyerEmail);
                            notificationClient.sendNotification(buyerNotif);
                        }
                        // Notification seller
                        String sellerEmail = null;
                        if (exec.getSellerUserId() != null && exec.getSellerUserId() != 9999L) {
                            try {
                                UserDTO sellerInfo = userInfoClient.getUserInfo(exec.getSellerUserId());
                                sellerEmail = sellerInfo != null ? sellerInfo.getEmail() : null;
                            } catch (Exception e) {
                                logger.warn("Impossible d'obtenir l'email du vendeur userId={}", exec.getSellerUserId());
                            }
                            String sellerMsg = String.format("Exécution d'ordre VENTE : %d %s @ %.2f. OrderId: %s. ExecutionReportId: %s. Date: %s.",
                                exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());
                            NotificationLogDTO sellerNotif = new NotificationLogDTO(
                                exec.getSellerUserId(), sellerMsg, Instant.now(), "WEBSOCKET", sellerEmail);
                            notificationClient.sendNotification(sellerNotif);
                        }
                        deals.add(String.format("Deal: %d %s @ %.2f entre acheteur %d et vendeur %d", exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getBuyerUserId(), exec.getSellerUserId()));
                    } catch (Exception ex) {
                        logger.error("Erreur lors de la mise à jour du portefeuille ou de la notification pour le deal : {}", ex.getMessage(), ex);
                    }
                }
            }
            // Gestion des annulations
            String annulationInfo = "";
            if (matchingResult != null && matchingResult.updatedOrder != null) {
                OrderBookDTO ob = matchingResult.updatedOrder;
                if ("REJETE".equals(ob.getStatus()) || (ob.getQuantityRemaining() > 0 && ob.getQuantityRemaining() < savedOrder.getQuantity())) {
                    annulationInfo = ob.getRejectReason() != null ? ob.getRejectReason() : "Ordre partiellement ou totalement annulé.";
                }
            }
            // Construction du message pour le frontend
            StringBuilder message = new StringBuilder();
            if (matchingResult != null && matchingResult.updatedOrder != null) {
                OrderBookDTO ob = matchingResult.updatedOrder;
                int qtyInitial = savedOrder.getQuantity();
                int qtyFilled = qtyInitial - ob.getQuantityRemaining();
                int qtyCancelled = ob.getQuantityRemaining();
                String type = savedOrder.getDuration().name();
                if ("FOK".equalsIgnoreCase(type)) {
                    if (qtyFilled == 0) {
                        message.append("Ordre FOK annulé : la quantité totale n'était pas disponible, aucune exécution.");
                    } else {
                        message.append("Ordre FOK exécuté : ").append(qtyFilled).append(" fulfilled.");
                    }
                } else if ("IOC".equalsIgnoreCase(type)) {
                    if (qtyFilled == 0) {
                        message.append("Ordre IOC annulé : aucune exécution possible.");
                    } else {
                        message.append("Ordre IOC partiellement exécuté : ")
                               .append(qtyFilled).append(" fulfilled, ")
                               .append(qtyCancelled).append(" annulés.");
                    }
                } else if ("DAY".equalsIgnoreCase(type)) {
                    if (qtyFilled == 0) {
                        message.append("Ordre DAY ouvert : aucune exécution pour l'instant, reste ouvert 24h.");
                    } else if (qtyCancelled > 0) {
                        message.append("Ordre DAY partiellement exécuté : ")
                               .append(qtyFilled).append(" fulfilled, reste ouvert pour ")
                               .append(qtyCancelled).append(" jusqu'à expiration (24h).");
                    } else {
                        message.append("Ordre DAY entièrement exécuté : ").append(qtyFilled).append(" fulfilled.");
                    }
                } else {
                    message.append("Ordre placé avec succès.");
                }
                if (!deals.isEmpty()) {
                    message.append(" Exécutions: ").append(String.join(", ", deals));
                }
                if (ob.getRejectReason() != null && !ob.getRejectReason().isEmpty()) {
                    message.append(" Raison: ").append(ob.getRejectReason());
                }
            } else {
                message.append("Ordre placé avec succès.");
                if (!deals.isEmpty()) {
                    message.append(" Exécutions: ").append(String.join(", ", deals));
                }
                if (!annulationInfo.isEmpty()) {
                    message.append(" Annulation: ").append(annulationInfo);
                }
            }
            return OrderPlacementResult.success(savedOrder.getId(), message.toString());
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

    private boolean isOrderExpired(Order order) {
        if (order.getDuration() == Order.DurationType.DAY) {
            Instant expiration = order.getTimestamp().plusSeconds(24 * 3600);
            return Instant.now().isAfter(expiration);
        }
        return false;
    }
}
