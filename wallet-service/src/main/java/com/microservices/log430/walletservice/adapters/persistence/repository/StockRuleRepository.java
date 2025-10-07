package com.microservices.log430.walletservice.adapters.persistence.repository;

import com.microservices.log430.walletservice.adapters.persistence.entities.StockRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRuleRepository extends JpaRepository<StockRuleEntity, String> {
    // Méthodes personnalisées si besoin
}
