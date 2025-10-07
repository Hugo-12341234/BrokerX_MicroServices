package com.microservices.log430.walletservice.adapters.persistence.map;

import com.microservices.log430.walletservice.adapters.persistence.entities.StockPositionEntity;
import com.microservices.log430.walletservice.domain.model.entities.StockPosition;

public class StockPositionMapper {
    public static StockPosition toDomain(StockPositionEntity entity) {
        if (entity == null) return null;
        return new StockPosition(
            entity.getId(),
            null, // wallet à mapper séparément si besoin
            entity.getSymbol(),
            entity.getQuantity(),
            entity.getAveragePrice(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static StockPositionEntity toEntity(StockPosition domain) {
        if (domain == null) return null;
        StockPositionEntity entity = new StockPositionEntity();
        entity.setId(domain.getId());
        entity.setSymbol(domain.getSymbol());
        entity.setQuantity(domain.getQuantity());
        entity.setAveragePrice(domain.getAveragePrice());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        // wallet à mapper séparément si besoin
        return entity;
    }
}
