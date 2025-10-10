package com.microservices.log430.walletservice.domain.service;

import com.microservices.log430.walletservice.domain.model.entities.StockRule;
import com.microservices.log430.walletservice.domain.port.in.StockPort;
import com.microservices.log430.walletservice.domain.port.out.StockRulePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StockService implements StockPort {

    private final StockRulePort stockRulePort;

    @Autowired
    public StockService(StockRulePort stockRulePort) {
        this.stockRulePort = stockRulePort;
    }

    @Override
    public Optional<StockRule> getStockRuleBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Optional.empty();
        }
        return stockRulePort.findBySymbol(symbol.trim());
    }
}
