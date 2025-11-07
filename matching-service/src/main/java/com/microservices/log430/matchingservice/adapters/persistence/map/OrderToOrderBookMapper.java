package com.microservices.log430.matchingservice.adapters.persistence.map;

import com.microservices.log430.matchingservice.adapters.web.dto.OrderDTO;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import java.time.LocalDateTime;

public class OrderToOrderBookMapper {
    public static OrderBook toOrderBook(OrderDTO order) {
        OrderBook ob = new OrderBook();
        ob.setId(order.id);
        ob.setClientOrderId(order.clientOrderId);
        ob.setUserId(order.userId);
        ob.setSymbol(order.symbol);
        ob.setSide(order.side);
        ob.setType(order.type);
        ob.setQuantity(order.quantity);
        ob.setPrice(order.price);
        ob.setDuration(order.duration);
        ob.setTimestamp(order.timestamp != null ? LocalDateTime.ofInstant(order.timestamp, java.time.ZoneId.systemDefault()) : null);
        ob.setStatus("Working"); // Initialisé à Working
        ob.setRejectReason(order.rejectReason);
        ob.setQuantityRemaining(order.quantity); // Initialisé à la quantité totale
        ob.setOrderId(order.id);
        ob.setVersion(order.version);
        return ob;
    }
}

