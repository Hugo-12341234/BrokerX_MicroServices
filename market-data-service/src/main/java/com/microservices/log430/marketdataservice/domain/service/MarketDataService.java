package com.microservices.log430.marketdataservice.domain.service;

import com.microservices.log430.marketdataservice.adapters.external.matching.LastPriceDTO;
import com.microservices.log430.marketdataservice.adapters.external.matching.MatchingClient;
import com.microservices.log430.marketdataservice.adapters.external.matching.OrderBookDTO;
import com.microservices.log430.marketdataservice.adapters.web.dto.MarketDataUpdateDTO;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MarketDataService implements MarketDataServicePort {
    private final StockRulePort stockRulePort;
    private final SimpMessagingTemplate messagingTemplate;
    private final MatchingClient matchingClient;
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    @Autowired
    public MarketDataService(StockRulePort stockRulePort, SimpMessagingTemplate messagingTemplate, MatchingClient matchingClient) {
        this.stockRulePort = stockRulePort;
        this.messagingTemplate = messagingTemplate;
        this.matchingClient = matchingClient;
    }

    @Override
    public List<String> getAllSymbols() {
        logger.info("Fetching all symbols");
        return stockRulePort.findAll().stream()
                .map(StockRule::getSymbol)
                .collect(Collectors.toList());
    }

    @Override
    public void streamMarketData(String symbol, MarketDataUpdateDTO marketDataUpdate) {
        logger.info("Streaming market data for symbol: {} | lastPrice: {}", symbol, marketDataUpdate.getLastPrice());
        var stockRuleOpt = stockRulePort.findBySymbol(symbol);
        if (stockRuleOpt.isEmpty()) {
            sendError(symbol, new ErrorResponse(java.time.Instant.now(), "NOT_FOUND", 404, "Symbol not found", symbol, null));
            return;
        }
        StockRule stockRule = stockRuleOpt.get();
        messagingTemplate.convertAndSend("/topic/market-data/" + symbol, marketDataUpdate);
    }

    @Override
    public void sendError(String symbol, ErrorResponse errorResponse) {
        logger.warn("Sending error for symbol {}: {}", symbol, errorResponse.getMessage());
        messagingTemplate.convertAndSend("/topic/market-data/" + symbol, errorResponse);
    }

    @Override
    public java.util.Optional<StockRule> getStockRuleBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        return stockRulePort.findBySymbol(symbol.trim());
    }

    @Override
    public Optional<OrderBookDTO> getOrderBookBySymbol(String symbol) {
        try {
            OrderBookDTO dto = matchingClient.getOrderBookBySymbol(symbol);
            return java.util.Optional.ofNullable(dto);
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel à matchingClient.getOrderBookBySymbol pour symbol {}", symbol, e);
            return java.util.Optional.empty();
        }
    }

    @Override
    public Optional<LastPriceDTO> getLastPriceBySymbol(String symbol) {
        try {
            LastPriceDTO dto = matchingClient.getLastPriceBySymbol(symbol);
            return java.util.Optional.ofNullable(dto);
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel à matchingClient.getLastPriceBySymbol pour symbol {}", symbol, e);
            return java.util.Optional.empty();
        }
    }
}
