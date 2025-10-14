package com.microservices.log430.matchingservice.adapters.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_report")
public class ExecutionReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "fill_quantity", nullable = false)
    private Integer fillQuantity;

    @Column(name = "fill_price", nullable = false)
    private Double fillPrice;

    @Column(name = "fill_type", nullable = false)
    private String fillType;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "buyer_user_id", nullable = false)
    private Long buyerUserId;

    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;

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

    public Long getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; }

    public Long getSellerUserId() { return sellerUserId; }
    public void setSellerUserId(Long sellerUserId) { this.sellerUserId = sellerUserId; }
}
