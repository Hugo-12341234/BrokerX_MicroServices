package com.microservices.log430.marketdataservice.domain.service;

import com.microservices.log430.marketdataservice.domain.port.in.MarketDataServicePort;
import com.microservices.log430.marketdataservice.domain.port.out.StockRulePort;
import com.microservices.log430.marketdataservice.domain.model.entities.StockRule;
import com.microservices.log430.marketdataservice.adapters.web.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketDataService implements MarketDataServicePort {
    private final StockRulePort stockRulePort;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    @Autowired
    public MarketDataService(StockRulePort stockRulePort, SimpMessagingTemplate messagingTemplate) {
        this.stockRulePort = stockRulePort;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<String> getAllSymbols() {
        logger.info("Fetching all symbols");
        return stockRulePort.findAll().stream()
                .map(StockRule::getSymbol)
                .collect(Collectors.toList());
    }

    @Override
    public void streamMarketData(String symbol) {
        logger.info("Sending market data for symbol: {}", symbol);
        var stockRuleOpt = stockRulePort.findBySymbol(symbol);
        if (stockRuleOpt.isEmpty()) {
            sendError(symbol, new ErrorResponse(java.time.Instant.now(), "NOT_FOUND", 404, "Symbol not found", symbol, null));
            return;
        }
        StockRule stockRule = stockRuleOpt.get();
        messagingTemplate.convertAndSend("/topic/market-data/" + symbol, stockRule);
    }

    @Override
    public void sendError(String symbol, ErrorResponse errorResponse) {
        logger.warn("Sending error for symbol {}: {}", symbol, errorResponse.getMessage());
        messagingTemplate.convertAndSend("/topic/market-data/" + symbol, errorResponse);
    }
}
