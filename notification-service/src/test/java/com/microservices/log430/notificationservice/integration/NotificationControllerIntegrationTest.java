package com.microservices.log430.notificationservice.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationControllerIntegrationTest {

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
    void sendNotification_shouldHandleValidRequest() throws Exception {
        String body = "{\"userId\":1,\"message\":\"Test notification\",\"channel\":\"WEBSOCKET\"}";
        mockMvc.perform(post("/api/v1/notification")
                .contentType("application/json")
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void sendNotification_shouldRejectInvalidRequest() throws Exception {
        String body = "{\"message\":\"Test notification\"}"; // missing userId
        mockMvc.perform(post("/api/v1/notification")
                .contentType("application/json")
                .content(body))
                .andExpect(status().is5xxServerError());
    }
}
