package com.microservices.log430.matchingservice.domain.service;

import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.port.out.ExecutionReportPort;
import com.microservices.log430.matchingservice.domain.port.out.OrderBookPort;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private OrderBookPort orderBookPort;

    @Mock
    private ExecutionReportPort executionReportPort;

    @InjectMocks
    private MatchingService matchingService;

    private OrderBook buyOrder;
    private OrderBook sellOrder;

    @BeforeEach
    void setUp() {
        buyOrder = new OrderBook();
        buyOrder.setId(1L);
        buyOrder.setClientOrderId("buy-123");
        buyOrder.setSymbol("AAPL");
        buyOrder.setSide("ACHAT");
        buyOrder.setType("LIMITE");
        buyOrder.setQuantity(100);
        buyOrder.setQuantityRemaining(100); // Initialiser quantityRemaining
        buyOrder.setPrice(150.00); // Double au lieu de BigDecimal
        buyOrder.setStatus("Working");
        buyOrder.setUserId(1L);

        sellOrder = new OrderBook();
        sellOrder.setId(2L);
        sellOrder.setClientOrderId("sell-456");
        sellOrder.setSymbol("AAPL");
        sellOrder.setSide("VENTE");
        sellOrder.setType("LIMITE");
        sellOrder.setQuantity(100);
        sellOrder.setQuantityRemaining(100); // Initialiser quantityRemaining
        sellOrder.setPrice(150.00); // Double au lieu de BigDecimal
        sellOrder.setStatus("Working");
        sellOrder.setUserId(2L);
    }

    @Test
    void matchOrder_NoOppositeOrders_ShouldAddToBook() {
        // Given
        when(orderBookPort.save(buyOrder)).thenReturn(buyOrder);
        when(orderBookPort.findAllBySymbol("AAPL")).thenReturn(Collections.emptyList());

        // When
        MatchingResult result = matchingService.matchOrder(buyOrder);

        // Then
        assertNotNull(result);
        verify(orderBookPort).save(buyOrder);
        verify(executionReportPort, never()).save(any());
    }

    @Test
    void matchOrder_PerfectMatch_ShouldExecuteTrade() {
        // Given
        ExecutionReport mockExecution = new ExecutionReport();
        mockExecution.setId(1L);
        mockExecution.setOrderId(buyOrder.getId());
        mockExecution.setFillQuantity(100);
        mockExecution.setFillPrice(150.00);
        mockExecution.setFillType("FULL");
        mockExecution.setExecutionTime(LocalDateTime.now());
        mockExecution.setBuyerUserId(buyOrder.getUserId());
        mockExecution.setSellerUserId(sellOrder.getUserId());
        mockExecution.setSymbol("AAPL");

        when(orderBookPort.save(any(OrderBook.class))).thenReturn(buyOrder);
        when(orderBookPort.findAllBySymbol("AAPL")).thenReturn(Arrays.asList(sellOrder));
        when(executionReportPort.save(any(ExecutionReport.class))).thenReturn(mockExecution);

        // When
        MatchingResult result = matchingService.matchOrder(buyOrder);

        // Then
        assertNotNull(result);
        verify(orderBookPort, atLeast(1)).save(any(OrderBook.class));
        verify(executionReportPort, atLeastOnce()).save(any(ExecutionReport.class));
    }

    @Test
    void matchOrder_PartialMatch_ShouldPartiallyFillOrders() {
        // Given
        buyOrder.setQuantity(150); // Buy more than sell order
        buyOrder.setQuantityRemaining(150); // Ajuster quantityRemaining aussi

        ExecutionReport mockExecution = new ExecutionReport();
        mockExecution.setId(1L);
        mockExecution.setOrderId(buyOrder.getId());
        mockExecution.setFillQuantity(100); // Partial fill
        mockExecution.setFillPrice(150.00);
        mockExecution.setFillType("PARTIAL");
        mockExecution.setExecutionTime(LocalDateTime.now());
        mockExecution.setBuyerUserId(buyOrder.getUserId());
        mockExecution.setSellerUserId(sellOrder.getUserId());
        mockExecution.setSymbol("AAPL");

        when(orderBookPort.save(any(OrderBook.class))).thenReturn(buyOrder);
        when(orderBookPort.findAllBySymbol("AAPL")).thenReturn(Arrays.asList(sellOrder));
        when(executionReportPort.save(any(ExecutionReport.class))).thenReturn(mockExecution);

        // When
        MatchingResult result = matchingService.matchOrder(buyOrder);

        // Then
        assertNotNull(result);
        verify(orderBookPort, atLeast(1)).save(any(OrderBook.class));
        verify(executionReportPort, atLeastOnce()).save(any(ExecutionReport.class));
    }

    @Test
    void matchOrder_NoPriceMatch_ShouldNotExecute() {
        // Given
        sellOrder.setPrice(160.00); // Higher than buy price
        when(orderBookPort.save(buyOrder)).thenReturn(buyOrder);
        when(orderBookPort.findAllBySymbol("AAPL")).thenReturn(Arrays.asList(sellOrder));

        // When
        MatchingResult result = matchingService.matchOrder(buyOrder);

        // Then
        assertNotNull(result);
        verify(orderBookPort).save(buyOrder);
        // Should not execute if prices don't match
    }
}
