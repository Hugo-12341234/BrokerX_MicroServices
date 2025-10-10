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

@Service
public class OrderService implements OrderPlacementPort {
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
        // Générer un clientOrderId unique si pas fourni
        String clientOrderId = generateClientOrderId(request.getUserId(), request.getOrderRequest());

        // Vérification d'idempotence : chercher un ordre existant avec le même clientOrderId
        Optional<Order> existingOrder = orderPort.findByClientOrderId(clientOrderId);
        if (existingOrder.isPresent()) {
            // Retourner le résultat de l'ordre existant (idempotence)
            Order existing = existingOrder.get();
            return OrderPlacementResult.of(
                    existing.getStatus() == Order.OrderStatus.ACCEPTE,
                    existing.getId(),
                    existing.getStatus().name(),
                    existing.getStatus() == Order.OrderStatus.ACCEPTE ?
                            "Ordre placé avec succès" : existing.getRejectReason()
            );
        }

        // Appel au wallet-service via Feign pour récupérer le portefeuille
        WalletResponse walletResponse;
        try {
            walletResponse = walletClient.getWallet(request.getUserId());
        } catch (Exception ex) {
            return OrderPlacementResult.failure(null, "Portefeuille introuvable ou inaccessible");
        }
        if (walletResponse == null || !walletResponse.isSuccess() || walletResponse.getWallet() == null) {
            return OrderPlacementResult.failure(null, "Portefeuille introuvable");
        }
        Wallet wallet = walletResponse.getWallet();
        // Vérification de la balance pour les ordres d'achat
        OrderRequest orderRequest = request.getOrderRequest();

        // Effectuer les contrôles pré-trades
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
        order.setClientOrderId(clientOrderId); // Assigner le clientOrderId généré
        order.setUserId(request.getUserId());
        order.setSymbol(orderRequest.getSymbol());
        order.setSide(Order.Side.valueOf(orderRequest.getSide()));
        order.setType(Order.OrderType.valueOf(orderRequest.getType()));
        order.setQuantity(orderRequest.getQuantity());
        order.setPrice(orderRequest.getPrice());
        order.setDuration(Order.DurationType.valueOf(orderRequest.getDuration()));
        order.setTimestamp(Instant.now());

        if (!validation.isValid()) {
            // Ordre rejeté
            order.setStatus(Order.OrderStatus.REJETE);
            order.setRejectReason(validation.getRejectReason());
            Order savedOrder = orderPort.save(order);
            return OrderPlacementResult.failure(savedOrder.getId(), validation.getRejectReason());
        } else {
            // Ordre accepté
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
