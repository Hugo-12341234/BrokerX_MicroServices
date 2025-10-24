package com.microservices.log430.orderservice.adapters.web.controllers;

import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
import com.microservices.log430.orderservice.domain.port.in.OrderPlacementPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderPlacementPort orderPlacementPort;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        orderRequest = new OrderRequest();
        orderRequest.setSymbol("AAPL");
        orderRequest.setSide("ACHAT");
        orderRequest.setType("LIMITE");
        orderRequest.setQuantity(100);
        orderRequest.setPrice(150.00); // Double au lieu de BigDecimal
        orderRequest.setDuration("DAY");
    }

    @Test
    void placeOrder_HappyPath_ShouldReturn200() throws Exception {
        // Given
        OrderPlacementPort.OrderPlacementResult successResult =
            OrderPlacementPort.OrderPlacementResult.of(true, 1L, "ACCEPTE", "Ordre placé avec succès");
        when(orderPlacementPort.placeOrder(any(), anyString())).thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/place")
                .header("X-User-Id", "1")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACCEPTE"));

        verify(orderPlacementPort).placeOrder(any(), eq("test-key-123"));
    }

    @Test
    void placeOrder_MissingUserId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders/place")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header X-User-Id manquant"));

        verify(orderPlacementPort, never()).placeOrder(any(), any());
    }

    @Test
    void placeOrder_InvalidUserId_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders/place")
                .header("X-User-Id", "invalid")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header X-User-Id invalide"));

        verify(orderPlacementPort, never()).placeOrder(any(), any());
    }

    @Test
    void placeOrder_OrderRejected_ShouldReturn400() throws Exception {
        // Given
        OrderPlacementPort.OrderPlacementResult rejectedResult =
            OrderPlacementPort.OrderPlacementResult.failure(null, "Fonds insuffisants");
        when(orderPlacementPort.placeOrder(any(), anyString())).thenReturn(rejectedResult);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/place")
                .header("X-User-Id", "1")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fonds insuffisants"));
    }
}
