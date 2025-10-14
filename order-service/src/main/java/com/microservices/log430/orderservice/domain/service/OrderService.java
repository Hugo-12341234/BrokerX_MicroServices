package com.microservices.log430.orderservice.domain.service;

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

    @Autowired
    public OrderService(OrderPort orderPort, PreTradeValidationPort preTradeValidationPort, WalletClient walletClient, MatchingClient matchingClient) {
        this.orderPort = orderPort;
        this.preTradeValidationPort = preTradeValidationPort;
        this.walletClient = walletClient;
        this.matchingClient = matchingClient;
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
                savedOrder.getRejectReason()
            );
            MatchingResult matchingResult = null;
            try {
                matchingResult = matchingClient.matchOrder(orderDTO);
            } catch (Exception e) {
                logger.error("Erreur lors de l'appel au matching-service : {}", e.getMessage(), e);
                return OrderPlacementResult.success(savedOrder.getId(), "Ordre placé, mais matching non effectué : " + e.getMessage());
            }
            // Mise à jour du statut du Order selon le résultat du matching-service
            if (matchingResult != null && matchingResult.updatedOrder != null) {
                OrderBookDTO ob = matchingResult.updatedOrder;
                savedOrder.setStatus(Order.OrderStatus.fromString(ob.getStatus()));
                savedOrder.setRejectReason(ob.getRejectReason());
                // Si applicable, mettre à jour la quantité restante ou autres champs
                // savedOrder.setQuantityRemaining(ob.getQuantityRemaining()); // si champ présent dans Order
                orderPort.save(savedOrder);
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
                        deals.add(String.format("Deal: %d %s @ %.2f entre acheteur %d et vendeur %d", exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getBuyerUserId(), exec.getSellerUserId()));
                    } catch (Exception ex) {
                        logger.error("Erreur lors de la mise à jour du portefeuille pour le deal : {}", ex.getMessage(), ex);
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
            StringBuilder message = new StringBuilder("Ordre placé avec succès.");
            if (!deals.isEmpty()) {
                message.append(" Exécutions: ").append(String.join(", ", deals));
            }
            if (!annulationInfo.isEmpty()) {
                message.append(" Annulation: ").append(annulationInfo);
            }
            return OrderPlacementResult.success(savedOrder.getId(), message.toString());
        }
    }
}
