package com.microservices.log430.walletservice.adapters.persistence.map;

import com.microservices.log430.walletservice.adapters.persistence.entities.StockRuleEntity;
import com.microservices.log430.walletservice.domain.model.entities.StockRule;

public class StockRuleMapper {
    public static StockRule toDomain(StockRuleEntity entity) {
        if (entity == null) return null;
        return new StockRule(
            entity.getSymbol(),
            entity.getTickSize(),
            entity.getMinBand(),
            entity.getMaxBand(),
            entity.getPrice(),
            entity.getUpdatedAt()
        );
    }

    public static StockRuleEntity toEntity(StockRule domain) {
        if (domain == null) return null;
        StockRuleEntity entity = new StockRuleEntity();
        entity.setSymbol(domain.getSymbol());
        entity.setTickSize(domain.getTickSize());
        entity.setMinBand(domain.getMinBand());
        entity.setMaxBand(domain.getMaxBand());
        entity.setPrice(domain.getPrice());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
