package com.microservices.log430.walletservice.domain.service;

import com.microservices.log430.walletservice.domain.model.entities.Stock;

import java.math.BigDecimal;

public class StockService {
    // Stock TEST avec tick size de 0.01 (1 cent) et bande de prix de 5%
    private static final Stock TEST_STOCK = new Stock(
            "TEST",
            "Stock de test",
            100.00,
            true,
            new BigDecimal("0.01"), // Tick size de 1 cent
            new BigDecimal("0.05")  // Bande de prix de 5% (95$ - 105$)
    );

    public Stock getStockBySymbol(String symbol) {
        if ("TEST".equalsIgnoreCase(symbol)) {
            return TEST_STOCK;
        }
        return null;
    }

    public Stock getTestStock() {
        return TEST_STOCK;
    }
}
