package com.microservices.log430.walletservice.domain.port.in;

import java.util.Optional;
import com.microservices.log430.walletservice.domain.model.entities.StockRule;

public interface StockPort {
    Optional<StockRule> getStockRuleBySymbol(String symbol);
}
