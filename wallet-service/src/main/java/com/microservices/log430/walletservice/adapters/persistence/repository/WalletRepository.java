package com.microservices.log430.walletservice.adapters.persistence.repository;

import com.microservices.log430.walletservice.adapters.persistence.entities.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {
    WalletEntity findByUserId(Long userId);
}
