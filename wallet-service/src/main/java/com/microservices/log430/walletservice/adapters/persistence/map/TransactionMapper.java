package com.microservices.log430.walletservice.adapters.persistence.map;

import com.microservices.log430.walletservice.adapters.persistence.entities.TransactionEntity;
import com.microservices.log430.walletservice.domain.model.entities.Transaction;

public class TransactionMapper {

    public static Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;

        Transaction transaction = new Transaction();
        transaction.setId(entity.getId());
        transaction.setUserId(entity.getUserId());
        transaction.setIdempotencyKey(entity.getIdempotencyKey());
        transaction.setAmount(entity.getAmount());
        transaction.setType(Transaction.TransactionType.valueOf(entity.getType().name()));
        transaction.setStatus(Transaction.TransactionStatus.valueOf(entity.getStatus().name()));
        transaction.setDescription(entity.getDescription());
        transaction.setCreatedAt(entity.getCreatedAt());
        transaction.setUpdatedAt(entity.getUpdatedAt());

        return transaction;
    }

    public static TransactionEntity toEntity(Transaction transaction) {
        if (transaction == null) return null;

        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setUserId(transaction.getUserId());
        entity.setIdempotencyKey(transaction.getIdempotencyKey());
        entity.setAmount(transaction.getAmount());
        entity.setType(TransactionEntity.TransactionType.valueOf(transaction.getType().name()));
        entity.setStatus(TransactionEntity.TransactionStatus.valueOf(transaction.getStatus().name()));
        entity.setDescription(transaction.getDescription());
        entity.setCreatedAt(transaction.getCreatedAt());
        entity.setUpdatedAt(transaction.getUpdatedAt());

        return entity;
    }
}
