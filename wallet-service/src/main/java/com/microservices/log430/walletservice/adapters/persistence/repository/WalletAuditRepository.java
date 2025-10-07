package com.microservices.log430.walletservice.adapters.persistence.repository;

import com.microservices.log430.walletservice.adapters.persistence.entities.WalletAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletAuditRepository extends JpaRepository<WalletAuditEntity, Long> {
    List<WalletAuditEntity> findByUserId(Long userId);
    List<WalletAuditEntity> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
