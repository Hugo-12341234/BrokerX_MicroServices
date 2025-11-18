package com.microservices.log430.marketdataservice.adapters.web.controllers;

import com.microservices.log430.marketdataservice.adapters.external.matching.LastPriceDTO;
import com.microservices.log430.marketdataservice.adapters.external.matching.OrderBookDTO;
import com.microservices.log430.marketdataservice.adapters.web.dto.MarketDataUpdateDTO;
import com.microservices.log430.marketdataservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.marketdataservice.adapters.web.dto.SymbolsResponse;
import com.microservices.log430.marketdataservice.domain.port.in.MarketDataServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market-data")
public class MarketDataController {
    private final MarketDataServicePort marketDataService;
    private final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    public MarketDataController(MarketDataServicePort marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/symbols")
    public ResponseEntity<?> getAllSymbols() {
        try {
            logger.info("GET /symbols called");
            List<String> symbols = marketDataService.getAllSymbols();
            return ResponseEntity.ok(new SymbolsResponse(symbols));
        } catch (Exception e) {
            logger.error("Error in GET /symbols", e);
            ErrorResponse errorResponse = new ErrorResponse(
                java.time.Instant.now(),
                "/api/v1/market-data/symbols",
                500,
                "INTERNAL_ERROR",
                "Erreur lors de la récupération des symboles : " + e.getMessage(),
                null
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/send-market-data/{symbol}")
    public ResponseEntity<?> streamMarketData(@PathVariable String symbol, @RequestBody MarketDataUpdateDTO marketDataUpdate) {
        try {
            logger.info("POST /send-market-data/{} called", symbol);
            marketDataService.streamMarketData(symbol, marketDataUpdate);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error in POST /send-market-data/{}", symbol, e);
            ErrorResponse errorResponse = new ErrorResponse(
                java.time.Instant.now(),
                "/api/v1/market-data/send-market-data/" + symbol,
                500,
                "INTERNAL_ERROR",
                "Erreur lors de la transmission des données de marché : " + e.getMessage(),
                null
            );
            marketDataService.sendError(symbol, errorResponse);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/stock")
    public ResponseEntity<?> getStockBySymbol(@RequestParam("symbol") String symbol, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        var stockRuleOpt = marketDataService.getStockRuleBySymbol(symbol);
        if (stockRuleOpt.isEmpty()) {
            logger.warn("StockRule introuvable pour le symbole '{}'. Path: {}, RequestId: {}", symbol, path, requestId);
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                path,
                404,
                "Not Found",
                "StockRule introuvable pour ce symbole",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(404).body(err);
        }
        var stockRule = stockRuleOpt.get();
        logger.info("StockRule récupéré avec succès pour le symbole '{}'", symbol);
        return ResponseEntity.ok(stockRule);
    }

    @GetMapping("/orderbook")
    public ResponseEntity<?> getOrderBookBySymbol(@RequestParam("symbol") String symbol) {
        logger.info("GET /orderbook pour symbol: {}", symbol);
        try {
            var orderBookOpt = marketDataService.getOrderBookBySymbol(symbol);
            if (orderBookOpt.isEmpty()) {
                ErrorResponse err = new ErrorResponse(
                    java.time.Instant.now(),
                    "/api/v1/market-data/orderbook?symbol=" + symbol,
                    404,
                    "Not Found",
                    "OrderBook introuvable pour ce symbole",
                    null
                );
                return ResponseEntity.status(404).body(err);
            }
            return ResponseEntity.ok(orderBookOpt.get());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du orderbook pour symbol {}", symbol, e);
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                "/api/v1/market-data/orderbook?symbol=" + symbol,
                500,
                "Internal Server Error",
                "Erreur interne lors de la récupération du orderbook",
                null
            );
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping("/last-price")
    public ResponseEntity<?> getLastPriceBySymbol(@RequestParam("symbol") String symbol) {
        logger.info("GET /last-price pour symbol: {}", symbol);
        try {
            var lastPriceOpt = marketDataService.getLastPriceBySymbol(symbol);
            if (lastPriceOpt.isEmpty()) {
                ErrorResponse err = new ErrorResponse(
                    java.time.Instant.now(),
                    "/api/v1/market-data/last-price?symbol=" + symbol,
                    404,
                    "Not Found",
                    "Dernier prix introuvable pour ce symbole",
                    null
                );
                return ResponseEntity.status(404).body(err);
            }
            return ResponseEntity.ok(lastPriceOpt.get());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du dernier prix pour symbol {}", symbol, e);
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                "/api/v1/market-data/last-price?symbol=" + symbol,
                500,
                "Internal Server Error",
                "Erreur interne lors de la récupération du dernier prix",
                null
            );
            return ResponseEntity.status(500).body(err);
        }
    }
}
