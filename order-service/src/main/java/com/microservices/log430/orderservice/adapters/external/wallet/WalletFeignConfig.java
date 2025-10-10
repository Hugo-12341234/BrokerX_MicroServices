package com.microservices.log430.orderservice.adapters.external.wallet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletFeignConfig {
    @Bean
    public feign.RequestInterceptor requestInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Call", "true");
    }
}

