package com.microservices.log430.walletservice.adapters.persistence.repository;

import com.microservices.log430.walletservice.adapters.persistence.entities.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringTransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
    List<TransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TransactionEntity t WHERE t.userId = ?1")
    void deleteByUserId(Long userId);
}
