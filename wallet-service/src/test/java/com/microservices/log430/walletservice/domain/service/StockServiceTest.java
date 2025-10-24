package com.microservices.log430.walletservice.domain.service;

import com.microservices.log430.walletservice.domain.model.entities.StockRule;
import com.microservices.log430.walletservice.domain.port.out.StockRulePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRulePort stockRulePort;

    @InjectMocks
    private StockService stockService;

    private StockRule mockStockRule;

    @BeforeEach
    void setUp() {
        mockStockRule = new StockRule();
        mockStockRule.setSymbol("AAPL");
        mockStockRule.setTickSize(BigDecimal.valueOf(0.01));
        mockStockRule.setMinBand(BigDecimal.valueOf(100));
        mockStockRule.setMaxBand(BigDecimal.valueOf(200));
    }

    @Test
    void getStockRuleBySymbol_HappyPath_ShouldReturnStockRule() {
        // Given
        when(stockRulePort.findBySymbol("AAPL")).thenReturn(Optional.of(mockStockRule));

        // When
        Optional<StockRule> result = stockService.getStockRuleBySymbol("AAPL");

        // Then
        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getSymbol());
        verify(stockRulePort).findBySymbol("AAPL");
    }

    @Test
    void getStockRuleBySymbol_NotFound_ShouldReturnEmpty() {
        // Given
        when(stockRulePort.findBySymbol("INVALID")).thenReturn(Optional.empty());

        // When
        Optional<StockRule> result = stockService.getStockRuleBySymbol("INVALID");

        // Then
        assertFalse(result.isPresent());
        verify(stockRulePort).findBySymbol("INVALID");
    }

    @Test
    void getStockRuleBySymbol_NullSymbol_ShouldReturnEmpty() {
        // When
        Optional<StockRule> result = stockService.getStockRuleBySymbol(null);

        // Then
        assertFalse(result.isPresent());
        verify(stockRulePort, never()).findBySymbol(any());
    }

    @Test
    void getStockRuleBySymbol_EmptySymbol_ShouldReturnEmpty() {
        // When
        Optional<StockRule> result = stockService.getStockRuleBySymbol("   ");

        // Then
        assertFalse(result.isPresent());
        verify(stockRulePort, never()).findBySymbol(any());
    }
}
