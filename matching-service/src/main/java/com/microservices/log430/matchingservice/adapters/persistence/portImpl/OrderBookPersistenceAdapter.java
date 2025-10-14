package com.microservices.log430.matchingservice.adapters.persistence.portImpl;

import com.microservices.log430.matchingservice.adapters.persistence.entities.OrderBookEntity;
import com.microservices.log430.matchingservice.adapters.persistence.map.OrderBookMapper;
import com.microservices.log430.matchingservice.adapters.persistence.repository.OrderBookRepository;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.port.out.OrderBookPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderBookPersistenceAdapter implements OrderBookPort {
    private final OrderBookRepository repository;

    public OrderBookPersistenceAdapter(OrderBookRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderBook save(OrderBook orderBook) {
        OrderBookEntity entity = OrderBookMapper.toEntity(orderBook);
        OrderBookEntity saved = repository.save(entity);
        return OrderBookMapper.toDomain(saved);
    }

    @Override
    public Optional<OrderBook> findById(Long id) {
        return repository.findById(id).map(OrderBookMapper::toDomain);
    }

    @Override
    public Optional<OrderBook> findByClientOrderId(String clientOrderId) {
        return repository.findByClientOrderId(clientOrderId).map(OrderBookMapper::toDomain);
    }

    @Override
    public List<OrderBook> findAllBySymbol(String symbol) {
        return repository.findAllBySymbol(symbol).stream().map(OrderBookMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<OrderBook> findAll() {
        return repository.findAll().stream().map(OrderBookMapper::toDomain).collect(Collectors.toList());
    }
}