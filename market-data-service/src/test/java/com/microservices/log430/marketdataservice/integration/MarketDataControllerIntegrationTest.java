package com.microservices.log430.marketdataservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MarketDataControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSymbols_shouldReturnValidResponse() throws Exception {
        mockMvc.perform(get("/api/v1/market-data/symbols"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderBook_shouldHandleValidSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/market-data/order-book?symbol=AAPL"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getLastPrice_shouldHandleValidSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/market-data/last-price?symbole=AAPL"))
                .andExpect(status().is4xxClientError());
    }
}
