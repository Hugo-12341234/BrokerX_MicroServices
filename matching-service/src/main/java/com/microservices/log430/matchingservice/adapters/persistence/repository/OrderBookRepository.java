package com.microservices.log430.matchingservice.adapters.persistence.repository;

import com.microservices.log430.matchingservice.adapters.persistence.entities.OrderBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderBookRepository extends JpaRepository<OrderBookEntity, Long> {
    Optional<OrderBookEntity> findByClientOrderId(String clientOrderId);
    List<OrderBookEntity> findAllBySymbol(String symbol);
    List<OrderBookEntity> findAllBySymbolOrderByTimestampDesc(String symbol);
}

