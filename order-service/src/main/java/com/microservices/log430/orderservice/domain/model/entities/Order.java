package com.microservices.log430.orderservice.domain.model.entities;

import java.time.Instant;

public class Order {
    private Long id;
    private String clientOrderId;
    private Long userId;
    private String symbol;
    private Side side;
    private OrderType type;
    private int quantity;
    private Double price;
    private DurationType duration;
    private Instant timestamp;
    private OrderStatus status;
    private String rejectReason;

    public enum Side {
        ACHAT, VENTE
    }
    public enum OrderType {
        MARCHE, LIMITE
    }
    public enum DurationType {
        DAY, IOC, FOK
    }
    public enum OrderStatus {
        ACCEPTE, REJETE, EN_ATTENTE
    }

    // Getters et setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getClientOrderId() {
        return clientOrderId;
    }
    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    public Side getSide() {
        return side;
    }
    public void setSide(Side side) {
        this.side = side;
    }
    public OrderType getType() {
        return type;
    }
    public void setType(OrderType type) {
        this.type = type;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
    public DurationType getDuration() {
        return duration;
    }
    public void setDuration(DurationType duration) {
        this.duration = duration;
    }
    public Instant getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    public OrderStatus getStatus() {
        return status;
    }
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public String getRejectReason() {
        return rejectReason;
    }
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}

