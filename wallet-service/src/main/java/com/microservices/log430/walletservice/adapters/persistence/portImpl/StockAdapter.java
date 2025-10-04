package com.microservices.log430.walletservice.adapters.persistence.portImpl;

import com.microservices.log430.walletservice.domain.model.entities.Stock;
import com.microservices.log430.walletservice.domain.service.StockService;
import org.springframework.stereotype.Service;

//TODO: This should be a @Component, not a Service
@Service
public class StockAdapter {
    private final StockService stockService;

    public StockAdapter() {
        this.stockService = new StockService();
    }

    public Stock getStockBySymbol(String symbol) {
        return stockService.getStockBySymbol(symbol);
    }

    public Stock getTestStock() {
        return stockService.getTestStock();
    }
}
