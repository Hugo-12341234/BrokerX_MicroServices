package com.microservices.log430.marketdataservice.adapters.persistence.repository;

import com.microservices.log430.marketdataservice.adapters.persistence.entities.StockRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRuleRepository extends JpaRepository<StockRuleEntity, String> {
    Optional<StockRuleEntity> findBySymbol(String symbol);
}
