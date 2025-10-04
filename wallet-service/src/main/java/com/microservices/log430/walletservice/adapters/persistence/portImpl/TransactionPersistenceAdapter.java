package com.microservices.log430.walletservice.adapters.persistence.portImpl;

import com.microservices.log430.walletservice.adapters.persistence.entities.TransactionEntity;
import com.microservices.log430.walletservice.adapters.persistence.map.TransactionMapper;
import com.microservices.log430.walletservice.adapters.persistence.repository.SpringTransactionRepository;
import com.microservices.log430.walletservice.domain.model.entities.Transaction;
import com.microservices.log430.walletservice.domain.port.out.TransactionPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TransactionPersistenceAdapter implements TransactionPort {

    private final SpringTransactionRepository repository;

    public TransactionPersistenceAdapter(SpringTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = TransactionMapper.toEntity(transaction);
        TransactionEntity savedEntity = repository.save(entity);
        return TransactionMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(TransactionMapper::toDomain);
    }

    @Override
    public List<Transaction> findByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TransactionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return repository.findById(id)
                .map(TransactionMapper::toDomain);
    }
}
