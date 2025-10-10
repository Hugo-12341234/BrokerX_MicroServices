package com.microservices.log430.orderservice.adapters.persistence.map;

import com.microservices.log430.orderservice.adapters.persistence.entities.OrderEntity;
import com.microservices.log430.orderservice.domain.model.entities.Order;

public class OrderMapper {
    public static OrderEntity toEntity(Order order) {
        if (order == null) return null;
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setClientOrderId(order.getClientOrderId());
        entity.setUserId(order.getUserId());
        entity.setSymbol(order.getSymbol());
        entity.setSide(order.getSide() == null ? null : OrderEntity.Side.valueOf(order.getSide().name()));
        entity.setType(order.getType() == null ? null : OrderEntity.OrderType.valueOf(order.getType().name()));
        entity.setQuantity(order.getQuantity());
        entity.setPrice(order.getPrice());
        entity.setDuration(order.getDuration() == null ? null : OrderEntity.DurationType.valueOf(order.getDuration().name()));
        entity.setTimestamp(order.getTimestamp());
        entity.setStatus(order.getStatus() == null ? null : OrderEntity.OrderStatus.valueOf(order.getStatus().name()));
        entity.setRejectReason(order.getRejectReason());
        return entity;
    }

    public static Order toDomain(OrderEntity entity) {
        if (entity == null) return null;
        Order order = new Order();
        order.setId(entity.getId());
        order.setClientOrderId(entity.getClientOrderId());
        order.setUserId(entity.getUserId());
        order.setSymbol(entity.getSymbol());
        order.setSide(entity.getSide() == null ? null : Order.Side.valueOf(entity.getSide().name()));
        order.setType(entity.getType() == null ? null : Order.OrderType.valueOf(entity.getType().name()));
        order.setQuantity(entity.getQuantity());
        order.setPrice(entity.getPrice());
        order.setDuration(entity.getDuration() == null ? null : Order.DurationType.valueOf(entity.getDuration().name()));
        order.setTimestamp(entity.getTimestamp());
        order.setStatus(entity.getStatus() == null ? null : Order.OrderStatus.valueOf(entity.getStatus().name()));
        order.setRejectReason(entity.getRejectReason());
        return order;
    }
}
