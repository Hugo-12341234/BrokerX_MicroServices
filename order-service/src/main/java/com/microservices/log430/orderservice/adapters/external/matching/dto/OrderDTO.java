package com.microservices.log430.orderservice.adapters.external.matching.dto;

import java.time.Instant;

public class OrderDTO {

    public Long id;
    public String clientOrderId;
    public Long userId;
    public String symbol;
    public String side;
    public String type;
    public int quantity;
    public Double price;
    public String duration;
    public Instant timestamp;
    public String status;
    public String rejectReason;

    public OrderDTO() {}

    public OrderDTO(Long id, String clientOrderId, Long userId, String symbol, String side, String type, int quantity, Double price, String duration, Instant timestamp, String status, String rejectReason) {
        this.id = id;
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
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}