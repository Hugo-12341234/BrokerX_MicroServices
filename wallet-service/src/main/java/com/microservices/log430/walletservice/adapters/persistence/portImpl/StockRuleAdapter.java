package com.microservices.log430.walletservice.adapters.persistence.portImpl;

import com.microservices.log430.walletservice.adapters.persistence.repository.StockRuleRepository;
import com.microservices.log430.walletservice.domain.model.entities.StockRule;
import com.microservices.log430.walletservice.domain.port.out.StockRulePort;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.microservices.log430.walletservice.adapters.persistence.map.StockRuleMapper;
import java.util.Optional;

@Component
public class StockRuleAdapter implements StockRulePort {
    private final StockRuleRepository stockRuleRepository;

    @Autowired
    public StockRuleAdapter(StockRuleRepository stockRuleRepository) {
        this.stockRuleRepository = stockRuleRepository;
    }

    @Override
    public Optional<StockRule> findBySymbol(String symbol) {
        return stockRuleRepository.findBySymbol(symbol)
                .map(StockRuleMapper::toDomain);
    }
}
