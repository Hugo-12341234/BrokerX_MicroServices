package com.microservices.log430.walletservice.domain.port.out;

import com.microservices.log430.walletservice.domain.model.entities.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionPort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByUserId(Long userId);
    Optional<Transaction> findById(Long id);
}
