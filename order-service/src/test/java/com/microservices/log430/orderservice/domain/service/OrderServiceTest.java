package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.in.OrderPlacementPort;
import com.microservices.log430.orderservice.domain.port.in.PreTradeValidationPort;
import com.microservices.log430.orderservice.domain.port.out.OrderPort;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletResponse;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import com.microservices.log430.orderservice.adapters.external.matching.MatchingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderPort orderPort;

    @Mock
    private PreTradeValidationPort preTradeValidationPort;

    @Mock
    private WalletClient walletClient;

    @Mock
    private MatchingClient matchingClient;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private OrderPlacementPort.OrderPlacementRequest placementRequest;
    private Wallet mockWallet;
    private WalletResponse mockWalletResponse;

    @BeforeEach
    void setUp() {
        orderRequest = new OrderRequest();
        orderRequest.setSymbol("AAPL");
        orderRequest.setSide("ACHAT");
        orderRequest.setType("LIMITE");
        orderRequest.setQuantity(100);
        orderRequest.setPrice(150.00); // Double au lieu de BigDecimal
        orderRequest.setDuration("DAY");

        placementRequest = new OrderPlacementPort.OrderPlacementRequest(orderRequest, 1L);

        mockWallet = new Wallet();
        mockWallet.setUserId(1L);
        mockWallet.setBalance(BigDecimal.valueOf(50000));

        mockWalletResponse = new WalletResponse();
        mockWalletResponse.setSuccess(true);
        mockWalletResponse.setWallet(mockWallet);
    }

    @Test
    void placeOrder_HappyPath_ShouldReturnSuccess() {
        // Given
        String clientOrderId = "test-order-123";
        when(orderPort.findByClientOrderId(clientOrderId)).thenReturn(Optional.empty());
        when(walletClient.getWallet(1L)).thenReturn(mockWalletResponse);

        PreTradeValidationPort.ValidationResult validationResult =
            new PreTradeValidationPort.ValidationResult(true, null);
        when(preTradeValidationPort.validateOrder(any())).thenReturn(validationResult);

        // Créer un Order complètement initialisé
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setClientOrderId(clientOrderId);
        savedOrder.setUserId(1L);
        savedOrder.setSymbol("AAPL");
        savedOrder.setSide(Order.Side.ACHAT); // Initialiser le side
        savedOrder.setType(Order.OrderType.LIMITE);
        savedOrder.setQuantity(100);
        savedOrder.setPrice(150.00);
        savedOrder.setDuration(Order.DurationType.DAY);
        savedOrder.setStatus(Order.OrderStatus.ACCEPTE);
        when(orderPort.save(any())).thenReturn(savedOrder);

        // When
        OrderPlacementPort.OrderPlacementResult result = orderService.placeOrder(placementRequest, clientOrderId);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("ACCEPTE", result.getStatus());
        verify(orderPort).save(any(Order.class));
    }

    @Test
    void placeOrder_WalletNotFound_ShouldReturnFailure() {
        // Given
        String clientOrderId = "test-order-123";
        when(orderPort.findByClientOrderId(clientOrderId)).thenReturn(Optional.empty());
        when(walletClient.getWallet(1L)).thenThrow(new RuntimeException("Wallet service unavailable"));

        // When
        OrderPlacementPort.OrderPlacementResult result = orderService.placeOrder(placementRequest, clientOrderId);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Portefeuille introuvable ou inaccessible", result.getMessage());
        verify(orderPort, never()).save(any());
    }

    @Test
    void placeOrder_DuplicateOrder_ShouldReturnExisting() {
        // Given
        String clientOrderId = "existing-order-123";
        Order existingOrder = new Order();
        existingOrder.setId(1L);
        existingOrder.setStatus(Order.OrderStatus.ACCEPTE);
        when(orderPort.findByClientOrderId(clientOrderId)).thenReturn(Optional.of(existingOrder));

        // When
        OrderPlacementPort.OrderPlacementResult result = orderService.placeOrder(placementRequest, clientOrderId);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("ACCEPTE", result.getStatus());
        verify(walletClient, never()).getWallet(anyLong());
    }
}
