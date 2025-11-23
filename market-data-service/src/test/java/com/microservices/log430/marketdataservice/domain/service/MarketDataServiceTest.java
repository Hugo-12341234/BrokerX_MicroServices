package com.microservices.log430.marketdataservice.domain.service;

import com.microservices.log430.marketdataservice.adapters.external.matching.MatchingClient;
import com.microservices.log430.marketdataservice.adapters.external.matching.LastPriceDTO;
import com.microservices.log430.marketdataservice.adapters.external.matching.OrderBookDTO;
import com.microservices.log430.marketdataservice.domain.port.out.StockRulePort;
import com.microservices.log430.marketdataservice.domain.model.entities.StockRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private StockRulePort stockRulePort;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MatchingClient matchingClient;

    @InjectMocks
    private MarketDataService marketDataService;

    @Test
    void getAllSymbols_shouldReturnAllSymbols() {
        // Arrange
        List<StockRule> stockRules = Arrays.asList(
            new StockRule("AAPL", BigDecimal.valueOf(0.01), BigDecimal.valueOf(150.0),
                         BigDecimal.valueOf(200.0), BigDecimal.valueOf(175.0), LocalDateTime.now()),
            new StockRule("GOOGL", BigDecimal.valueOf(0.01), BigDecimal.valueOf(2000.0),
                         BigDecimal.valueOf(3000.0), BigDecimal.valueOf(2500.0), LocalDateTime.now()),
            new StockRule("TSLA", BigDecimal.valueOf(0.01), BigDecimal.valueOf(200.0),
                         BigDecimal.valueOf(300.0), BigDecimal.valueOf(250.0), LocalDateTime.now())
        );
        when(stockRulePort.findAll()).thenReturn(stockRules);

        // Act
        List<String> symbols = marketDataService.getAllSymbols();

        // Assert
        assertEquals(3, symbols.size());
        assertTrue(symbols.contains("AAPL"));
        assertTrue(symbols.contains("GOOGL"));
        assertTrue(symbols.contains("TSLA"));
        verify(stockRulePort).findAll();
    }

    @Test
    void getOrderBookBySymbol_shouldCallMatchingClient() {
        // Arrange
        String symbol = "AAPL";
        OrderBookDTO expectedOrderBook = new OrderBookDTO(symbol, java.util.Collections.emptyList());
        when(matchingClient.getOrderBookBySymbol(symbol)).thenReturn(expectedOrderBook);

        // Act
        java.util.Optional<OrderBookDTO> result = marketDataService.getOrderBookBySymbol(symbol);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedOrderBook, result.get());
        verify(matchingClient).getOrderBookBySymbol(symbol);
    }

    @Test
    void getLastPriceBySymbol_shouldCallMatchingClient() {
        // Arrange
        String symbol = "AAPL";
        LastPriceDTO expectedLastPrice = new LastPriceDTO(symbol, 150.0, "2025-01-01T10:00:00");
        when(matchingClient.getLastPriceBySymbol(symbol)).thenReturn(expectedLastPrice);

        // Act
        java.util.Optional<LastPriceDTO> result = marketDataService.getLastPriceBySymbol(symbol);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedLastPrice, result.get());
        verify(matchingClient).getLastPriceBySymbol(symbol);
    }
}
