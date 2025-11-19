package com.microservices.log430.orderservice.adapters.messaging.events;

import java.time.Instant;

/**
 * Événement reçu quand un ordre est rejeté
 * Correspond exactement au format envoyé par matching-service
 */
public class OrderRejectedEvent {

    private Long orderId;
    private String clientOrderId;
    private Long userId;
    private String symbol;
    private String side;
    private String type;
    private int quantity;
    private Double price;
    private String duration;
    private Instant timestamp;
    private String status;
    private String rejectReason;
    private Long version;

    public OrderRejectedEvent() {}

    public OrderRejectedEvent(Long orderId, String clientOrderId, Long userId, String symbol,
                             String side, String type, int quantity, Double price, String duration,
                             Instant timestamp, String status, String rejectReason, Long version) {
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.duration = duration;
        this.timestamp = timestamp;
        this.status = status;
        this.rejectReason = rejectReason;
        this.version = version;
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
