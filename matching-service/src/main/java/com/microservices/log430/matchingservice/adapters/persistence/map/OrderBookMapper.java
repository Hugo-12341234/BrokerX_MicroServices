package com.microservices.log430.matchingservice.adapters.persistence.map;

import com.microservices.log430.matchingservice.adapters.persistence.entities.OrderBookEntity;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;

public class OrderBookMapper {
    public static OrderBook toDomain(OrderBookEntity entity) {
        if (entity == null) return null;
        OrderBook domain = new OrderBook();
        domain.setId(entity.getId());
        domain.setClientOrderId(entity.getClientOrderId());
        domain.setUserId(entity.getUserId());
        domain.setSymbol(entity.getSymbol());
        domain.setSide(entity.getSide());
        domain.setType(entity.getType());
        domain.setQuantity(entity.getQuantity());
        domain.setPrice(entity.getPrice());
        domain.setDuration(entity.getDuration());
        domain.setTimestamp(entity.getTimestamp());
        domain.setStatus(entity.getStatus());
        domain.setRejectReason(entity.getRejectReason());
        domain.setQuantityRemaining(entity.getQuantityRemaining());
        domain.setOrderId(entity.getOrderId());
        return domain;
    }

    public static OrderBookEntity toEntity(OrderBook domain) {
        if (domain == null) return null;
        OrderBookEntity entity = new OrderBookEntity();
        entity.setId(domain.getId());
        entity.setClientOrderId(domain.getClientOrderId());
        entity.setUserId(domain.getUserId());
        entity.setSymbol(domain.getSymbol());
        entity.setSide(domain.getSide());
        entity.setType(domain.getType());
        entity.setQuantity(domain.getQuantity());
        entity.setPrice(domain.getPrice());
        entity.setDuration(domain.getDuration());
        entity.setTimestamp(domain.getTimestamp());
        entity.setStatus(domain.getStatus());
        entity.setRejectReason(domain.getRejectReason());
        entity.setQuantityRemaining(domain.getQuantityRemaining());
        entity.setOrderId(domain.getOrderId());
        return entity;
    }
}