package com.microservices.log430.marketdataservice.domain.port.in;

import com.microservices.log430.marketdataservice.adapters.web.dto.ErrorResponse;

import java.util.List;

public interface MarketDataServicePort {
    List<String> getAllSymbols();
    void sendError(String symbol, ErrorResponse errorResponse);
    void streamMarketData(String symbol);
}
