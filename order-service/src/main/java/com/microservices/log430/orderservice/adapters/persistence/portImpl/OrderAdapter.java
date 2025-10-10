package com.microservices.log430.orderservice.adapters.persistence.portImpl;

import com.microservices.log430.orderservice.adapters.persistence.entities.OrderEntity;
import com.microservices.log430.orderservice.adapters.persistence.map.OrderMapper;
import com.microservices.log430.orderservice.adapters.persistence.repository.OrderRepository;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.out.OrderPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderAdapter implements OrderPort {
    private final OrderRepository orderRepository;

    public OrderAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderMapper.toEntity(order);
        OrderEntity savedEntity = orderRepository.save(entity);
        return OrderMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id)
                .map(OrderMapper::toDomain);
    }

    @Override
    public Optional<Order> findByClientOrderId(String clientOrderId) {
        return orderRepository.findByClientOrderId(clientOrderId)
                .map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderMapper::toDomain)
                .collect(Collectors.toList());
    }
}
