package com.microservices.log430.marketdataservice.domain.port.out;

import com.microservices.log430.marketdataservice.domain.model.entities.StockRule;

import java.util.List;
import java.util.Optional;

public interface StockRulePort {
    Optional<StockRule> findBySymbol(String symbol);
    List<StockRule> findAll();
}
