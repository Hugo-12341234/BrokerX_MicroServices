package com.microservices.log430.apigateway.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = com.microservices.log430.apigateway.ApiGatewayApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayE2ETest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealth_shouldReturnOk() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void nonExistentRoute_shouldReturnNotFound() {
        webTestClient.get()
                .uri("/invalid/path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void gatewayStartup_shouldBeSuccessful() {
        // Test that the application context loads successfully
        // This test validates that the gateway configuration is valid
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
