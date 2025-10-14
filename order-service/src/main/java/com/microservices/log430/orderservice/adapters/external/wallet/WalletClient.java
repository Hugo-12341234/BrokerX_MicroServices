package com.microservices.log430.orderservice.adapters.external.wallet;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wallet-client", url = "${gateway.url:http://localhost:8079}", configuration = WalletFeignConfig.class)
public interface WalletClient {
    @GetMapping("/api/v1/wallet")
    WalletResponse getWallet(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/v1/wallet/stock")
    StockRule getStockBySymbol(@RequestParam("symbol") String symbol);

    @PostMapping("/api/v1/wallet/update")
    WalletResponse updateWallet(@RequestBody WalletUpdateRequest request);
}
