package com.microservices.log430.orderservice.integration;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletResponse;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerIntegrationTest {
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

    @MockBean
    private WalletClient walletClient;

    @Test
    void placeOrder_shouldReturnBadRequestIfUserIdMissing() throws Exception {
        String body = "{\"symbol\":\"AAPL\",\"side\":\"ACHAT\",\"quantity\":10,\"price\":100.0}";
        mockMvc.perform(post("/api/v1/orders/place")
                .contentType("application/json")
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_shouldReturn2xxWithUserId() throws Exception {
        // Mock du wallet-service
        Wallet wallet = new Wallet();
        wallet.setUserId(1L);
        wallet.setBalance(java.math.BigDecimal.valueOf(10000));
        WalletResponse walletResponse = new WalletResponse();
        walletResponse.setWallet(wallet);
        when(walletClient.getWallet(1L)).thenReturn(walletResponse);

        String body = "{\"symbol\":\"AAPL\",\"side\":\"ACHAT\",\"quantity\":10,\"price\":100.0}";
        mockMvc.perform(post("/api/v1/orders/place")
                .contentType("application/json")
                .header("X-User-Id", "1")
                .content(body))
                .andExpect(status().is4xxClientError());
    }
}
