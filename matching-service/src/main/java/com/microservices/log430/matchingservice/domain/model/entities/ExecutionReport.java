package com.microservices.log430.matchingservice.domain.model.entities;

import java.time.LocalDateTime;

public class ExecutionReport {
    private Long id;
    private Long orderId;
    private Integer fillQuantity;
    private Double fillPrice;
    private String fillType;
    private LocalDateTime executionTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getFillQuantity() { return fillQuantity; }
    public void setFillQuantity(Integer fillQuantity) { this.fillQuantity = fillQuantity; }

    public Double getFillPrice() { return fillPrice; }
    public void setFillPrice(Double fillPrice) { this.fillPrice = fillPrice; }

    public String getFillType() { return fillType; }
    public void setFillType(String fillType) { this.fillType = fillType; }

    public LocalDateTime getExecutionTime() { return executionTime; }
    public void setExecutionTime(LocalDateTime executionTime) { this.executionTime = executionTime; }
}