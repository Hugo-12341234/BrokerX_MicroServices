package com.microservices.log430.walletservice.adapters.persistence.map;

import com.microservices.log430.walletservice.adapters.persistence.entities.StockPositionEntity;
import com.microservices.log430.walletservice.adapters.persistence.entities.WalletEntity;
import com.microservices.log430.walletservice.domain.model.entities.StockPosition;

public class StockPositionMapper {
    public static StockPosition toDomain(StockPositionEntity entity) {
        if (entity == null) return null;
        // Correction : on ne mappe pas le wallet pour éviter la récursivité infinie
        return new StockPosition(
            entity.getId(),
            null, // NE PAS remapper le wallet ici !
            entity.getSymbol(),
            entity.getQuantity(),
            entity.getAveragePrice(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    // Ajout d'un paramètre WalletEntity pour garantir le mapping correct
    public static StockPositionEntity toEntity(StockPosition domain, WalletEntity walletEntity) {
        if (domain == null) return null;
        StockPositionEntity entity = new StockPositionEntity();
        entity.setId(domain.getId());
        entity.setSymbol(domain.getSymbol());
        entity.setQuantity(domain.getQuantity());
        entity.setAveragePrice(domain.getAveragePrice());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setWallet(walletEntity); // mapping obligatoire
        return entity;
    }
}
