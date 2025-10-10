package com.microservices.log430.walletservice.domain.port.out;

import com.microservices.log430.walletservice.domain.model.entities.StockRule;
import java.util.Optional;

public interface StockRulePort {
    Optional<StockRule> findBySymbol(String symbol);
}

