package com.microservices.log430.orderservice.adapters.external.marketdata;

import com.microservices.log430.orderservice.adapters.external.config.GlobalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.microservices.log430.orderservice.adapters.external.marketdata.StockRule;

@FeignClient(name = "market-data-service", url = "${gateway.url:http://api-gateway:8079}", configuration = GlobalFeignConfig.class)
public interface MarketDataClient {
    @GetMapping("/api/v1/market-data/stock")
    StockRule getStockBySymbol(@RequestParam("symbol") String symbol);
}

