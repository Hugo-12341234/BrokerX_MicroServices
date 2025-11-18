package com.microservices.log430.marketdataservice.adapters.external.matching;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.microservices.log430.marketdataservice.adapters.external.matching.GlobalFeignConfig;
import com.microservices.log430.marketdataservice.adapters.external.matching.LastPriceDTO;
import com.microservices.log430.marketdataservice.adapters.external.matching.OrderBookDTO;

@FeignClient(name = "matching-service", url = "${gateway.url:http://api-gateway:8079}", configuration = GlobalFeignConfig.class)
public interface MatchingClient {
    @GetMapping("/api/v1/orderbook")
    OrderBookDTO getOrderBookBySymbol(@RequestParam("symbol") String symbol);

    @GetMapping("/api/v1/orderbook/last-price")
    LastPriceDTO getLastPriceBySymbol(@RequestParam("symbol") String symbol);
}
