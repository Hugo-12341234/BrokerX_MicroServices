package com.microservices.log430.marketdataservice.adapters.persistence.portImpl;

import com.microservices.log430.marketdataservice.adapters.persistence.map.StockRuleMapper;
import com.microservices.log430.marketdataservice.adapters.persistence.repository.StockRuleRepository;
import com.microservices.log430.marketdataservice.domain.model.entities.StockRule;
import com.microservices.log430.marketdataservice.domain.port.out.StockRulePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Override
    public List<StockRule> findAll() {
        return stockRuleRepository.findAll().stream()
                .map(StockRuleMapper::toDomain)
                .toList();
    }
}
