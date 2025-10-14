package com.microservices.log430.orderservice.adapters.external.matching;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.microservices.log430.orderservice.adapters.external.matching.dto.OrderDTO;
import com.microservices.log430.orderservice.adapters.external.matching.dto.MatchingResult;

@FeignClient(name = "matching-service", url = "${gateway.url:http://localhost:8079}", configuration = MatchingFeignConfig.class)
public interface MatchingClient {
    @PostMapping("/api/v1/orderbook")
    MatchingResult matchOrder(@RequestBody OrderDTO orderDTO);
}
