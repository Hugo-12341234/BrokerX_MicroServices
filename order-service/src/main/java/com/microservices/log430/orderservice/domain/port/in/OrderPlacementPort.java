package com.microservices.log430.orderservice.domain.port.in;

import com.microservices.log430.orderservice.adapters.messaging.events.OrderMatchedEvent;
import com.microservices.log430.orderservice.adapters.messaging.events.OrderRejectedEvent;
import com.microservices.log430.orderservice.adapters.web.dto.OrderRequest;
import com.microservices.log430.orderservice.adapters.web.dto.OrderResponse;
import com.microservices.log430.orderservice.domain.model.entities.Order;

import java.util.List;

public interface OrderPlacementPort {
    OrderPlacementResult placeOrder(OrderPlacementRequest request, String clientOrderId);
    OrderResponse modifyOrder(Long orderId, OrderRequest orderRequest, Long userId);
    OrderResponse cancelOrder(Long orderId);
    List<Order> findOrdersByUserId(Long userId);
    void processOrderMatched(OrderMatchedEvent event);
    void processOrderRejected(OrderRejectedEvent event);

    class OrderPlacementRequest {
        private final OrderRequest orderRequest;
        private final Long userId;

        public OrderPlacementRequest(OrderRequest orderRequest, Long userId) {
            this.orderRequest = orderRequest;
            this.userId = userId;
        }

        public OrderRequest getOrderRequest() { return orderRequest; }
        public Long getUserId() { return userId; }
    }

    class OrderPlacementResult {
        private final boolean success;
        private final Long orderId;
        private final String status;
        private final String message;

        public OrderPlacementResult(boolean success, Long orderId, String status, String message) {
            this.success = success;
            this.orderId = orderId;
            this.status = status;
            this.message = message;
        }

        public static OrderPlacementResult success(Long orderId, String message) {
            return new OrderPlacementResult(true, orderId, "ACCEPTE", message);
        }

        public static OrderPlacementResult failure(Long orderId, String message) {
            return new OrderPlacementResult(false, orderId, "REJETE", message);
        }

        public static OrderPlacementResult of(boolean success, Long orderId, String status, String message) {
            return new OrderPlacementResult(success, orderId, status, message);
        }

        public boolean isSuccess() { return success; }
        public Long getOrderId() { return orderId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }

        public OrderResponse toOrderResponse() {
            return new OrderResponse(orderId, status, message);
        }
    }
}
