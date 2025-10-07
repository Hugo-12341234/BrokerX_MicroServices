package com.microservices.log430.walletservice.adapters.persistence.repository;

import com.microservices.log430.walletservice.adapters.persistence.entities.StockPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface StockPositionRepository extends JpaRepository<StockPositionEntity, UUID> {
    List<StockPositionEntity> findByWalletId(UUID walletId);
    List<StockPositionEntity> findBySymbol(String symbol);
}
