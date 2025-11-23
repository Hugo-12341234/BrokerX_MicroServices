package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.external.marketdata.StockRule;
import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataClient;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.in.PreTradeValidationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreTradeValidationServiceTest {

    @Mock
    private MarketDataClient marketDataClient;

    @InjectMocks
    private PreTradeValidationService preTradeValidationService;

    private Wallet mockWallet;
    private StockRule mockStockRule;
    private PreTradeValidationPort.ValidationRequest validationRequest;

    @BeforeEach
    void setUp() {
        mockWallet = new Wallet();
        mockWallet.setUserId(1L);
        mockWallet.setBalance(BigDecimal.valueOf(50000));

        mockStockRule = new StockRule();
        mockStockRule.setSymbol("AAPL");
        mockStockRule.setTickSize(BigDecimal.valueOf(0.01));
        mockStockRule.setMinBand(BigDecimal.valueOf(100));
        mockStockRule.setMaxBand(BigDecimal.valueOf(200));
        mockStockRule.setPrice(BigDecimal.valueOf(150.00)); // Prix actuel du stock

        validationRequest = new PreTradeValidationPort.ValidationRequest(
            "AAPL",
            Order.Side.ACHAT,
            Order.OrderType.LIMITE,
            100,
            150.00,
            mockWallet
        );
    }

    @Test
    void validateOrder_HappyPath_ShouldReturnValid() {
        // Given
        when(marketDataClient.getStockBySymbol("AAPL")).thenReturn(mockStockRule);

        // When
        PreTradeValidationPort.ValidationResult result = preTradeValidationService.validateOrder(validationRequest);

        // Then
        assertTrue(result.isValid());
        assertNull(result.getRejectReason());
    }

    @Test
    void validateOrder_InvalidSymbol_ShouldReturnInvalid() {
        // Given
        when(marketDataClient.getStockBySymbol("INVALID")).thenReturn(null);
        validationRequest = new PreTradeValidationPort.ValidationRequest(
            "INVALID", Order.Side.ACHAT, Order.OrderType.LIMITE, 100, 150.00, mockWallet
        );

        // When
        PreTradeValidationPort.ValidationResult result = preTradeValidationService.validateOrder(validationRequest);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getRejectReason().contains("Symbole invalide"));
    }

    @Test
    void validateOrder_PriceOutOfRange_ShouldReturnInvalid() {
        // Given
        when(marketDataClient.getStockBySymbol("AAPL")).thenReturn(mockStockRule);
        validationRequest = new PreTradeValidationPort.ValidationRequest(
            "AAPL", Order.Side.ACHAT, Order.OrderType.LIMITE, 100, 250.00, mockWallet
        );

        // When
        PreTradeValidationPort.ValidationResult result = preTradeValidationService.validateOrder(validationRequest);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getRejectReason().contains("bande"));
    }

    @Test
    void validateOrder_InvalidTickSize_ShouldReturnInvalid() {
        // Given
        when(marketDataClient.getStockBySymbol("AAPL")).thenReturn(mockStockRule);
        validationRequest = new PreTradeValidationPort.ValidationRequest(
            "AAPL", Order.Side.ACHAT, Order.OrderType.LIMITE, 100, 150.005, mockWallet
        );

        // When
        PreTradeValidationPort.ValidationResult result = preTradeValidationService.validateOrder(validationRequest);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getRejectReason().contains("tick size"));
    }
}
