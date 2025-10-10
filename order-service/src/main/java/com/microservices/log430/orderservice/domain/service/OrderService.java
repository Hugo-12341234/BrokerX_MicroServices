package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
import com.microservices.log430.orderservice.adapters.web.dto.OrderResponse;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.in.OrderPlacementPort;
import com.microservices.log430.orderservice.domain.port.in.PreTradeValidationPort;
import com.microservices.log430.orderservice.domain.port.out.OrderPort;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletResponse;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService implements OrderPlacementPort {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderPort orderPort;
    private final PreTradeValidationPort preTradeValidationPort;
    private final WalletClient walletClient;

    @Value("${gateway.url:http://localhost:8079}")
    private String gatewayUrl;

    @Autowired
    public OrderService(OrderPort orderPort, PreTradeValidationPort preTradeValidationPort, WalletClient walletClient) {
        this.orderPort = orderPort;
        this.preTradeValidationPort = preTradeValidationPort;
        this.walletClient = walletClient;
    }

    @Override
    public OrderPlacementResult placeOrder(OrderPlacementRequest request) {
        String clientOrderId = generateClientOrderId(request.getUserId(), request.getOrderRequest());
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
            order.setStatus(Order.OrderStatus.ACCEPTE);
            order.setRejectReason(null);
            Order savedOrder = orderPort.save(order);
            return OrderPlacementResult.success(savedOrder.getId(), "Ordre placé avec succès");
        }
    }

    // Méthode pour générer un clientOrderId unique basé sur les paramètres de l'ordre
    private String generateClientOrderId(Long userId, OrderRequest orderRequest) {
        // Génération déterministe basée sur les paramètres de l'ordre pour assurer l'idempotence
        // Si les mêmes paramètres sont envoyés, le même clientOrderId sera généré
        String orderData = String.format("%d-%s-%s-%s-%d-%s-%s",
                userId,
                orderRequest.getSymbol() != null ? orderRequest.getSymbol().toUpperCase() : "null",
                orderRequest.getSide(),
                orderRequest.getType(),
                orderRequest.getQuantity(),
                orderRequest.getPrice() != null ? String.format("%.4f", orderRequest.getPrice()) : "null",
                orderRequest.getDuration()
        );

        // Utiliser hashCode() pour générer un ID déterministe
        // Le même input donnera toujours le même clientOrderId (idempotence)
        int hash = orderData.hashCode();
        return String.format("ORD-%d-%08X", userId, Math.abs(hash));
    }

    // Méthode helper pour compatibilité avec le contrôleur existant
    public OrderResponse placeOrder(OrderRequest request, Long userId) {
        OrderPlacementRequest portRequest = new OrderPlacementRequest(request, userId);
        OrderPlacementPort.OrderPlacementResult result = placeOrder(portRequest);
        return result.toOrderResponse();
    }
}
