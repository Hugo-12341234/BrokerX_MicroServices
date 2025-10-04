package com.microservices.log430.orderservice.domain.port.out;

import com.microservices.log430.orderservice.domain.model.entities.Order;

import java.util.List;
import java.util.Optional;

public interface OrderPort {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByClientOrderId(String clientOrderId);
    List<Order> findByUserId(Long userId);
}
