package com.microservices.log430.marketdataservice.domain.port.in;

import com.microservices.log430.marketdataservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.marketdataservice.domain.model.entities.StockRule;

import java.util.List;
import java.util.Optional;

public interface MarketDataServicePort {
    List<String> getAllSymbols();
    void sendError(String symbol, ErrorResponse errorResponse);
    void streamMarketData(String symbol);
    Optional<StockRule> getStockRuleBySymbol(String symbol);
}
