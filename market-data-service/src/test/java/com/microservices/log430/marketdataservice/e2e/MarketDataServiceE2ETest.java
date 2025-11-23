package com.microservices.log430.marketdataservice.e2e;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest(classes = com.microservices.log430.marketdataservice.MarketDataServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class MarketDataServiceE2ETest {

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
    void getSymbols_shouldReturnOkStatus() throws Exception {
        mockMvc.perform(get("/api/v1/market-data/symbols"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderBook_shouldReturnOkForValidSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/market-data/order-book?symbol=AAPL"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getLastPrice_shouldReturnOkForValidSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/market-data/last-price?symbol=AAPL"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void healthCheck_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }
}
