package com.microservices.log430.walletservice.adapters.persistence.map;

import com.microservices.log430.walletservice.adapters.persistence.entities.WalletEntity;
import com.microservices.log430.walletservice.adapters.persistence.entities.StockPositionEntity;
import com.microservices.log430.walletservice.domain.model.entities.Wallet;
import com.microservices.log430.walletservice.domain.model.entities.StockPosition;
import java.util.List;
import java.util.stream.Collectors;

public class WalletMapper {
    public static Wallet toDomain(WalletEntity entity) {
        if (entity == null) return null;
        List<StockPosition> positions = null;
        if (entity.getStockPositions() != null) {
            positions = entity.getStockPositions().stream()
                .map(StockPositionMapper::toDomain)
                .collect(Collectors.toList());
        }
        return new Wallet(
            entity.getId(),
            entity.getUserId(),
            entity.getBalance(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            positions
        );
    }

    public static WalletEntity toEntity(Wallet domain) {
        if (domain == null) return null;
        WalletEntity entity = new WalletEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setBalance(domain.getBalance());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        if (domain.getStockPositions() != null) {
            List<StockPositionEntity> entities = domain.getStockPositions().stream()
                .map(StockPositionMapper::toEntity)
                .collect(Collectors.toList());
            entity.setStockPositions(entities);
        }
        return entity;
    }
}
