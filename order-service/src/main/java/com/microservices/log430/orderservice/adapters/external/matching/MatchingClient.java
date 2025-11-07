package com.microservices.log430.orderservice.adapters.external.matching;

import com.microservices.log430.orderservice.adapters.external.config.GlobalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.microservices.log430.orderservice.adapters.external.matching.dto.OrderDTO;
import com.microservices.log430.orderservice.adapters.external.matching.dto.MatchingResult;

@FeignClient(name = "matching-service", url = "${gateway.url:http://api-gateway:8079}", configuration = GlobalFeignConfig.class)
public interface MatchingClient {
    @PostMapping("/api/v1/orderbook")
    MatchingResult matchOrder(@RequestBody OrderDTO orderDTO);

    @PutMapping("/api/v1/orderbook/{orderId}")
    void modifyOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderDTO orderDTO);

    @DeleteMapping("/api/v1/orderbook/{orderId}")
    void cancelOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderDTO orderDTO);
}
