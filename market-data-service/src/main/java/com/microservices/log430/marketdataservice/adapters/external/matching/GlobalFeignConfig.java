package com.microservices.log430.marketdataservice.adapters.external.matching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalFeignConfig {
    @Bean
    public feign.RequestInterceptor requestInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Call", "true");
    }
}

